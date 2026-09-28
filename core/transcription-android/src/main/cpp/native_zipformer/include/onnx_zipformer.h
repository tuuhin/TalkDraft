#pragma once

#include "sherpa-onnx/c-api/c-api.h"
#include <memory>
#include <mutex>
#include <string>

struct transcription_result {
    uint64_t segment_id{0};
    std::string text;
    [[nodiscard]] bool is_empty() const { return text.empty(); }
};

class zip_former {
public:
    zip_former();
    ~zip_former();

    zip_former(const zip_former&)            = delete;
    zip_former& operator=(const zip_former&) = delete;

    // Allow explicit move operations
    zip_former(zip_former&& other) noexcept;
    zip_former& operator=(zip_former&& other) noexcept;

    bool Initialize(const std::string& encoder_path, const std::string& decoder_path, const std::string& joiner_path,
                    const std::string& tokens_path);

    transcription_result ProcessPCM(const float* pcm_data, int sample_count);
    void Reset();

private:
    void cleanup();
    void cleanup_locked();

    const SherpaOnnxOnlineRecognizer* _online_recognizer = nullptr;
    const SherpaOnnxOnlineStream* _online_stream         = nullptr;

    std::string _current_segment;
    bool _isReady = false;
    std::mutex _mutex{};
    uint64_t _segment_id{1};
};
