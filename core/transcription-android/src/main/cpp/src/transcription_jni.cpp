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

JNIEXPORT jlong JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_initializeNative(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jstring model_path,
                                                                             jstring language, jboolean use_gpu) {
  LOGI("INITIALIZING NATIVE WHISPER INSTANCE");

  const char *c_model_path = env->GetStringUTFChars(model_path, nullptr);
  const char *c_language = env->GetStringUTFChars(language, nullptr);

  if (!c_model_path) {
    LOGE("CRITICAL: MODEL PATH STRING CONVERSION FAILED!");
    if (c_language)
      env->ReleaseStringUTFChars(language, c_language);
    return 0L;
  }

  whisper_context_params params = whisper_context_default_params();
  params.use_gpu = use_gpu;

  whisper_context *ctx = whisper_init_from_file_with_params(c_model_path, params);
  env->ReleaseStringUTFChars(model_path, c_model_path);

  if (!ctx) {
    LOGE("CRITICAL: FAILED TO INITIALIZE WHISPER CONTEXT FROM PATH");
    if (c_language)
      env->ReleaseStringUTFChars(language, c_language);
    return 0L;
  }

  whisper_state *state = whisper_init_state(ctx);
  if (!state) {
    LOGE("CRITICAL: FAILED TO CREATE WHISPER STATE!");
    whisper_free(ctx);
    if (c_language)
      env->ReleaseStringUTFChars(language, c_language);
    return 0L;
  }

  auto *wrapper = new whisper_context_wrapper();
  wrapper->ctx = ctx;
  wrapper->state = state;
  wrapper->language = c_language ? std::string(c_language) : "auto";

  if (c_language) {
    env->ReleaseStringUTFChars(language, c_language);
  }

  LOGI("=== NATIVE WHISPER INITIALIZATION COMPLETE. WRAPPER AT: %p ===", (void *) wrapper);
  return reinterpret_cast<jlong>(wrapper);
}

JNIEXPORT jboolean JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_processNativeDirectBuffer(JNIEnv *env,
                                                                                      jobject thiz,
                                                                                      jlong handle,
                                                                                      jobject direct_buffer,
                                                                                      jint length) {
  auto *wrapper = reinterpret_cast<whisper_context_wrapper *>(handle);
  if (!wrapper || !wrapper->ctx || !wrapper->state) {
    LOGE("PROCESS ERROR: WRAPPER OR CONTEXT IS NULL!");
    return JNI_FALSE;
  }

  std::lock_guard<std::mutex> lock(wrapper->mtx);
  wrapper->clear_error();

  if (length <= 0) {
    LOGE("PROCESS ERROR: INVALID LENGTH (%d)", length);
    wrapper->set_error(3); // INVALID_BUFFER
    return JNI_FALSE;
  }

  // Direct pointer access to float array stored in DirectByteBuffer
  auto *pcm32 = static_cast<float *>(env->GetDirectBufferAddress(direct_buffer));
  if (!pcm32) {
    LOGE("PROCESS ERROR: DIRECT BUFFER POINTER IS NULL!");
    wrapper->set_error(3); // INVALID_BUFFER
    return JNI_FALSE;
  }

  whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
  params.print_realtime = false;
  params.print_progress = false;
  params.print_timestamps = false;
  params.print_special = false;
  params.language = wrapper->language.c_str();

  if (whisper_full_with_state(wrapper->ctx, wrapper->state, params, pcm32, length) != 0) {
    LOGE("CRITICAL: WHISPER INFERENCE FAILED!");
    wrapper->set_error(2); // INFERENCE_FAILED
    return JNI_FALSE;
  }

  std::string full_text;
  std::string latest_segment;

  int n_segments = whisper_full_n_segments_from_state(wrapper->state);
  for (int i = 0; i < n_segments; ++i) {
    const char *text = whisper_full_get_segment_text_from_state(wrapper->state, i);
    if (!text)
      continue;
    full_text += text;
    if (i == n_segments - 1) {
      latest_segment = text;
    }
  }

  wrapper->accumulated_text = full_text;
  wrapper->last_segment = latest_segment;

  return JNI_TRUE;
}

JNIEXPORT jobject JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readStateNative(JNIEnv *env, jobject thiz, jlong handle) {
  auto *wrapper = reinterpret_cast<whisper_context_wrapper *>(handle);
  if (!wrapper)
    return nullptr;

  std::lock_guard<std::mutex> lock(wrapper->mtx);

  jclass stateClass = env->FindClass("com/sam/talkdraft/transcription_android/models/WhisperState");
  if (!stateClass)
    return nullptr;

  jmethodID constructor = env->GetMethodID(stateClass, "<init>", "(Ljava/lang/String;Ljava/util/List;)V");
  if (!constructor) {
    env->DeleteLocalRef(stateClass);
    return nullptr;
  }

  jstring fullTextObj = env->NewStringUTF(wrapper->accumulated_text.c_str());

  // Instantiating empty list for segments (can be extended to pass WhisperSegment list)
  jclass listClass = env->FindClass("java/util/Collections");
  jmethodID emptyListMethod = env->GetStaticMethodID(listClass, "emptyList", "()Ljava/util/List;");
  jobject emptyList = env->CallStaticObjectMethod(listClass, emptyListMethod);

  jobject result = env->NewObject(stateClass, constructor, fullTextObj, emptyList);

  env->DeleteLocalRef(stateClass);
  env->DeleteLocalRef(fullTextObj);
  env->DeleteLocalRef(listClass);
  env->DeleteLocalRef(emptyList);

  return result;
}

JNIEXPORT jint JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readErrorNative(JNIEnv *env, jobject thiz, jlong handle) {
  auto *wrapper = reinterpret_cast<whisper_context_wrapper *>(handle);
  if (!wrapper)
    return -1;

  std::lock_guard<std::mutex> lock(wrapper->mtx);
  return wrapper->last_error_code;
}

JNIEXPORT void JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_destroyNative(JNIEnv *env, jobject thiz, jlong handle) {
  auto *wrapper = reinterpret_cast<whisper_context_wrapper *>(handle);
  if (!wrapper)
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
  LOGI("=== NATIVE WHISPER INSTANCE DESTROYED CLEANLY ===");
}

}
