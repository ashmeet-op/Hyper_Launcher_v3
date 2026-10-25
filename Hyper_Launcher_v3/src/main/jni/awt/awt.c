//
// Created by whbex on 20.08.2026.
//

#include <stdbool.h>
#include <pthread.h>
#include "awt.h"

jclass class_AWTBridge = NULL;

float inputXRatio = 0;
float inputYRatio = 0;

JavaVM *androidVM = NULL;
JavaVM *runtimeVM = NULL;

pthread_mutex_t vm_wait_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t vm_wait_cond = PTHREAD_COND_INITIALIZER;

_Atomic bool isVmConnected = false;

// This is used across all HyperExec AWT library
JNIEnv *JNIEnv_InputRuntime = NULL;

jclass ensure_AWTBridge(JNIEnv *env) {
    if (class_AWTBridge != NULL) return class_AWTBridge;
    if (env == NULL) return NULL;
    jclass localAWTBridge = (*env)->FindClass(env, "com/ashmeet/hyperbackend/awt/AWTBridge");
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        (*env)->ExceptionClear(env);
    }
    if (localAWTBridge != NULL) {
        class_AWTBridge = (*env)->NewGlobalRef(env, localAWTBridge);
        (*env)->DeleteLocalRef(env, localAWTBridge);
        register_methods_util(env);
        register_methods_clipboard(env);
    }
    return class_AWTBridge;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    if (androidVM == NULL) {
        // Save dalvik global JavaVM pointer
        androidVM = vm;
        JNIEnv *env = NULL;
        if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_4) == JNI_OK && env != NULL) {
            ensure_AWTBridge(env);
        }
    } else if (runtimeVM != vm) {
        runtimeVM = vm;
        isVmConnected = true;
        pthread_cond_broadcast(&vm_wait_cond);
    }

    return JNI_VERSION_1_4;
}
