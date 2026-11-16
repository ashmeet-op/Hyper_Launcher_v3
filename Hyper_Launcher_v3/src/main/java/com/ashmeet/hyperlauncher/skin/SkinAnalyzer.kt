package com.ashmeet.hyperlauncher.skin

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.get
import com.ashmeet.hyperlauncher.skin.model.SkinModelType
import java.security.MessageDigest


fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(it) }


fun detectSkinModel(imageHeight: Int, getPixelAlpha: (Int, Int) -> Int): SkinModelType {
    if (imageHeight == 32) return SkinModelType.STEVE

    fun allTransparent(xs: IntRange, ys: IntRange) =
        xs.all { x -> ys.all { y -> getPixelAlpha(x, y) == 0 } }

    val isSlim =
        allTransparent(50..51, 16..19) &&
        allTransparent(54..55, 20..31) &&
        allTransparent(42..43, 48..51) &&
        allTransparent(46..47, 52..63)

    return if (isSlim) SkinModelType.ALEX else SkinModelType.STEVE
}

object AndroidSkinAnalyzer {


    fun validate(bytes: ByteArray): Boolean {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return (opts.outWidth == 64 && opts.outHeight == 64) ||
               (opts.outWidth == 64 && opts.outHeight == 32)
    }


    fun detectModel(bytes: ByteArray): SkinModelType {
        val opts = BitmapFactory.Options()
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: return SkinModelType.STEVE
        return try {
            detectSkinModel(opts.outHeight) { x, y -> Color.alpha(bmp[x, y]) }
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }


    fun prepareSkin(bytes: ByteArray): PlayerSkin? {
        if (!validate(bytes)) return null
        return PlayerSkin(bytes = bytes, hash = bytes.sha256Hex(), model = detectModel(bytes))
    }


    fun prepareCape(bytes: ByteArray): PlayerCape =
        PlayerCape(bytes = bytes, hash = bytes.sha256Hex())
}
