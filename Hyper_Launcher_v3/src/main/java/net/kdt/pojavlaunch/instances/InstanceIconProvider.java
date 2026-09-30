package net.kdt.pojavlaunch.instances;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import net.ashmeet.hyperlauncher.R;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InstanceIconProvider {
    public static final String FALLBACK_ICON_NAME = "default";
    private static final Map<Integer, Drawable> sIconCache = new HashMap<>();
    private static final Map<String, Drawable> sStaticIconCache = new HashMap<>();
    private static final Map<String, Integer> sStaticIcons = new HashMap<>();
    private static int sLastUiMode = -1;

    static {
        sStaticIcons.put("default", R.drawable.ic_hyper_full);
        sStaticIcons.put("fabric", R.drawable.ic_fabric);
        sStaticIcons.put("quilt", R.drawable.ic_quilt);
        sStaticIcons.put("forge", R.drawable.ic_forge);
        sStaticIcons.put("neoforge", R.drawable.ic_neoforge);
        sStaticIcons.put("optifine", R.drawable.img_loader_optifine);
    }


    public static @NonNull Drawable fetchIcon(Resources resources, @NonNull DisplayInstance instance) {
        checkUiMode(resources);
        int identityHashCode = System.identityHashCode(instance);

        Drawable cachedIcon = sIconCache.get(identityHashCode);
        if(cachedIcon != null) return cachedIcon;

        Drawable instanceIcon = fetchInstanceFileIcon(resources, identityHashCode, instance.getInstanceIconLocation());
        if(instanceIcon != null) return instanceIcon;

        return fetchStaticIcon(resources, identityHashCode, instance.icon);
    }


    public static void dropIcon(@NonNull Instance key) {
        sIconCache.remove(System.identityHashCode(key));
    }

    private static void checkUiMode(Resources resources) {
        int currentUiMode = resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (sLastUiMode != currentUiMode) {
            sStaticIconCache.clear();
            sIconCache.clear();
            sLastUiMode = currentUiMode;
        }
    }

    private static Drawable fetchInstanceFileIcon(Resources resources, int identityHash, File iconLocation) {
        if(!iconLocation.isFile() || !iconLocation.canRead()) return null;
        Bitmap iconBitmap = BitmapFactory.decodeFile(iconLocation.getAbsolutePath());
        if(iconBitmap == null) return null;
        Drawable iconDrawable = new BitmapDrawable(resources, iconBitmap);
        sIconCache.put(identityHash, iconDrawable);
        return iconDrawable;
    }

    private static Drawable fetchStaticIcon(Resources resources, int identityHash, String icon) {
        Drawable staticIcon = sStaticIconCache.get(icon);
        if(staticIcon == null) {
            if(icon != null) staticIcon = getStaticIcon(resources, icon);
            if(staticIcon == null) staticIcon = fetchFallbackIcon(resources);
            sStaticIconCache.put(icon, staticIcon);
        }
        sIconCache.put(identityHash, staticIcon);
        return staticIcon;
    }

    private static @NonNull Drawable fetchFallbackIcon(Resources resources) {
        Drawable fallbackIcon = sStaticIconCache.get(FALLBACK_ICON_NAME);
        if(fallbackIcon == null) {
            fallbackIcon = Objects.requireNonNull(getStaticIcon(resources, FALLBACK_ICON_NAME));
            sStaticIconCache.put(FALLBACK_ICON_NAME, fallbackIcon);
        }
        return fallbackIcon;
    }

    private static Drawable getStaticIcon(Resources resources, @NonNull String icon) {
        int staticIconResource = getStaticIconResource(icon);
        if(staticIconResource == -1) return null;
        Drawable drawable = ResourcesCompat.getDrawable(resources, staticIconResource, null);
        if (drawable != null && FALLBACK_ICON_NAME.equals(icon)) {
            int nightModeFlags = resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags != Configuration.UI_MODE_NIGHT_YES) {
                drawable = drawable.mutate();
                DrawableCompat.setTint(drawable, Color.BLACK);
            }
        }
        return drawable;
    }

    public static int getStaticIconResource(String icon) {
        Integer iconResource = sStaticIcons.get(icon);
        if(iconResource == null) return -1;
        return iconResource;
    }


    public static boolean hasStaticIcon(String name) {
        return sStaticIcons.containsKey(name);
    }
}
