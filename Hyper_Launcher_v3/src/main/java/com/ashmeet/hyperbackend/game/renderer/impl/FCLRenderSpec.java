package com.ashmeet.hyperbackend.game.renderer.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.ashmeet.hyperlauncher.plugin.Plugin;
import com.ashmeet.hyperlauncher.plugin.natives.NativePlugin;
import com.ashmeet.hyperlauncher.plugin.renderer.RendererPlugin;
import com.ashmeet.hyperlauncher.renderer.RendererInterface;

import net.ashmeet.hyperlauncher.R;
import com.ashmeet.hyperlauncher.utils.HyperUtils;
import com.ashmeet.hyperbackend.game.renderer.RenderSpec;

import java.io.File;
import java.util.List;
import java.util.Map;

import git.artdeell.mojoexec.MojoExec;

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
        String lib;
        String defaultDir = HyperUtils.NATIVE_LIB_DIR != null ? HyperUtils.NATIVE_LIB_DIR : "";
        String searchPath = defaultDir;

        if (rendererInterface != null) {
            lib = rendererInterface.getRendererLibrary();
            if (rendererInterface instanceof Plugin) {
                searchPath = ((Plugin) rendererInterface).getNativeLibPath();
            } else if (lib.startsWith("/")) {
                File libFile = new File(lib);
                if (libFile.getParent() != null) {
                    searchPath = libFile.getParent();
                }
            }
        } else if (rendererPlugin != null) {
            lib = rendererPlugin.getRendererLibrary();
            searchPath = rendererPlugin.getPath();
        } else if (plugin != null) {
            lib = null;
            searchPath = plugin.getPath();
        } else {
            lib = null;
        }

        if (lib == null || !lib.endsWith(".so") || lib.contains("plugin")) {
            Log.w(TAG, "Invalid or non-so library specified: " + lib + ", searching for .so in " + searchPath);
            File searchDir = new File(searchPath);
            if (searchDir.exists() && searchDir.isDirectory()) {
                File[] so = searchDir.listFiles((d, n) -> n.endsWith(".so"));
                if (so != null && so.length > 0) {
                    java.util.Arrays.sort(so);
                    for (File f : so) {
                        String n = f.getName().toLowerCase();
                        if (n.contains("egl") || n.contains("gl4es") || n.contains("angle") || n.contains("ltw")) {
                            Log.i(TAG, "Picked renderer lib: " + f.getAbsolutePath());
                            return f.getAbsolutePath();
                        }
                    }
                    Log.i(TAG, "Picked first available .so: " + so[0].getAbsolutePath());
                    return so[0].getAbsolutePath();
                }
            }
            lib = "libltw.so";
        }

        if (lib.startsWith("/")) {
            File f = new File(lib);
            if (f.exists()) return f.getAbsolutePath();
        }

        for (String dir : new String[]{searchPath, defaultDir}) {
            if (dir == null || dir.isEmpty()) continue;
            File f = new File(dir, lib);
            if (f.exists()) {
                Log.i(TAG, "Found renderer lib: " + f.getAbsolutePath());
                return f.getAbsolutePath();
            }
        }

        Log.w(TAG, "Renderer lib not found: " + lib + " in " + searchPath + ", falling back to ltw");
        String ltwLib = "libltw.so";
        for (String dir : new String[]{searchPath, defaultDir}) {
            if (dir == null || dir.isEmpty()) continue;
            File f = new File(dir, ltwLib);
            if (f.exists()) {
                Log.i(TAG, "Fallback renderer lib found: " + f.getAbsolutePath());
                return f.getAbsolutePath();
            }
        }

        Log.e(TAG, "Fallback ltw lib not found, returning: " + ltwLib);
        return ltwLib;
    }

    @Override
    public String librarySearchPath() {
        String defaultDir = HyperUtils.NATIVE_LIB_DIR != null ? HyperUtils.NATIVE_LIB_DIR : "";
        if (rendererInterface instanceof Plugin) {
            return ((Plugin) rendererInterface).getNativeLibPath();
        }
        if (rendererPlugin != null) {
            return rendererPlugin.getPath();
        }
        if (plugin != null) {
            return plugin.getPath();
        }
        if (rendererInterface != null) {
            String lib = rendererInterface.getRendererLibrary();
            if (lib.startsWith("/")) {
                File f = new File(lib);
                if (f.getParent() != null) {
                    return f.getParent();
                }
            }
        }
        return defaultDir;
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
        return MojoExec.prepareEgl(library(), true, true,4);
    }

    @Override
    public boolean compatibleDevice(Context context) {
        return true;
    }
}
