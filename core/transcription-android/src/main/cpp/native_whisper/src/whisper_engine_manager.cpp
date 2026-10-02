#include "whisper_engine_manager.h"

uint8_t whisper_engine_manager::create_engine(const std::string& modelPath, const std::string& language, bool useGpu) {
    auto engine = std::make_shared<whisper_engine>(modelPath, language, useGpu);
    if (!engine->isInitialized()) return INVALID_HANDLE;
    std::lock_guard<std::mutex> lock(_mutex);
    const auto id = _nextHandle++;
    _engines.emplace(id, std::move(engine));
    return id;
}

bool whisper_engine_manager::destroy_engine(uint8_t id) {
    std::shared_ptr<whisper_engine> victim;
    {
        std::lock_guard<std::mutex> lock(_mutex);
        const auto it = _engines.find(id);
        if (it == _engines.end()) return false;
        victim = std::move(it->second);
        _engines.erase(it);
    }
    return true;
}

std::shared_ptr<whisper_engine> whisper_engine_manager::read_engine_from_handle(uint8_t id) {
    std::lock_guard<std::mutex> lock(_mutex);
    const auto it = _engines.find(id);
    return it == _engines.end() ? nullptr : it->second;
}
whisper_engine_manager& whisper_engine_manager::instance() {
    static whisper_engine_manager mgr;
    return mgr;
}
