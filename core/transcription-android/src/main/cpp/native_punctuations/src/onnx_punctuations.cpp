#include "onnx_punctuations.h"
#include <android/log.h>
#include <cstring>
#include <stdexcept>
#include <utility>

#define LOG_TAG "SHERPA_PUNCTUATIONS"
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOG_W(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#if defined(DEBUG) || !defined(NDEBUG)
#define IS_DEBUG 1
#else
#define IS_DEBUG 0
#endif

onnx_punctuation_provider::onnx_punctuation_provider(const punctuation_config& config) {
    if (config.model_path.empty()) {
        LOG_E("MODEL FILE IS NEEDED OTHERWISE THIS CANNOT BE USED");
        return;
    }

    _is_online_mode = config.isOnline;

    if (config.isOnline) {

        if (!config.vocab_path.has_value() || config.vocab_path->empty()) {
            LOG_E("ONLINE PUNCTUATION REQUIRES A VOCAB FILE WHICH IS MISSING");
            throw std::invalid_argument("Online punctuation model requires a vocab file, but none was provided");
        }

        const auto& vocab_path = *config.vocab_path;
        LOG_I("Initializing Online Sherpa-ONNX Punctuation Provider with model: %s, vocab: %s",
              config.model_path.c_str(), vocab_path.c_str());

        SherpaOnnxOnlinePunctuationConfig online_config = {};
        online_config.model.cnn_bilstm                  = config.model_path.c_str();
        online_config.model.bpe_vocab                   = vocab_path.c_str();
        online_config.model.num_threads                 = 2;
        online_config.model.provider                    = "cpu";
        online_config.model.debug                       = IS_DEBUG;

        _online_punctuation = SherpaOnnxCreateOnlinePunctuation(&online_config);
        if (!_online_punctuation) {
            LOG_E("FAILED TO CREATE ONLINE PUNCTUATION PROVIDER");
            _isReady = false;
            return;
        }
        _isReady = true;
        LOG_I("Sherpa-ONNX ONLINE PUNCTUATION PROVIDER INITIATED SUCCESSFULLY");
    } else {
        LOG_I("Initializing Offline Sherpa-ONNX Punctuation Provider with model: %s", config.model_path.c_str());

        SherpaOnnxOfflinePunctuationConfig offline_config = {};
        offline_config.model.ct_transformer               = config.model_path.c_str();
        offline_config.model.num_threads                  = 2;
        offline_config.model.provider                     = "cpu";
        offline_config.model.debug                        = IS_DEBUG;

        _offline_punctuation = SherpaOnnxCreateOfflinePunctuation(&offline_config);
        if (!_offline_punctuation) {
            LOG_E("FAILED TO CREATE OFFLINE PUNCTUATION PROVIDER");
            _isReady = false;
            return;
        }
        _isReady = true;
        LOG_I("Sherpa-ONNX OFFLINE PUNCTUATION PROVIDER INITIATED SUCCESSFULLY");
    }
}

onnx_punctuation_provider::~onnx_punctuation_provider() { cleanup(); }

onnx_punctuation_provider::onnx_punctuation_provider(onnx_punctuation_provider&& other) noexcept {
    std::lock_guard<std::mutex> lock(other._mutex);
    _offline_punctuation = other._offline_punctuation;
    _online_punctuation  = other._online_punctuation;
    _is_online_mode      = other._is_online_mode;
    _isReady             = other._isReady;

    other._offline_punctuation = nullptr;
    other._online_punctuation  = nullptr;
    other._is_online_mode      = false;
    other._isReady             = false;
}

onnx_punctuation_provider& onnx_punctuation_provider::operator=(onnx_punctuation_provider&& other) noexcept {
    if (this != &other) {
        std::unique_lock<std::mutex> lhs_lock(_mutex, std::defer_lock);
        std::unique_lock<std::mutex> rhs_lock(other._mutex, std::defer_lock);
        std::lock(lhs_lock, rhs_lock);

        cleanup();

        _offline_punctuation = other._offline_punctuation;
        _online_punctuation  = other._online_punctuation;
        _is_online_mode      = other._is_online_mode;
        _isReady             = other._isReady;

        other._offline_punctuation = nullptr;
        other._online_punctuation  = nullptr;
        other._is_online_mode      = false;
        other._isReady             = false;
    }
    return *this;
}

punctuation_based_result onnx_punctuation_provider::process_text(const std::string& text) {
    std::lock_guard<std::mutex> lock(_mutex);

    if (!_isReady || (!_offline_punctuation && !_online_punctuation)) {
        LOG_W("MISSING INITIALIZATION");
        return {text, _is_online_mode};
    }

    if (text.empty()) {
        LOG_W("NOTHING TO PROCESS FOR THE SHERPA PUNCTUATION PROVIDER");
        return {"", _is_online_mode};
    }

    if (_is_online_mode && _online_punctuation) {
        const char* result_c_str = SherpaOnnxOnlinePunctuationAddPunct(_online_punctuation, text.c_str());
        if (result_c_str) {
            std::string result(result_c_str);
            SherpaOnnxOnlinePunctuationFreeText(result_c_str);
            return {std::move(result), _is_online_mode};
        }
    }

    if (_offline_punctuation) {
        const char* result_c_str = SherpaOfflinePunctuationAddPunct(_offline_punctuation, text.c_str());
        if (result_c_str) {
            std::string result(result_c_str);
            SherpaOfflinePunctuationFreeText(result_c_str);
            return {std::move(result), _is_online_mode};
        }
    }

    LOG_W("FALLBACK SENDING THE INPUT ITSELF");
    // failed result
    return {text, _is_online_mode};
}

void onnx_punctuation_provider::cleanup() {
    if (_online_punctuation) {
        LOG_D("Destroying SherpaOnnxOnlinePunctuation instance.");
        SherpaOnnxDestroyOnlinePunctuation(_online_punctuation);
        _online_punctuation = nullptr;
    }
    if (_offline_punctuation) {
        LOG_D("Destroying SherpaOnnxOfflinePunctuation instance.");
        SherpaOnnxDestroyOfflinePunctuation(_offline_punctuation);
        _offline_punctuation = nullptr;
    }
    _isReady        = false;
    _is_online_mode = false;
}
