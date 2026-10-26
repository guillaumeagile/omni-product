#!/usr/bin/env bash
# Lance harness.sh dans le conteneur omni-harness (voir Dockerfile) sur le dépôt courant : même code, même verdict
# que le check harness-game, sans GitHub. Les arguments sont ceux de harness.sh, relatifs à la racine du dépôt.
#
# Usage : ./harness-docker.sh [--clean] [étape] [--json rapport.json]   (étape : 1 par défaut ; 1, 2, 3 ou all)
#         ./harness-docker.sh --build      (reconstruit l'image après une modification du Dockerfile)
#
# Le cache Gradle est gardé dans le volume Docker omni-harness-gradle : la première exécution télécharge tout,
# les suivantes sont rapides.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

IMAGE=omni-harness
CACHE=omni-harness-gradle

if [ "${1:-}" = "--build" ] || ! docker image inspect "$IMAGE" > /dev/null 2>&1; then
    docker build -t "$IMAGE" .
    [ "${1:-}" = "--build" ] && exit 0
fi

# Le dépôt est monté en lecture-écriture : harness.sh écrit ses rapports JSON dans le dossier courant.
exec docker run --rm \
    -v "$PWD":/work \
    -v "$CACHE":/root/.gradle \
    "$IMAGE" "$@"
