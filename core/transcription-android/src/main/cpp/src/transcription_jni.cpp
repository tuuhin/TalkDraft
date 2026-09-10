#include "whisper.h"
#include "whisper_ctx.h"
#include <algorithm>
#include <android/log.h>
#include <cmath>
#include <jni.h>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#define LOG_TAG "NativeWhisperJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

bool isDebugBuild() {
#if !defined(NDEBUG) || defined(_DEBUG) || defined(DEBUG)
    return true;
#else
    return false;
#endif
}

JNIEXPORT jlong JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_initializeNative(
    JNIEnv* env, jobject thiz, jstring model_path, jstring language, jboolean use_gpu) {
    LOGI("INITIALIZING NATIVE WHISPER INSTANCE");

    if (!model_path) {
        LOGE("CRITICAL: MODEL PATH IS NULL!");
        return 0L;
    }

    const char* c_model_path = env->GetStringUTFChars(model_path, nullptr);
    const char* c_language   = language ? env->GetStringUTFChars(language, nullptr) : nullptr;

    if (!c_model_path) {
        LOGE("CRITICAL: MODEL PATH CONVERSION FAILED!");
        if (c_language) env->ReleaseStringUTFChars(language, c_language);
        return 0L;
    }

    whisper_context_params params = whisper_context_default_params();
    params.use_gpu                = use_gpu;

    LOGI("LOADING MODEL FROM PATH : %s IS_USING_GPU: %s", c_model_path, use_gpu ? "ENABLED" : "DISABLED");

    whisper_context* ctx = whisper_init_from_file_with_params(c_model_path, params);
    env->ReleaseStringUTFChars(model_path, c_model_path);

    if (!ctx) {
        LOGE("CRITICAL: FAILED TO INITIALIZE WHISPER CONTEXT FROM PATH");
        if (c_language) env->ReleaseStringUTFChars(language, c_language);
        return 0L;
    }

    whisper_state* state = whisper_init_state(ctx);
    if (!state) {
        LOGE("CRITICAL: FAILED TO CREATE WHISPER STATE!");
        whisper_free(ctx);
        if (c_language) env->ReleaseStringUTFChars(language, c_language);
        return 0L;
    }

    auto* wrapper     = new whisper_context_wrapper();
    wrapper->ctx      = ctx;
    wrapper->state    = state;
    wrapper->language = (c_language && strlen(c_language) > 0) ? std::string(c_language) : "en";

    if (c_language) env->ReleaseStringUTFChars(language, c_language);

    LOGI("=== NATIVE WHISPER INITIALIZATION COMPLETE. WRAPPER AT: %p ===", static_cast<void*>(wrapper));
    return reinterpret_cast<jlong>(wrapper);
}

JNIEXPORT jboolean JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_processNativeDirectBuffer(
    JNIEnv* env, jobject thiz, jlong handle, jobject direct_buffer, jint sample_count) {
    auto* wrapper = reinterpret_cast<whisper_context_wrapper*>(handle);
    if (!wrapper || !wrapper->ctx || !wrapper->state) {
        LOGE("PROCESS ERROR: WRAPPER OR CONTEXT IS NULL!");
        return JNI_FALSE;
    }

    std::lock_guard<std::mutex> lock(wrapper->mtx);
    wrapper->clear_error();

    if (sample_count < 16000) {
        LOGE("PROCESS WARNING: SAMPLE COUNT IS SMALL (%d samples). WHISPER REQUIRES AT LEAST 16000 SAMPLES (1s)",
             sample_count);
        wrapper->set_error(3); // INVALID_BUFFER
        return JNI_FALSE;
    }

    // Safety check buffer capacity
    jlong capacity_bytes = env->GetDirectBufferCapacity(direct_buffer);
    if (capacity_bytes < 0 || capacity_bytes < static_cast<jlong>(sample_count * sizeof(float))) {
        LOGE("PROCESS ERROR: DIRECT BUFFER CAPACITY TOO SMALL (%lld bytes, NEEDED %zu)",
             static_cast<long long>(capacity_bytes), sample_count * sizeof(float));
        wrapper->set_error(3); // INVALID_BUFFER
        return JNI_FALSE;
    }

    auto* pcm32 = static_cast<float*>(env->GetDirectBufferAddress(direct_buffer));
    if (!pcm32) {
        LOGE("PROCESS ERROR: DIRECT BUFFER POINTER IS NULL OR NOT A DIRECT BYTE BUFFER!");
        wrapper->set_error(3); // INVALID_BUFFER
        return JNI_FALSE;
    }

    if (isDebugBuild()) {
        float max_amp = 0.0f;
        for (int i = 0; i < std::min(sample_count, 1000); ++i) {
            max_amp = std::max(max_amp, std::abs(pcm32[i]));
        }

        LOGD("AUDIO INPUT: SAMPLE_COUNT=%d, FIRST_SAMPLE=%.4f, MAX_AMPLITUDE=%.4f", sample_count, pcm32[0], max_amp);
        if (max_amp > 1.0f) LOGE("WARNING: AUDIO VALUES ARE OUT OF RANGE [-1.0, 1.0]");
    }

    // Set Up Inference Options
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    params.n_threads        = 2;
    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.no_context       = true;
    params.no_speech_thold  = 0.4f;
    params.language         = wrapper->language.c_str();

    params.single_segment = true;
    params.greedy.best_of = 1;
    params.temperature    = 0.0f;
    params.audio_ctx      = 0;

    LOGD("WHISPER PARAMS: n_threads=%d, language=%s, no_context=%d, no_speech_thold=%.2f, "
         "greedy_best_of=%d, temperature=%.2f, single_segment=%d, translate=%d",
         params.n_threads, params.language, params.no_context, params.no_speech_thold, params.greedy.best_of,
         params.temperature, params.single_segment, params.translate);

    LOGD("TIME TO PROCESS IT");

    if (whisper_full_with_state(wrapper->ctx, wrapper->state, params, pcm32, sample_count) != 0) {
        LOGE("CRITICAL: WHISPER INFERENCE FAILED!");
        wrapper->set_error(2); // INFERENCE_FAILED
        return JNI_FALSE;
    }

    std::string full_text;
    std::string latest_segment;

    int n_segments = whisper_full_n_segments_from_state(wrapper->state);
    LOGD("INFERENCE COMPLETED: %d SEGMENTS GENERATED", n_segments);

    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text_from_state(wrapper->state, i);
        if (!text) continue;

        full_text += text;
        if (i == n_segments - 1) latest_segment = text;
    }

    wrapper->accumulated_text = full_text;
    wrapper->last_segment     = latest_segment;

    return JNI_TRUE;
}

JNIEXPORT jobject JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readStateNative(JNIEnv* env,
                                                                                                      jobject thiz,
                                                                                                      jlong handle) {
    auto* wrapper = reinterpret_cast<whisper_context_wrapper*>(handle);
    if (!wrapper) return nullptr;

    std::lock_guard<std::mutex> lock(wrapper->mtx);

    jclass stateClass     = env->FindClass("com/sam/talkdraft/transcription_android/models/WhisperState");
    jclass segmentClass   = env->FindClass("com/sam/talkdraft/transcription_android/models/WhisperSegment");
    jclass arrayListClass = env->FindClass("java/util/ArrayList");

    if (!stateClass || !segmentClass || !arrayListClass) return nullptr;

    jmethodID arrayListInit = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID arrayListAdd  = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");

    jmethodID segmentInit = env->GetMethodID(segmentClass, "<init>", "(JJLjava/lang/String;)V");
    jmethodID stateInit   = env->GetMethodID(stateClass, "<init>", "(Ljava/lang/String;Ljava/util/List;)V");

    if (!segmentInit || !stateInit) return nullptr;

    jobject segmentList = env->NewObject(arrayListClass, arrayListInit);

    int n_segments = whisper_full_n_segments_from_state(wrapper->state);
    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text_from_state(wrapper->state, i);
        if (!text) continue;

        int64_t t0_ms = whisper_full_get_segment_t0_from_state(wrapper->state, i) * 10;
        int64_t t1_ms = whisper_full_get_segment_t1_from_state(wrapper->state, i) * 10;

        jstring jtext = env->NewStringUTF(text);

        jobject segObj = env->NewObject(segmentClass, segmentInit, (jlong)t0_ms, (jlong)t1_ms, jtext);

        if (segObj) {
            env->CallBooleanMethod(segmentList, arrayListAdd, segObj);
            env->DeleteLocalRef(segObj);
        }
        env->DeleteLocalRef(jtext);
    }

    jstring fullTextObj = env->NewStringUTF(wrapper->accumulated_text.c_str());
    jobject result      = env->NewObject(stateClass, stateInit, fullTextObj, segmentList);

    env->DeleteLocalRef(stateClass);
    env->DeleteLocalRef(segmentClass);
    env->DeleteLocalRef(arrayListClass);
    env->DeleteLocalRef(fullTextObj);
    env->DeleteLocalRef(segmentList);

    return result;
}

JNIEXPORT jint JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readErrorNative(JNIEnv* env,
                                                                                                   jobject thiz,
                                                                                                   jlong handle) {
    auto* wrapper = reinterpret_cast<whisper_context_wrapper*>(handle);
    if (!wrapper) return -1;

    std::lock_guard<std::mutex> lock(wrapper->mtx);
    return wrapper->last_error_code;
}

JNIEXPORT void JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_destroyNative(JNIEnv* env,
                                                                                                 jobject thiz,
                                                                                                 jlong handle) {
    auto* wrapper = reinterpret_cast<whisper_context_wrapper*>(handle);
    if (!wrapper) return;

    {
        std::lock_guard<std::mutex> lock(wrapper->mtx);
        if (wrapper->state) {
            whisper_free_state(wrapper->state);
            wrapper->state = nullptr;
        }
        if (wrapper->ctx) {
            whisper_free(wrapper->ctx);
            wrapper->ctx = nullptr;
        }
    }

    delete wrapper;
    LOGI("=== NATIVE WHISPER INSTANCE DESTROYED CLEANLY ===");
}
}
