package net.kdt.pojavlaunch.game.renderer.impl;

import android.content.Context;

import com.ashmeet.hyperlauncher.plugins.interfaces.NativePlugin;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.game.renderer.RenderSpec;

import java.io.File;
import java.util.Map;

import git.artdeell.mojoexec.MojoExec;

/**
 * FCLRenderSpec represents a dynamic render spec provided by NativePlugin.
 */
public class FCLRenderSpec implements RenderSpec {
    private final NativePlugin plugin;

    public FCLRenderSpec(NativePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return plugin.getDisplayName() != null ? plugin.getDisplayName() : plugin.getRendererName();
    }

    @Override
    public int displayName() {
        return R.string.mcl_setting_renderer_holy;
    }

    @Override
    public String tag() {
        return plugin.getRendererName();
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public String library() {
        Map<String, String> env = plugin.getJVMEnv();
        if (env.containsKey("POJAVEXEC_EGL") && !env.get("POJAVEXEC_EGL").isEmpty()) return env.get("POJAVEXEC_EGL");
        if (env.containsKey("LIBGL_EGL") && !env.get("LIBGL_EGL").isEmpty()) return env.get("LIBGL_EGL");
        if (env.containsKey("POJAV_EGL") && !env.get("POJAV_EGL").isEmpty()) return env.get("POJAV_EGL");
        for (String path : plugin.getPaths()) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] soFiles = dir.listFiles((d, name) -> name.endsWith(".so"));
                if (soFiles != null && soFiles.length > 0) {
                    return soFiles[0].getAbsolutePath();
                }
            }
        }
        return "libgl4es_114.so";
    }

    @Override
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        envMap.putAll(plugin.getJVMEnv());
    }

    @Override
    public boolean setupRenderer() {
        MojoExec.preloadVulkan();
        return MojoExec.prepareEgl(library(), true, true, 3);
    }

    @Override
    public boolean compatibleDevice(Context context) {
        return plugin.supportsVersion(null);
    }
}
