package com.ashmeet.hyperbackend.game.renderer.impl;

import android.content.Context;
import android.util.Log;


import com.ashmeet.hyperbackend.game.renderer.RenderSpec;
import com.ashmeet.hyperbackend.game.renderer.def.Renderers;
import com.ashmeet.hyperbackend.game.renderer.extra.GLESProvider;
import com.ashmeet.hyperbackend.utils.JREUtils;
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;
import com.ashmeet.hyperlauncher.utils.HyperUtils;

import net.ashmeet.hyperlauncher.R;

import java.io.File;
import java.util.Map;

import git.artdeell.mojoexec.MojoExec;

/**
 * Base GLES RenderSpec. Represents a desktop OpenGL wrapper running on-top of {@link GLESProvider}
 */
public abstract class GLESRenderSpec implements RenderSpec {
    private boolean nsBypass = false;
    protected abstract int glesVersion();
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        GLESProvider provider = GLESProvider.getGlesProvider(context, LauncherPreferences.PREF_USE_ANGLE);
        Log.i("GLESRenderSpec", "Using GLESProvider: " + provider.type());
        provider.setEnvironment(envMap);
        this.nsBypass = provider.requiresNamespace();
        if (LauncherPreferences.PREF_DUMP_SHADERS)
            envMap.put("LIBGL_VGPU_DUMP", "1");
        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");
        // Prevent OptiFine (and other error-reporting stuff in Minecraft) from balooning the log
        envMap.put("LIBGL_NOERROR", "1");
    }
    public boolean setupRenderer() {
        return MojoExec.prepareEgl(library(), nsBypass, true, glesVersion());
    }

    public static class LTWRenderSpec extends GLESRenderSpec {
        public boolean compatibleDevice(Context context) {
            String libName = this.library();
            boolean nativeLibExists = (HyperUtils.NATIVE_LIB_DIR != null && new File(HyperUtils.NATIVE_LIB_DIR, libName).exists()) ||
                    (context != null && context.getApplicationInfo() != null && context.getApplicationInfo().nativeLibraryDir != null && new File(context.getApplicationInfo().nativeLibraryDir, libName).exists());
            return nativeLibExists || JREUtils.getDetectedVersion() >= 2;
        }
        public String name() {
            return "OpenLTW";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_ltw;
        }
        public String tag() {
            return Renderers.LTW_RENDERER;
        }
        public String library() {
            return "libltw.so";
        }
        protected int glesVersion() {
            return 3;
        }
    }

    public static class MobileGluesRenderSpec extends GLESRenderSpec {
        public boolean compatibleDevice(Context context) {
            String libName = this.library();
            return new File(HyperUtils.NATIVE_LIB_DIR, libName).exists() || new File(context.getApplicationInfo().nativeLibraryDir, libName).exists();
        }
        public String name() {
            return "MobileGlues";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_mobileglues;
        }
        public String tag() {
            return Renderers.MOBILEGLUES_RENDERER;
        }
        public String library() {
            return "libmobileglues.so";
        }
        protected int glesVersion() {
            return 3;
        }
    }

    public static class GL4ESRenderSpec extends GLESRenderSpec {
        public boolean compatibleDevice(Context context) {
            return true;
        }
        public String name() {
            return "GL4ES";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_gles2_4;
        }
        public String tag() {
            return Renderers.GL4ES_RENDERER;
        }
        public String library() {
            return "libgl4es_114.so";
        }
        protected int glesVersion() {
            return 2;
        }
    }
}