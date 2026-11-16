package com.ashmeet.hyperlauncher.utils

import android.os.Build


object Architecture {
    const val UNSUPPORTED_ARCH = -1
    const val ARCH_ARM64 = 0x1
    const val ARCH_ARM = 0x2
    const val ARCH_X86 = 0x4
    const val ARCH_X86_64 = 0x8


    const val ADDRESS_SPACE_LIMIT_32_BIT = 0xbfffffffL


    const val ADDRESS_SPACE_LIMIT_64_BIT = 0x7fffffffffL


    @JvmStatic
    fun getAddressSpaceLimit(): Long {
        return if (is64BitsDevice()) ADDRESS_SPACE_LIMIT_64_BIT else ADDRESS_SPACE_LIMIT_32_BIT
    }


    @JvmStatic
    fun is64BitsDevice(): Boolean {
        return Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
    }


    @JvmStatic
    fun is32BitsDevice(): Boolean {
        return !is64BitsDevice()
    }


    @JvmStatic
    fun getDeviceArchitecture(): Int {
        return if (isx86Device()) {
            if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        } else {
            if (is64BitsDevice()) ARCH_ARM64 else ARCH_ARM
        }
    }


    @JvmStatic
    fun isx86Device(): Boolean {


        val abi = if (is64BitsDevice()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val comparedArch = if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        for (str in abi) {
            if (archAsInt(str) == comparedArch) return true
        }
        return false
    }


    @JvmStatic
    fun archAsInt(arch: String): Int {
        val normalizedArch = arch.lowercase().trim().replace(" ", "")
        if (normalizedArch.contains("arm64") || normalizedArch == "aarch64") return ARCH_ARM64
        if (normalizedArch.contains("arm") || normalizedArch == "aarch32") return ARCH_ARM
        if (normalizedArch.contains("x86_64") || normalizedArch == "amd64") return ARCH_X86_64
        if (normalizedArch.contains("x86") || (normalizedArch.startsWith("i") && normalizedArch.endsWith("86"))) return ARCH_X86

        return UNSUPPORTED_ARCH
    }


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
