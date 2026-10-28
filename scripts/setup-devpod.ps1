# Configure the local DevPod CLI to use a modern openvscode-server build
# instead of DevPod's default (v1.84.2, bundled with an outdated Node.js
# runtime that crashes recent versions of the redhat.java extension).
#
# Run this ONCE per machine, before your first `devpod up` on this project.
# It only touches this machine's local DevPod config (%USERPROFILE%\.devpod\config.yaml),
# never anything inside the workspace/container itself.

$ErrorActionPreference = "Stop"

$OpenVscodeVersion = "v1.109.5"

if (-not (Get-Command devpod -ErrorAction SilentlyContinue)) {
    Write-Error "devpod CLI not found. Install it first: https://devpod.sh/docs/getting-started/install"
    exit 1
}

devpod ide use openvscode -o "VERSION=$OpenVscodeVersion"

Write-Host "DevPod will now install openvscode-server $OpenVscodeVersion for new/reset workspaces."
Write-Host "If a workspace is already running, reload it now: Ctrl+P > 'Developer: Reload Window'."
