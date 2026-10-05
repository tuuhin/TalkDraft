#include <jni.h>
#include <string>

class scoped_jni_string {
    JNIEnv* _env;
    jstring _jString;
    const char* _chars;

public:
    scoped_jni_string(JNIEnv* env, jstring jstr) : _env(env), _jString(jstr), _chars(nullptr) {
        if (jstr == nullptr) return;
        _chars = _env->GetStringUTFChars(jstr, nullptr);
    }
    ~scoped_jni_string() {
        if (_chars == nullptr || _jString == nullptr) return;
        _env->ReleaseStringUTFChars(_jString, _chars);
    }

    [[nodiscard]] const char* get() const { return _chars; }
    operator bool() const { return _chars != nullptr; }
};
