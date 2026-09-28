#pragma once

#include "sherpa-onnx/c-api/c-api.h"
#include <cstddef>
#include <cstdint>
#include <mutex>
#include <string>
#include <vector>

class sherpa_vad {
public:
    struct Config {
        std::string model_path;
        int sample_rate           = 16000;
        float threshold           = 0.5f;
        float min_silence_seconds = 0.5f;
        float min_speech_seconds  = 0.25f;
        float max_speech_seconds  = 20.0f;
        int num_threads           = 1;
        float buffer_seconds      = 30.0f;
    };

    explicit sherpa_vad(const Config& config);
    ~sherpa_vad();

    sherpa_vad(const sherpa_vad&)            = delete;
    sherpa_vad& operator=(const sherpa_vad&) = delete;

    bool accept(const float* samples, size_t count);
    bool pop_segment(std::vector<float>& out, int32_t& start_sample);
    void flush();
    void reset();

private:
    const SherpaOnnxVoiceActivityDetector* _vad = nullptr;
    std::mutex _lock;
};
