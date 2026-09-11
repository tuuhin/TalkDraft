#include <jni.h>
#include <string>

#include "onnx_zipformer.h"

extern "C" {

JNIEXPORT jlong JNICALL Java_com_example_transcription_NativeZipformer_initializeNative(
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

JNIEXPORT jstring JNICALL Java_com_example_transcription_NativeZipformer_processNativeDirectBuffer(
    JNIEnv* env, jobject, jlong handle, jobject direct_buffer, jint length) {
    auto* instance = reinterpret_cast<zip_former*>(handle);
    if (!instance) return env->NewStringUTF("");

    auto* samples    = static_cast<float*>(env->GetDirectBufferAddress(direct_buffer));
    std::string text = instance->ProcessPCM(samples, length);

    return env->NewStringUTF(text.c_str());
}

JNIEXPORT void JNICALL Java_com_example_transcription_NativeZipformer_resetNative(JNIEnv* env, jobject, jlong handle) {
    auto* instance = reinterpret_cast<zip_former*>(handle);
    if (instance) instance->Reset();
}

JNIEXPORT void JNICALL Java_com_example_transcription_NativeZipformer_destroyNative(JNIEnv* env, jobject,
                                                                                    jlong handle) {
    auto* instance = reinterpret_cast<zip_former*>(handle);
    delete instance;
}
}
