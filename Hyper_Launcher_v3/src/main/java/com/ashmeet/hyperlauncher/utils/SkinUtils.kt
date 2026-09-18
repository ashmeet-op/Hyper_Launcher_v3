package com.ashmeet.hyperlauncher.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import com.ashmeet.hyperlauncher.skin.model.SkinModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.authenticator.accounts.SkinHeadRenderer

object SkinUtils {

    private const val TAG = "SkinUtils"

    fun getSkinUrl(account: Account?): String? {
        if (account == null) return null
        if (!account.skinPath.isNullOrEmpty()) {
            return "file://${account.skinPath}"
        }

        if (account.authType == AuthType.LOCAL) return null

        val template = account.authType.skinUrl ?: return null

        val idToUse = when (account.authType) {
            AuthType.MICROSOFT -> {
                if (!account.profileId.isNullOrEmpty() && !account.profileId.contains("00000000")) {
                    account.profileId
                } else {
                    account.username
                }
            }
            AuthType.ELY_BY -> {
                account.username
            }
            else -> account.username
        }

        return try {
            String.format(template, idToUse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to format skin URL", e)
            null
        }
    }


    fun getModelType(account: Account?): String {
        return when (account?.skinModel) {
            SkinModelType.ALEX -> "slim"
            else -> "default"
        }
    }


    suspend fun renderHead(context: Context, account: Account?): Bitmap? =
        withContext(Dispatchers.IO) {
            val skinUrl = getSkinUrl(account)
            val skinBitmap = getSkinBitmap(context, skinUrl) ?: return@withContext loadSteveHead3D(context)

            val head = try {
                SkinHeadRenderer().render(120, skinBitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Renderer error", e)
                null
            }

            return@withContext head ?: loadSteveHead3D(context)
        }


    suspend fun renderHead2D(context: Context, account: Account?): Bitmap? =
        withContext(Dispatchers.IO) {
            val skinUrl = getSkinUrl(account)
            val skinBitmap = getSkinBitmap(context, skinUrl) ?: return@withContext loadSteveHead2D(context)

            val head = try {
                SkinHeadRenderer().render2D(128, skinBitmap)
            } catch (e: Exception) {
                Log.e(TAG, "2D Renderer error", e)
                null
            }

            return@withContext head ?: loadSteveHead2D(context)
        }

    private suspend fun getSkinBitmap(context: Context, skinUrl: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (skinUrl == null) return@withContext null

        if (skinUrl.startsWith("file://")) {
            val path = skinUrl.substring(7)
            return@withContext try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeFile(path, this)
                    inSampleSize = calculateInSampleSize(this, 128, 128)
                    inJustDecodeBounds = false
                }
                BitmapFactory.decodeFile(path, options)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode file $path", e)
                null
            }
        }

        return@withContext try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(skinUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load skin bitmap from $skinUrl", e)
            null
        }
    }

    private fun loadSteveHead3D(context: Context): Bitmap? {
        val steveBitmap = try {
            context.assets.open("steve.png").use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeStream(inputStream, null, this)
                    inSampleSize = calculateInSampleSize(this, 128, 128)
                    inJustDecodeBounds = false
                }

                context.assets.open("steve.png").use { innerInput ->
                    BitmapFactory.decodeStream(innerInput, null, options)
                }
            }
        } catch (_: Exception) {
            Log.w(TAG, "steve.png not found in assets")
            null
        } ?: return null

        val head = try {
            SkinHeadRenderer().render(120, steveBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Renderer failed for steve.png", e)
            null
        }

        steveBitmap.recycle()
        return head
    }

    private fun loadSteveHead2D(context: Context): Bitmap? {
        val steveBitmap = try {
            context.assets.open("steve.png").use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeStream(inputStream, null, this)
                    inSampleSize = calculateInSampleSize(this, 128, 128)
                    inJustDecodeBounds = false
                }
                context.assets.open("steve.png").use { innerInput ->
                    BitmapFactory.decodeStream(innerInput, null, options)
                }
            }
        } catch (_: Exception) {
            Log.w(TAG, "steve.png not found in assets")
            null
        } ?: return null

        val head = try {
            SkinHeadRenderer().render2D(128, steveBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "2D Renderer failed for steve.png", e)
            null
        }

        steveBitmap.recycle()
        return head
    }


    @Composable
    fun rememberSkinHead(account: Account?): State<Bitmap?> {
        val context = LocalContext.current
        val stableKey = "${account?.profileId}_${account?.skinPath}_${account?.username}_3D"
        return produceState(initialValue = null, stableKey) {
            value = renderHead(context, account)
        }
    }


    @Composable
    fun rememberSkinHead2D(account: Account?): State<Bitmap?> {
        val context = LocalContext.current
        val stableKey = "${account?.profileId}_${account?.skinPath}_${account?.username}_2D"
        return produceState(initialValue = null, stableKey) {
            value = renderHead2D(context, account)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
