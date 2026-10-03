#pragma once

#include "whisper.h"
#include <mutex>
#include <string>

enum class whisper_engine_error_codes : int {
    UNKNOWN            = -1,
    NONE               = 0,
    MODEL_SETUP_FAILED = 1,
    INFERENCE_FAILED   = 2,
    INVALID_BUFFER     = 3,
    MODEL_RESET_FAILED = 4
};

class whisper_engine {
public:
    whisper_engine(const std::string& modelPath, const std::string& language, bool useGpu);
    ~whisper_engine();

    whisper_engine(const whisper_engine&)            = delete;
    whisper_engine& operator=(const whisper_engine&) = delete;
    bool process(const float* pcm, int sampleCount);

    [[nodiscard]] int segmentCount() const;
    [[nodiscard]] const char* segmentText(int index) const;
    [[nodiscard]] int64_t segmentStartMs(int index) const;
    [[nodiscard]] int64_t segmentEndMs(int index) const;

    [[nodiscard]] bool isInitialized() const { return _wh_ctx != nullptr && _whisper_state != nullptr; };
    [[nodiscard]] whisper_engine_error_codes lastError() const { return _lastError; };

    void reset();
    void clear_error();

private:
    whisper_context* _wh_ctx      = nullptr;
    whisper_state* _whisper_state = nullptr;
    whisper_full_params _params   = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    std::string _default_language         = "en";
    whisper_engine_error_codes _lastError = whisper_engine_error_codes::NONE;
    std::mutex _mutex;
    void setError(whisper_engine_error_codes error);
    void destroy();
};
