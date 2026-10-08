#include "punctuations_manager.h"
#include <jni.h>
#include <stdexcept>
#include <string>
#include <utility>

extern "C" JNIEXPORT jlong JNICALL Java_com_sam_talkdraft_transcription_1android_NativePunctuationWriter_initInstance(
    JNIEnv* env, jobject, jobject config) {

    if (config == nullptr) {
        return punctuation_manager::kInvalidHandle;
    }

    jclass klass = env->GetObjectClass(config);
    if (klass == nullptr) {
        return punctuation_manager::kInvalidHandle;
    }

    const jfieldID modelPathField = env->GetFieldID(klass, "modelPath", "Ljava/lang/String;");
    const jfieldID isOnlineField  = env->GetFieldID(klass, "isOnline", "Z");
    const jfieldID vocabPathField = env->GetFieldID(klass, "vocabPath", "Ljava/lang/String;");

    // if fields are missing clear delete the ref
    if (modelPathField == nullptr || isOnlineField == nullptr || vocabPathField == nullptr) {
        env->DeleteLocalRef(klass);
        return punctuation_manager::kInvalidHandle;
    }

    // read in j path
    auto j_model_path        = reinterpret_cast<jstring>(env->GetObjectField(config, modelPathField));
    const jboolean is_online = env->GetBooleanField(config, isOnlineField);
    auto j_vocab_path        = reinterpret_cast<jstring>(env->GetObjectField(config, vocabPathField));

    env->DeleteLocalRef(klass);

    // modelPath is required.
    if (j_model_path == nullptr) {
        // in case model path is missing clear the vocab path ref
        if (j_vocab_path != nullptr) env->DeleteLocalRef(j_vocab_path);
        return punctuation_manager::kInvalidHandle;
    }

    const char* model_path_chars = env->GetStringUTFChars(j_model_path, nullptr);

    if (model_path_chars == nullptr) {
        //  in case missing the model path delete ref and vocab path
        env->DeleteLocalRef(j_model_path);
        if (j_vocab_path != nullptr) env->DeleteLocalRef(j_vocab_path);
        return punctuation_manager::kInvalidHandle;
    }

    std::string model_path(model_path_chars);
    env->ReleaseStringUTFChars(j_model_path, model_path_chars);
    env->DeleteLocalRef(j_model_path);

    // vocabPath is optional.
    std::optional<std::string> vocab_path;

    if (j_vocab_path != nullptr) {
        // read in the vocab path
        const char* vocab_path_chars = env->GetStringUTFChars(j_vocab_path, nullptr);
        if (vocab_path_chars != nullptr) {
            vocab_path = std::string(vocab_path_chars);
            env->ReleaseStringUTFChars(j_vocab_path, vocab_path_chars);
        }
        env->DeleteLocalRef(j_vocab_path);
    }

    onnx_punctuation_provider::punctuation_config native_config;

    native_config.model_path = std::move(model_path);
    native_config.vocab_path = std::move(vocab_path);
    native_config.isOnline   = (is_online == JNI_TRUE);

    try {
        return punctuation_manager::instance().create_instance(native_config);
    } catch (const std::invalid_argument& e) {
        jclass ex_class = env->FindClass("java/lang/IllegalArgumentException");
        if (ex_class != nullptr) {
            env->ThrowNew(ex_class, e.what());
            env->DeleteLocalRef(ex_class);
        }
        return punctuation_manager::kInvalidHandle;

    } catch (const std::exception& e) {
        jclass ex_class = env->FindClass("java/lang/RuntimeException");
        if (ex_class != nullptr) {
            env->ThrowNew(ex_class, e.what());
            env->DeleteLocalRef(ex_class);
        }
        return punctuation_manager::kInvalidHandle;
    }
}

extern "C" JNIEXPORT jobject JNICALL Java_com_sam_talkdraft_transcription_1android_NativePunctuationWriter_processText(
    JNIEnv* env, jobject thiz, jlong handle, jstring input) {

    if (handle == punctuation_manager::kInvalidHandle || !input) return nullptr;

    auto provider = punctuation_manager::instance().create_from_handle(handle);
    if (!provider) return nullptr;

    const char* input_chars = env->GetStringUTFChars(input, nullptr);
    if (!input_chars) return nullptr;

    std::string text_str(input_chars);
    env->ReleaseStringUTFChars(input, input_chars);

    punctuation_based_result result = provider->process_text(text_str);

    jclass klass = env->FindClass("com/sam/talkdraft/transcription_android/punctuation/JniTextPunctuationResult");
    if (!klass) return nullptr;

    jmethodID init = env->GetMethodID(klass, "<init>", "(Ljava/lang/String;Z)V");
    if (!init) {
        env->DeleteLocalRef(klass);
        return nullptr;
    }

    jstring result_str = env->NewStringUTF(result.result.c_str());
    jobject result_obj = env->NewObject(klass, init, result_str, result.is_online_mode ? JNI_TRUE : JNI_FALSE);
    if (result_str) env->DeleteLocalRef(result_str);
    env->DeleteLocalRef(klass);
    return result_obj;
}

extern "C" JNIEXPORT void JNICALL Java_com_sam_talkdraft_transcription_1android_NativePunctuationWriter_destroyNative(
    JNIEnv* env, jobject thiz, jlong instance) {

    if (instance == punctuation_manager::kInvalidHandle) return;
    punctuation_manager::instance().destroy_instance(instance);
}
