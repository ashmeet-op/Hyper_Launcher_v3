package com.ashmeet.hyperlauncher.utils

object MavenNameUtils {
    @JvmStatic
    fun mavenBaseName(libName: String): String {
        val libInfos = libName.split(":")
        val builder = StringBuilder()
            .append(libInfos[0]).append(':').append(libInfos[1])
        for (i in 3 until libInfos.size) {
            builder.append(':').append(libInfos[i])
        }
        return builder.toString()
    }

    @JvmStatic
    fun mavenNameToPathBuilder(libName: String, suffix: String): StringBuilder {
        return mavenNameToPathBuilder(libName).append('-').append(suffix)
    }

    @JvmStatic
    fun mavenNameToPathBuilder(libName: String): StringBuilder {
        val libInfos = libName.split(":")
        val builder = StringBuilder()
            .append(libInfos[0].replace(".", "/"))
            .append('/')
            .append(libInfos[1])
            .append('/')
            .append(libInfos[2])
            .append('/')
            .append(libInfos[1])
        for (i in 2 until libInfos.size) {
            val libInfo = libInfos[i]
            if (libInfo.isEmpty()) continue
            builder.append('-').append(libInfo)
        }
        return builder
    }

    @JvmStatic
    fun mavenNameToAarPath(libName: String): String {
        return mavenNameToPathBuilder(libName).append(".aar").toString()
    }

    @JvmStatic
    fun mavenNameToPath(libName: String, suffix: String): String {
        return mavenNameToPathBuilder(libName, suffix).append(".jar").toString()
    }

    @JvmStatic
    fun mavenNameToPath(libName: String): String {
        return mavenNameToPathBuilder(libName).append(".jar").toString()
    }
}
