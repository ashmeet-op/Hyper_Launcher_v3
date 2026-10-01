package net.kdt.pojavlaunch.game.renderer;

import static net.kdt.pojavlaunch.game.renderer.def.Renderers.FREEDRENO_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.LEGACYZINK_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MESA_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MESA_RENDERER_EXT;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.ZINK_RENDERER;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import com.ashmeet.hyperlauncher.plugins.interfaces.NativePlugin;
import com.ashmeet.hyperlauncher.plugins.manager.NativePluginManager;
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;
import com.ashmeet.hyperlauncher.utils.Tools;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.game.renderer.def.Renderers;
import net.kdt.pojavlaunch.game.renderer.impl.GLESRenderSpec;
import net.kdt.pojavlaunch.game.renderer.impl.MesaRenderSpec;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import git.artdeell.mojoexec.MojoExec;

/**
 * Class for managing game renderers (OpenGL ES & Vulkan)
 */
public class GameRenderer {
    public static final String LTW_RENDERER = Renderers.LTW_RENDERER;
    public static final String GL4ES_RENDERER = Renderers.GL4ES_RENDERER;
    public static final String MOBILEGLUES_RENDERER = Renderers.MOBILEGLUES_RENDERER;

    private final static String TAG = "Renderer";
    private final static String FALLBACK_RENDERER = GL4ES_RENDERER;
    private RenderSpec currentRenderer;
    private Map<String, String> environment = new HashMap<>();

    public GameRenderer(String currentRenderer) {
        this.currentRenderer = getKnownRenderer(currentRenderer);
        if(this.currentRenderer == null) this.currentRenderer = getKnownRenderer(GL4ES_RENDERER);
        if(this.currentRenderer == null) throw new IllegalStateException("Failed to create the current renderer!");
    }

    /**
     * Map renderer string to a known RenderSpec
     *
     * @param renderer renderer string
     * @return RenderSpec instance if found, null otherwise
     */
    public static RenderSpec getKnownRenderer(String renderer) {
        switch (renderer) {
            // For compatibility
            case "opengles2_4":
            case "opengles2_5":
            case "holy":
            case GL4ES_RENDERER: return new GLESRenderSpec.GL4ESRenderSpec();
            case LTW_RENDERER: return new GLESRenderSpec.LTWRenderSpec();
            case MOBILEGLUES_RENDERER: return new GLESRenderSpec.MobileGluesRenderSpec();
            case ZINK_RENDERER: return new MesaRenderSpec.ZinkRenderSpec();
            case FREEDRENO_RENDERER: return new MesaRenderSpec.FreedrenoRenderSpec();
            case MESA_RENDERER: return new MesaRenderSpec();
            case MESA_RENDERER_EXT: return new MesaRenderSpec.ExtMesaRenderSpec();
            case LEGACYZINK_RENDERER: return new MesaRenderSpec.LegacyZinkRenderSpec();
            default:
                for (NativePlugin plugin : NativePluginManager.getPlugins()) {
                    if (renderer.equals(plugin.getRendererName())) {
                        return new FCLRenderSpec(plugin);
                    }
                }
                Log.e(TAG, "Unknown renderer " + renderer);
                return null;
        }
    }

    public static class FCLRenderSpec implements RenderSpec {
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

        @Override
        public String library() {
            Map<String, String> env = plugin.getJVMEnv();
            String egl = env.get("POJAVEXEC_EGL");
            if (egl == null) egl = env.get("LIBGL_EGL");
            if (egl == null) egl = env.get("POJAV_EGL");
            if (egl != null && !egl.isEmpty()) {
                return egl;
            }
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
            return MojoExec.prepareEgl(library(), true, false, 3);
        }

        @Override
        public boolean compatibleDevice(Context context) {
            return plugin.supportsVersion(null);
        }
    }

    /**
     * Set renderer library path
     *
     * @param mainPath       base library path
     * @param additionalPath additional library path to search libs at
     */
    public static void setRendererLibraryPath(String mainPath, String additionalPath) {
        if (additionalPath != null) mainPath = additionalPath + ":" + mainPath;
        MojoExec.setNativeLibraryDir(mainPath);
    }

    public void setupEnvironment(Context context, Map<String, String> envMap) {
        currentRenderer.setupEnvironment(context, envMap);
    }

    /**
     * Setup current selected renderer environment. Call before using {@link GameRenderer#maybeSetupRenderer()}
     *
     * @param context application context
     * @throws ErrnoException if underlying Os#setenv call threw an exception
     */
    public void setupEnvironment(Context context) throws ErrnoException {
        if(environment == null) {
            Log.w(TAG, "Tried to call setupEnvironment in already initialized environment");
            return;
        }
        currentRenderer.setupEnvironment(context, environment);
        for(Map.Entry<String, String> e : environment.entrySet()) {
            Logger.appendToLog("Added renderer env: " + e.getKey() + '=' + e.getValue());
            Os.setenv(e.getKey(), e.getValue(), true);
        }
        environment.clear();
        environment = null;
    }

    /**
     * Get current selected renderer in this GameRenderer instance
     *
     * @return renderer
     */
    public RenderSpec getCurrentRenderer() {
        return currentRenderer;
    }

    /**
     * Set current selected renderer. Call this before {@link GameRenderer#setupEnvironment} or bad things may happen
     *
     * @param spec renderer
     */
    public void setCurrentRenderer(RenderSpec spec) {
        Log.i(TAG, "Replacing default renderer with the new: " + spec.name());
        currentRenderer = spec;
    }

    /**
     * Set current selected renderer. Call this before {@link GameRenderer#setupEnvironment} or bad things may happen
     *
     * @param renderer renderer string
     * @throws IllegalArgumentException if incorrect renderer string is given
     */
    public void setCurrentRenderer(String renderer) throws IllegalArgumentException {
        RenderSpec spec = getKnownRenderer(renderer);
        if(spec == null) throw new IllegalArgumentException("Invalid renderer string" + renderer + "!");
        this.setCurrentRenderer(spec);
    }

    /**
     * Set up the current renderer or fallback to {@link GameRenderer#FALLBACK_RENDERER} if failed
     *
     * @return whether the renderer setup was successful
     */
    public boolean maybeSetupRenderer() {
        setRendererLibraryPath(Tools.NATIVE_LIB_DIR, currentRenderer.librarySearchPath());
        if (!currentRenderer.setupRenderer()) {
            Log.e(TAG, "Failed to setup renderer " + currentRenderer.name() + ", falling back to " + FALLBACK_RENDERER);
            // Hopefully (yes, it's going to be fun if it returns null for the fallback renderer. Shouldn't happen though)
            return getKnownRenderer(FALLBACK_RENDERER).setupRenderer();
        }
        return true;
    }

    /**
     * Enable custom Vulkan driver (Turnip) usage
     */
    public void overrideVulkanDriver() {
        if(LauncherPreferences.PREF_FREEDRENO_SYSMEM) environment.put("TU_DEBUG", "sysmem");
        MojoExec.setUseTurnip(true);
    }

    /**
     * Release compatible renderers cache
     */
    public static void releaseCache() {
        RendererCache.releaseRendererCache();
    }
}
