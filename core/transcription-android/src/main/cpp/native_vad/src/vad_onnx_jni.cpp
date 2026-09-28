#include "sherpa_vad_manager.h"
#include <jni.h>
#include <vector>

extern "C" JNIEXPORT jlong JNICALL Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_initNative(
    JNIEnv* env, jobject thiz, jstring model_path, jobject config) {

    if (config == nullptr) return -1;

    jclass config_class            = env->GetObjectClass(config);
    jfieldID sampleRateField       = env->GetFieldID(config_class, "sampleRate", "I");
    jfieldID silenceThresholdField = env->GetFieldID(config_class, "silenceThreshold", "F");
    jfieldID minSilenceField       = env->GetFieldID(config_class, "minSilenceInSeconds", "F");
    jfieldID minSpeechField        = env->GetFieldID(config_class, "minSpeechInSeconds", "F");
    jfieldID maxSpeechField        = env->GetFieldID(config_class, "maxSpeechInSeconds", "F");
    jfieldID noOfThreadsField      = env->GetFieldID(config_class, "noOfThreads", "I");
    jfieldID bufferSecondsField    = env->GetFieldID(config_class, "bufferSeconds", "F");

    if (!sampleRateField || !silenceThresholdField || !minSilenceField || !minSpeechField || !maxSpeechField ||
        !noOfThreadsField || !bufferSecondsField) {
        env->DeleteLocalRef(config_class);
        return 0;
    }

    jint sampleRate            = env->GetIntField(config, sampleRateField);
    jfloat silenceThreshold    = env->GetFloatField(config, silenceThresholdField);
    jfloat minSilenceInSeconds = env->GetFloatField(config, minSilenceField);
    jfloat minSpeechInSeconds  = env->GetFloatField(config, minSpeechField);
    jfloat maxSpeechInSeconds  = env->GetFloatField(config, maxSpeechField);
    jint noOfThreads           = env->GetIntField(config, noOfThreadsField);
    jfloat bufferSeconds       = env->GetFloatField(config, bufferSecondsField);

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    sherpa_vad::Config nativeConfig;

    nativeConfig.model_path          = std::string(path);
    nativeConfig.sample_rate         = sampleRate;
    nativeConfig.threshold           = silenceThreshold;
    nativeConfig.min_silence_seconds = minSilenceInSeconds;
    nativeConfig.min_speech_seconds  = minSpeechInSeconds;
    nativeConfig.max_speech_seconds  = maxSpeechInSeconds;
    nativeConfig.num_threads         = noOfThreads;
    nativeConfig.buffer_seconds      = bufferSeconds;

    env->ReleaseStringUTFChars(model_path, path);
    auto handle = sherpa_vad_manager::instance().create_vad(nativeConfig);

    // delete local ref  and config class
    env->DeleteLocalRef(config_class);

    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_initFromAssets(
    JNIEnv* env, jobject thiz, jobject assetsManager, jstring cacheDir, jstring model_name, jobject config) {

    if (config == nullptr) return -1;

    jclass config_class            = env->GetObjectClass(config);
    jfieldID sampleRateField       = env->GetFieldID(config_class, "sampleRate", "I");
    jfieldID silenceThresholdField = env->GetFieldID(config_class, "silenceThreshold", "F");
    jfieldID minSilenceField       = env->GetFieldID(config_class, "minSilenceInSeconds", "F");
    jfieldID minSpeechField        = env->GetFieldID(config_class, "minSpeechInSeconds", "F");
    jfieldID maxSpeechField        = env->GetFieldID(config_class, "maxSpeechInSeconds", "F");
    jfieldID noOfThreadsField      = env->GetFieldID(config_class, "noOfThreads", "I");
    jfieldID bufferSecondsField    = env->GetFieldID(config_class, "bufferSeconds", "F");

    if (!sampleRateField || !silenceThresholdField || !minSilenceField || !minSpeechField || !maxSpeechField ||
        !noOfThreadsField || !bufferSecondsField) {
        env->DeleteLocalRef(config_class);
        return 0;
    }

    jint sampleRate            = env->GetIntField(config, sampleRateField);
    jfloat silenceThreshold    = env->GetFloatField(config, silenceThresholdField);
    jfloat minSilenceInSeconds = env->GetFloatField(config, minSilenceField);
    jfloat minSpeechInSeconds  = env->GetFloatField(config, minSpeechField);
    jfloat maxSpeechInSeconds  = env->GetFloatField(config, maxSpeechField);
    jint noOfThreads           = env->GetIntField(config, noOfThreadsField);
    jfloat bufferSeconds       = env->GetFloatField(config, bufferSecondsField);

    const char* model_file_name       = env->GetStringUTFChars(model_name, nullptr);
    const char* cache_dir_path_string = env->GetStringUTFChars(cacheDir, nullptr);

    AAssetManager* mgr = AAssetManager_fromJava(env, assetsManager);

    sherpa_vad::Config nativeConfig;
    // model file name is being passed but will be mutated inside the caller with model path
    nativeConfig.model_path          = model_file_name;
    nativeConfig.sample_rate         = sampleRate;
    nativeConfig.threshold           = silenceThreshold;
    nativeConfig.min_silence_seconds = minSilenceInSeconds;
    nativeConfig.min_speech_seconds  = minSpeechInSeconds;
    nativeConfig.max_speech_seconds  = maxSpeechInSeconds;
    nativeConfig.num_threads         = noOfThreads;
    nativeConfig.buffer_seconds      = bufferSeconds;

    env->ReleaseStringUTFChars(model_name, model_file_name);
    env->ReleaseStringUTFChars(cacheDir, cache_dir_path_string);
    auto handle = sherpa_vad_manager::instance().create_vad_from_asset(mgr, cache_dir_path_string, nativeConfig);

    // delete local ref config class
    env->DeleteLocalRef(config_class);

    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_popNativeSegmentFromSpeech(JNIEnv* env,
                                                                                                     jobject /*thiz*/,
                                                                                                     jlong handle) {
    auto vad = sherpa_vad_manager::instance().create_from_handle(handle);
    if (!vad) return nullptr;

    std::vector<float> samples;
    int32_t start = 0;
    if (!vad->pop_segment(samples, start)) return nullptr;

    jfloatArray arr = env->NewFloatArray(static_cast<jsize>(samples.size()));
    if (!arr) return nullptr;
    env->SetFloatArrayRegion(arr, 0, static_cast<jsize>(samples.size()), samples.data());
    return arr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_flushNative(JNIEnv*, jobject, jlong handle) {
    auto vad = sherpa_vad_manager::instance().create_from_handle(handle);
    if (vad) vad->flush();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_processNativeDirectBuffer(
    JNIEnv* env, jobject thiz, jlong handle, jobject direct_buffer, jint sample_count) {

    auto vad = sherpa_vad_manager::instance().create_from_handle(handle);
    if (!vad) return false;
    auto* pcm = static_cast<float*>(env->GetDirectBufferAddress(direct_buffer));
    if (!pcm) return false;

    auto probability = vad->accept(pcm, static_cast<size_t>(sample_count)) ? 1.0f : 0.0f;
    return static_cast<jboolean>(probability);
}

extern "C" JNIEXPORT void JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_resetStatesNative(JNIEnv* env, jobject thiz,
                                                                                            jlong handle) {

    auto vad = sherpa_vad_manager::instance().create_from_handle(handle);
    if (vad) vad->reset();
}

extern "C" JNIEXPORT void JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_destroyNative(JNIEnv* env, jobject thiz,
                                                                                        jlong handle) {
    sherpa_vad_manager::instance().destroy_vad(handle);
}
