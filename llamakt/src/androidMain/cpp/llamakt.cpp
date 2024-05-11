#include <jni.h>
#include <string>
#include "llama.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_org_pinelang_llamakt_LLMKt_nativeSystemInfo(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}