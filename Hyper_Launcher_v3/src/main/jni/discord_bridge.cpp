#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"
#include <jni.h>
#include <memory>
#include <ctime>

static std::shared_ptr<discordpp::Client> gClient;

extern "C" JNIEXPORT void JNICALL
Java_com_ashmeet_hyperlauncher_utils_Discord_init(JNIEnv*, jobject, jlong appId) {
    gClient = std::make_shared<discordpp::Client>();
    gClient->SetApplicationId((uint64_t)appId);   // no Connect() needed
}

extern "C" JNIEXPORT void JNICALL
Java_com_ashmeet_hyperlauncher_utils_Discord_setPresence(JNIEnv* env, jobject, jstring jDetails, jstring jState) {
    if (!gClient) return;
    const char* d = env->GetStringUTFChars(jDetails, nullptr);
    const char* s = env->GetStringUTFChars(jState, nullptr);

    discordpp::Activity activity;
    activity.SetType(discordpp::ActivityTypes::Playing);
    activity.SetDetails(d);
    activity.SetState(s);

    discordpp::ActivityTimestamps ts;
    ts.SetStart(time(nullptr));          // elapsed timer
    activity.SetTimestamps(ts);

    discordpp::ActivityAssets assets;
    assets.SetLargeImage("hyper_logo");  // your asset key, lowercase
    assets.SetLargeText("Hyper Launcher");
    activity.SetAssets(assets);

    gClient->UpdateRichPresence(activity, [](discordpp::ClientResult r) {
        // check r.Successful()
    });

    env->ReleaseStringUTFChars(jDetails, d);
    env->ReleaseStringUTFChars(jState, s);
}

extern "C" JNIEXPORT void JNICALL
Java_com_ashmeet_hyperlauncher_utils_Discord_runCallbacks(JNIEnv*, jobject) {
    discordpp::RunCallbacks();
}

extern "C" JNIEXPORT void JNICALL
Java_com_ashmeet_hyperlauncher_utils_Discord_authorize(JNIEnv*, jobject) {
    if (gClient) {
        gClient->Authorize([](discordpp::ClientResult) {
            // handle authorization result
        });
    }
}
