#pragma once

#include "whisper.h"
#include <mutex>
#include <string>

class whisper_engine {
public:
    whisper_engine(const std::string& modelPath, const std::string& language, bool useGpu);
    ~whisper_engine();

    whisper_engine(const whisper_engine&)            = delete;
    whisper_engine& operator=(const whisper_engine&) = delete;
    bool process(const float* pcm, int sampleCount);

    [[nodiscard]] bool isInitialized() const;
    [[nodiscard]] int segmentCount() const;
    [[nodiscard]] const char* segmentText(int index) const;
    [[nodiscard]] int64_t segmentStartMs(int index) const;
    [[nodiscard]] int64_t segmentEndMs(int index) const;
    [[nodiscard]] int lastError() const { return _lastError; };
    void clear_error();

private:
    whisper_context* ctx_         = nullptr;
    whisper_state* _whisper_state = nullptr;

    std::string _default_language = "en";
    int _lastError                = 0;
    std::mutex _mutex;
    void setError(int error);
    void destroy();
};
