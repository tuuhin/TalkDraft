#pragma once

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>

#include "sherpa_vad.h"

class sherpa_vad_manager {
public:
    static sherpa_vad_manager& instance();

    int64_t create_vad(const sherpa_vad::Config& config);

    int64_t create_vad_from_asset(AAssetManager* mgr, const std::string& cache_dir, sherpa_vad::Config& config);

    std::shared_ptr<sherpa_vad> create_from_handle(int64_t handle);

    void destroy_vad(int64_t handle);

    // flag
    static constexpr int64_t kInvalidHandle = -1;
    // copy functionality
    sherpa_vad_manager(const sherpa_vad_manager&)            = delete;
    sherpa_vad_manager& operator=(const sherpa_vad_manager&) = delete;

private:
    sherpa_vad_manager() = default;

    ~sherpa_vad_manager() = default;
    static bool copy_asset_to_file(AAssetManager* mgr, const std::string& asset_name, const std::string& dst_path);

    std::mutex _lock;
    int64_t _next_handle = 1;
    std::unordered_map<int64_t, std::shared_ptr<sherpa_vad>> _instances;
};
