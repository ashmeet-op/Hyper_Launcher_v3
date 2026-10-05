package net.kdt.pojavlaunch.game.renderer.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.ashmeet.hyperlauncher.plugin.Plugin;
import com.ashmeet.hyperlauncher.plugin.natives.NativePlugin;
import com.ashmeet.hyperlauncher.plugin.renderer.RendererPlugin;
import com.ashmeet.hyperlauncher.renderer.RendererInterface;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.game.renderer.RenderSpec;

import java.io.File;
import java.util.List;
import java.util.Map;

import git.artdeell.mojoexec.MojoExec;

/**
 * FCLRenderSpec represents a dynamic render spec provided by NativePlugin, RendererPlugin, or RendererInterface.
 */
public class FCLRenderSpec implements RenderSpec {
    private static final String TAG = "FCLRenderSpec";
    private final NativePlugin plugin;
    private final RendererPlugin rendererPlugin;
    private final RendererInterface rendererInterface;

    public FCLRenderSpec(NativePlugin plugin) {
        this.plugin = plugin;
        this.rendererPlugin = null;
        this.rendererInterface = null;
    }

    public FCLRenderSpec(RendererInterface rendererInterface) {
        this.rendererInterface = rendererInterface;
        this.rendererPlugin = (rendererInterface instanceof RendererPlugin) ? (RendererPlugin) rendererInterface : null;
        this.plugin = null;
    }

    @Override
    public String name() {
        if (rendererInterface != null) {
            return rendererInterface.getRendererName();
        }
        if (rendererPlugin != null) {
            return rendererPlugin.getDisplayName();
        }
        if (plugin != null) {
            plugin.getDisplayName();
            return plugin.getDisplayName();
        }
        return "FCL Renderer Plugin";
    }

    @Override
    public int displayName() {
        return R.string.mcl_setting_renderer_holy;
    }

    @Override
    public String tag() {
        if (rendererInterface != null) {
            return rendererInterface.getUniqueIdentifier();
        }
        if (rendererPlugin != null) {
            return rendererPlugin.getId();
        }
        if (plugin != null) {
            return plugin.getPackageName();
        }
        return "fcl_renderer";
    }




    @Override
    public String library() {
        if (rendererInterface != null) {
            return rendererInterface.getRendererLibrary();
        }
        if (rendererPlugin != null) {
            return rendererPlugin.getRendererLibrary();
        }
        if (plugin != null) {
            File dir = new File(plugin.getPath());
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
    public String librarySearchPath() {
        if (rendererInterface instanceof Plugin) {
            return ((Plugin) rendererInterface).getNativeLibPath();
        }
        if (rendererPlugin != null) {
            return rendererPlugin.getPath();
        }
        if (plugin != null) {
            return plugin.getPath();
        }
        return null;
    }

    @Override
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        if (rendererInterface != null) {
            envMap.putAll(rendererInterface.getRendererEnv().getValue());
        } else if (rendererPlugin != null) {
            envMap.putAll(rendererPlugin.getRendererEnv().getValue());
        } else if (plugin != null) {
            for (String envStr : plugin.getEnvList()) {
                if (envStr.contains("=")) {
                    String[] parts = envStr.split("=", 2);
                    envMap.put(parts[0], parts[1]);
                }
            }
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    @Override
    public boolean setupRenderer() {
        MojoExec.preloadVulkan();
        if (rendererInterface != null) {
            List<String> dlLibs = rendererInterface.getDlopenLibrary().getValue();
            for (String dlLib : dlLibs) {
                try {
                    System.load(dlLib);
                } catch (Throwable t) {
                    Log.w(TAG, "Failed to load dlopen library: " + dlLib, t);
                }
            }
        } else if (rendererPlugin != null) {
            for (String dlLib : rendererPlugin.getDlopenLibrary().getValue()) {
                try {
                    System.load(dlLib);
                } catch (Throwable t) {
                    Log.w(TAG, "Failed to load dlopen library: " + dlLib, t);
                }
            }
        }
        return MojoExec.prepareEgl(library(), true, true, 3);
    }

    @Override
    public boolean compatibleDevice(Context context) {
        return true;
    }
}
