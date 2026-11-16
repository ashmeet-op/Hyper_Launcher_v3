package com.ashmeet.hyperlauncher.skin

import com.ashmeet.hyperlauncher.skin.model.SkinModelType


data class PlayerSkin(
    val bytes: ByteArray,
    val hash: String,
    val model: SkinModelType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerSkin) return false
        return hash == other.hash
    }

    override fun hashCode(): Int = hash.hashCode()
}


data class PlayerCape(
    val bytes: ByteArray,
    val hash: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerCape) return false
        return hash == other.hash
    }

    override fun hashCode(): Int = hash.hashCode()
}
