#ifndef VAD_ONNX_H
#define VAD_ONNX_H

#include <vector>
#include <string>
#include <memory>
#include <cstdint>
#include "onnxruntime_cxx_api.h"
using namespace Ort;

class silero_vad {
public:

  silero_vad(const std::string &model_path, int sample_rate = 16000, float threshold = 0.5f);

  explicit silero_vad(const void *model_data, size_t model_size, int sample_rate = 16000, float threshold = 0.5f);

  ~silero_vad() = default;

  // Prevent copy semantics to avoid double-freeing ONNX resources
  silero_vad(const silero_vad &) = delete;
  silero_vad &operator=(const silero_vad &) = delete;

  float process_frame(const float *pcm_floats, size_t sample_count);
  void reset_states();

private:
  void init_session();

  // ONNX Runtime Core Objects
  Env env_{ORT_LOGGING_LEVEL_WARNING, "SILERO-VAD"};
  SessionOptions _session_options;
  std::unique_ptr<Session> _session;
  MemoryInfo memory_info_ = Ort::MemoryInfo::CreateCpu(
      OrtAllocatorType::OrtDeviceAllocator,
      OrtMemType::OrtMemTypeDefault);

  // Model parameters
  int64_t _sample_rate;
  float _threshold;

  // Silero VAD v5 internal state tensor shape: [2, 1, 128]
  // (If using Silero VAD v4, tensor shape is [2, 1, 64] divided into 'h' and 'c')
  static constexpr size_t STATE_SIZE = 2 * 1 * 128;
  std::vector<float> _state = std::vector<float>(STATE_SIZE, 0.0f);

  // Node Names
  const char *input_node_names[3] = {"input", "state", "sr"};
  const char *output_node_names[2] = {"output", "stateN"};
};

#endif // VAD_ONNX_H
