package com.ashmeet.hyperbackend.game.renderer;

import static com.ashmeet.hyperbackend.game.renderer.def.Renderers.FREEDRENO_RENDERER;
import static com.ashmeet.hyperbackend.game.renderer.def.Renderers.LEGACYZINK_RENDERER;
import static com.ashmeet.hyperbackend.game.renderer.def.Renderers.MESA_RENDERER;
import static com.ashmeet.hyperbackend.game.renderer.def.Renderers.MESA_RENDERER_EXT;
import static com.ashmeet.hyperbackend.game.renderer.def.Renderers.ZINK_RENDERER;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import com.ashmeet.hyperbackend.Logger;
import com.ashmeet.hyperbackend.vulkan.driver.Driver;
import com.ashmeet.hyperlauncher.plugin.natives.NativePlugin;
import com.ashmeet.hyperlauncher.plugin.natives.NativePluginManager;
import com.ashmeet.hyperlauncher.plugin.renderer.RendererPlugin;
import com.ashmeet.hyperlauncher.plugin.renderer.RendererPluginManager;
import com.ashmeet.hyperlauncher.plugin.renderer_v2.RendererV2Data;
import com.ashmeet.hyperlauncher.plugin.renderer_v2.RendererV2PluginManager;
import com.ashmeet.hyperlauncher.renderer.RendererInterface;
import com.ashmeet.hyperlauncher.renderer.Renderers;
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;
import com.ashmeet.hyperlauncher.utils.HyperUtils;
import com.ashmeet.hyperbackend.game.renderer.impl.FCLRenderSpec;
import com.ashmeet.hyperbackend.game.renderer.impl.GLESRenderSpec;
import com.ashmeet.hyperbackend.game.renderer.impl.MesaRenderSpec;
import com.ashmeet.hyperbackend.vulkan.DriverManager;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import git.artdeell.mojoexec.MojoExec;

/*
 How to add an extra renderer (guide 2026 mediafire works):

 1. Create RenderSpec for that renderer
 2. Add all requires locale strings and wire them up inside your freshly cooked RenderSpec
 3. Add the renderer tag onto the getKnownRenderer() mapping
 4. (Optional): Add the renderer tag into constant list in Renderers class
 5. Add the renderer tag onto the list of renderers to check for the compatibility (see RendererCache)
 6. ???
 7. PROFIT

*/

/**
 * Class for managing game renderers (OpenGL ES & Vulkan)
 */
public class GameRenderer {
    public static final String LTW_RENDERER = com.ashmeet.hyperbackend.game.renderer.def.Renderers.LTW_RENDERER;
    public static final String GL4ES_RENDERER = com.ashmeet.hyperbackend.game.renderer.def.Renderers.GL4ES_RENDERER;
    public static final String MOBILEGLUES_RENDERER = com.ashmeet.hyperbackend.game.renderer.def.Renderers.MOBILEGLUES_RENDERER;

    private final static String TAG = "Renderer";
    private final static String FALLBACK_RENDERER = GL4ES_RENDERER;
    private RenderSpec currentRenderer;
    private Map<String, String> environment = new HashMap<>();

    public GameRenderer(String currentRenderer) {
        this.currentRenderer = getKnownRenderer(currentRenderer);
        if(this.currentRenderer == null) this.currentRenderer = getKnownRenderer(GL4ES_RENDERER);
        if(this.currentRenderer == null) throw new IllegalStateException("Failed to create the current renderer!");
        // Setting it up here so SDL/GLFW can take it later
        MojoExec.setUseBigCoreAffinity(LauncherPreferences.PREF_BIG_CORE_AFFINITY);
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
            case GL4ES_RENDERER: return new GLESRenderSpec.GL4ESRenderSpec();
            case LTW_RENDERER: return new GLESRenderSpec.LTWRenderSpec();
            case MOBILEGLUES_RENDERER: return new GLESRenderSpec.MobileGluesRenderSpec();
            case ZINK_RENDERER: return new MesaRenderSpec.ZinkRenderSpec();
            case FREEDRENO_RENDERER: return new MesaRenderSpec.FreedrenoRenderSpec();
            case MESA_RENDERER: return new MesaRenderSpec();
            case MESA_RENDERER_EXT: return new MesaRenderSpec.ExtMesaRenderSpec();
            case LEGACYZINK_RENDERER: return new MesaRenderSpec.LegacyZinkRenderSpec();
            default:
                for (RendererInterface pluginRenderer : Renderers.INSTANCE.getRenderers()) {
                    if (renderer.equals(pluginRenderer.getUniqueIdentifier()) ||
                        renderer.equals(pluginRenderer.getRendererId()) ||
                        renderer.equals(pluginRenderer.getRendererName())) {
                        return new FCLRenderSpec(pluginRenderer);
                    }
                }
                for (RendererPlugin plugin : RendererPluginManager.INSTANCE.getRendererList()) {
                    if (renderer.equals(plugin.getPackageName()) ||
                        renderer.equals(plugin.getId()) ||
                        renderer.equals(plugin.getDisplayName())) {
                        return new FCLRenderSpec(plugin);
                    }
                }
                for (RendererV2Data plugin : RendererV2PluginManager.INSTANCE.getRendererList()) {
                    if (renderer.equals(plugin.getPackageName()) ||
                        renderer.equals(plugin.getRendererId()) ||
                        renderer.equals(plugin.getRendererName())) {
                        return new FCLRenderSpec(plugin);
                    }
                }
                for (NativePlugin plugin : NativePluginManager.INSTANCE.getPlugins()) {
                    if (renderer.equals(plugin.getPackageName()) || renderer.equals(plugin.getAppName())) {
                        return new FCLRenderSpec(plugin);
                    }
                }
                Log.e(TAG, "Unknown renderer " + renderer);
                return null;
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

    public void setupEnvironment(Context context, Map<String, String> envMap) {
        if (envMap != null) {
            currentRenderer.setupEnvironment(context, envMap);
        }
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
        setRendererLibraryPath(HyperUtils.NATIVE_LIB_DIR, currentRenderer.librarySearchPath());
        if (!currentRenderer.setupRenderer()) {
            String fallback = (currentRenderer instanceof FCLRenderSpec) ? LTW_RENDERER : FALLBACK_RENDERER;
            Log.e(TAG, "Failed to setup renderer " + currentRenderer.name() + ", falling back to " + fallback);
            RenderSpec fallbackSpec = getKnownRenderer(fallback);
            if (fallbackSpec != null) {
                currentRenderer = fallbackSpec;
                return currentRenderer.setupRenderer();
            }
            return false;
        }
        return true;
    }

    /**
     * Enable custom Vulkan driver (Turnip) usage
     */
    public void overrideVulkanDriver() {
        if (environment == null) {
            environment = new HashMap<>();
        }
        // In most cases we load Turnip so use Turnip environment variables
        if(LauncherPreferences.PREF_FREEDRENO_SYSMEM) environment.put("TU_DEBUG", "sysmem");
        if(LauncherPreferences.PREF_UBWC_WORKAROUND) environment.put("FD_DEV_FEATURES", "enable_tp_ubwc_flag_hint=1");

        Driver driver = DriverManager.getPreferredDriver();
        File root = DriverManager.getDriverRoot(driver);
        File config = new File(HyperUtils.DIR_GAME_HOME, "qgl_config.txt");
        if(config.exists() && config.isFile()) MojoExec.setQglConfigPath(config.getAbsolutePath());
        MojoExec.configureCustomVulkanDriver(driver.getMainLibrary(), root == null ? null : root.getAbsolutePath());
    }

    /**
     * Release compatible renderers cache
     */
    public static void releaseCache() {
        RendererCache.releaseRendererCache();
    }
}
