#include "punctuations_manager.h"
#include <cstdio>
#include <vector>

punctuation_manager& punctuation_manager::instance() {
    static punctuation_manager mgr;
    return mgr;
}

int64_t punctuation_manager::create_instance(const onnx_punctuation_provider::punctuation_config& config) {
    auto provider = std::make_shared<onnx_punctuation_provider>(config);
    if (!provider->is_valid()) return kInvalidHandle;
    std::lock_guard<std::mutex> lock(_lock);
    const int64_t handle = _next_handle++;
    _instances.emplace(handle, std::move(provider));
    return handle;
}

std::shared_ptr<onnx_punctuation_provider> punctuation_manager::create_from_handle(int64_t handle) {
    std::lock_guard<std::mutex> lock(_lock);
    auto it = _instances.find(handle);
    return it == _instances.end() ? nullptr : it->second;
}

void punctuation_manager::destroy_instance(int64_t handle) {
    std::shared_ptr<onnx_punctuation_provider> victim;
    {
        std::lock_guard<std::mutex> lock(_lock);
        auto it = _instances.find(handle);
        if (it == _instances.end()) return;
        victim = std::move(it->second);
        _instances.erase(it);
    }
}
