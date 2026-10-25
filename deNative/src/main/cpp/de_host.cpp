#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_ap_andruav_denative_DeNative_version(JNIEnv* env, jclass /* clazz */)
{
    return env->NewStringUTF("de_host 0.1");
}
