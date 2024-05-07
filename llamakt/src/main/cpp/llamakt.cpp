#include <jni.h>
#include <string>
#include "llama.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_pinelab_llammakt_LLM_systemInfo(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}