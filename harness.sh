#!/usr/bin/env bash
# Harnais CUPID : vérifications structurelles déterministes (même code en entrée, même verdict en sortie).
#
# Usage : ./harness.sh [étape] [--json rapport.json]   (étape : 1 par défaut ; 1 ou all)
#         ./harness.sh --test         (lance les tests du harnais lui-même)
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
    exec "${GRADLE[@]}" :harness:test
fi

STEP="${1:-1}"
[ $# -gt 0 ] && shift

echo "== Compilation =="
"${GRADLE[@]}" classes :harness:installDist -q

echo "== Harnais CUPID, étape $STEP =="
# Lancé directement (pas via 'gradle run') pour rendre fidèlement le code de sortie 0 / 1 / 2.
harness/build/install/harness/bin/harness --step "$STEP" ${@+"$@"} \
    --classes build/classes/java/main src/main/java 2> >(grep -v '^SLF4J' >&2)
