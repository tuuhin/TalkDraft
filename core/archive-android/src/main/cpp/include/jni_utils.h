#include <jni.h>
#include <string>

class scoped_jni_string {
    JNIEnv* env_;
    jstring jstr_;
    const char* chars_;

public:
    scoped_jni_string(JNIEnv* env, jstring jstr) : env_(env), jstr_(jstr), chars_(nullptr) {
        if (jstr != nullptr) {
            chars_ = env_->GetStringUTFChars(jstr, nullptr);
        }
    }
    ~scoped_jni_string() {
        if (chars_ != nullptr && jstr_ != nullptr) {
            env_->ReleaseStringUTFChars(jstr_, chars_);
        }
    }
    [[nodiscard]] const char* get() const { return chars_; }
    operator bool() const { return chars_ != nullptr; }
};
