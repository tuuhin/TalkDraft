#include "sherpa_vad_manager.h"
#include <jni.h>
#include <vector>

extern "C" JNIEXPORT jlong JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_initializeNative(JNIEnv* env, jobject thiz,
                                                                                           jstring model_path,
                                                                                           jint sample_rate) {

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    sherpa_vad::Config config;
    config.model_path  = std::string(path);
    config.sample_rate = sample_rate;
    auto handle        = sherpa_vad_manager::instance().create_vad(config);
    env->ReleaseStringUTFChars(model_path, path);
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_initializeNativeFromAssets(
    JNIEnv* env, jobject thiz, jobject assetsManager, jstring cacheDir, jstring asset_name, jint sample_rate) {

    const char* asset_name_string     = env->GetStringUTFChars(asset_name, nullptr);
    const char* cache_dir_path_string = env->GetStringUTFChars(cacheDir, nullptr);
    AAssetManager* mgr                = AAssetManager_fromJava(env, assetsManager);

    const auto handle = sherpa_vad_manager::instance().create_vad_from_asset(mgr, asset_name_string,
                                                                             cache_dir_path_string, sample_rate);

    env->ReleaseStringUTFChars(asset_name, asset_name_string);
    env->ReleaseStringUTFChars(cacheDir, cache_dir_path_string);

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

extern "C" JNIEXPORT jobject JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_processNativeDirectBuffer(
    JNIEnv* env, jobject thiz, jlong handle, jobject direct_buffer, jint sample_count) {

    auto vad = sherpa_vad_manager::instance().create_from_handle(handle);
    if (!vad) return nullptr;
    auto* pcm = static_cast<float*>(env->GetDirectBufferAddress(direct_buffer));
    if (!pcm) return nullptr;

    auto probability = vad->accept(pcm, static_cast<size_t>(sample_count)) ? 1.0f : 0.0f;
    auto isSpeech    = vad->is_speech();

    // create the model
    jclass klass = env->FindClass("com/sam/talkdraft/transcription_android/models/VADResult");
    if (klass == nullptr) return nullptr;

    jmethodID init = env->GetMethodID(klass, "<init>", "(FZ)V");
    if (init == nullptr) return nullptr;

    jobject return_result = env->NewObject(klass, init, probability, isSpeech);
    return return_result;
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
