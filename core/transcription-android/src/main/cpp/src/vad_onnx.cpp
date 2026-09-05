#include "vad_onnx.h"

#include <algorithm>
using namespace Ort;

silero_vad::silero_vad(const std::string &model_path, int sample_rate, float threshold)
    : _sample_rate(sample_rate), _threshold(threshold) {
  init_session();
  _session = std::make_unique<Session>(env_, model_path.c_str(), _session_options);
}

silero_vad::silero_vad(const void *model_data, size_t model_size, int sample_rate, float threshold)
    : _sample_rate(sample_rate), _threshold(threshold) {
  init_session();
  _session = std::make_unique<Session>(env_, model_data, model_size, _session_options);
}

void silero_vad::init_session() {
  _session_options.SetIntraOpNumThreads(1);
  _session_options.SetInterOpNumThreads(1);
  _session_options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
}

float silero_vad::process_frame(const float *pcm_floats, size_t sample_count) {
  // 1. Input Tensor: Audio PCM Samples [1, sample_count]
  std::vector<int64_t> input_shape = {1, static_cast<int64_t>(sample_count)};
  Value input_tensor = Value::CreateTensor<float>(
      memory_info_, const_cast<float *>(pcm_floats), sample_count,
      input_shape.data(), input_shape.size());

  // 2. State Tensor: Recurrent LSTM States [2, 1, 128]
  std::vector<int64_t> state_shape = {2, 1, 128};
  Value state_tensor = Ort::Value::CreateTensor<float>(
      memory_info_, _state.data(), _state.size(),
      state_shape.data(), state_shape.size());

  // 3. Sample Rate Tensor: scalar/array [1]
  std::vector<int64_t> sr_shape = {1};
  Value sr_tensor = Ort::Value::CreateTensor<int64_t>(
      memory_info_, &_sample_rate, 1,
      sr_shape.data(), sr_shape.size());

  Value inputs[] = {
      std::move(input_tensor),
      std::move(state_tensor),
      std::move(sr_tensor)
  };

  // Run Model Inference
  auto outputs = _session->Run(
      Ort::RunOptions{nullptr},
      input_node_names, inputs, 3,
      output_node_names, 2);

  // Extract speech probability output [0]
  float probability = outputs[0].GetTensorData<float>()[0];

  // Update recurrent state buffer [1] for subsequent frame
  const auto *updated_state = outputs[1].GetTensorData<float>();
  std::copy(updated_state, updated_state + STATE_SIZE, _state.begin());

  return probability;
}

void silero_vad::reset_states() {
  std::fill(_state.begin(), _state.end(), 0.0f);
}
