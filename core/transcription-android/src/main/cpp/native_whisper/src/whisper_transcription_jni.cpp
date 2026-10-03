#include "whisper_engine_manager.h"
#include <android/log.h>
#include <jni.h>

#define LOG_TAG "NativeWhisperJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_initializeNative(
    JNIEnv* env, jobject thiz, jstring model_path, jstring language, jboolean use_gpu) {
    if (!model_path) {
        LOGE("MISSING MODEL PATH");
        return 0L;
    }

    const char* modelPath = env->GetStringUTFChars(model_path, nullptr);

    if (!modelPath) {
        LOGE("MODEL PATH CONVERSION FAILED");
        return 0L;
    }

    const char* languageValue = language ? env->GetStringUTFChars(language, nullptr) : nullptr;

    const auto handle = whisper_engine_manager::instance().create_engine(
        modelPath, languageValue ? languageValue : "en", use_gpu == JNI_TRUE);

    env->ReleaseStringUTFChars(model_path, modelPath);
    if (languageValue) env->ReleaseStringUTFChars(language, languageValue);

    if (handle == whisper_engine_manager::INVALID_HANDLE) {
        LOGE("FAILED TO CREATE WHISPER ENGINE");
        return 0L;
    }
    return static_cast<jlong>(handle);
}

JNIEXPORT jboolean JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_processNativeDirectBuffer(
    JNIEnv* env, jobject thiz, jlong handle, jobject direct_buffer, jint sample_count) {

    auto engine = whisper_engine_manager::instance().read_engine_from_handle(handle);

    if (!engine) {
        LOGE("INVALID WHISPER ENGINE HANDLE: %lld", static_cast<long long>(handle));
        return JNI_FALSE;
    }

    if (sample_count < 16000) {
        LOGE("SAMPLE COUNT TOO SMALL: %d", sample_count);
        return JNI_FALSE;
    }

    const jlong capacity = env->GetDirectBufferCapacity(direct_buffer);

    if (capacity < 0 || capacity < static_cast<jlong>(sample_count * sizeof(float))) {
        LOGE("DIRECT BUFFER TOO SMALL: capacity=%lld required=%lld", static_cast<long long>(capacity),
             static_cast<long long>(sample_count * sizeof(float)));
        return JNI_FALSE;
    }

    auto* pcm = static_cast<float*>(env->GetDirectBufferAddress(direct_buffer));

    if (!pcm) {
        LOGE("DIRECT BUFFER ADDRESS IS NULL");
        return JNI_FALSE;
    }
    return engine->process(pcm, sample_count) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readStateNative(JNIEnv* env,
                                                                                                      jobject thiz,
                                                                                                      jlong handle) {

    auto engine = whisper_engine_manager::instance().read_engine_from_handle(handle);

    if (!engine) {
        LOGE("INVALID WHISPER ENGINE HANDLE");
        return nullptr;
    }

    jclass segmentClass = env->FindClass("com/sam/talkdraft/transcription_android/models/JniWhisperSegment");

    if (!segmentClass) return nullptr;
    jmethodID segmentInit = env->GetMethodID(segmentClass, "<init>", "(Ljava/lang/String;JJ)V");

    if (!segmentInit) {
        env->DeleteLocalRef(segmentClass);
        return nullptr;
    }

    const int count = engine->segmentCount();

    if (count <= 0) {
        env->DeleteLocalRef(segmentClass);
        return nullptr;
    }

    const int index  = count - 1;
    const char* text = engine->segmentText(index);

    if (!text) {
        env->DeleteLocalRef(segmentClass);
        return nullptr;
    }

    jstring jText = env->NewStringUTF(text);

    if (!jText) {
        env->DeleteLocalRef(segmentClass);
        return nullptr;
    }

    const auto startTimeMs = static_cast<jlong>(engine->segmentStartMs(index));
    const auto endTimeMs   = static_cast<jlong>(engine->segmentEndMs(index));
    jobject result         = env->NewObject(segmentClass, segmentInit, jText, startTimeMs, endTimeMs);
    env->DeleteLocalRef(jText);
    env->DeleteLocalRef(segmentClass);

    return result;
}

JNIEXPORT jint JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readErrorNative(JNIEnv* env,
                                                                                                   jobject thiz,
                                                                                                   jlong handle) {
    auto engine = whisper_engine_manager::instance().read_engine_from_handle(handle);
    if (!engine) return -1;
    return static_cast<jint>(engine->lastError());
}

JNIEXPORT void JNICALL Java_com_sam_talkdraft_transcription_1android_NativeWhisper_destroyNative(JNIEnv* env,
                                                                                                 jobject thiz,
                                                                                                 jlong handle) {
    whisper_engine_manager::instance().destroy_engine(handle);
}
}
