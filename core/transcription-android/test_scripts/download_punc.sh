#!/usr/bin/env bash

set -euo pipefail

PUNCTUATION_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/punctuation-models/sherpa-onnx-online-punct-en-2024-08-06.tar.bz2"

TARGET_DIR="src/androidTest/assets/punctuation"
TEMP_DIR="$(mktemp -d -t punctuation_download_XXXXXX)"

ARCHIVE_NAME="sherpa-onnx-online-punct-en-2024-08-06.tar.bz2"
MODEL_DIR="sherpa-onnx-online-punct-en-2024-08-06"

cleanup() {
    echo "==> Cleaning up temporary workspace..."
    rm -rf "${TEMP_DIR}"
}
trap cleanup EXIT

mkdir -p "${TARGET_DIR}"

echo "==> Downloading online punctuation model..."

curl -fSL \
    --retry 3 \
    --retry-delay 2 \
    -o "${TEMP_DIR}/${ARCHIVE_NAME}" \
    "${PUNCTUATION_URL}"

echo "==> Extracting punctuation model..."

tar \
    -xjf "${TEMP_DIR}/${ARCHIVE_NAME}" \
    -C "${TEMP_DIR}"

echo "==> Deploying punctuation model..."

cp \
    "${TEMP_DIR}/${MODEL_DIR}/model.int8.onnx" \
    "${TARGET_DIR}/model.int8.onnx"

cp \
    "${TEMP_DIR}/${MODEL_DIR}/bpe.vocab" \
    "${TARGET_DIR}/bpe.vocab"

echo "==> Punctuation model deployed to ${TARGET_DIR}"
