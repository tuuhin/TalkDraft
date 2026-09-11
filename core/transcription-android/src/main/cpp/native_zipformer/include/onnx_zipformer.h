#pragma once

#include "sherpa-c-api.h"
#include <android/asset_manager.h>
#include <memory>
#include <string>

class zip_former {
public:
    zip_former();
    ~zip_former();

    bool Initialize(const std::string& encoder_path, const std::string& decoder_path, const std::string& joiner_path,
                    const std::string& tokens_path);

    std::string ProcessPCM(const float* pcm_data, int sample_count);
    void Reset();

private:
    void cleanup();

    const SherpaOnnxOnlineRecognizer* _online_recognizer   = nullptr;
    const SherpaOnnxOnlineStream* _online_stream           = nullptr;
    const SherpaOnnxOfflineRecognizer* _offline_recognizer = nullptr;

    std::string _acc_transcript;
    std::string _current_segment;
    bool _is_online = false;
    bool _isReady   = false;
};
