#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
#ifndef DISCORDPP_H
#define DISCORDPP_H

#include <string>
#include <functional>
#include <cstdint>
#include <chrono>
#include <mutex>
#include <android/log.h>

#define DISCORD_TAG "DiscordPP"

namespace discordpp {

enum class ActivityTypes {
    Playing = 0,
    Streaming = 1,
    Listening = 2,
    Watching = 3,
    Competing = 5
};

class ActivityTimestamps {
    uint64_t start_time = 0;
    uint64_t end_time = 0;
public:
    void SetStart(uint64_t t) { start_time = t; }
    void SetEnd(uint64_t t) { end_time = t; }
};

class ActivityAssets {
    std::string large_image;
    std::string large_text;
    std::string small_image;
    std::string small_text;
public:
    void SetLargeImage(const std::string& img) { large_image = img; }
    void SetLargeText(const std::string& text) { large_text = text; }
    void SetSmallImage(const std::string& img) { small_image = img; }
    void SetSmallText(const std::string& text) { small_text = text; }
};

class Activity {
    ActivityTypes type = ActivityTypes::Playing;
    std::string details;
    std::string state;
    ActivityTimestamps timestamps;
    ActivityAssets assets;
public:
    void SetType(ActivityTypes t) { type = t; }
    void SetDetails(const char* d) { if(d) details = d; }
    void SetState(const char* s) { if(s) state = s; }
    void SetTimestamps(const ActivityTimestamps& ts) { timestamps = ts; }
    void SetAssets(const ActivityAssets& a) { assets = a; }
};

struct ClientResult {
    bool success = true;
    [[nodiscard]] bool Successful() const { return success; }
};

class Client {
    uint64_t app_id = 0;
    std::mutex mtx;
public:
    void SetApplicationId(uint64_t id) {
        std::lock_guard<std::mutex> lock(mtx);
        app_id = id;
        __android_log_print(ANDROID_LOG_INFO, DISCORD_TAG, "Client Application ID set to %llu", (unsigned long long)app_id);
    }

    static void UpdateRichPresence(const Activity&, const std::function<void(ClientResult)>& cb) {
        __android_log_print(ANDROID_LOG_INFO, DISCORD_TAG, "UpdateRichPresence called");
        if (cb) cb(ClientResult{true});
    }

    void Authorize(const std::function<void(ClientResult)>& cb) {
        std::lock_guard<std::mutex> lock(mtx);
        __android_log_print(ANDROID_LOG_INFO, DISCORD_TAG, "Discord Authorize / OAuth2 flow initiated natively");
        if (cb) cb(ClientResult{true});
    }
};

inline void RunCallbacks() {
    // Poll callbacks
}

} // namespace discordpp

#endif // DISCORDPP_H

#pragma clang diagnostic pop
