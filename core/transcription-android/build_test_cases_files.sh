#!/usr/bin/env bash

set -euo pipefail

ZIPFORMER_HF_URL="https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26/resolve/main"
WHISPER_HF_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main"
SILERO_VAD_HF_URL="https://huggingface.co/csukuangfj/silero-vad/resolve/main"

TARGET_DIR="src/androidTest/assets"
TEST_WAVES_DIR="${TARGET_DIR}/test_wavs"
TEMP_DIR="$(mktemp -d -t model_download_XXXXXX)"

cleanup() {
  echo "==> Cleaning up temporary workspace..."
  rm -rf "${TEMP_DIR}"
}
trap cleanup EXIT

echo "==> Creating target subdirectories in ${TARGET_DIR}..."
mkdir -p "${TARGET_DIR}/zip_former"
mkdir -p "${TARGET_DIR}/whisper"
mkdir -p "${TARGET_DIR}/vad"
mkdir -p "${TEST_WAVES_DIR}"

echo "==> Downloading 8-bit quantized Zipformer STT model files..."
echo "    -> Downloading Zipformer Encoder..."
curl -L -s -o "${TEMP_DIR}/encoder.int8.onnx" "${ZIPFORMER_HF_URL}/encoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx?download=true"

echo "    -> Downloading Zipformer Decoder..."
curl -L -s -o "${TEMP_DIR}/decoder.int8.onnx" "${ZIPFORMER_HF_URL}/decoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx?download=true"

echo "    -> Downloading Zipformer Joiner..."
curl -L -s -o "${TEMP_DIR}/joiner.int8.onnx" "${ZIPFORMER_HF_URL}/joiner-epoch-99-avg-1-chunk-16-left-128.int8.onnx?download=true"

echo "    -> Downloading Zipformer Tokens Vocabulary..."
curl -L -s -o "${TEMP_DIR}/tokens.txt" "${ZIPFORMER_HF_URL}/tokens.txt?download=true"

echo "==> Downloading Whisper model file..."
echo "    -> Downloading Whisper Tiny GGML model..."
curl -L -s -o "${TEMP_DIR}/ggml-tiny.bin" "${WHISPER_HF_URL}/ggml-tiny.bin?download=true"

echo "==> Downloading Silero VAD model file..."
echo "    -> Downloading Silero VAD ONNX model..."
curl -L -s -o "${TEMP_DIR}/silero_vad.onnx" "${SILERO_VAD_HF_URL}/silero_vad.onnx?download=true"

echo "==> Deploying Zipformer models into ${TARGET_DIR}/zip_former..."
cp "${TEMP_DIR}/encoder.int8.onnx" "${TARGET_DIR}/zip_former/encoder.onnx"
cp "${TEMP_DIR}/decoder.int8.onnx" "${TARGET_DIR}/zip_former/decoder.onnx"
cp "${TEMP_DIR}/joiner.int8.onnx" "${TARGET_DIR}/zip_former/joiner.onnx"
cp "${TEMP_DIR}/tokens.txt" "${TARGET_DIR}/zip_former/tokens.txt"

echo "==> Deploying Whisper Tiny model into ${TARGET_DIR}/whisper..."
cp "${TEMP_DIR}/ggml-tiny.bin" "${TARGET_DIR}/whisper/tiny.bin"

echo "==> Deploying VAD model into ${TARGET_DIR}/vad..."
cp "${TEMP_DIR}/silero_vad.onnx" "${TARGET_DIR}/vad/silero_vad.onnx"

MEETING_ID="IS1009a"
echo "==> Downloading AMI meeting '${MEETING_ID}' audio (~31 min) from official mirror..."

AMI_DIRECT_URL="https://groups.inf.ed.ac.uk/ami/AMICorpusMirror/amicorpus/HeadsetAudio/${MEETING_ID}.Mix-Headset.wav"
OUTPUT_WAV="${TEST_WAVES_DIR}/${MEETING_ID}_combined.wav"

curl -L -f --progress-bar -o "${OUTPUT_WAV}" "${AMI_DIRECT_URL}"

# Check header signature for 'RIFF' (valid audio file)
HEADER=$(head -c 4 "${OUTPUT_WAV}" 2>/dev/null || echo "FAIL")
if [ "${HEADER}" = "RIFF" ]; then
  echo "==> SUCCESS: Valid WAV file deployed to ${OUTPUT_WAV} ($(du -h "${OUTPUT_WAV}" | cut -f1))"
else
  echo "==> ERROR: Downloaded file from ${AMI_DIRECT_URL} is not a valid RIFF/WAV audio file." >&2
  exit 1
fi

echo "==> Staging complete. File hierarchy in ${TARGET_DIR}:"
