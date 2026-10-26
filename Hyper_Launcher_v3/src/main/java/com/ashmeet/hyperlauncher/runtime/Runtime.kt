package com.ashmeet.hyperlauncher.runtime

import java.util.Objects

class Runtime(
    @JvmField val name: String,
    @JvmField val versionString: String? = null,
    @JvmField val arch: String? = null,
    @JvmField val javaVersion: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val runtime = other as Runtime
        return name == runtime.name
    }

    override fun hashCode(): Int {
        return Objects.hash(name)
    }
}
