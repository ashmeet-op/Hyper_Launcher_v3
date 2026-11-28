package com.ashmeet.hyperlauncher.utils

import java.io.File
import java.io.IOException

object FileUtils {

    @JvmStatic
    fun exists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    @JvmStatic
    fun getFileName(pathOrUrl: String): String {
        val lastSlashIndex = pathOrUrl.lastIndexOf('/')
        return if (lastSlashIndex == -1) pathOrUrl else pathOrUrl.substring(lastSlashIndex)
    }

    @JvmStatic
    fun ensureDirectorySilently(targetFile: File): Boolean {
        if (targetFile.isFile) return false
        return if (targetFile.exists()) targetFile.canWrite() else targetFile.mkdirs()
    }

    @JvmStatic
    fun ensureParentDirectorySilently(targetFile: File): Boolean {
        val parentFile = targetFile.parentFile ?: return false
        return ensureDirectorySilently(parentFile)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun ensureDirectory(targetFile: File) {
        if (targetFile.isFile) throw IOException("Target directory is a file")
        if (targetFile.exists()) {
            if (!targetFile.canWrite()) throw IOException("Target directory is not writable")
        } else if (!targetFile.mkdirs()) {
            if (!targetFile.isDirectory) throw IOException("Unable to create target directory")
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun ensureParentDirectory(targetFile: File) {
        val parentFile = targetFile.parentFile ?: throw IOException("targetFile does not have a parent")
        ensureDirectory(parentFile)
    }
}
