package com.ashmeet.hyperbackend.game.renderer.impl;

import android.content.Context;


import com.ashmeet.hyperbackend.game.renderer.RenderSpec;
import com.ashmeet.hyperbackend.game.renderer.def.Renderers;
import com.ashmeet.hyperlauncher.utils.GpuUtils;
import com.ashmeet.hyperlauncher.utils.plugins.LibraryPlugin;
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;
import com.ashmeet.hyperlauncher.utils.HyperUtils;

import net.ashmeet.hyperlauncher.R;

import java.io.File;
import java.util.Map;


import git.artdeell.mojoexec.MojoExec;

/**
 * Mesa3D RenderSpec. Provides desktop Mesa, zink & freedreno
 */
public class MesaRenderSpec implements RenderSpec {
    public String library() {
        return "libEGL_mesa.so";
    }
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        assert HyperUtils.DIR_CACHE != null;
        envMap.put("MESA_GLSL_CACHE_DIR", HyperUtils.DIR_CACHE.getAbsolutePath());
    }
    protected boolean hasMesa(Context context) {
        String libName = this.library();
        boolean inNativeLib = HyperUtils.NATIVE_LIB_DIR != null && new File(HyperUtils.NATIVE_LIB_DIR, libName).exists();
        boolean inAppNativeDir = context != null && context.getApplicationInfo() != null && context.getApplicationInfo().nativeLibraryDir != null && new File(context.getApplicationInfo().nativeLibraryDir, libName).exists();
        return inNativeLib || inAppNativeDir;
    }
    public boolean compatibleDevice(Context context) {
        return hasMesa(context) && GpuUtils.checkChromebook(context.getPackageManager());
    }
    public String name() {
        return "Mesa";
    }
    public int displayName() {
        return R.string.mcl_setting_renderer_mesa_desktop;
    }
    public String tag() {
        return Renderers.MESA_RENDERER;
    }
    public boolean setupRenderer() {
        return MojoExec.prepareEgl(library(), true, false, 3);
    }

    public static class ZinkRenderSpec extends MesaRenderSpec {
        public String name() {
            return "ZINK";
        }
        public String tag() {
            return Renderers.ZINK_RENDERER;
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_vulkan_zink;
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
            // This is needed because mobile drivers often don't implement required features for zink
            // hence making it fall back to OpenGL 2.1 and break the modern game completely
            // We don't care much about passing CTS hence this is fine
            envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
            envMap.put("MESA_GL_VERSION_OVERRIDE", "4.6");
            super.setupEnvironment(context, envMap);
        }
        public boolean setupRenderer() {
            MojoExec.preloadVulkan();
            return super.setupRenderer();
        }
        public boolean compatibleDevice(Context context) {
            return hasMesa(context) && GpuUtils.checkVulkanSupport(context.getPackageManager());
        }
    }

    public static class FreedrenoRenderSpec extends MesaRenderSpec {
        public String name() {
            return "Freedreno";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_freedreno_kgsl;
        }
        public String tag() {
            return Renderers.FREEDRENO_RENDERER;
        }
        public boolean compatibleDevice(Context context) {
            return hasMesa(context) && GpuUtils.getGlInfo().isAdreno();
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            if (LauncherPreferences.PREF_FREEDRENO_SYSMEM) envMap.put("FD_MESA_DEBUG", "sysmem");
            if(LauncherPreferences.PREF_UBWC_WORKAROUND) envMap.put("FD_DEV_FEATURES", "enable_tp_ubwc_flag_hint=1");
            envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "kgsl");
            // On Adreno 5XX and lower only Core 3.1 is exposed by default due to missing hardware extensions.
            // 3.3 is required for modern games so let's force 3.3 if running on such GPU - it's known to be working.
            if (GpuUtils.getGlInfo().isAdreno500Lower()) {
                envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                envMap.put("MESA_GLSL_VERSION_OVERRIDE", "330");
            }
            super.setupEnvironment(context, envMap);
        }
    }
    public static class ExtMesaRenderSpec extends MesaRenderSpec {
        private LibraryPlugin provider;
        protected String plugin() {
            return LibraryPlugin.ID_MESA_PLUGIN;
        }
        public String name() {
            return "Mesa (external)";
        }
        public String librarySearchPath() {
            return provider.getLibraryPath();
        }
        public String tag() {
            return Renderers.MESA_RENDERER_EXT;
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_mesa_desktop_ext;
        }
        private boolean discover(Context context) {
            if(provider == null) provider = LibraryPlugin.discoverPlugin(context, plugin());
            return provider != null;
        }
        public boolean compatibleDevice(Context context) {
            return discover(context) && provider.checkLibraries(library());
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            discover(context);
            super.setupEnvironment(context, envMap);
        }
        public boolean setupRenderer() {
            if(provider == null) return false;
            return MojoExec.prepareEgl(provider.resolveAbsolutePath(library()), true, false, 0);
        }
    }
    public static class LegacyZinkRenderSpec extends ExtMesaRenderSpec {
        protected String plugin() {
            return LibraryPlugin.ID_ZINK_PLUGIN;
        }
        public String name() {
            return "ZINK (Legacy)";
        }
        public String tag() {
            return Renderers.LEGACYZINK_RENDERER;
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_vulkan_lzink;
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            super.setupEnvironment(context, envMap);
            envMap.put("MESA_GL_VERSION_OVERRIDE", "4.3");
            envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
            envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
        }
        public boolean setupRenderer() {
            MojoExec.preloadVulkan();
            return super.setupRenderer();
        }
        public boolean compatibleDevice(Context context) {
            return GpuUtils.checkVulkanSupport(context.getPackageManager()) && super.compatibleDevice(context);
        }
        public String library() {
            return "libEGL_legacy.so";
        }
    }
}