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
  LOGI("INITIALIZING NATIVE WHISPER INSTANCE");

  // Safely cleanup existing wrapper instance
  whisper_context_wrapper *existing = get_wrapper(env, thiz);
  if (existing != nullptr) {
    LOGD("NATIVE WHISPER INSTANCE ALREADY FOUND. STARTING CLEANUP...");
    {
      std::lock_guard<std::mutex> lock(existing->mtx);
      if (existing->state) {
        LOGD("FREEING PREVIOUS WHISPER STATE...");
        whisper_free_state(existing->state);
        existing->state = nullptr;
      }
      if (existing->ctx) {
        LOGD("FREEING PREVIOUS WHISPER CONTEXT...");
        whisper_free(existing->ctx);
        existing->ctx = nullptr;
      }
    }
    delete existing;
    set_wrapper(env, thiz, nullptr);
    LOGD("PREVIOUS INSTANCE SUCCESSFULLY DESTROYED.");
  }

  const char *c_model_path = env->GetStringUTFChars(model_path, nullptr);
  const char *c_language = env->GetStringUTFChars(language, nullptr);

  if (!c_model_path) {
    LOGE("CRITICAL: MODEL PATH STRING CONVERSION FAILED!");
    if (c_language)
      env->ReleaseStringUTFChars(language, c_language);
    return JNI_FALSE;
  }

  LOGI("MODEL PATH: %s", c_model_path);
  LOGI("TARGET LANGUAGE: %s", c_language ? c_language : "NULL");

  whisper_context_params params = whisper_context_default_params();
  LOGD("INITIALIZING WHISPER CONTEXT FROM FILE...");
  whisper_context *ctx = whisper_init_from_file_with_params(c_model_path, params);

  env->ReleaseStringUTFChars(model_path, c_model_path);

  if (ctx == nullptr) {
    LOGE("CRITICAL: FAILED TO INITIALIZE WHISPER CONTEXT FROM PATH: %s", c_model_path);
    if (c_language)
      env->ReleaseStringUTFChars(language, c_language);
    return JNI_FALSE;
  }
  LOGI("WHISPER CONTEXT INITIALIZED SUCCESSFULLY AT ADDRESS: %p", (void *) ctx);

  LOGD("INITIALIZING WHISPER STATE...");
  whisper_state *state = whisper_init_state(ctx);
  if (state == nullptr) {
    LOGE("CRITICAL: FAILED TO CREATE WHISPER STATE FROM CONTEXT!");
    whisper_free(ctx);
    if (c_language)
      env->ReleaseStringUTFChars(language, c_language);
    return JNI_FALSE;
  }
  LOGI("WHISPER STATE INITIALIZED SUCCESSFULLY AT ADDRESS: %p", (void *) state);

  auto *wrapper = new whisper_context_wrapper();
  wrapper->ctx = ctx;
  wrapper->state = state;

  // Store language safely before releasing string chars
  if (c_language) {
    wrapper->language = std::string(c_language);
    env->ReleaseStringUTFChars(language, c_language);
  } else {
    wrapper->language = "en";
  }

  set_wrapper(env, thiz, wrapper);
  LOGI("=== NATIVE WHISPER INITIALIZATION COMPLETE. WRAPPER AT: %p ===", (void *) wrapper);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_processBytes(JNIEnv *env,
                                                                         jobject thiz,
                                                                         jshortArray samples,
                                                                         jint length) {
  LOGD("=== PROCESS BYTES CALLED. SAMPLES LENGTH: %d ===", length);

  whisper_context_wrapper *wrapper = get_wrapper(env, thiz);
  if (!wrapper || !wrapper->ctx || !wrapper->state) {
    LOGE("PROCESS BYTES ERROR: CONTEXT OR STATE IS UNINITIALIZED OR NULL!");
    return JNI_FALSE;
  }

  LOGD("ACQUIRED WRAPPER AT ADDRESS: %p. LOCKING MUTEX...", (void *) wrapper);
  std::lock_guard<std::mutex> lock(wrapper->mtx);
  LOGD("MUTEX LOCK ACQUIRED FOR PROCESS BYTES.");

  wrapper->clear_error();

  if (length <= 0) {
    LOGE("PROCESS BYTES ERROR: INVALID AUDIO SAMPLE LENGTH (%d)!", length);
    wrapper->set_error(100);
    return JNI_FALSE;
  }

  LOGD("ACQUIRING JNI SHORT ARRAY ELEMENTS...");
  jshort *audio_data = env->GetShortArrayElements(samples, nullptr);
  if (!audio_data) {
    LOGE("PROCESS BYTES ERROR: UNABLE TO ACQUIRE AUDIO ARRAY ELEMENTS FROM JNI!");
    wrapper->set_error(101);
    return JNI_FALSE;
  }
  LOGD("JNI SHORT ARRAY ELEMENTS ACQUIRED SUCCESSFULLY.");

  LOGD("CONVERTING %d PCM16 SAMPLES TO FLOAT PCM32 ARRAY...", length);
  std::vector<float> pcm32(length);
  for (int i = 0; i < length; ++i) {
    pcm32[i] = static_cast<float>(audio_data[i]) / 32768.0f;
  }

  LOGD("RELEASING JNI SHORT ARRAY ELEMENTS (JNI_ABORT)...");
  env->ReleaseShortArrayElements(samples, audio_data, JNI_ABORT);

  whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
  params.print_realtime = false;
  params.print_progress = false;
  params.print_timestamps = false;
  params.print_special = false;
  params.language = wrapper->language.c_str();

  LOGI("STARTING WHISPER INFERENCE WITH %d SAMPLES (LANGUAGE: '%s')...", length, params.language);

  if (whisper_full_with_state(wrapper->ctx, wrapper->state, params, pcm32.data(), length) != 0) {
    LOGE("CRITICAL: WHISPER_FULL_WITH_STATE INFERENCE FAILED DURING EXECUTION!");
    wrapper->set_error(102);
    return JNI_FALSE;
  }
  LOGI("WHISPER INFERENCE COMPLETED SUCCESSFULLY.");

  std::string full_text;
  std::string latest_segment;

  int n_segments = whisper_full_n_segments_from_state(wrapper->state);
  LOGD("EXTRACTING TRANSCRIPTION SEGMENTS. TOTAL SEGMENTS DETECTED: %d", n_segments);

  for (int i = 0; i < n_segments; ++i) {
    const char *text = whisper_full_get_segment_text_from_state(wrapper->state, i);
    if (text == nullptr) {
      LOGD("SEGMENT [%d]: NULL TEXT DETECTED, SKIPPING...", i);
      continue;
    }

    LOGD("SEGMENT [%d]: \"%s\"", i, text);
    full_text += text;
    if (i == n_segments - 1) {
      latest_segment = text;
    }
  }

  wrapper->accumulated_text = full_text;
  wrapper->last_segment = latest_segment;

  LOGI("=== PROCESS BYTES FINISHED. PROCESSED SAMPLES: %d | RESULT LENGTH: %zu CHARS ===", length, full_text.length());
  return JNI_TRUE;
}

JNIEXPORT jobject JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readState(JNIEnv *env, jobject thiz) {
  LOGD("=== READ STATE CALLED ===");

  whisper_context_wrapper *wrapper = get_wrapper(env, thiz);
  if (!wrapper) {
    LOGE("READ STATE ERROR: NATIVE WRAPPER IS NULL!");
    return nullptr;
  }

  LOGD("LOCKING MUTEX FOR READ STATE...");
  std::lock_guard<std::mutex> lock(wrapper->mtx);
  LOGD("MUTEX LOCK ACQUIRED FOR READ STATE.");

  LOGD("FINDING JAVA CLASS: com/sam/talkdraft/transcription_android/models/WhisperState");
  jclass stateClass = env->FindClass("com/sam/talkdraft/transcription_android/models/WhisperState");
  if (!stateClass) {
    LOGE("CRITICAL: READ STATE FAILED - WHISPERSTATE JAVA CLASS NOT FOUND!");
    return nullptr;
  }

  LOGD("GETTING METHOD ID FOR WHISPERSTATE CONSTRUCTOR...");
  jmethodID constructor = env->GetMethodID(stateClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
  if (!constructor) {
    LOGE("CRITICAL: READ STATE FAILED - WHISPERSTATE CONSTRUCTOR NOT FOUND!");
    env->DeleteLocalRef(stateClass);
    return nullptr;
  }

  LOGD("CREATING JNI STRINGS FOR STATE DATA...");
  jstring fullTextObj = env->NewStringUTF(wrapper->accumulated_text.c_str());
  jstring segmentObj = wrapper->last_segment.empty()
      ? nullptr
      : env->NewStringUTF(wrapper->last_segment.c_str());

  LOGD("INSTANTIATING NEW WHISPERSTATE JAVA OBJECT...");
  jobject result = env->NewObject(stateClass, constructor, fullTextObj, segmentObj);

  LOGD("CLEANING UP JNI LOCAL REFERENCES IN READ STATE...");
  env->DeleteLocalRef(stateClass);
  env->DeleteLocalRef(fullTextObj);
  if (segmentObj)
    env->DeleteLocalRef(segmentObj);

  LOGD("=== READ STATE COMPLETED SUCCESSFULLY ===");
  return result;
}

JNIEXPORT jobject JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_readError(JNIEnv *env, jobject thiz) {
  LOGD("=== READ ERROR CALLED ===");

  whisper_context_wrapper *wrapper = get_wrapper(env, thiz);
  if (!wrapper) {
    LOGE("READ ERROR FAILED: NATIVE WRAPPER IS NULL!");
    return nullptr;
  }

  LOGD("LOCKING MUTEX FOR READ ERROR...");
  std::lock_guard<std::mutex> lock(wrapper->mtx);
  LOGD("MUTEX LOCK ACQUIRED FOR READ ERROR.");

  if (wrapper->last_error_code == 0) {
    LOGD("NO ERROR FOUND (ERROR CODE = 0). RETURNING NULL.");
    return nullptr;
  }

  LOGI("ERROR CODE DETECTED: %d. FINDING WHISPERERRORCODE JAVA CLASS...", wrapper->last_error_code);
  jclass errorClass = env->FindClass("com/sam/talkdraft/transcription_android/models/WhisperErrorCode");
  if (!errorClass) {
    LOGE("CRITICAL: READ ERROR FAILED - WHISPERERRORCODE CLASS NOT FOUND!");
    return nullptr;
  }

  jmethodID constructor = env->GetMethodID(errorClass, "<init>", "(I)V");
  if (!constructor) {
    LOGE("CRITICAL: READ ERROR FAILED - WHISPERERRORCODE CONSTRUCTOR NOT FOUND!");
    env->DeleteLocalRef(errorClass);
    return nullptr;
  }

  LOGD("INSTANTIATING WHISPERERRORCODE JAVA OBJECT FOR CODE: %d", wrapper->last_error_code);
  jobject result = env->NewObject(errorClass, constructor, wrapper->last_error_code);

  env->DeleteLocalRef(errorClass);

  LOGD("=== READ ERROR COMPLETED SUCCESSFULLY ===");
  return result;
}

JNIEXPORT void JNICALL
Java_com_sam_talkdraft_transcription_1android_NativeWhisper_close(JNIEnv *env, jobject thiz) {
  LOGI("=== CLOSING NATIVE WHISPER INSTANCE ===");

  whisper_context_wrapper *wrapper = get_wrapper(env, thiz);
  if (wrapper == nullptr) {
    LOGD("CLOSE CALLED BUT WRAPPER IS ALREADY NULL. NOTHING TO CLEAN UP.");
    return;
  }

  LOGD("ACQUIRING MUTEX LOCK FOR CLEANUP OF WRAPPER AT ADDRESS: %p...", (void *) wrapper);
  {
    std::lock_guard<std::mutex> lock(wrapper->mtx);
    LOGD("MUTEX LOCK ACQUIRED FOR CLOSE.");

    if (wrapper->state) {
      LOGD("FREEING WHISPER STATE AT ADDRESS: %p...", (void *) wrapper->state);
      whisper_free_state(wrapper->state);
      wrapper->state = nullptr;
      LOGD("WHISPER STATE FREED.");
    }
    if (wrapper->ctx) {
      LOGD("FREEING WHISPER CONTEXT AT ADDRESS: %p...", (void *) wrapper->ctx);
      whisper_free(wrapper->ctx);
      wrapper->ctx = nullptr;
      LOGD("WHISPER CONTEXT FREED.");
    }
  }

  LOGD("DELETING WRAPPER INSTANCE...");
  delete wrapper;
  set_wrapper(env, thiz, nullptr);

  LOGI("=== NATIVE WHISPER INSTANCE DESTROYED CLEANLY ===");
}

}
