#include "sherpa_vad.h"
#include <android/log.h>

#define LOG_TAG "SHERPA_ONNX_VAD"
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOG_W(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#if defined(DEBUG) || !defined(NDEBUG)
#define IS_DEBUG 1
#else
#define IS_DEBUG 0
#endif

sherpa_vad::sherpa_vad(const Config& c) {
    SherpaOnnxVadModelConfig cfg = {};

    // Silero VAD settings
    cfg.silero_vad.model                = c.model_path.c_str();
    cfg.silero_vad.threshold            = c.threshold;
    cfg.silero_vad.min_silence_duration = c.min_silence_seconds;
    cfg.silero_vad.min_speech_duration  = c.min_speech_seconds;
    cfg.silero_vad.max_speech_duration  = c.max_speech_seconds;
    cfg.silero_vad.window_size          = (c.sample_rate == 8000) ? 256 : 512;

    // General configuration
    cfg.sample_rate = c.sample_rate;
    cfg.num_threads = c.num_threads;
    cfg.provider    = "cpu";
    cfg.debug       = IS_DEBUG;

    LOG_I("CREATING ACTIVITY DETECTOR BUFFER :%.2f", c.buffer_seconds);
    _vad = SherpaOnnxCreateVoiceActivityDetector(&cfg, c.buffer_seconds);

    if (_vad == nullptr) {
        LOG_E("FAILED TO CREATED SHERPA ONNX VOICE DETECTOR", c.model_path.c_str(), c.sample_rate);
    } else {
        LOG_I("SUCCESSFULLY CREATED VAD (%p) at %d Hz", _vad, c.sample_rate);
    }
}

sherpa_vad::~sherpa_vad() {
    if (_vad == nullptr) return;
    SherpaOnnxDestroyVoiceActivityDetector(_vad);

    LOG_I("Destroyed Sherpa VAD instance (%p)", _vad);
    _vad = nullptr;
}

bool sherpa_vad::accept(const float* samples, size_t count) {
    std::lock_guard<std::mutex> lock(_lock);
    if (_vad == nullptr) {
        LOG_E("accept(): VAD INSTANCE IS NOT SET, TO ACCEPT SAMPLES");
        return false;
    }
    if (!samples || count == 0) {
        LOG_W("BUFFER IS EMPTY");
        return false;
    }
    SherpaOnnxVoiceActivityDetectorAcceptWaveform(_vad, samples, static_cast<int32_t>(count));
    return SherpaOnnxVoiceActivityDetectorDetected(_vad) != 0;
}

bool sherpa_vad::pop_segment(std::vector<float>& out, int32_t& start_sample, int32_t& end_sample) {
    std::lock_guard<std::mutex> lock(_lock);
    if (_vad == nullptr) {
        LOG_E("pop_segment(): VAD INSTANCE IS NOT SET, SET INSTANCE TO POP OUT THE SEGMENTS");
        return false;
    }
    // speech queue is empty skip
    if (SherpaOnnxVoiceActivityDetectorEmpty(_vad)) return false;

    // now read in the segments
    const SherpaOnnxSpeechSegment* seg = SherpaOnnxVoiceActivityDetectorFront(_vad);
    if (!seg) {
        LOG_W("NO SEGMENTS AT THE FRONT");
        return false;
    }

    out.assign(seg->samples, seg->samples + seg->n);
    // segment start and end
    start_sample = seg->start;
    end_sample   = seg->start + seg->n;

    LOG_I("POPPED SEGMENT: START=%d, END=%d", seg->start, seg->start + seg->n);

    SherpaOnnxDestroySpeechSegment(seg);
    SherpaOnnxVoiceActivityDetectorPop(_vad);
    return true;
}

void sherpa_vad::flush() {
    std::lock_guard<std::mutex> lock(_lock);
    if (_vad == nullptr) {
        LOG_E("flush(): VAD INSTANCE IS NOT SET, SET INSTANCE TO FLUSH");
        return;
    }
    SherpaOnnxVoiceActivityDetectorFlush(_vad);
    LOG_D("FLUSHED SAMPLES TO BUFFER");
}

void sherpa_vad::reset() {
    std::lock_guard<std::mutex> lock(_lock);
    if (_vad == nullptr) {
        LOG_E("reset(): VAD INSTANCE IS NOT SET, SET INSTANCE TO RESET");
        return;
    }
    SherpaOnnxVoiceActivityDetectorReset(_vad);
    LOG_D("VAD DETECTOR REST");
}
