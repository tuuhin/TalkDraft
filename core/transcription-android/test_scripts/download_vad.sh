#!/usr/bin/env bash

set -euo pipefail

SILERO_VAD_HF_URL="https://huggingface.co/csukuangfj/silero-vad/resolve/main"

TARGET_DIR="src/androidTest/assets/vad"
TEMP_DIR="$(mktemp -d -t vad_download_XXXXXX)"

cleanup() {
    echo "==> Cleaning up temporary workspace..."
    rm -rf "${TEMP_DIR}"
}
trap cleanup EXIT

mkdir -p "${TARGET_DIR}"

echo "==> Downloading Silero VAD model..."

curl -fSL \
    --retry 3 \
    --retry-delay 2 \
    -o "${TEMP_DIR}/silero_vad.onnx" \
    "${SILERO_VAD_HF_URL}/silero_vad.onnx?download=true"

echo "==> Deploying Silero VAD model..."

cp \
    "${TEMP_DIR}/silero_vad.onnx" \
    "${TARGET_DIR}/silero_vad.onnx"

echo "==> Silero VAD model deployed to ${TARGET_DIR}"
