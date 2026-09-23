package com.ashmeet.hyperlauncher.utils.skin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import com.ashmeet.hyperlauncher.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.authenticator.accounts.Account
import java.io.File
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

object SkinLoader {
    private const val TAG = "SkinLoader"

    suspend fun getAvatarBitmap(context: Context, account: Account, size: Int, downloadedSkin: Bitmap? = null): Bitmap? = withContext(Dispatchers.IO) {
        if (downloadedSkin != null) {
            return@withContext getAvatar(downloadedSkin, size)
        }

        val skinPath = account.skinPath
        if (!skinPath.isNullOrEmpty()) {
            val skinFile = File(skinPath)
            if (skinFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(skinFile.absolutePath)
                    if (bitmap != null) {
                        val avatar = getAvatar(bitmap, size)
                        bitmap.recycle()
                        return@withContext avatar
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load avatar from local path: $skinPath", e)
                }
            }
        }
        
        // Fallback to caching based on profileId if skinPath is empty or failed
        val cachedSkinFaceFile = File(Tools.DIR_CACHE, "skin-face-${account.profileId}-${account.authType.name}.webp")
        if (cachedSkinFaceFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(cachedSkinFaceFile.absolutePath)
                if (bitmap != null) {
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cached skin face", e)
            }
        }

        return@withContext getDefaultAvatar(context, size)
    }

    private fun getDefaultAvatar(context: Context, size: Int): Bitmap? {
        return try {
            context.assets.open("steve.png").use { isInputStream ->
                val bitmap = BitmapFactory.decodeStream(isInputStream)
                if (bitmap != null) {
                    val avatar = getAvatar(bitmap, size)
                    bitmap.recycle()
                    return avatar
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load default steve skin from assets", e)
            null
        }
    }

    fun getAvatar(skin: Bitmap, size: Int): Bitmap {
        val faceOffset = (size / 18.0).roundToInt().toFloat()
        val scaleFactor = skin.width / 64.0f
        val faceSize = (8 * scaleFactor).roundToInt()
        
        val faceBitmap = Bitmap.createBitmap(skin, faceSize, faceSize, faceSize, faceSize, null, false)
        val hatBitmap = Bitmap.createBitmap(skin,
            (40 * scaleFactor).roundToInt(), faceSize, faceSize, faceSize, null, false)
        
        val avatar = createBitmap(size, size)
        val canvas = Canvas(avatar)
        
        val faceScale = (size - 2 * faceOffset) / faceSize
        val hatScale = size.toFloat() / faceSize
        
        var matrix = Matrix()
        matrix.postScale(faceScale, faceScale)
        val newFaceBitmap = Bitmap.createBitmap(faceBitmap, 0, 0, faceSize, faceSize, matrix, false)
        
        matrix = Matrix()
        matrix.postScale(hatScale, hatScale)
        val newHatBitmap = Bitmap.createBitmap(hatBitmap, 0, 0, faceSize, faceSize, matrix, false)
        
        canvas.drawBitmap(newFaceBitmap, faceOffset, faceOffset, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.drawBitmap(newHatBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        
        faceBitmap.recycle()
        hatBitmap.recycle()
        newFaceBitmap.recycle()
        newHatBitmap.recycle()
        
        return avatar
    }
}
