package com.ashmeet.hyperlauncher.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ashmeet.hyperlauncher.activity.TestStorageActivity
import net.kdt.pojavlaunch.instances.DisplayInstance
import net.kdt.pojavlaunch.instances.InstanceIconProvider

object ShortcutUtils {
    const val EXTRA_INSTANCE_NAME = "instance_name"

    fun createShortcut(context: Context, instance: DisplayInstance) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Shortcuts not supported on this launcher", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(context, TestStorageActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_INSTANCE_NAME, instance.mInstanceRoot.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val drawable = InstanceIconProvider.fetchIcon(context.resources, instance)
        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
            val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(b)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            b
        }

        val shortcut = ShortcutInfoCompat.Builder(context, instance.mInstanceRoot.name)
            .setShortLabel(instance.name?.takeIf { it.isNotBlank() } ?: "Game")
            .setLongLabel("Launch ${instance.name?.takeIf { it.isNotBlank() } ?: "Game"}")
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        Toast.makeText(context, "Adding shortcut for ${instance.name}", Toast.LENGTH_SHORT).show()
    }
}
