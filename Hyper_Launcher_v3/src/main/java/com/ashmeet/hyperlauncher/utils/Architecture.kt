package com.ashmeet.hyperlauncher.utils

import android.os.Build

/**
 * This class aims at providing a simple and easy way to deal with the device architecture.
 */
object Architecture {
    const val UNSUPPORTED_ARCH = -1
    const val ARCH_ARM64 = 0x1
    const val ARCH_ARM = 0x2
    const val ARCH_X86 = 0x4
    const val ARCH_X86_64 = 0x8

    /* On both 32-bit ARM and x86, the top 1GB is reserved for kernel use. */
    const val ADDRESS_SPACE_LIMIT_32_BIT = 0xbfffffffL

    /*
     * Technically, this is supposed to be 48 bits on x86_64, but nobody's allocating
     * 524288 terabytes of RAM on Pojav any time soon.
     */
    const val ADDRESS_SPACE_LIMIT_64_BIT = 0x7fffffffffL

    /**
     * Get the highest byte accessible within the process's address space.
     * @return the highest byte accessible within the process's address space.
     */
    @JvmStatic
    fun getAddressSpaceLimit(): Long {
        return if (is64BitsDevice()) ADDRESS_SPACE_LIMIT_64_BIT else ADDRESS_SPACE_LIMIT_32_BIT
    }

    /**
     * Tell us if the device supports 64 bits architecture
     * @return If the device supports 64 bits architecture
     */
    @JvmStatic
    fun is64BitsDevice(): Boolean {
        return Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
    }

    /**
     * Tell us if the device supports 32 bits architecture
     * Note, that a 64 bits device won't be reported as supporting 32 bits.
     * @return If the device supports 32 bits architecture
     */
    @JvmStatic
    fun is32BitsDevice(): Boolean {
        return !is64BitsDevice()
    }

    /**
     * Tells the device supported architecture.
     * Since MIPS(/64) has been phased out long ago, isn't checked here.
     *
     * @return ARCH_ARM || ARCH_ARM64 || ARCH_X86 || ARCH_86_64
     */
    @JvmStatic
    fun getDeviceArchitecture(): Int {
        return if (isx86Device()) {
            if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        } else {
            if (is64BitsDevice()) ARCH_ARM64 else ARCH_ARM
        }
    }

    /**
     * Tell is the device is based on an x86 processor.
     * It doesn't tell if the device is 64 or 32 bits.
     * @return Whether the device is x86 based.
     */
    @JvmStatic
    fun isx86Device(): Boolean {
        // We check the whole range of supported ABIs,
        // Since Asus Zenfone can place arm before their native instruction set.
        val abi = if (is64BitsDevice()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val comparedArch = if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        for (str in abi) {
            if (archAsInt(str) == comparedArch) return true
        }
        return false
    }

    /**
     * Convert an architecture from a String to an int.
     * @param arch The architecture as a String
     * @return The architecture as an int, can be UNSUPPORTED_ARCH if unknown.
     */
    @JvmStatic
    fun archAsInt(arch: String): Int {
        val normalizedArch = arch.lowercase().trim().replace(" ", "")
        if (normalizedArch.contains("arm64") || normalizedArch == "aarch64") return ARCH_ARM64
        if (normalizedArch.contains("arm") || normalizedArch == "aarch32") return ARCH_ARM
        if (normalizedArch.contains("x86_64") || normalizedArch == "amd64") return ARCH_X86_64
        if (normalizedArch.contains("x86") || (normalizedArch.startsWith("i") && normalizedArch.endsWith("86"))) return ARCH_X86
        // Shouldn't happen
        return UNSUPPORTED_ARCH
    }

    /**
     * Convert to a string an architecture.
     * @param arch The architecture as an int.
     * @return "arm64" || "arm" || "x86_64" || "x86" || "UNSUPPORTED_ARCH"
     */
    @JvmStatic
    fun archAsString(arch: Int): String {
        return when (arch) {
            ARCH_ARM64 -> "arm64"
            ARCH_ARM -> "arm"
            ARCH_X86_64 -> "x86_64"
            ARCH_X86 -> "x86"
            else -> "UNSUPPORTED_ARCH"
        }
    }

    /**
     * Convert to a string an architecture.
     * @param arch The architecture as an int.
     * @return "arm64" || "arm" || "x86_64" || "x86" || "UNSUPPORTED_ARCH"
     */
    @JvmStatic
    fun archAsStringAndroid(arch: Int): String {
        return when (arch) {
            ARCH_ARM64 -> "arm64-v8a"
            ARCH_ARM -> "armeabi-v7a"
            ARCH_X86_64 -> "x86_64"
            ARCH_X86 -> "x86"
            else -> "UNSUPPORTED_ARCH"
        }
    }
}
