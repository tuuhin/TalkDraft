#include "compression_engine.h"
#include "jni_utils.h"
#include <jni.h>
#include <string>

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
    }

    scoped_jni_string input(env, input_path);
    scoped_jni_string output(env, output_path);

    if (!input || !output) {
        throwCompressionException(env, "Failed to access native file paths");
        return JNI_FALSE;
    }

    auto result = compression_engine::compressBzip2(input.get(), output.get());

    if (!result.success) {
        throwCompressionException(env, result.error);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_sam_talkdraft_archive_1android_NativeBzip2Compressor_decompressBzip2(
    JNIEnv* env, jobject thiz, jstring input_path, jstring output_path) {
    if (input_path == nullptr || output_path == nullptr) {
        throwCompressionException(env, "Input path and output path must not be null");
        return JNI_FALSE;
    }

    scoped_jni_string input(env, input_path);
    scoped_jni_string output(env, output_path);

    if (!input || !output) {
        throwCompressionException(env, "Failed to access native file paths");
        return JNI_FALSE;
    }

    auto result = compression_engine::decompressBzip2(input.get(), output.get());

    if (!result.success) {
        throwCompressionException(env, result.error);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
