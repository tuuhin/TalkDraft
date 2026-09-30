#include "sherpa_vad_manager.h"
#include <android/log.h>

#include <cstdio>
#include <vector>

#define LOG_TAG "VAD_SHERPA_MANAGER"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

sherpa_vad_manager& sherpa_vad_manager::instance() {
    static sherpa_vad_manager mgr;
    return mgr;
}

int64_t sherpa_vad_manager::create_vad(const sherpa_vad::Config& config) {
    auto vad = std::make_shared<sherpa_vad>(config);
    if (!vad->is_valid()) {
        LOGE("Failed to create VAD from model: %s", config.model_path.c_str());
        return kInvalidHandle;
    }

    std::lock_guard<std::mutex> lock(_lock);
    const int64_t handle = _next_handle++;
    _instances.emplace(handle, std::move(vad));
    return handle;
}

int64_t sherpa_vad_manager::create_vad_from_asset(AAssetManager* mgr, const std::string& cache_dir,
                                                  sherpa_vad::Config& config) {
    if (!mgr) return kInvalidHandle;

    const std::string dst = cache_dir + "/" + config.model_path;
    if (!copy_asset_to_file(mgr, config.model_path, dst)) return kInvalidHandle;

    // mutating model path to the asset path destination
    config.model_path = dst;
    return create_vad(config);
}

std::shared_ptr<sherpa_vad> sherpa_vad_manager::create_from_handle(int64_t handle) {
    std::lock_guard<std::mutex> lock(_lock);
    auto it = _instances.find(handle);
    return it == _instances.end() ? nullptr : it->second;
}

void sherpa_vad_manager::destroy_vad(int64_t handle) {
    std::shared_ptr<sherpa_vad> victim;
    {
        std::lock_guard<std::mutex> lock(_lock);
        auto it = _instances.find(handle);
        if (it == _instances.end()) return;
        victim = std::move(it->second);
        _instances.erase(it);
    }
}

bool sherpa_vad_manager::copy_asset_to_file(AAssetManager* mgr, const std::string& asset_name,
                                            const std::string& dst_path) {
    AAsset* asset = AAssetManager_open(mgr, asset_name.c_str(), AASSET_MODE_STREAMING);
    if (!asset) {
        LOGE("Asset not found: %s", asset_name.c_str());
        return false;
    }

    const off_t size = AAsset_getLength(asset);

    // Skip the copy if a file of the same size is already there.
    if (FILE* existing = std::fopen(dst_path.c_str(), "rb")) {
        std::fseek(existing, 0, SEEK_END);
        const long existing_size = std::ftell(existing);
        std::fclose(existing);
        if (existing_size == static_cast<long>(size)) {
            AAsset_close(asset);
            return true;
        }
    }

    FILE* out = std::fopen(dst_path.c_str(), "wb");
    if (!out) {
        LOGE("Cannot write %s", dst_path.c_str());
        AAsset_close(asset);
        return false;
    }

    std::vector<char> buf(64 * 1024);
    bool ok = true;
    int n;
    while ((n = AAsset_read(asset, buf.data(), buf.size())) > 0) {
        if (std::fwrite(buf.data(), 1, static_cast<size_t>(n), out) != static_cast<size_t>(n)) {
            ok = false;
            break;
        }
    }
    if (n < 0) ok = false;

    std::fclose(out);
    AAsset_close(asset);

    if (!ok) {
        std::remove(dst_path.c_str());
        LOGE("Failed copying asset %s", asset_name.c_str());
    }
    return ok;
}
