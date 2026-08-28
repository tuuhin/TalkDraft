#include <jni.h>
#include <android/log.h>
#include <mutex>
#include <vector>
#include <string>
#include "whisper_ctx.h"

// Android Logging Macros
#define LOG_TAG "NativeWhisperJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_init(JNIEnv *env,
                                                                 jobject thiz,
                                                                 jstring model_path,
                                                                 jstring language) {
  LOGI("Initializing NativeWhisper...");

  // Safely cleanup existing wrapper instance
  whisper_context_wrapper *existing = get_wrapper(env, thiz);
  if (existing != nullptr) {
    LOGD("Cleaning up previous NativeWhisper instance");
    {
      std::lock_guard<std::mutex> lock(existing->mtx);
      if (existing->state)
        whisper_free_state(existing->state);
      if (existing->ctx)
        whisper_free(existing->ctx);
      existing->state = nullptr;
      existing->ctx = nullptr;
    }
    delete existing;
    set_wrapper(env, thiz, nullptr);
  }

  const char *c_model_path = env->GetStringUTFChars(model_path, nullptr);
  const char *c_language = env->GetStringUTFChars(language, nullptr);

  whisper_context_params params = whisper_context_default_params();
  whisper_context *ctx = whisper_init_from_file_with_params(c_model_path, params);

  env->ReleaseStringUTFChars(model_path, c_model_path);

  if (ctx == nullptr) {
    LOGE("Failed to initialize whisper context from path!");
    env->ReleaseStringUTFChars(language, c_language);
    return JNI_FALSE;
  }

  whisper_state *state = whisper_init_state(ctx);
  if (state == nullptr) {
    LOGE("Failed to create whisper state!");
    whisper_free(ctx);
    env->ReleaseStringUTFChars(language, c_language);
    return JNI_FALSE;
  }

  auto *wrapper = new whisper_context_wrapper();
  wrapper->ctx = ctx;
  wrapper->state = state;
  wrapper->language = c_language;

  env->ReleaseStringUTFChars(language, c_language);

  set_wrapper(env, thiz, wrapper);
  LOGI("NativeWhisper initialization successful.");

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_processBytes(JNIEnv *env,
                                                                         jobject thiz,
                                                                         jshortArray samples,
                                                                         jint length) {
  whisper_context_wrapper *wrapper = get_wrapper(env, thiz);
  if (!wrapper || !wrapper->ctx || !wrapper->state) {
    LOGE("processBytes failed: Context or state is uninitialized");
    return JNI_FALSE;
  }

  std::lock_guard<std::mutex> lock(wrapper->mtx);
  wrapper->clear_error();

  jshort *audio_data = env->GetShortArrayElements(samples, nullptr);
  if (!audio_data) {
    LOGE("processBytes failed: Unable to acquire audio array elements");
    wrapper->set_error(101);
    return JNI_FALSE;
  }

  std::vector<float> pcm32(length);
  for (int i = 0; i < length; ++i) {
    pcm32[i] = static_cast<float>(audio_data[i]) / 32768.0f;
  }
  env->ReleaseShortArrayElements(samples, audio_data, JNI_ABORT);

  whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
  params.print_realtime = false;
  params.print_progress = false;
  params.print_timestamps = false;
  params.print_special = false;
  params.language = wrapper->language.c_str();

  // Run inference
  if (whisper_full_with_state(wrapper->ctx, wrapper->state, params, pcm32.data(), length) != 0) {
    LOGE("whisper_full_with_state failed during execution");
    wrapper->set_error(102);
    return JNI_FALSE;
  }

  std::string full_text;
  std::string latest_segment;

  int n_segments = whisper_full_n_segments_from_state(wrapper->state);
  for (int i = 0; i < n_segments; ++i) {
    const char *text = whisper_full_get_segment_text_from_state(wrapper->state, i);
    if (text == nullptr)
      continue;

    full_text += text;
    if (i == n_segments - 1) {
      latest_segment = text;
    }
  }

  // Replace accumulated_text cleanly rather than endlessly appending
  wrapper->accumulated_text = full_text;
  wrapper->last_segment = latest_segment;

  LOGD("Processed %d samples. Result length: %zu chars", length, full_text.length());
  return JNI_TRUE;
}

JNIEXPORT jobject JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readState(JNIEnv *env, jobject thiz) {
  whisper_context_wrapper *wrapper = get_wrapper(env, thiz);
  if (!wrapper)
    return nullptr;

  std::lock_guard<std::mutex> lock(wrapper->mtx);

  jclass stateClass = env->FindClass("com/sam/talkdraft/transcription_android/models/WhisperState");
  if (!stateClass) {
    LOGE("readState failed: WhisperState class not found");
    return nullptr;
  }

  jmethodID constructor = env->GetMethodID(stateClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
  if (!constructor) {
    LOGE("readState failed: WhisperState constructor not found");
    env->DeleteLocalRef(stateClass);
    return nullptr;
  }

  jstring fullTextObj = env->NewStringUTF(wrapper->accumulated_text.c_str());
  jstring segmentObj = wrapper->last_segment.empty()
      ? nullptr
      : env->NewStringUTF(wrapper->last_segment.c_str());

  jobject result = env->NewObject(stateClass, constructor, fullTextObj, segmentObj);

  // Free local JNI object references
  env->DeleteLocalRef(stateClass);
  env->DeleteLocalRef(fullTextObj);
  if (segmentObj)
    env->DeleteLocalRef(segmentObj);

  return result;
}

JNIEXPORT jobject JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readError(JNIEnv *env, jobject thiz) {
  whisper_context_wrapper *wrapper = get_wrapper(env, thiz);
  if (!wrapper)
    return nullptr;

  std::lock_guard<std::mutex> lock(wrapper->mtx);

  if (wrapper->last_error_code == 0) {
    return nullptr;
  }

  jclass errorClass = env->FindClass("com/sam/talkdraft/transcription_android/models/WhisperErrorCode");
  if (!errorClass) {
    LOGE("readError failed: WhisperErrorCode class not found");
    return nullptr;
  }

  jmethodID constructor = env->GetMethodID(errorClass, "<init>", "(I)V");
  if (!constructor) {
    LOGE("readError failed: WhisperErrorCode constructor not found");
    env->DeleteLocalRef(errorClass);
    return nullptr;
  }

  jobject result = env->NewObject(errorClass, constructor, wrapper->last_error_code);

  env->DeleteLocalRef(errorClass);
  return result;
}

JNIEXPORT void JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_close(JNIEnv *env, jobject thiz) {
  LOGI("Closing NativeWhisper instance");
  whisper_context_wrapper *wrapper = get_wrapper(env, thiz);
  if (wrapper == nullptr)
    return;

  {
    std::lock_guard<std::mutex> lock(wrapper->mtx);
    if (wrapper->state) {
      whisper_free_state(wrapper->state);
      wrapper->state = nullptr;
    }
    if (wrapper->ctx) {
      whisper_free(wrapper->ctx);
      wrapper->ctx = nullptr;
    }
  }

  delete wrapper;
  set_wrapper(env, thiz, nullptr);
  LOGI("NativeWhisper destroyed cleanly.");
}

}
