package net.kdt.pojavlaunch.utils;

import android.util.Log;

public class GameOptionsUtils {

    public static int parseIntDefault(String value, int defaultValue) {
        if(value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        }catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    private static void fixDeathCloud() {
        GLInfoUtils.GLInfo info = GLInfoUtils.getGlInfo();
        if(!info.isArm()) return;
        int cloudRange = parseIntDefault(MCOptionUtils.get("cloudRange"), 128);
        if(cloudRange <= 64) return;
        MCOptionUtils.set("cloudRange", "64");
    }


    private static void disableNarrator() {
        if(parseIntDefault(MCOptionUtils.get("narrator"), 0) == 0) return;
        MCOptionUtils.set("narrator", "0");
    }


    private static void disableFullscreen() {
        String fullscreen = MCOptionUtils.get("fullscreen");
        if(fullscreen == null) return;
        if(fullscreen.equals("true")) MCOptionUtils.set("fullscreen", "false");
        else if(fullscreen.equals("1")) MCOptionUtils.set("fullscreen","0");
    }

    private static void enableCape() {
        MCOptionUtils.set("showCape", "true");
        MCOptionUtils.set("modelPart_cape", "true");
    }

    public static void fixOptions(boolean isLtw) {
        try {
            MCOptionUtils.load();
        }catch (Exception e) {
            Log.e("Tools", "Failed to load config", e);
        }

        if(isLtw) fixDeathCloud();
        disableFullscreen();
        disableNarrator();
        enableCape();

        try {
            MCOptionUtils.save();
        }catch (Exception e) {
            Log.e("Tools", "Failed to save config", e);
        }
    }
}
