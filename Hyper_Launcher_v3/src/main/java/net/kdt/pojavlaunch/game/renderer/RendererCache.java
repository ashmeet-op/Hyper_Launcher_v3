package net.kdt.pojavlaunch.game.renderer;

import static net.kdt.pojavlaunch.game.renderer.def.Renderers.FREEDRENO_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.GL4ES_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.LEGACYZINK_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.LTW_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MESA_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MESA_RENDERER_EXT;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MOBILEGLUES_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.ZINK_RENDERER;

import android.content.Context;
import android.content.res.Resources;

import com.ashmeet.hyperlauncher.plugins.interfaces.NativePlugin;
import com.ashmeet.hyperlauncher.plugins.manager.NativePluginManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatible renderers cache. Used for the UI renderer list
 */
public class RendererCache {
    private static RendererCache sCompatibleRenderers;

    public final List<String> rendererIds;
    public final String[] rendererDisplayNames;

    public RendererCache(List<String> rendererIds, String[] rendererDisplayNames) {
        this.rendererIds = rendererIds;
        this.rendererDisplayNames = rendererDisplayNames;
    }

    /**
     * Return a list of renderers compatible with the current device
     * Don't forget to clean the cache when the list isn't needed anymore (i.e. when starting the game) - {@link GameRenderer#releaseCache()}
     *
     * @param context application context
     * @return RendererCache containing all compatible renderers
     */
    public static RendererCache getCompatibleRenderers(Context context) {
        if (sCompatibleRenderers != null) return sCompatibleRenderers;
        Resources resources = context.getResources();
        // This is the list that controls em all!
        String[] renderers = {
                GL4ES_RENDERER, LTW_RENDERER, MOBILEGLUES_RENDERER, ZINK_RENDERER, FREEDRENO_RENDERER, MESA_RENDERER, MESA_RENDERER_EXT, LEGACYZINK_RENDERER
        };
        ArrayList<String> rendererIds = new ArrayList<>(renderers.length);
        ArrayList<String> rendererNames = new ArrayList<>(renderers.length);
        for (String renderer : renderers) {
            RenderSpec r = GameRenderer.getKnownRenderer(renderer);
            if (r == null) continue;
            if (!r.compatibleDevice(context)) continue;
            rendererIds.add(renderer);
            rendererNames.add(resources.getString(r.displayName()));
        }

        for (NativePlugin plugin : NativePluginManager.getPlugins()) {
            String rendererId = plugin.getRendererName();
            if (rendererId != null && !rendererIds.contains(rendererId)) {
                rendererIds.add(rendererId);
                String displayName = plugin.getDisplayName() != null ? plugin.getDisplayName() : ("FCL: " + rendererId);
                String pluginName = plugin.getName();
                if (pluginName != null) {
                    rendererNames.add(displayName + " (from " + pluginName + " plugin)");
                } else {
                    rendererNames.add(displayName);
                }
            }
        }

        rendererIds.trimToSize();
        rendererNames.trimToSize();
        return (sCompatibleRenderers = new RendererCache(rendererIds, rendererNames.toArray(new String[0])));
    }

    public static RendererCache getCompatibleDrivers(Context context) {
        ArrayList<String> driverIds = new ArrayList<>();
        ArrayList<String> driverNames = new ArrayList<>();

        driverIds.add("default");
        driverNames.add("Default");

        for (NativePlugin plugin : NativePluginManager.getPlugins()) {
            String driverId = plugin.getDriverName();
            if (driverId != null && !driverIds.contains(driverId)) {
                driverIds.add(driverId);
                String displayName = plugin.getDisplayName() != null ? plugin.getDisplayName() : ("FCL: " + driverId);
                String pluginName = plugin.getName();
                if (pluginName != null) {
                    driverNames.add(displayName + " (from " + pluginName + " plugin)");
                } else {
                    driverNames.add(displayName);
                }
            }
        }

        return new RendererCache(driverIds, driverNames.toArray(new String[0]));
    }

    public static boolean checkRendererCompatible(Context context, String rendererName) {
        return getCompatibleRenderers(context).rendererIds.contains(rendererName);
    }

    /**
     * Destroy compatible renderers cache
     */
    public static void releaseRendererCache() {
        if (sCompatibleRenderers != null) {
            sCompatibleRenderers.rendererIds.clear();
            sCompatibleRenderers = null;
        }
    }
}
