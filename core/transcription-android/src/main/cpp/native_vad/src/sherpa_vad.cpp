#include "sherpa_vad.h"
#include <android/log.h>

#define LOG_TAG "ONNX_VAD"
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOG_W(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

sherpa_vad::sherpa_vad(const Config& c) {
    SherpaOnnxVadModelConfig cfg = {};

    cfg.silero_vad.model                = c.model_path.c_str();
    cfg.silero_vad.threshold            = c.threshold;
    cfg.silero_vad.min_silence_duration = c.min_silence_seconds;
    cfg.silero_vad.min_speech_duration  = c.min_speech_seconds;
    cfg.silero_vad.max_speech_duration  = c.max_speech_seconds;
    cfg.silero_vad.window_size          = (c.sample_rate == 8000) ? 256 : 512;

    cfg.sample_rate = c.sample_rate;
    cfg.num_threads = c.num_threads;
    cfg.provider    = "cpu";
    cfg.debug       = 0;

    // Returns nullptr if the model can't be loaded.
    _vad = SherpaOnnxCreateVoiceActivityDetector(&cfg, c.buffer_seconds);
    if (_vad == nullptr) LOG_W("FAILED TO SETUP ONNX VOICE ACTIVITY DETECTOR");
}

sherpa_vad::~sherpa_vad() {
    if (_vad == nullptr) {
        if (_vad == nullptr) LOG_W("INSTANCE ALREADY RELEASED");
        return;
    }
    SherpaOnnxDestroyVoiceActivityDetector(_vad);
    _vad = nullptr;
}

bool sherpa_vad::accept(const float* samples, size_t count) {
    std::lock_guard<std::mutex> lock(_lock);
    if (!_vad || !samples || count == 0) {
        LOG_D("SAMPLES CANNOT BE PROCESSED");
        return false;
    }
    SherpaOnnxVoiceActivityDetectorAcceptWaveform(_vad, samples, static_cast<int32_t>(count));
    return SherpaOnnxVoiceActivityDetectorDetected(_vad) != 0;
}

bool sherpa_vad::is_speech() {
    std::lock_guard<std::mutex> lock(_lock);
    if (_vad == nullptr) {
        LOG_D("MISSING VAD SETUP");
        return false;
    }
    return SherpaOnnxVoiceActivityDetectorDetected(_vad) != 0;
}

bool sherpa_vad::pop_segment(std::vector<float>& out, int32_t& start_sample) {
    std::lock_guard<std::mutex> lock(_lock);
    if (_vad == nullptr) {
        LOG_D("MISSING VAD SETUP");
        return false;
    }
    if (SherpaOnnxVoiceActivityDetectorEmpty(_vad)) {
        LOG_D("NO VOICE ACTIVITY DETECTED");
        return false;
    }

    const SherpaOnnxSpeechSegment* seg = SherpaOnnxVoiceActivityDetectorFront(_vad);
    if (!seg) return false;

    out.assign(seg->samples, seg->samples + seg->n);
    start_sample = seg->start;

    SherpaOnnxDestroySpeechSegment(seg);
    SherpaOnnxVoiceActivityDetectorPop(_vad);
    return true;
}

void sherpa_vad::flush() {
    std::lock_guard<std::mutex> lock(_lock);
    if (_vad == nullptr) {
        LOG_D("MISSING VAD SETUP");
        return;
    }
    SherpaOnnxVoiceActivityDetectorFlush(_vad);
}

void sherpa_vad::reset() {
    std::lock_guard<std::mutex> lock(_lock);
    if (_vad == nullptr) {
        LOG_D("MISSING VAD SETUP");
        return;
    }
    SherpaOnnxVoiceActivityDetectorReset(_vad);
}
