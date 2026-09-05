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
  std::string language = "auto";
  std::string accumulated_text;
  std::string last_segment;
  int last_error_code = 0;
  std::mutex mtx;

  void set_error(int code) {
    last_error_code = code;
  }

  void clear_error() {
    last_error_code = 0;
  }
};

#endif
