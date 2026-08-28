#ifndef WHISPER_CTX_H
#define WHISPER_CTX_H

#include "whisper.h"
#include <jni.h>
#include <memory>
#include <mutex>
#include <string>

struct whisper_context_wrapper {
  whisper_context *ctx = nullptr;
  whisper_state *state = nullptr;
  std::string language = "en";

  std::mutex mtx;
  std::string accumulated_text;
  std::string last_segment;
  bool is_finished = false;

  int last_error_code = 0;

  void set_error(int code) {
    last_error_code = code;
  }

  void clear_error() {
    last_error_code = 0;
  }
};

whisper_context_wrapper *get_wrapper(
    JNIEnv *env,
    jobject thiz
);

void set_wrapper(
    JNIEnv *env,
    jobject thiz,
    whisper_context_wrapper *wrapper
);

#endif
