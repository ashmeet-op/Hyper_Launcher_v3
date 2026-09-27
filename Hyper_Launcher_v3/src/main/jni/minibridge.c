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

typedef int (*get_fps_fn)(void);
//this is the best i could do, i don't think it will work but who knows
JNIEXPORT jint JNICALL
Java_net_kdt_pojavlaunch_CallbackBridge_nativeGetFps(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;

    static get_fps_fn func_get_fps = NULL;
    if (!func_get_fps) {
        func_get_fps = (get_fps_fn)dlsym(RTLD_DEFAULT, "nativeGetFps");
        if (!func_get_fps) {
            void* handle = dlopen("libglfw.so", RTLD_NOW | RTLD_GLOBAL);
            if (handle) {
                func_get_fps = (get_fps_fn)dlsym(handle, "nativeGetFps");
            }
        }
        if (!func_get_fps) {
            void* handle = dlopen("libSDL3.so", RTLD_NOW | RTLD_GLOBAL);
            if (handle) {
                func_get_fps = (get_fps_fn)dlsym(handle, "nativeGetFps");
            }
        }
        if (!func_get_fps) {
            void* handle = dlopen("libSDL.so", RTLD_NOW | RTLD_GLOBAL);
            if (handle) {
                func_get_fps = (get_fps_fn)dlsym(handle, "nativeGetFps");
            }
        }
    }

    if (func_get_fps) {
        return (jint)func_get_fps();
    }
    return 0;
}

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
