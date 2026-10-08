#pragma once

#include "sherpa-onnx/c-api/c-api.h"
#include <memory>
#include <mutex>
#include <string>

struct punctuation_based_result {
    std::string result;
    bool is_online_mode;
};

class onnx_punctuation_provider {
public:
    struct punctuation_config {
        std::string model_path;
        std::optional<std::string> vocab_path;
        bool isOnline;
    };

    explicit onnx_punctuation_provider(const punctuation_config& config);
    ~onnx_punctuation_provider();

    onnx_punctuation_provider(const onnx_punctuation_provider&)            = delete;
    onnx_punctuation_provider& operator=(const onnx_punctuation_provider&) = delete;

    onnx_punctuation_provider(onnx_punctuation_provider&& other) noexcept;
    onnx_punctuation_provider& operator=(onnx_punctuation_provider&& other) noexcept;

    [[nodiscard]] bool is_valid() const {
        return (_offline_punctuation != nullptr || _online_punctuation != nullptr) && _isReady;
    }
    [[nodiscard]] punctuation_based_result process_text(const std::string& text);

private:
    void cleanup();

    const SherpaOnnxOfflinePunctuation* _offline_punctuation = nullptr;
    const SherpaOnnxOnlinePunctuation* _online_punctuation   = nullptr;
    bool _is_online_mode                                     = false;
    bool _isReady                                            = false;
    mutable std::mutex _mutex{};
};
