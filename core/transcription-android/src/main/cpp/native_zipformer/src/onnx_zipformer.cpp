#include "onnx_zipformer.h"
#include <android/log.h>
#include <fstream>
#include <sstream>

#define LOG_TAG "ZIP_FORMER_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

zip_former::zip_former() {
    _session_options.SetIntraOpNumThreads(2);
    _session_options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
}

zip_former::~zip_former() = default;

void zip_former::load_tokens(const std::string& tokens_path) {
    std::ifstream file(tokens_path);
    std::string line;
    while (std::getline(file, line)) {
        if (line.empty()) continue;
        std::istringstream iss(line);
        std::string token;
        int id;
        if (iss >> token >> id) {
            // Replace space symbol   with actual space
            if (token.rfind(' ', 0) == 0) {
                token = " " + token.substr(3);
            }
            _token_map[id] = token;
        }
    }
}

bool zip_former::Initialize(const std::string& encoder_path, const std::string& decoder_path,
                            const std::string& joiner_path, const std::string& tokens_path) {
    try {
        load_tokens(tokens_path);
        _encoder_session = std::make_unique<Ort::Session>(_env, encoder_path.c_str(), _session_options);
        _decoder_session = std::make_unique<Ort::Session>(_env, decoder_path.c_str(), _session_options);
        _joiner_session  = std::make_unique<Ort::Session>(_env, joiner_path.c_str(), _session_options);
        _isReady         = true;
        LOGI("ONNX sessions initialized successfully.");
        return true;
    } catch (const Ort::Exception& e) {
        LOGE("ONNX Error: %s", e.what());
        return false;
    }
}

std::string zip_former::ProcessPCM(const float* pcm_data, int sample_count) {
    if (!_isReady) return "";
    return _acc_transcript;
}

void zip_former::Reset() { _acc_transcript.clear(); }
