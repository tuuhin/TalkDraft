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
    JNIEnv* env, jobject thiz, jstring input_path, jstring output_path, jobject progress_listener) {
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

    jobject global_listener      = nullptr;
    jmethodID on_progress_method = nullptr;

    if (progress_listener != nullptr) {
        global_listener       = env->NewGlobalRef(progress_listener);
        jclass listener_class = env->GetObjectClass(progress_listener);
        if (listener_class != nullptr) {
            on_progress_method = env->GetMethodID(listener_class, "onProgress", "(F)V");
            env->DeleteLocalRef(listener_class);
        }
    }

    auto progressCallback = [env, global_listener, on_progress_method](float percentage) {
        if (global_listener != nullptr && on_progress_method != nullptr) {
            env->CallVoidMethod(global_listener, on_progress_method, percentage);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
        }
    };

    auto result = compression_engine::compressBzip2(input.get(), output.get(), progressCallback);

    if (global_listener != nullptr) {
        env->DeleteGlobalRef(global_listener);
    }

    if (!result.success) {
        throwCompressionException(env, result.error);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_sam_talkdraft_archive_1android_NativeBzip2Compressor_decompressBzip2(
    JNIEnv* env, jobject thiz, jstring input_path, jstring output_path, jobject progress_listener) {
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

    jobject global_listener = nullptr;
    jmethodID on_progress   = nullptr;

    if (progress_listener != nullptr) {
        global_listener = env->NewGlobalRef(progress_listener);
        jclass klass    = env->GetObjectClass(progress_listener);
        if (klass != nullptr) {
            on_progress = env->GetMethodID(klass, "onProgress", "(F)V");
            env->DeleteLocalRef(klass);
        }
    }

    auto progressCallback = [env, global_listener, on_progress](float percentage) {
        if (global_listener == nullptr || on_progress == nullptr) return;
        // call the progress listener
        env->CallVoidMethod(global_listener, on_progress, percentage);
        // check for exception and clear
        if (env->ExceptionCheck()) env->ExceptionClear();
    };

    auto result = compression_engine::decompressBzip2(input.get(), output.get(), progressCallback);

    // clear the listener ref
    if (global_listener != nullptr) env->DeleteGlobalRef(global_listener);

    if (!result.success) {
        throwCompressionException(env, result.error);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
