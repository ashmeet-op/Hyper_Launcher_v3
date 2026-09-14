package net.kdt.pojavlaunch.game.platform.cursor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;

import net.ashmeet.hyperlauncher.R;
import com.ashmeet.hyperlauncher.utils.Tools;

public class CursorUtils {
    public static PlatformCursor loadStandardCursor(Context context, int shapeName) {
        String customPath = null;
        int customHotX = -1;
        int customHotY = -1;

        switch (shapeName) {
            case 0: // ARROW
                customPath = LauncherPreferences.PREF_POINTER_ICON_PATH_ARROW;
                customHotX = LauncherPreferences.PREF_POINTER_HOTSPOT_X_ARROW;
                customHotY = LauncherPreferences.PREF_POINTER_HOTSPOT_Y_ARROW;
                break;
            case 1: // IBEAM
                customPath = LauncherPreferences.PREF_POINTER_ICON_PATH_IBEAM;
                customHotX = LauncherPreferences.PREF_POINTER_HOTSPOT_X_IBEAM;
                customHotY = LauncherPreferences.PREF_POINTER_HOTSPOT_Y_IBEAM;
                break;
            case 2: // CROSSHAIR
                customPath = LauncherPreferences.PREF_POINTER_ICON_PATH_CROSSHAIR;
                customHotX = LauncherPreferences.PREF_POINTER_HOTSPOT_X_CROSSHAIR;
                customHotY = LauncherPreferences.PREF_POINTER_HOTSPOT_Y_CROSSHAIR;
                break;
            case 3: // HAND / LINK
                customPath = LauncherPreferences.PREF_POINTER_ICON_PATH_HAND;
                customHotX = LauncherPreferences.PREF_POINTER_HOTSPOT_X_HAND;
                customHotY = LauncherPreferences.PREF_POINTER_HOTSPOT_Y_HAND;
                break;
            case 4: // HRESIZE / SIZEWE
                customPath = LauncherPreferences.PREF_POINTER_ICON_PATH_HRESIZE;
                customHotX = LauncherPreferences.PREF_POINTER_HOTSPOT_X_HRESIZE;
                customHotY = LauncherPreferences.PREF_POINTER_HOTSPOT_Y_HRESIZE;
                break;
            case 5: // VRESIZE / SIZENS
                customPath = LauncherPreferences.PREF_POINTER_ICON_PATH_VRESIZE;
                customHotX = LauncherPreferences.PREF_POINTER_HOTSPOT_X_VRESIZE;
                customHotY = LauncherPreferences.PREF_POINTER_HOTSPOT_Y_VRESIZE;
                break;
            case 6: // SIZEALL / MOVE
                customPath = LauncherPreferences.PREF_POINTER_ICON_PATH_ALL_RESIZE;
                customHotX = LauncherPreferences.PREF_POINTER_HOTSPOT_X_ALL_RESIZE;
                customHotY = LauncherPreferences.PREF_POINTER_HOTSPOT_Y_ALL_RESIZE;
                break;
            case 7: // NOT_ALLOWED
                customPath = LauncherPreferences.PREF_POINTER_ICON_PATH_NOT_ALLOWED;
                customHotX = LauncherPreferences.PREF_POINTER_HOTSPOT_X_NOT_ALLOWED;
                customHotY = LauncherPreferences.PREF_POINTER_HOTSPOT_Y_NOT_ALLOWED;
                break;
        }

        Bitmap bitmap = null;
        if (customPath != null) {
            try {
                bitmap = BitmapFactory.decodeFile(customPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bitmap == null) {
            int drawableId = 0;
            switch (shapeName) {
                case 0: drawableId = R.drawable.img_mouse_pointer_arrow; break;
                case 1: drawableId = R.drawable.img_mouse_pointer_ibeam; break;
                case 2: drawableId = R.drawable.img_mouse_pointer_crosshair; break;
                case 3: drawableId = R.drawable.img_mouse_pointer_link; break;
                case 4: drawableId = R.drawable.img_mouse_pointer_resize_ew; break;
                case 5: drawableId = R.drawable.img_mouse_pointer_resize_ns; break;
                case 6: drawableId = R.drawable.img_mouse_pointer_resize_move; break;
                case 7: drawableId = R.drawable.img_mouse_pointer_not_allowed; break;
            }

            if (drawableId != 0) {
                Drawable drawable = ContextCompat.getDrawable(context, drawableId);
                if (drawable != null) {
                    if (drawable instanceof BitmapDrawable) {
                        bitmap = ((BitmapDrawable) drawable).getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    } else {
                        bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        drawable.draw(canvas);
                    }
                }
            }
        }

        if (bitmap == null) return null;

        float targetSize = Tools.dpToPx(24);
        float scale = 1f;
        if (bitmap.getWidth() > targetSize || bitmap.getHeight() > targetSize) {
            scale = targetSize / Math.max(bitmap.getWidth(), bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), true);
        }

        int hotX, hotY;
        if (customHotX != -1 && customHotY != -1) {
            hotX = (int) (customHotX * scale);
            hotY = (int) (customHotY * scale);
        } else {
            if (shapeName == 1 || shapeName == 2 || shapeName == 4 || shapeName == 5 || shapeName == 6 || shapeName == 7) {
                hotX = bitmap.getWidth() / 2;
                hotY = bitmap.getHeight() / 2;
            } else if (shapeName == 3) {
                hotX = (int) (bitmap.getWidth() * 0.3f);
                hotY = 0;
            } else {
                hotX = 0;
                hotY = 0;
            }
        }

        return new PlatformCursor(bitmap, hotX, hotY);
    }
}
