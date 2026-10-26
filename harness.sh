#!/usr/bin/env bash
# Harnais CUPID : vérifications structurelles déterministes (même code en entrée, même verdict en sortie).
#
# Usage : ./harness.sh [--clean] [étape] [--json rapport.json]   (étape : 1 par défaut ; 1 ou all)
#         ./harness.sh --test         (lance les tests du harnais lui-même : réservé à l'animateur)
#
# --clean : recompile tout depuis zéro (gradle clean), pour écarter tout doute sur un cache.
#
# Le harnais analyse le code du dossier où se trouve ce script, tel qu'il est sur le disque (fichiers non commités
# compris, fichiers non sauvegardés dans l'IDE exclus). L'en-tête affiche le dossier, la branche et le commit analysés.
#
# Compile le projet, puis lance le harnais (tree-sitter sur les sources, ArchUnit sur le bytecode).
# Code de sortie : 0 si tout passe, 1 si une règle échoue, 2 si usage incorrect ou code non compilé.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

# Le wrapper n'est utilisable que si son jar est présent ; sinon, Gradle installé sur la machine.
if [ -f gradle/wrapper/gradle-wrapper.jar ]; then
    GRADLE=(./gradlew)
else
    GRADLE=(gradle)
fi

if [ "${1:-}" = "--test" ]; then
    exec "${GRADLE[@]}" :harness:test -PharnessTests
fi

CLEAN=()
if [ "${1:-}" = "--clean" ]; then
    CLEAN=(clean)
    shift
fi

STEP="${1:-1}"
[ $# -gt 0 ] && shift

echo "== Code analysé =="
echo "Dossier : $PWD"
if git rev-parse --git-dir > /dev/null 2>&1; then
    echo "Branche : $(git branch --show-current 2>/dev/null || true) @ $(git log -1 --format='%h %s' 2>/dev/null || true)"
    CHANGED=$(git status --porcelain -- src/main/java | wc -l | tr -d ' ')
    [ "$CHANGED" != "0" ] && echo "Non commités dans src/main/java : $CHANGED fichier(s), analysés tels quels"
fi

echo "== Compilation =="
"${GRADLE[@]}" ${CLEAN[@]+"${CLEAN[@]}"} classes :harness:installDist -q

echo "== Harnais CUPID, étape $STEP =="
# Lancé directement (pas via 'gradle run') pour rendre fidèlement le code de sortie 0 / 1 / 2.
harness/build/install/harness/bin/harness --step "$STEP" ${@+"$@"} \
    --classes build/classes/java/main src/main/java 2> >(grep -v '^SLF4J' >&2)
