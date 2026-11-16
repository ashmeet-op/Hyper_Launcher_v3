package com.ashmeet.hyperlauncher.utils

import com.ashmeet.hyperlauncher.skin.model.SkinModelType


object LocalUuidUtils {

    private fun strFill(str: String, code: Char, length: Int): String =
        if (str.length > length) str.take(length)
        else str.padEnd(length, code).drop(str.length) + str

    private fun baseUuid(name: String): String {
        val lengthPart = strFill(name.length.toString(16), '0', 16)
        val hashPart = strFill(
            (name.hashCode().toLong() and 0xFFFFFFFFL).toString(16), '0', 16
        )
        return buildString(32) {
            append(lengthPart.take(12))
            append('3')
            append(lengthPart.substring(13, 16))
            append('9')
            append(hashPart.take(15))
        }
    }


    fun generateProfileId(username: String, model: SkinModelType): String {
        val base = baseUuid(username)
        if (model == SkinModelType.NONE) return base

        val prefix = base.take(27)
        val a = base[7].digitToInt(16)
        val b = base[15].digitToInt(16)
        val c = base[23].digitToInt(16)
        val maxSuffix = 0xFFFFFL
        var suffix = base.substring(27).toLong(16)

        repeat(maxSuffix.toInt() + 1) {
            val d = (suffix and 0xFL).toInt()

            if ((a xor b xor c xor d) % 2 == model.targetParity) {
                return prefix + suffix.toString(16).padStart(5, '0').uppercase()
            }
            suffix = if (suffix == maxSuffix) 0L else suffix + 1
        }
        return prefix + suffix.toString(16).padStart(5, '0').uppercase()
    }


    fun String.toFormattedUuid(): String {
        val s = this.replace("-", "")
        if (s.length != 32) return this
        return "${s.take(8)}-${s.substring(8, 12)}-${s.substring(12, 16)}-${s.substring(16, 20)}-${s.substring(20)}"
    }
}