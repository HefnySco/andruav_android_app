#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

extern "C" JNIEXPORT jstring JNICALL
Java_ap_andruav_denative_DeNative_version(JNIEnv* env, jclass /* clazz */)
{
    return env->NewStringUTF("de_host 0.1");
}

namespace {

typedef int (*ModuleMainFn)(int, char**);
typedef void (*ModuleRequestStopFn)(void);

// One DE module per process (megaplan decision N1): globals are safe.
void*               g_module_handle = nullptr;
ModuleRequestStopFn g_module_request_stop = nullptr;
int                 g_pipe_read_fd = -1;
pthread_t           g_log_thread{};
std::string         g_log_tag;

void* logReaderMain(void* /* unused */)
{
    char buffer[2048];
    std::string pending;
    for (;;) {
        ssize_t n = read(g_pipe_read_fd, buffer, sizeof(buffer));
        if (n <= 0) break;
        for (ssize_t i = 0; i < n; ++i) {
            const char c = buffer[i];
            if (c == '\n') {
                __android_log_write(ANDROID_LOG_INFO, g_log_tag.c_str(), pending.c_str());
                pending.clear();
            } else {
                pending.push_back(c);
            }
        }
    }
    if (!pending.empty()) {
        __android_log_write(ANDROID_LOG_INFO, g_log_tag.c_str(), pending.c_str());
    }
    return nullptr;
}

} // namespace

extern "C" JNIEXPORT jint JNICALL
Java_ap_andruav_denative_DeModuleService_nativeRun(
        JNIEnv* env, jobject /* thiz */,
        jstring jlib_name, jstring jwork_dir, jobjectArray jargv)
{
    const char* lib_name = env->GetStringUTFChars(jlib_name, nullptr);
    const char* work_dir = env->GetStringUTFChars(jwork_dir, nullptr);
    g_log_tag = lib_name;

    if (chdir(work_dir) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, lib_name,
                            "de_host: chdir(%s) failed: %s", work_dir, strerror(errno));
        env->ReleaseStringUTFChars(jlib_name, lib_name);
        env->ReleaseStringUTFChars(jwork_dir, work_dir);
        return -1;
    }

    // stdout/stderr -> logcat under the module name as tag.
    int fds[2];
    if (pipe(fds) == 0) {
        g_pipe_read_fd = fds[0];
        if (pthread_create(&g_log_thread, nullptr, logReaderMain, nullptr) == 0) {
            dup2(fds[1], STDOUT_FILENO);
            dup2(fds[1], STDERR_FILENO);
            setvbuf(stdout, nullptr, _IOLBF, 0);
            setvbuf(stderr, nullptr, _IOLBF, 0);
        } else {
            close(fds[0]);
            close(fds[1]);
        }
    }

    const std::string so_name = std::string("lib") + lib_name + ".so";
    g_module_handle = dlopen(so_name.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (!g_module_handle) {
        __android_log_print(ANDROID_LOG_ERROR, lib_name,
                            "de_host: dlopen %s failed: %s", so_name.c_str(), dlerror());
        env->ReleaseStringUTFChars(jlib_name, lib_name);
        env->ReleaseStringUTFChars(jwork_dir, work_dir);
        return -1;
    }

    auto module_main = reinterpret_cast<ModuleMainFn>(dlsym(g_module_handle, "de_module_main"));
    g_module_request_stop =
            reinterpret_cast<ModuleRequestStopFn>(dlsym(g_module_handle, "de_module_request_stop"));
    if (!module_main || !g_module_request_stop) {
        __android_log_print(ANDROID_LOG_ERROR, lib_name,
                            "de_host: missing de_module_ symbols in %s", so_name.c_str());
        env->ReleaseStringUTFChars(jlib_name, lib_name);
        env->ReleaseStringUTFChars(jwork_dir, work_dir);
        return -1;
    }

    const jsize argc = env->GetArrayLength(jargv);
    std::vector<std::string> argv_store(static_cast<size_t>(argc));
    std::vector<char*> argv(static_cast<size_t>(argc) + 1, nullptr);
    for (jsize i = 0; i < argc; ++i) {
        auto s = static_cast<jstring>(env->GetObjectArrayElement(jargv, i));
        const char* chars = env->GetStringUTFChars(s, nullptr);
        argv_store[static_cast<size_t>(i)] = chars;
        env->ReleaseStringUTFChars(s, chars);
        env->DeleteLocalRef(s);
        argv[static_cast<size_t>(i)] = argv_store[static_cast<size_t>(i)].data();
    }

    __android_log_print(ANDROID_LOG_INFO, lib_name,
                        "de_host: starting lib%s.so in %s", lib_name, work_dir);
    const int rc = module_main(static_cast<int>(argc), argv.data());
    __android_log_print(ANDROID_LOG_INFO, lib_name,
                        "de_host: lib%s.so exited rc=%d", lib_name, rc);

    env->ReleaseStringUTFChars(jlib_name, lib_name);
    env->ReleaseStringUTFChars(jwork_dir, work_dir);
    return rc;
}

extern "C" JNIEXPORT void JNICALL
Java_ap_andruav_denative_DeModuleService_nativeRequestStop(JNIEnv* /* env */, jobject /* thiz */)
{
    if (g_module_request_stop) {
        g_module_request_stop();
    }
}
