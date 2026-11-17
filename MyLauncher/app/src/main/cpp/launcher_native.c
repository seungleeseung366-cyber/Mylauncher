// Jembatan native: memuat libjvm.so (OpenJDK Android) lalu menjalankan main class.
#include <jni.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#define TAG "LauncherNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

typedef jint (*CreateJavaVM_t)(JavaVM **, void **, void *);

// Window Android yang dipakai lapisan GLFW/EGL (libglfw.so buatan Pojav akan memanggil ini).
static ANativeWindow *g_window = NULL;
ANativeWindow *launcher_get_window(void) { return g_window; }

JNIEXPORT void JNICALL
Java_com_example_mylauncher_launcher_NativeBridge_setSurface(JNIEnv *env, jclass c, jobject surface) {
    if (g_window) { ANativeWindow_release(g_window); g_window = NULL; }
    if (surface) g_window = ANativeWindow_fromSurface(env, surface);
    LOGI("surface set: %p", (void *) g_window);
}

JNIEXPORT void JNICALL
Java_com_example_mylauncher_launcher_NativeBridge_setenv(JNIEnv *env, jclass c, jstring k, jstring v) {
    const char *ck = (*env)->GetStringUTFChars(env, k, NULL);
    const char *cv = (*env)->GetStringUTFChars(env, v, NULL);
    setenv(ck, cv, 1);
    (*env)->ReleaseStringUTFChars(env, k, ck);
    (*env)->ReleaseStringUTFChars(env, v, cv);
}

JNIEXPORT jint JNICALL
Java_com_example_mylauncher_launcher_NativeBridge_dlopenGlobal(JNIEnv *env, jclass c, jstring path) {
    const char *p = (*env)->GetStringUTFChars(env, path, NULL);
    void *h = dlopen(p, RTLD_NOW | RTLD_GLOBAL);
    if (!h) LOGE("dlopen %s gagal: %s", p, dlerror());
    (*env)->ReleaseStringUTFChars(env, path, p);
    return h ? 0 : -1;
}

JNIEXPORT jint JNICALL
Java_com_example_mylauncher_launcher_NativeBridge_startJvm(
        JNIEnv *env, jclass c, jstring jreHome, jobjectArray jvmArgs,
        jstring mainClass, jobjectArray appArgs) {

    const char *home = (*env)->GetStringUTFChars(env, jreHome, NULL);
    char libjvm[1024];
    snprintf(libjvm, sizeof(libjvm), "%s/lib/server/libjvm.so", home);
    (*env)->ReleaseStringUTFChars(env, jreHome, home);

    void *h = dlopen(libjvm, RTLD_NOW | RTLD_GLOBAL);
    if (!h) { LOGE("Gagal dlopen libjvm: %s", dlerror()); return -1; }
    CreateJavaVM_t create = (CreateJavaVM_t) dlsym(h, "JNI_CreateJavaVM");
    if (!create) { LOGE("JNI_CreateJavaVM tidak ditemukan"); return -2; }

    jsize nOpt = (*env)->GetArrayLength(env, jvmArgs);
    JavaVMOption *opts = calloc(nOpt, sizeof(JavaVMOption));
    for (jsize i = 0; i < nOpt; i++) {
        jstring s = (jstring) (*env)->GetObjectArrayElement(env, jvmArgs, i);
        opts[i].optionString = strdup((*env)->GetStringUTFChars(env, s, NULL));
    }
    JavaVMInitArgs vmArgs = {0};
    vmArgs.version = JNI_VERSION_1_8;
    vmArgs.nOptions = nOpt;
    vmArgs.options = opts;
    vmArgs.ignoreUnrecognized = JNI_TRUE;

    JavaVM *vm = NULL; JNIEnv *jenv = NULL;
    jint rc = create(&vm, (void **) &jenv, &vmArgs);
    if (rc != JNI_OK) { LOGE("JNI_CreateJavaVM gagal: %d", rc); return -3; }
    LOGI("JVM dibuat");

    const char *mc = (*env)->GetStringUTFChars(env, mainClass, NULL);
    char cls[512]; strncpy(cls, mc, sizeof(cls) - 1); cls[sizeof(cls) - 1] = 0;
    (*env)->ReleaseStringUTFChars(env, mainClass, mc);
    for (char *p = cls; *p; p++) if (*p == '.') *p = '/';

    jclass mainC = (*jenv)->FindClass(jenv, cls);
    if (!mainC) { (*jenv)->ExceptionDescribe(jenv); LOGE("Main class tidak ketemu: %s", cls); return -4; }
    jmethodID mainM = (*jenv)->GetStaticMethodID(jenv, mainC, "main", "([Ljava/lang/String;)V");
    if (!mainM) { LOGE("main() tidak ada"); return -5; }

    jsize nArg = (*env)->GetArrayLength(env, appArgs);
    jclass strC = (*jenv)->FindClass(jenv, "java/lang/String");
    jobjectArray arr = (*jenv)->NewObjectArray(jenv, nArg, strC, NULL);
    for (jsize i = 0; i < nArg; i++) {
        jstring s = (jstring) (*env)->GetObjectArrayElement(env, appArgs, i);
        const char *u = (*env)->GetStringUTFChars(env, s, NULL);
        (*jenv)->SetObjectArrayElement(jenv, arr, i, (*jenv)->NewStringUTF(jenv, u));
        (*env)->ReleaseStringUTFChars(env, s, u);
    }
    (*jenv)->CallStaticVoidMethod(jenv, mainC, mainM, arr);
    if ((*jenv)->ExceptionCheck(jenv)) { (*jenv)->ExceptionDescribe(jenv); return -6; }
    return 0;
}
