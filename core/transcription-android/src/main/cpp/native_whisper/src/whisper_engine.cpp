#include "whisper_engine.h"
#include <android/log.h>

#define LOG_TAG "Native_Whisper_Engine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

whisper_engine::whisper_engine(const std::string& modelPath, const std::string& language, bool useGpu)
    : _default_language(language.empty() ? "en" : language) {

    whisper_context_params params = whisper_context_default_params();

    params.use_gpu = useGpu;

    ctx_ = whisper_init_from_file_with_params(modelPath.c_str(), params);
    if (!ctx_) {
        _lastError = 1;
        return;
    }
    _whisper_state = whisper_init_state(ctx_);
    if (!_whisper_state) {
        _lastError = 1;
        whisper_free(ctx_);
        ctx_ = nullptr;
        return;
    }
}

whisper_engine::~whisper_engine() { destroy(); }

void whisper_engine::destroy() {
    if (_whisper_state) {
        whisper_free_state(_whisper_state);
        _whisper_state = nullptr;
    }

    if (ctx_) {
        whisper_free(ctx_);
        ctx_ = nullptr;
    }
}

bool whisper_engine::isInitialized() const { return ctx_ != nullptr && _whisper_state != nullptr; }

bool whisper_engine::process(const float* pcm, int sampleCount) {
    if (!isInitialized() || !pcm) {
        setError(3);
        return false;
    }

    if (sampleCount < 16000) {
        setError(3);
        return false;
    }

    clear_error();

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    params.n_threads = 2;

    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;

    params.no_context      = true;
    params.no_speech_thold = 0.4f;

    params.language = _default_language.c_str();

    params.single_segment = true;

    params.greedy.best_of = 1;

    params.temperature = 0.0f;
    params.audio_ctx   = 0;

    if (whisper_full_with_state(ctx_, _whisper_state, params, pcm, sampleCount) != 0) {

        setError(2); // INFERENCE_FAILED
        return false;
    }

    const int nSegments = whisper_full_n_segments_from_state(_whisper_state);

    for (int i = 0; i < nSegments; ++i) {
        const char* text = whisper_full_get_segment_text_from_state(_whisper_state, i);
    }

    return true;
}

int whisper_engine::segmentCount() const {
    if (!_whisper_state) return 0;
    return whisper_full_n_segments_from_state(_whisper_state);
}

const char* whisper_engine::segmentText(int index) const {
    if (!_whisper_state) return nullptr;
    return whisper_full_get_segment_text_from_state(_whisper_state, index);
}

int64_t whisper_engine::segmentStartMs(int index) const {
    if (!_whisper_state) return 0;
    return whisper_full_get_segment_t0_from_state(_whisper_state, index) * 10;
}

int64_t whisper_engine::segmentEndMs(int index) const {
    if (!_whisper_state) return 0;
    return whisper_full_get_segment_t1_from_state(_whisper_state, index) * 10;
}

void whisper_engine::clear_error() { _lastError = 0; }

void whisper_engine::setError(int error) { _lastError = error; }
