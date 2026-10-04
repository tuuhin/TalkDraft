#include <android/asset_manager_jni.h>
#include <cstring>
#include <jni.h>
#include <string>

#include "onnx_zipformer.h"

extern "C" {

JNIEXPORT jlong JNICALL Java_com_sam_talkdraft_transcription_1android_NativeZipFormer_initializeNative(
    JNIEnv* env, jobject, jstring encoder_path, jstring decoder_path, jstring joiner_path, jstring tokens_path) {
    const char* c_encoder = env->GetStringUTFChars(encoder_path, nullptr);
    const char* c_decoder = env->GetStringUTFChars(decoder_path, nullptr);
    const char* c_joiner  = env->GetStringUTFChars(joiner_path, nullptr);
    const char* c_tokens  = env->GetStringUTFChars(tokens_path, nullptr);

    auto* instance = new zip_former();
    bool success   = instance->Initialize(c_encoder, c_decoder, c_joiner, c_tokens);

    env->ReleaseStringUTFChars(encoder_path, c_encoder);
    env->ReleaseStringUTFChars(decoder_path, c_decoder);
    env->ReleaseStringUTFChars(joiner_path, c_joiner);
    env->ReleaseStringUTFChars(tokens_path, c_tokens);

    if (!success) {
        delete instance;
        return 0L;
    }

    return reinterpret_cast<jlong>(instance);
}

JNIEXPORT jobject JNICALL Java_com_sam_talkdraft_transcription_1android_NativeZipFormer_processNativeDirectBuffer(
    JNIEnv* env, jobject, jlong handle, jobject direct_buffer, jint length) {
    auto* instance = reinterpret_cast<zip_former*>(handle);
    if (!instance || !direct_buffer || length == 0) return nullptr;

    auto* samples               = static_cast<float*>(env->GetDirectBufferAddress(direct_buffer));
    transcription_result result = instance->ProcessPCM(samples, length);
    if (result.is_empty()) return nullptr;

    jclass klass = env->FindClass("com/sam/talkdraft/transcription_android/models/JniZipFormerSegment");
    if (klass == nullptr) return nullptr;

    jmethodID init = env->GetMethodID(klass, "<init>", "(JLjava/lang/String;JJ)V");
    if (init == nullptr) return nullptr;

    jstring j_text = env->NewStringUTF(result.text.c_str());
    jobject return_result =
        env->NewObject(klass, init, static_cast<jlong>(result.segment_id), j_text,
                       static_cast<jlong>(result.start_time_millis), static_cast<jlong>(result.end_time_millis));

    env->DeleteLocalRef(j_text);
    return return_result;
}

JNIEXPORT void JNICALL Java_com_sam_talkdraft_transcription_1android_NativeZipFormer_resetNative(JNIEnv* env, jobject,
                                                                                                 jlong handle) {
    auto* instance = reinterpret_cast<zip_former*>(handle);
    if (instance) instance->Reset();
}

JNIEXPORT void JNICALL Java_com_sam_talkdraft_transcription_1android_NativeZipFormer_destroyNative(JNIEnv* env, jobject,
                                                                                                   jlong handle) {
    auto* instance = reinterpret_cast<zip_former*>(handle);
    delete instance;
}
}
