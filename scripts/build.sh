#!/usr/bin/env bash
#
# Build the Focus Mode app inside the Podman container.
#
#   scripts/build.sh                         # builds the debug APK
#   scripts/build.sh assembleRelease         # any gradle task
#   scripts/build.sh --rebuild assembleDebug # force rebuild of the image
#
# The container runs as the invoking host user (via -u) and keeps its Gradle
# cache inside the project so file ownership stays correct on the host.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
IMAGE="focus-mode-builder"
CONTAINERFILE="${PROJECT_DIR}/Containerfile"
GRADLE_HOME_IN_CONTAINER="/workspace/.gradle-container"

REBUILD=0
if [[ "${1:-}" == "--rebuild" ]]; then
    REBUILD=1
    shift
fi

if (( REBUILD )) || ! podman image exists "${IMAGE}"; then
    echo ">> Building container image '${IMAGE}'..."
    podman build -t "${IMAGE}" -f "${CONTAINERFILE}" "${PROJECT_DIR}"
fi

GRADLE_ARGS=("$@")
if [[ ${#GRADLE_ARGS[@]} -eq 0 ]]; then
    GRADLE_ARGS=(assembleDebug)
fi

exec podman run --rm \
    --userns=keep-id \
    -v "${PROJECT_DIR}":/workspace:Z \
    -w /workspace \
    -e HOME=/tmp \
    -e GRADLE_USER_HOME="${GRADLE_HOME_IN_CONTAINER}" \
    "${IMAGE}" \
    gradle --no-daemon "${GRADLE_ARGS[@]}"
