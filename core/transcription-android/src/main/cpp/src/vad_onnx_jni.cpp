#include "vad_onnx.h"
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <jni.h>
#include <vector>

extern "C" JNIEXPORT jlong JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_initializeNative(JNIEnv* env, jobject thiz,
                                                                                           jstring model_path,
                                                                                           jint sample_rate) {

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    auto* vad        = new silero_vad(std::string(path), sample_rate);
    env->ReleaseStringUTFChars(model_path, path);
    return reinterpret_cast<jlong>(vad);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_initializeNativeFromAssets(
    JNIEnv* env, jobject thiz, jobject assetsManager, jstring asset_name, jint sample_rate) {
    const char* asset_name_string = env->GetStringUTFChars(asset_name, nullptr);
    AAssetManager* mgr            = AAssetManager_fromJava(env, assetsManager);

    const auto asset_obj = AAssetManager_open(mgr, asset_name_string, AASSET_MODE_BUFFER);
    if (asset_obj == nullptr) {
        env->ReleaseStringUTFChars(asset_name, asset_name_string);
        return -1L;
    }

    size_t asset_size = AAsset_getLength(asset_obj);
    std::vector<uint8_t> buffer(asset_size);
    AAsset_read(asset_obj, buffer.data(), asset_size);
    AAsset_close(asset_obj);

    env->ReleaseStringUTFChars(asset_name, asset_name_string);
    auto* vad = new silero_vad(buffer.data(), asset_size, sample_rate);

    return reinterpret_cast<jlong>(vad);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_processNativeDirectBuffer(
    JNIEnv* env, jobject thiz, jlong handle, jobject direct_buffer, jint sample_count) {

    auto* vad        = reinterpret_cast<silero_vad*>(handle);
    auto* pcm_floats = static_cast<float*>(env->GetDirectBufferAddress(direct_buffer));
    return vad->process_frame(pcm_floats, sample_count);
}

extern "C" JNIEXPORT void JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_resetStatesNative(JNIEnv* env, jobject thiz,
                                                                                            jlong handle) {
    auto* vad = reinterpret_cast<silero_vad*>(handle);
    if (!vad) return;
    vad->reset_states();
}

extern "C" JNIEXPORT void JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeVoiceActivityDetector_destroyNative(JNIEnv* env, jobject thiz,
                                                                                        jlong handle) {
    auto* vad = reinterpret_cast<silero_vad*>(handle);
    delete vad;
}
