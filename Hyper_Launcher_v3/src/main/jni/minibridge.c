//
// Created by maks on 09.04.2026.
//

#include "utils.h"
#include <jni.h>
#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>

static JavaVM* dalivk;
static jclass class_CallbackBridge;
static jmethodID method_openLink;


void openLink(const char* link) {
    JNIEnv *attachedEnv = get_attached_env(dalivk);
    (*attachedEnv)->CallStaticVoidMethod(attachedEnv, class_CallbackBridge, method_openLink, (*attachedEnv)->NewStringUTF(attachedEnv, link));
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_minibridgeInit(JNIEnv *env, jclass clazz) {
    (*env)->GetJavaVM(env, &dalivk);
    class_CallbackBridge = (*env)->NewGlobalRef(env, clazz);
    method_openLink = (*env)->GetStaticMethodID(env, clazz, "openLink", "(Ljava/lang/String;)V");
}

JNIEXPORT void JNICALL
Java_git_artdeell_mojo_CallbackBridge_minibridgeInit(JNIEnv *env, jclass clazz) {
    Java_net_kdt_pojavlaunch_CallbackBridge_minibridgeInit(env, clazz);
}

typedef int (*get_fps_fn_t)(void);

JNIEXPORT jint JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_nativeGetFps(JNIEnv *env, jclass clazz) {
    static get_fps_fn_t cachedFpsFunc = NULL;
    if (cachedFpsFunc != NULL) {
        return cachedFpsFunc();
    }

    get_fps_fn_t getFpsFunc = (get_fps_fn_t) dlsym(RTLD_DEFAULT, "nativeGetFps");
    if (getFpsFunc == NULL) {
        const char* libs[] = {"libglfw.so", "libglfw_fcl.so", "libSDL3.so", "libGLMojo.so", NULL};
        for (int i = 0; libs[i] != NULL; i++) {
            void* handle = dlopen(libs[i], RTLD_NOLOAD | RTLD_LAZY);
            if (handle != NULL) {
                getFpsFunc = (get_fps_fn_t) dlsym(handle, "nativeGetFps");
                if (getFpsFunc != NULL) break;
            }
        }
    }

    if (getFpsFunc != NULL) {
        cachedFpsFunc = getFpsFunc;
        return cachedFpsFunc();
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_git_artdeell_mojo_CallbackBridge_nativeGetFps(JNIEnv *env, jclass clazz) {
    return Java_net_kdt_pojavlaunch_CallbackBridge_nativeGetFps(env, clazz);
}