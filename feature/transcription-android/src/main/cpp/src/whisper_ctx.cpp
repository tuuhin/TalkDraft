#include "jni.h"
#include "whisper_ctx.h"

whisper_context_wrapper *get_wrapper(JNIEnv *env, jobject thiz) {
  jclass clazz = env->GetObjectClass(thiz);
  jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
  return reinterpret_cast<whisper_context_wrapper *>(env->GetLongField(thiz, fieldId));
}

void set_wrapper(JNIEnv *env, jobject thiz, whisper_context_wrapper *wrapper) {
  jclass clazz = env->GetObjectClass(thiz);
  jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
  env->SetLongField(thiz, fieldId, reinterpret_cast<jlong>(wrapper));
}
