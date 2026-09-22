#include "compression_engine.h"
#include <jni.h>

namespace {

void throwCompressionException(JNIEnv* env, const std::string& message) {
    jclass exceptionClass = env->FindClass("com/sam/talkdraft/archive_android/exceptions/NativeCompressionException");
    if (exceptionClass == nullptr) return;
    env->ThrowNew(exceptionClass, message.c_str());
    env->DeleteLocalRef(exceptionClass);
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL Java_com_sam_talkdraft_archive_1android_NativeBzip2Compressor_compressBzip2(
    JNIEnv* env, jobject thiz, jstring input_path, jstring output_path) {
    if (input_path == nullptr || output_path == nullptr) {
        throwCompressionException(env, "Input path and output path must not be null");

        return JNI_FALSE;
        return JNI_FALSE;
    }

    const char* input  = env->GetStringUTFChars(input_path, nullptr);
    const char* output = env->GetStringUTFChars(output_path, nullptr);

    if (input == nullptr || output == nullptr) {
        if (input != nullptr) env->ReleaseStringUTFChars(input_path, input);
        if (output != nullptr) env->ReleaseStringUTFChars(output_path, output);

        throwCompressionException(env, "Failed to access native file paths");
        return JNI_FALSE;
    }

    auto result = compression_engine::compressBzip2(input, output);

    env->ReleaseStringUTFChars(input_path, input);
    env->ReleaseStringUTFChars(output_path, output);

    if (!result.success) {
        const auto error_message = result.error;
        throwCompressionException(env, error_message);
    }

    return result.success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_sam_talkdraft_archive_1android_NativeBzip2Compressor_decompressBzip2(
    JNIEnv* env, jobject thiz, jstring input_path, jstring output_path) {
    if (input_path == nullptr || output_path == nullptr) {
        throwCompressionException(env, "Input path and output path must not be null");

        return JNI_FALSE;
        return JNI_FALSE;
    }

    const char* input  = env->GetStringUTFChars(input_path, nullptr);
    const char* output = env->GetStringUTFChars(output_path, nullptr);

    if (input == nullptr || output == nullptr) {
        if (input != nullptr) env->ReleaseStringUTFChars(input_path, input);
        if (output != nullptr) env->ReleaseStringUTFChars(output_path, output);

        throwCompressionException(env, "Failed to access native file paths");
        return JNI_FALSE;
    }

    auto result = compression_engine::decompressBzip2(input, output);

    env->ReleaseStringUTFChars(input_path, input);
    env->ReleaseStringUTFChars(output_path, output);

    if (!result.success) {
        const auto error_message = result.error;
        throwCompressionException(env, error_message);
    }

    return result.success ? JNI_TRUE : JNI_FALSE;
}
