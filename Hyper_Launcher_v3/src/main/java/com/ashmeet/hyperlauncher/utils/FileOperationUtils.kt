package com.ashmeet.hyperlauncher.utils

import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileOperationUtils {

    @Throws(IOException::class)
    fun copy(source: File, dest: File) {
        if (source.isDirectory) {
            FileUtils.copyDirectory(source, dest)
        } else {
            FileUtils.copyFile(source, dest)
        }
    }

    @Throws(IOException::class)
    fun move(source: File, dest: File) {
        if (source.isDirectory) {
            FileUtils.moveDirectory(source, dest)
        } else {
            FileUtils.moveFile(source, dest)
        }
    }

    @Throws(IOException::class)
    fun compressZip(source: File, dest: File) {
        ZipOutputStream(FileOutputStream(dest)).use { zos ->
            if (source.isDirectory) {
                compressDirectoryToZip(source, source.name, zos)
            } else {
                addToZip(source, source.name, zos)
            }
        }
    }

    private fun compressDirectoryToZip(dir: File, baseName: String, zos: ZipOutputStream) {
        dir.listFiles()?.forEach { file ->
            val entryName = "$baseName/${file.name}"
            if (file.isDirectory) {
                compressDirectoryToZip(file, entryName, zos)
            } else {
                addToZip(file, entryName, zos)
            }
        }
    }

    private fun addToZip(file: File, entryName: String, zos: ZipOutputStream) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    @Throws(IOException::class)
    fun compress7z(source: File, dest: File) {
        SevenZOutputFile(dest).use { szos ->
            if (source.isDirectory) {
                compressDirectoryTo7z(source, source.name, szos)
            } else {
                addTo7z(source, source.name, szos)
            }
        }
    }

    private fun compressDirectoryTo7z(dir: File, baseName: String, szos: SevenZOutputFile) {
        dir.listFiles()?.forEach { file ->
            val entryName = "$baseName/${file.name}"
            if (file.isDirectory) {
                compressDirectoryTo7z(file, entryName, szos)
            } else {
                addTo7z(file, entryName, szos)
            }
        }
    }

    private fun addTo7z(file: File, entryName: String, szos: SevenZOutputFile) {
        val entry = szos.createArchiveEntry(file, entryName)
        szos.putArchiveEntry(entry)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                szos.write(buffer, 0, bytesRead)
            }
        }
        szos.closeArchiveEntry()
    }
}
