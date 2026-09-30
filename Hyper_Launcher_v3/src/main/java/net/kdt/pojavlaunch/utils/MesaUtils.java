package net.kdt.pojavlaunch.utils;

import android.content.Context;

import com.ashmeet.hyperlauncher.utils.Architecture;
import com.ashmeet.hyperlauncher.plugins.natives.LibraryPlugin;
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;

import java.util.Map;


public class MesaUtils {

    public static final String MESA_EGL = "libEGL_mesa.so";
    public static final String MESA_EGL_LEGACY = "libEGL_legacy.so";

    private static LibraryPlugin zink;

    public static void initEnvironment(Context context, String renderer, Map<String, String> envMap){
        switch(renderer) {
            case "vulkan_zink":
                envMap.put("GALLIUM_DRIVER", "zink");
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");


                envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
                if(!Architecture.isx86Device() && (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm())) {
                    zink = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ZINK_PLUGIN);
                    if(zink == null) return;

                    envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                }
                break;
            case "freedreno_kgsl":
                if(GLInfoUtils.getGlInfo().isAdreno()) {
                    envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "kgsl");


                    if(GLInfoUtils.getGlInfo().isAdreno500Lower()) {
                        envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                        envMap.put("MESA_GLSL_VERSION_OVERRIDE", "330");
                    }
                }
                break;
        }
    }


    public static void destroyZink(){
        if(zink != null) {
            zink = null;
            System.gc();
        }
    }


    public static String getPreferredEGL() {
        if (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm()) {
            if (zink == null) return MESA_EGL;
            if (!zink.checkLibraries(MESA_EGL_LEGACY)) return MESA_EGL;
            return zink.resolveAbsolutePath(MESA_EGL_LEGACY);
        } else return MESA_EGL;
    }


    public static String getCustomZinkLibraryPath() {
        if ((LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm()) && zink != null)
            return zink.getLibraryPath();
        return null;
    }
}
