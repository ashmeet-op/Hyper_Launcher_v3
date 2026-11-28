package com.ashmeet.hyperlauncher.utils

import android.os.Build
import androidx.annotation.RequiresApi
import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Hex
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object HashUtils {

    @JvmField
    val REQW_HASH: ByteArray = byteArrayOf(
        0x51.toByte(), 0x5d.toByte(), 0x5f.toByte(), 0x1c.toByte(), 0x56.toByte(), 0x5c.toByte(), 0x53.toByte(), 0x5f.toByte(),
        0x5d.toByte(), 0x50.toByte(), 0x5b.toByte(), 0x5e.toByte(), 0x57.toByte(), 0x1c.toByte(), 0x5f.toByte(), 0x5d.toByte(),
        0x56.toByte(), 0x5e.toByte(), 0x4b.toByte(), 0x5f.toByte(), 0x5d.toByte(), 0x56.toByte(), 0x5f.toByte(), 0x53.toByte(),
        0x5c.toByte(), 0x53.toByte(), 0x55.toByte(), 0x57.toByte(), 0x40.toByte(),
    )

    @RequiresApi(26)
    @Throws(IOException::class)
    private fun fileHashNio(messageDigest: MessageDigest, p: Path): ByteArray {
        val buffer = ByteBuffer.allocateDirect(65535)
        Files.newByteChannel(p, StandardOpenOption.READ).use { channel ->
            while (true) {
                buffer.rewind()
                if (channel.read(buffer) == -1) break
                buffer.flip()
                messageDigest.update(buffer)
            }
        }
        return messageDigest.digest()
    }

    @Throws(IOException::class)
    private fun fileHashLegacy(messageDigest: MessageDigest, f: File): ByteArray {
        val sha1Buffer = ByteArray(65535)
        FileInputStream(f).use { stream ->
            var readLen: Int
            while (stream.read(sha1Buffer).also { readLen = it } != -1) {
                messageDigest.update(sha1Buffer, 0, readLen)
            }
        }
        return messageDigest.digest()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun fileHash(messageDigest: MessageDigest, f: File): ByteArray {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fileHashNio(messageDigest, f.toPath())
        } else {
            fileHashLegacy(messageDigest, f)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun compareSHA1(f: File, sourceSHA: String): Boolean {
        return try {
            val messageDigest = MessageDigest.getInstance("SHA-1")
            val wantedBytes = Hex.decodeHex(sourceSHA.toCharArray())
            val localFileBytes = fileHash(messageDigest, f)
            localFileBytes.contentEquals(wantedBytes)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("WTF? SHA-1 digest missing!", e)
        } catch (e: DecoderException) {
            throw IOException("Bad SHA-1 hash: $sourceSHA for file ${f.name}", e)
        }
    }
}
