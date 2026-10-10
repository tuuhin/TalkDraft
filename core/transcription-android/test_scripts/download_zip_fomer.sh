#!/usr/bin/env bash

set -euo pipefail

ZIPFORMER_HF_URL="https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26/resolve/main"

TARGET_DIR="../src/androidTest/assets/zip_former"
TEMP_DIR="$(mktemp -d -t zipformer_download_XXXXXX)"

cleanup() {
    echo "==> Cleaning up temporary workspace..."
    rm -rf "${TEMP_DIR}"
}
trap cleanup EXIT

mkdir -p "${TARGET_DIR}"

download_file() {
    local url="$1"
    local dest="$2"
    local name="$3"

    echo "    [START] ${name}"

    curl -fSL \
        --retry 3 \
        --retry-delay 2 \
        -o "${dest}" \
        "${url}"

    echo "    [DONE]  ${name}"
}

echo "==> Downloading Zipformer models..."

download_file \
    "${ZIPFORMER_HF_URL}/encoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx?download=true" \
    "${TEMP_DIR}/encoder.int8.onnx" \
    "Zipformer Encoder"

download_file \
    "${ZIPFORMER_HF_URL}/decoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx?download=true" \
    "${TEMP_DIR}/decoder.int8.onnx" \
    "Zipformer Decoder"

download_file \
    "${ZIPFORMER_HF_URL}/joiner-epoch-99-avg-1-chunk-16-left-128.int8.onnx?download=true" \
    "${TEMP_DIR}/joiner.int8.onnx" \
    "Zipformer Joiner"

download_file \
    "${ZIPFORMER_HF_URL}/tokens.txt?download=true" \
    "${TEMP_DIR}/tokens.txt" \
    "Zipformer Tokens"

echo "==> Deploying Zipformer models..."

cp "${TEMP_DIR}/encoder.int8.onnx" "${TARGET_DIR}/encoder.onnx"
cp "${TEMP_DIR}/decoder.int8.onnx" "${TARGET_DIR}/decoder.onnx"
cp "${TEMP_DIR}/joiner.int8.onnx" "${TARGET_DIR}/joiner.onnx"
cp "${TEMP_DIR}/tokens.txt" "${TARGET_DIR}/tokens.txt"

echo "==> Zipformer models deployed to ${TARGET_DIR}"
