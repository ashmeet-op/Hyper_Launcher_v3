//
// Created by whbex on 20.08.2026.
//

#include "awt.h"

jmethodID method_OpenLink = NULL;
jmethodID method_OpenPath = NULL;
jmethodID method_NotifyAwtWindow = NULL;

void register_methods_util(JNIEnv *env) {
    if (class_AWTBridge == NULL) return;

    method_OpenLink = (*env)->GetStaticMethodID(env, class_AWTBridge, "openLink",
                                                "(Ljava/lang/String;)V");
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);

    method_OpenPath = (*env)->GetStaticMethodID(env, class_AWTBridge, "openPath",
                                                "(Ljava/lang/String;)V");
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        method_OpenPath = method_OpenLink;
    }

    method_NotifyAwtWindow = (*env)->GetStaticMethodID(env, class_AWTBridge, "notifyWindowOpened",
                                                       "()V");
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
}

JNIEXPORT void JNICALL
Java_net_java_openjdk_cacio_ctc_CTCDesktopPeer_openFile(JNIEnv *env, jclass clazz,
                                                        jstring filePath) {
    DVMENV_ENTER()
    ensure_AWTBridge(dalvikEnv);
    if (class_AWTBridge != NULL && method_OpenPath != NULL) {
        const char *stringChars = (*env)->GetStringUTFChars(env, filePath, NULL);
        (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTBridge, method_OpenPath,
                                           (*dalvikEnv)->NewStringUTF(dalvikEnv, stringChars));
        (*env)->ReleaseStringUTFChars(env, filePath, stringChars);
    }
    DVMENV_EXIT()
}

JNIEXPORT void JNICALL
Java_net_java_openjdk_cacio_ctc_CTCDesktopPeer_openUri(JNIEnv *env, jclass clazz, jstring uri) {
    DVMENV_ENTER()
    ensure_AWTBridge(dalvikEnv);
    if (class_AWTBridge != NULL && method_OpenLink != NULL) {
        const char *stringChars = (*env)->GetStringUTFChars(env, uri, NULL);
        (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTBridge, method_OpenLink,
                                           (*dalvikEnv)->NewStringUTF(dalvikEnv, stringChars));
        (*env)->ReleaseStringUTFChars(env, uri, stringChars);
    }
    DVMENV_EXIT()
}

JNIEXPORT void JNICALL
Java_net_java_openjdk_cacio_ctc_NotifierWindowFactory_onToplevelWindowCreated(JNIEnv *env,
                                                                              jclass clazz) {
    DVMENV_ENTER()
    ensure_AWTBridge(dalvikEnv);
    if (class_AWTBridge != NULL && method_NotifyAwtWindow != NULL) {
        (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTBridge, method_NotifyAwtWindow);
    }
    DVMENV_EXIT()
}

JNIEXPORT void JNICALL
Java_com_github_caciocavallosilano_cacio_ctc_NotifierWindowFactory_onToplevelWindowCreated(
        JNIEnv *env, jclass clazz) {
    Java_net_java_openjdk_cacio_ctc_NotifierWindowFactory_onToplevelWindowCreated(env, clazz);
}
