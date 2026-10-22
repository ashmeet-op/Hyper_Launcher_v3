//
// Created by whbex on 20.08.2026.
//

#include <jni.h>

#include "awt.h"

static jclass class_CTCClipboard = NULL;
static jmethodID method_putClipboardString = NULL;
static jmethodID method_queryClipboardString = NULL;
static jmethodID method_SystemClipboardDataReceived = NULL;

void register_methods_clipboard(JNIEnv* env) {
    if (class_AWTBridge == NULL) return;

    method_queryClipboardString = (*env)->GetStaticMethodID(env, class_AWTBridge, "queryClipboardString", "()V");
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);

    method_putClipboardString = (*env)->GetStaticMethodID(env, class_AWTBridge, "putClipboardString", "(Ljava/lang/String;)V");
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
}

JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeClipboardReceived(JNIEnv *env, jclass clazz, jstring clipboardData, jstring clipboardDataMime) {
    if(method_SystemClipboardDataReceived == NULL || class_CTCClipboard == NULL) return;
    if (JNIEnv_InputRuntime == NULL) {
        if (runtimeVM == NULL) {
            return;
        } else {
            (*runtimeVM)->AttachCurrentThreadAsDaemon(runtimeVM, &JNIEnv_InputRuntime, NULL);
        }
    }
    const char* dataChars = clipboardData != NULL ? (*env)->GetStringUTFChars(env, clipboardData, NULL) : NULL;
    const char* mimeChars = clipboardDataMime != NULL ? (*env)->GetStringUTFChars(env, clipboardDataMime, NULL) : NULL;
    (*JNIEnv_InputRuntime)->CallStaticVoidMethod(JNIEnv_InputRuntime, class_CTCClipboard, method_SystemClipboardDataReceived,
                                                    clipboardData != NULL ? (*JNIEnv_InputRuntime)->NewStringUTF(JNIEnv_InputRuntime, dataChars) : NULL,
                                                    clipboardDataMime != NULL ? (*JNIEnv_InputRuntime)->NewStringUTF(JNIEnv_InputRuntime, mimeChars) : NULL);
    if(dataChars != NULL) (*env)->ReleaseStringUTFChars(env, clipboardData, dataChars);
    if(mimeChars != NULL) (*env)->ReleaseStringUTFChars(env, clipboardDataMime, mimeChars);
}

JNIEXPORT void JNICALL Java_git_artdeell_mojo_awt_AWTBridge_nativeClipboardReceived(JNIEnv *env, jclass clazz, jstring clipboardData, jstring clipboardDataMime) {
    Java_net_kdt_pojavlaunch_awt_AWTBridge_nativeClipboardReceived(env, clazz, clipboardData, clipboardDataMime);
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCClipboard_nQuerySystemClipboard(JNIEnv *env, jclass clazz) {
    DVMENV_ENTER()
    ensure_AWTBridge(dalvikEnv);
    if(method_SystemClipboardDataReceived == NULL) {
        class_CTCClipboard = (*env)->NewGlobalRef(env, clazz);
        method_SystemClipboardDataReceived = (*env)->GetStaticMethodID(env, clazz, "systemClipboardDataReceived", "(Ljava/lang/String;Ljava/lang/String;)V");
        if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
    }
    if (class_AWTBridge != NULL && method_queryClipboardString != NULL) {
        (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTBridge, method_queryClipboardString);
    }
    DVMENV_EXIT()
}

JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCClipboard_nPutClipboardData(JNIEnv* env, jclass clazz, jstring clipboardData, jstring clipboardDataMime) {
    DVMENV_ENTER()
    ensure_AWTBridge(dalvikEnv);
    if (class_AWTBridge != NULL && method_putClipboardString != NULL) {
        const char* dataChars = (*env)->GetStringUTFChars(env, clipboardData, NULL);
        (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv, class_AWTBridge, method_putClipboardString,
                                           (*dalvikEnv)->NewStringUTF(dalvikEnv, dataChars));
        (*env)->ReleaseStringUTFChars(env, clipboardData, dataChars);
    }
    DVMENV_EXIT()
}

JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_CTCClipboard_nQuerySystemClipboard(JNIEnv *env, jclass clazz) {
    Java_net_java_openjdk_cacio_ctc_CTCClipboard_nQuerySystemClipboard(env, clazz);
}

JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_CTCClipboard_nPutClipboardData(JNIEnv* env, jclass clazz, jstring clipboardData, jstring clipboardDataMime) {
    Java_net_java_openjdk_cacio_ctc_CTCClipboard_nPutClipboardData(env, clazz, clipboardData, clipboardDataMime);
}
