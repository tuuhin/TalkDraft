#include "onnx_zipformer.h"

#include <android/log.h>
#include <cctype>
#include <cstring>
#include <fstream>
#include <sys/stat.h>
#include <unistd.h>
#include <vector>

#define LOG_TAG "ONNX_ZIP_FORMER"
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#if defined(DEBUG) || !defined(NDEBUG)
#define IS_DEBUG 1
#else
#define IS_DEBUG 0
#endif

namespace {
bool check_file_exists(const std::string& path) {
    struct stat buffer{};
    return (stat(path.c_str(), &buffer) == 0);
}
} // namespace

zip_former::zip_former() = default;

zip_former::~zip_former() { cleanup(); }

zip_former::zip_former(zip_former&& other) noexcept { *this = std::move(other); }

zip_former& zip_former::operator=(zip_former&& other) noexcept {
    if (this != &other) {
        // Lock both mutexes safely using std::scoped_lock to avoid deadlock
        std::scoped_lock lock(_mutex, other._mutex);

        // lock free cleanup
        cleanup_locked();

        _online_recognizer = other._online_recognizer;
        _online_stream     = other._online_stream;
        _isReady           = other._isReady;
        _current_segment   = std::move(other._current_segment);
        _segment_id        = other._segment_id;

        other._online_recognizer = nullptr;
        other._online_stream     = nullptr;
        other._isReady           = false;
        other._segment_id        = 1;
    }
    return *this;
}

void zip_former::cleanup() {
    std::lock_guard<std::mutex> lock(_mutex);
    cleanup_locked();
}

void zip_former::cleanup_locked() {
    if (_online_stream) {
        LOG_D("CLEARING ONLINE STREAM");
        SherpaOnnxDestroyOnlineStream(_online_stream);
        _online_stream = nullptr;
    }
    if (_online_recognizer) {
        LOG_D("CLEARING ONLINE RECOGNIZER");
        SherpaOnnxDestroyOnlineRecognizer(_online_recognizer);
        _online_recognizer = nullptr;
    }
    _isReady = false;
    _current_segment.clear();
}

bool zip_former::Initialize(const std::string& encoder_path, const std::string& decoder_path,
                            const std::string& joiner_path, const std::string& tokens_path) {
    cleanup();

    if (!check_file_exists(encoder_path) || !check_file_exists(decoder_path) || !check_file_exists(joiner_path) ||
        !check_file_exists(tokens_path)) {
        LOG_E("MODEL FILE NOT FOUND. encoder=%s, decoder=%s, joiner=%s, tokens=%s", encoder_path.c_str(),
              decoder_path.c_str(), joiner_path.c_str(), tokens_path.c_str());
        return false;
    }

    // locking
    std::lock_guard<std::mutex> lock(_mutex);

    SherpaOnnxOnlineRecognizerConfig online_cfg;
    std::memset(&online_cfg, 0, sizeof(online_cfg));
    online_cfg.feat_config.sample_rate         = 16000;
    online_cfg.feat_config.feature_dim         = 80;
    online_cfg.model_config.transducer.encoder = encoder_path.c_str();
    online_cfg.model_config.transducer.decoder = decoder_path.c_str();
    online_cfg.model_config.transducer.joiner  = joiner_path.c_str();
    online_cfg.model_config.tokens             = tokens_path.c_str();
    online_cfg.model_config.num_threads        = 2;
    online_cfg.model_config.model_type         = "zipformer2";
    online_cfg.model_config.provider           = "cpu";
    online_cfg.model_config.debug              = IS_DEBUG;
    online_cfg.decoding_method                 = "modified_beam_search";
    online_cfg.max_active_paths                = 4;
    online_cfg.enable_endpoint                 = 0;
    online_cfg.blank_penalty                   = 0.0f;

    LOG_D("TRYING TO CREATE ONLINE STREAM FOR RECOGNIZER");
    _online_recognizer = SherpaOnnxCreateOnlineRecognizer(&online_cfg);

    if (_online_recognizer == nullptr) {
        LOG_E("FAILED TO SETUP ZIP FORMER WITH GIVEN MODEL PATHS");
        return false;
    }
    _online_stream = SherpaOnnxCreateOnlineStream(_online_recognizer);
    if (_online_stream == nullptr) {
        LOG_E("FAILED TO CREATE  A STREAM CLEARING RECOGNIZER");
        SherpaOnnxDestroyOnlineRecognizer(_online_recognizer);
        _online_recognizer = nullptr;
        return false;
    }
    LOG_I("ZIP_FORMER SETUP COMPLETE");
    _segment_id = 1;
    _isReady    = true;
    return true;
}

transcription_result zip_former::ProcessPCM(const float* pcm_data, int sample_count) {
    // no data
    if (!pcm_data || sample_count <= 0) {
        LOG_D("NO SAMPLE DATA TO WORK WITH");
        return {};
    }

    std::lock_guard<std::mutex> lock(_mutex);

    // nothing is ready
    if (!_isReady || !_online_recognizer || !_online_stream) {
        LOG_D("ONLINE RECOGNIZER IS NOT READY");
        return {};
    }

    SherpaOnnxOnlineStreamAcceptWaveform(_online_stream, 16000, pcm_data, sample_count);

    // decode loop
    while (SherpaOnnxIsOnlineStreamReady(_online_recognizer, _online_stream)) {
        SherpaOnnxDecodeOnlineStream(_online_recognizer, _online_stream);
    }

    std::string new_text;
    const auto* result = SherpaOnnxGetOnlineStreamResult(_online_recognizer, _online_stream);
    if (result) {
        if (result->text && std::strlen(result->text) > 0) new_text = result->text;
        // clean the result
        SherpaOnnxDestroyOnlineRecognizerResult(result);
    }

    if (!new_text.empty() && new_text != _current_segment) {
        _current_segment = new_text;

        LOG_D("[SEGMENT %d HYPOTHESIS UPDATE='%s' | LENGTH=%zu", _segment_id, _current_segment.c_str(),
              _current_segment.length());

        return {_segment_id, _current_segment};
    }
    return {};
}

void zip_former::Reset() {
    std::lock_guard<std::mutex> lock(_mutex);
    if (!_online_recognizer || !_online_stream) return;

    // mark online stream input done
    SherpaOnnxOnlineStreamInputFinished(_online_stream);

    // stream out anything that is left
    while (SherpaOnnxIsOnlineStreamReady(_online_recognizer, _online_stream)) {
        SherpaOnnxDecodeOnlineStream(_online_recognizer, _online_stream);
    }

    const auto* final_result = SherpaOnnxGetOnlineStreamResult(_online_recognizer, _online_stream);
    if (final_result) {
        LOG_D("CLEARING AWAITING SEGMENTS");
        SherpaOnnxDestroyOnlineRecognizerResult(final_result);
    }

    _current_segment.clear();
    _segment_id++;
    LOG_D("SHERPA RESET");
    SherpaOnnxOnlineStreamReset(_online_recognizer, _online_stream);
}
