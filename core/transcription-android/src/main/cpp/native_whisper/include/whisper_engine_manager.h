#pragma once

#include "whisper_engine.h"

#include <memory>
#include <mutex>
#include <unordered_map>

class whisper_engine_manager {
public:
    static whisper_engine_manager& instance();

    uint8_t create_engine(const std::string& modelPath, const std::string& language, bool useGpu);
    [[nodiscard]] std::shared_ptr<whisper_engine> read_engine_from_handle(uint8_t id);
    bool destroy_engine(uint8_t id);

    whisper_engine_manager(const whisper_engine_manager&)            = delete;
    whisper_engine_manager& operator=(const whisper_engine_manager&) = delete;
    static constexpr uint8_t INVALID_HANDLE                          = 0;

private:
    whisper_engine_manager()  = default;
    ~whisper_engine_manager() = default;
    mutable std::mutex _mutex;
    std::unordered_map<uint8_t, std::shared_ptr<whisper_engine>> _engines;
    uint8_t _nextHandle = 1;
};
