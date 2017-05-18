#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_im_wangchao_imageoptapp_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello!";
    return env->NewStringUTF(hello.c_str());
}
