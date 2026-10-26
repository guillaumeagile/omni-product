#!/usr/bin/env bash
# Mutation du harnais : la preuve qu'une règle voit le défaut qu'elle vise, rejouée à chaque PR d'animateur.
#
# Une règle verte sur le legacy (parce que le défaut n'y est pas encore, comme HA15 : aucune fabrique) n'est prouvée
# que par une mutation : on introduit le défaut dans le code, on lance le harnais, on vérifie qu'il le signale au bon
# fichier et à la bonne ligne, puis on restaure le code. Chaque mutation est un dossier harness/mutations/<nom>/ :
#   - les fichiers mutants, au même chemin que dans le dépôt (src/main/java/com/omniproduct/model/Slug.java) ;
#     ils doivent compiler, le harnais compile avant d'analyser ;
#   - expected.txt : une ligne « <règle> <fichier>:<ligne> » par détection attendue (# pour un commentaire).
#
# Usage : harness/mutate.sh [nom de mutation...]     (toutes par défaut)
# Code de sortie : 0 si chaque détection attendue est vue, 1 sinon. Les fichiers sont toujours restaurés, même en cas
# d'échec ou d'interruption ; le script refuse de partir d'un src/main/java modifié, pour ne jamais confondre une
# mutation et un travail en cours.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
MUTATIONS=harness/mutations
BACKUP=$(mktemp -d)
MUTATED=()

if [ -n "$(git status --porcelain -- src/main/java)" ]; then
    echo "src/main/java contient des modifications non commitées : commitez-les ou rangez-les avant une mutation." >&2
    exit 2
fi

restore() {
    for file in ${MUTATED[@]+"${MUTATED[@]}"}; do
        if [ -f "$BACKUP/$file" ]; then
            cp "$BACKUP/$file" "$file"
        else
            rm -f "$file"
        fi
    done
    MUTATED=()
}
trap 'restore; rm -rf "$BACKUP"' EXIT

mutate() {
    local dir=$1
    while IFS= read -r mutant; do
        local file=${mutant#"$dir/"}
        if [ -f "$file" ]; then
            mkdir -p "$BACKUP/$(dirname "$file")"
            cp "$file" "$BACKUP/$file"
        fi
        mkdir -p "$(dirname "$file")"
        cp "$mutant" "$file"
        MUTATED+=("$file")
    done < <(find "$dir/src" -type f -name '*.java')
}

# Les lignes signalées par UNE règle : celles qui suivent son « [FAIL] <règle> » jusqu'à la règle suivante.
# Sans cette découpe, une ligne signalée par HA15 validerait aussi une attente sur HA14, rouge ailleurs.
findings_of() {
    awk -v rule="$1" '/^\[(PASS|FAIL|WARN)\]/ { inside = ($1 == "[FAIL]" && $2 == rule); next } inside' "$2"
}

check() {
    local dir=$1 report=$2 failed=0
    while read -r rule location; do
        [ -z "$rule" ] && continue
        case "$rule" in \#*) continue ;; esac
        if findings_of "$rule" "$report" | grep -qE "^[[:space:]]+$location[[:space:]]"; then
            echo "  [SEEN] $rule $location"
        else
            echo "  [MISSED] $rule $location"
            failed=1
        fi
    done < "$dir/expected.txt"
    return $failed
}

names=("$@")
if [ ${#names[@]} -eq 0 ]; then
    while IFS= read -r dir; do names+=("$(basename "$dir")"); done < <(find "$MUTATIONS" -mindepth 1 -maxdepth 1 -type d | sort)
fi

status=0
for name in "${names[@]}"; do
    dir="$MUTATIONS/$name"
    echo "== Mutation $name =="
    mutate "$dir"
    report="$BACKUP/$name.txt"
    ./harness.sh all > "$report" 2>&1 || true
    if ! grep -q 'HARNESS RESULT' "$report"; then
        echo "  le harnais n'a pas rendu de verdict (le mutant compile-t-il ?) :" && sed 's/^/  | /' "$report" | tail -20
        status=1
    elif ! check "$dir" "$report"; then
        status=1
        grep -E '^\[(FAIL|WARN)\]|^[[:space:]]+src/' "$report" | sed 's/^/  | /'
    fi
    restore
done

if [ "$status" -eq 0 ]; then
    echo "MUTATIONS: PASS (chaque défaut introduit a été vu)"
else
    echo "MUTATIONS: FAIL (un défaut introduit n'a pas été vu)"
fi
exit $status
