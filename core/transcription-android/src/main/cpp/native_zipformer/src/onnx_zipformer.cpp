#include "onnx_zipformer.h"

#include <android/asset_manager.h>
#include <android/log.h>
#include <cctype>
#include <cstring>
#include <fstream>
#include <sys/stat.h>
#include <unistd.h>
#include <vector>

#define LOG_TAG "ZIP_FORMER_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

zip_former::zip_former() = default;

zip_former::~zip_former() { cleanup(); }

void zip_former::cleanup() {
    if (_online_stream) {
        SherpaOnnxDestroyOnlineStream(_online_stream);
        _online_stream = nullptr;
    }
    if (_online_recognizer) {
        SherpaOnnxDestroyOnlineRecognizer(_online_recognizer);
        _online_recognizer = nullptr;
    }
    if (_offline_recognizer) {
        SherpaOnnxDestroyOfflineRecognizer(_offline_recognizer);
        _offline_recognizer = nullptr;
    }
    _isReady   = false;
    _is_online = false;
    _acc_transcript.clear();
    _current_segment.clear();
}

bool zip_former::Initialize(const std::string& encoder_path, const std::string& decoder_path,
                            const std::string& joiner_path, const std::string& tokens_path) {
    cleanup();

    auto file_exists = [](const std::string& path) {
        std::ifstream f(path);
        return f.good();
    };

    if (!file_exists(encoder_path) || !file_exists(decoder_path) || !file_exists(joiner_path) ||
        !file_exists(tokens_path)) {
        LOGE("Model file not found. encoder=%s, decoder=%s, joiner=%s, tokens=%s", encoder_path.c_str(),
             decoder_path.c_str(), joiner_path.c_str(), tokens_path.c_str());
        return false;
    }

    // 1. Attempt to initialize as an Online (Streaming) Zipformer model first.
    SherpaOnnxOnlineRecognizerConfig online_cfg;
    std::memset(&online_cfg, 0, sizeof(online_cfg));
    online_cfg.feat_config.sample_rate         = 16000;
    online_cfg.feat_config.feature_dim         = 80;
    online_cfg.model_config.transducer.encoder = encoder_path.c_str();
    online_cfg.model_config.transducer.decoder = decoder_path.c_str();
    online_cfg.model_config.transducer.joiner  = joiner_path.c_str();
    online_cfg.model_config.tokens             = tokens_path.c_str();
    online_cfg.model_config.num_threads        = 2;
    online_cfg.model_config.provider           = "cpu";
    online_cfg.model_config.debug              = 0;
    online_cfg.decoding_method                 = "greedy_search";
    online_cfg.enable_endpoint                 = 1;

    _online_recognizer = SherpaOnnxCreateOnlineRecognizer(&online_cfg);
    if (_online_recognizer != nullptr) {
        _online_stream = SherpaOnnxCreateOnlineStream(_online_recognizer);
        if (_online_stream != nullptr) {
            _is_online = true;
            _isReady   = true;
            LOGI("Streaming Zipformer initialized successfully.");
            return true;
        }
        SherpaOnnxDestroyOnlineRecognizer(_online_recognizer);
        _online_recognizer = nullptr;
    }

    // 2. If online initialization fails, fallback to Offline (Non-streaming) Zipformer.
    SherpaOnnxOfflineRecognizerConfig offline_cfg;
    std::memset(&offline_cfg, 0, sizeof(offline_cfg));
    offline_cfg.feat_config.sample_rate         = 16000;
    offline_cfg.feat_config.feature_dim         = 80;
    offline_cfg.model_config.transducer.encoder = encoder_path.c_str();
    offline_cfg.model_config.transducer.decoder = decoder_path.c_str();
    offline_cfg.model_config.transducer.joiner  = joiner_path.c_str();
    offline_cfg.model_config.tokens             = tokens_path.c_str();
    offline_cfg.model_config.num_threads        = 2;
    offline_cfg.model_config.provider           = "cpu";
    offline_cfg.model_config.debug              = 0;
    offline_cfg.decoding_method                 = "greedy_search";

    _offline_recognizer = SherpaOnnxCreateOfflineRecognizer(&offline_cfg);
    if (_offline_recognizer != nullptr) {
        _is_online = false;
        _isReady   = true;
        LOGI("Offline Zipformer initialized successfully.");
        return true;
    }

    LOGE("Failed to initialize Zipformer with provided models.");
    return false;
}

std::string zip_former::ProcessPCM(const float* pcm_data, int sample_count) {
    if (!_isReady || !pcm_data || sample_count <= 0) {
        return _acc_transcript;
    }

    if (_is_online && _online_recognizer && _online_stream) {
        SherpaOnnxOnlineStreamAcceptWaveform(_online_stream, 16000, pcm_data, sample_count);
        while (SherpaOnnxIsOnlineStreamReady(_online_recognizer, _online_stream)) {
            SherpaOnnxDecodeOnlineStream(_online_recognizer, _online_stream);
        }

        const SherpaOnnxOnlineRecognizerResult* r = SherpaOnnxGetOnlineStreamResult(_online_recognizer, _online_stream);
        if (r) {
            if (r->text && std::strlen(r->text) > 0) {
                _current_segment = r->text;
            }
            SherpaOnnxDestroyOnlineRecognizerResult(r);
        }

        if (SherpaOnnxOnlineStreamIsEndpoint(_online_recognizer, _online_stream)) {
            if (!_current_segment.empty()) {
                if (!_acc_transcript.empty()) {
                    _acc_transcript += ' ';
                }
                _acc_transcript += _current_segment;
                _current_segment.clear();
            }
            SherpaOnnxOnlineStreamReset(_online_recognizer, _online_stream);
        }

        std::string full_transcript = _acc_transcript;
        if (!_current_segment.empty()) {
            if (!full_transcript.empty()) {
                full_transcript += ' ';
            }
            full_transcript += _current_segment;
        }
        return full_transcript;
    }

    if (!_is_online && _offline_recognizer) {
        const SherpaOnnxOfflineStream* stream = SherpaOnnxCreateOfflineStream(_offline_recognizer);
        if (stream) {
            SherpaOnnxAcceptWaveformOffline(stream, 16000, pcm_data, sample_count);
            SherpaOnnxDecodeOfflineStream(_offline_recognizer, stream);
            const SherpaOnnxOfflineRecognizerResult* r = SherpaOnnxGetOfflineStreamResult(stream);
            if (r) {
                if (r->text && std::strlen(r->text) > 0) {
                    _acc_transcript = r->text;
                }
                SherpaOnnxDestroyOfflineRecognizerResult(r);
            }
            SherpaOnnxDestroyOfflineStream(stream);
        }
        return _acc_transcript;
    }

    return _acc_transcript;
}

void zip_former::Reset() {
    _acc_transcript.clear();
    _current_segment.clear();
    if (_is_online && _online_recognizer && _online_stream) {
        SherpaOnnxOnlineStreamReset(_online_recognizer, _online_stream);
    }
}
