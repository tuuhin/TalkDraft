#pragma once

#include "onnx_punctuations.h"
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>

class punctuation_manager {
public:
    static punctuation_manager& instance();
    [[nodiscard]] int64_t create_instance(const onnx_punctuation_provider::punctuation_config& config);
    [[nodiscard]] std::shared_ptr<onnx_punctuation_provider> create_from_handle(int64_t handle);
    void destroy_instance(int64_t handle);
    // flag
    static constexpr int64_t kInvalidHandle = -1;
    // copy functionality
    punctuation_manager(const punctuation_manager&)            = delete;
    punctuation_manager& operator=(const punctuation_manager&) = delete;

private:
    punctuation_manager()  = default;
    ~punctuation_manager() = default;

    std::mutex _lock;
    int64_t _next_handle = 1;
    std::unordered_map<int64_t, std::shared_ptr<onnx_punctuation_provider>> _instances;
};
