#!/usr/bin/env bash

set -euo pipefail

ZIPFORMER_HF_URL="https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/resolve/main"
WHISPER_HF_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main"

TARGET_DIR="src/androidTest/assets"
TEMP_DIR="$(mktemp -d -t model_download_XXXXXX)"

# Trapped cleanup to ensure temporary files are deleted on exit
cleanup() {
  echo "==> Cleaning up temporary workspace..."
  rm -rf "${TEMP_DIR}"
}
trap cleanup EXIT

echo "==> Creating target subdirectories in ${TARGET_DIR}..."
mkdir -p "${TARGET_DIR}/zip_former"
mkdir -p "${TARGET_DIR}/whisper"
mkdir -p "${TARGET_DIR}/test_wavs"

echo "==> Downloading 8-bit quantized Zipformer STT model files..."
curl -L -o "${TEMP_DIR}/encoder.int8.onnx" "${ZIPFORMER_HF_URL}/encoder-epoch-99-avg-1.int8.onnx"
curl -L -o "${TEMP_DIR}/decoder.int8.onnx" "${ZIPFORMER_HF_URL}/decoder-epoch-99-avg-1.int8.onnx"
curl -L -o "${TEMP_DIR}/joiner.int8.onnx" "${ZIPFORMER_HF_URL}/joiner-epoch-99-avg-1.int8.onnx"
curl -L -o "${TEMP_DIR}/tokens.txt" "${ZIPFORMER_HF_URL}/tokens.txt"

echo "==> Downloading Whisper Tiny model file..."
curl -L -o "${TEMP_DIR}/ggml-tiny.bin" "${WHISPER_HF_URL}/ggml-tiny.bin"

echo "==> Downloading test WAV files..."
curl -L -o "${TARGET_DIR}/test_wavs/0.wav" "${ZIPFORMER_HF_URL}/test_wavs/0.wav"
curl -L -o "${TARGET_DIR}/test_wavs/1.wav" "${ZIPFORMER_HF_URL}/test_wavs/1.wav"

echo "==> Deploying Zipformer models into ${TARGET_DIR}/zip_former..."
cp "${TEMP_DIR}/encoder.int8.onnx" "${TARGET_DIR}/zip_former/encoder.onnx"
cp "${TEMP_DIR}/decoder.int8.onnx" "${TARGET_DIR}/zip_former/decoder.onnx"
cp "${TEMP_DIR}/joiner.int8.onnx" "${TARGET_DIR}/zip_former/joiner.onnx"
cp "${TEMP_DIR}/tokens.txt" "${TARGET_DIR}/zip_former/tokens.txt"

echo "==> Deploying Whisper Tiny model into ${TARGET_DIR}/whisper..."
cp "${TEMP_DIR}/ggml-tiny.bin" "${TARGET_DIR}/whisper/tiny.bin"

echo "==> Staging complete. File hierarchy:"
ls -lh "${TARGET_DIR}/zip_former"
ls -lh "${TARGET_DIR}/whisper"
ls -lh "${TARGET_DIR}/test_wavs"
