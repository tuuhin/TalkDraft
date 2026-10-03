#include "whisper_engine.h"
#include <android/log.h>

#define LOG_TAG "Native_Whisper_Engine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_W(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace {
inline bool isDebug() {
#if defined(DEBUG) || !defined(NDEBUG)
    return true;
#else
    return false;
#endif
}
} // namespace

whisper_engine::whisper_engine(const std::string& modelPath, const std::string& language, bool useGpu)
    : _default_language(language.empty() ? "en" : language) {

    whisper_context_params ctx_params = whisper_context_default_params();
    ctx_params.use_gpu                = useGpu;

    _wh_ctx = whisper_init_from_file_with_params(modelPath.c_str(), ctx_params);
    if (_wh_ctx == nullptr) {
        LOG_W("FAILED TO PREPARE WHISPER INSTANCE FROM PARAMS");
        _lastError = whisper_engine_error_codes::MODEL_SETUP_FAILED;
        return;
    }
    _whisper_state = whisper_init_state(_wh_ctx);
    if (!_whisper_state) {
        LOG_W("FAILED TO INITIALIZE WHISPER STATE");
        _lastError = whisper_engine_error_codes::MODEL_SETUP_FAILED;
        whisper_free(_wh_ctx);
        _wh_ctx = nullptr;
        return;
    }
    LOG_D("WHISPER ENGINE SETUP DONE");

    auto params             = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads        = 2;
    params.print_realtime   = isDebug();
    params.print_progress   = isDebug();
    params.print_timestamps = isDebug();
    params.print_special    = false;
    params.single_segment   = false;
    params.debug_mode       = isDebug();
    params.no_context       = false;
    params.no_timestamps    = true;
    params.no_speech_thold  = 0.6f;
    params.max_len          = 0;
    params.detect_language  = false;
    params.language         = _default_language.c_str();
    params.suppress_blank   = true;
    params.suppress_nst     = true;
    params.greedy.best_of   = 1;
    params.temperature      = 0.0f;

    _params = params;
    LOG_D("WHISPER PARAMS SETUP DONE");
}

whisper_engine::~whisper_engine() { destroy(); }

void whisper_engine::destroy() {
    if (_whisper_state) {
        whisper_free_state(_whisper_state);
        _whisper_state = nullptr;
        LOG_D("WHISPER STATE CLEANUP");
    }

    if (_wh_ctx) {
        whisper_free(_wh_ctx);
        _wh_ctx = nullptr;
        LOG_D("WHISPER CLEANUP");
    }
}

bool whisper_engine::process(const float* pcm, int sampleCount) {
    if (!isInitialized() || !pcm || sampleCount < 16000) {
        LOGE("NO PCM DATA INCLUDED OR SAMPLE COUNT IS INVALID");
        setError(whisper_engine_error_codes::INVALID_BUFFER);
        return false;
    }

    clear_error();

    if (whisper_full_with_state(_wh_ctx, _whisper_state, _params, pcm, sampleCount) != 0) {
        LOG_W("FAILED TO ADD INTERFERENCE WITH THE WHISPER STATE");
        setError(whisper_engine_error_codes::INFERENCE_FAILED); // INFERENCE_FAILED
        return false;
    }
    return true;
}

int whisper_engine::segmentCount() const {
    if (!isInitialized()) return 0;
    return whisper_full_n_segments_from_state(_whisper_state);
}

const char* whisper_engine::segmentText(int index) const {
    if (!isInitialized()) return nullptr;
    return whisper_full_get_segment_text_from_state(_whisper_state, index);
}

int64_t whisper_engine::segmentStartMs(int index) const {
    if (!isInitialized()) return 0;
    int64_t centi_seconds = whisper_full_get_segment_t0_from_state(_whisper_state, index);
    return (centi_seconds < 0) ? 0 : (centi_seconds * 10);
}

int64_t whisper_engine::segmentEndMs(int index) const {
    if (!isInitialized()) return 0;
    int64_t centi_seconds = whisper_full_get_segment_t1_from_state(_whisper_state, index);
    return (centi_seconds < 0) ? 0 : (centi_seconds * 10);
}

void whisper_engine::reset() {
    if (!_wh_ctx) return;
    if (_whisper_state) {
        LOG_W("WHISPER STATE CLEANUP");
        whisper_free_state(_whisper_state);
        _whisper_state = nullptr;
    }

    _whisper_state = whisper_init_state(_wh_ctx);
    if (_whisper_state == nullptr) {
        LOGE("reset(): FAILED TO RE-INITIALIZE WHISPER STATE");
        setError(whisper_engine_error_codes::MODEL_RESET_FAILED);
        return;
    }
    clear_error();
    LOG_D("WHISPER CONTEXT AND STATE RESET SUCCESSFUL");
}

void whisper_engine::clear_error() { _lastError = whisper_engine_error_codes::NONE; }

void whisper_engine::setError(whisper_engine_error_codes error) { _lastError = error; }
