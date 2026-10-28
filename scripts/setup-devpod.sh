#!/usr/bin/env bash
# Configure the local DevPod CLI to use a modern openvscode-server build
# instead of DevPod's default (v1.84.2, bundled with an outdated Node.js
# runtime that crashes recent versions of the redhat.java extension).
#
# Run this ONCE per machine, before your first `devpod up` on this project.
# It only touches this machine's local DevPod config (~/.devpod/config.yaml),
# never anything inside the workspace/container itself.
set -euo pipefail

OPENVSCODE_VERSION="v1.109.5"

if ! command -v devpod >/dev/null 2>&1; then
    echo "devpod CLI not found. Install it first: https://devpod.sh/docs/getting-started/install" >&2
    exit 1
fi

devpod ide use openvscode -o "VERSION=${OPENVSCODE_VERSION}"

echo "DevPod will now install openvscode-server ${OPENVSCODE_VERSION} for new/reset workspaces."
echo "If a workspace is already running, reload it now: Ctrl+P (Cmd+P on macOS) > 'Developer: Reload Window'."
