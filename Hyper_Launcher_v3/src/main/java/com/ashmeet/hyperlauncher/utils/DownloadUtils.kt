package com.ashmeet.hyperlauncher.utils

import android.os.Build
import android.util.Log
import org.apache.commons.io.IOUtils
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable

@Suppress("IOStreamConstructor")
object DownloadUtils {
    @JvmField
    val USER_AGENT: String = HyperUtils.APP_NAME

    @Throws(IOException::class)
    @JvmStatic
    fun download(url: String, os: OutputStream) {
        download(URL(url), os)
    }

    @Throws(IOException::class)
    @JvmStatic
    fun openHttpConnection(urlStr: String): HttpURLConnection {
        var urlString = urlStr
        var redirectCount = 0
        var attempt = 0
        val maxAttempts = 3
        while (true) {
            attempt++
            var conn: HttpURLConnection? = null
            try {
                conn = URL(urlString).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = false
                conn.connect()
                val responseCode = conn.responseCode
                if (responseCode == 307 ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == 308 ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER
                ) {
                    val redirect = conn.getHeaderField("Location")
                    if (redirect != null) {
                        urlString = redirect
                        conn.disconnect()
                        redirectCount++
                        if (redirectCount > 10) throw IOException("Too many redirects")
                        attempt = 0
                        continue
                    }
                }
                if (responseCode in 500..599 && attempt < maxAttempts) {
                    conn.disconnect()
                    try {
                        Thread.sleep((attempt * 1000L))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Download interrupted", ie)
                    }
                    continue
                }
                if (responseCode >= 400) {
                    conn.disconnect()
                    throw IOException("Server returned HTTP $responseCode")
                }
                return conn
            } catch (e: IOException) {
                conn?.disconnect()
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep((attempt * 1000L))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Download interrupted", ie)
                    }
                    continue
                }
                throw e
            }
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun download(url: URL, os: OutputStream) {
        var isStream: InputStream? = null
        var conn: HttpURLConnection? = null
        try {
            conn = openHttpConnection(url.toString())
            isStream = conn.inputStream
            IOUtils.copy(isStream, os)
        } catch (e: IOException) {
            throw IOException("Unable to download from $url", e)
        } finally {
            try {
                isStream?.close()
            } catch (e: Exception) {
                Log.e("DownloadUtils", "Failed to close input stream", e)
            }
            try {
                conn?.disconnect()
            } catch (e: Exception) {
                Log.e("DownloadUtils", "Failed to disconnect connection", e)
            }
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun downloadString(url: String): String {
        val bos = ByteArrayOutputStream()
        download(url, bos)
        bos.close()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bos.toString(StandardCharsets.UTF_8)
        } else {
            url
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun downloadFile(url: String, out: File) {
        FileUtils.ensureParentDirectory(out)
        FileOutputStream(out).use { fileOutputStream ->
            download(url, fileOutputStream)
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun downloadFileMonitored(
        urlInput: String,
        outputFile: File,
        buf: ByteArray?,
        monitor: HyperUtils.DownloaderFeedback
    ) {
        FileUtils.ensureParentDirectory(outputFile)
        val conn = URL(urlInput).openConnection() as HttpURLConnection
        val readStr = conn.inputStream
        FileOutputStream(outputFile).use { fos ->
            var current: Int
            var overall = 0
            val length = conn.contentLength
            val buffer = buf ?: ByteArray(65535)

            while (readStr.read(buffer).also { current = it } != -1) {
                overall += current
                fos.write(buffer, 0, current)
                monitor.updateProgress(overall, length)
            }
            conn.disconnect()
        }
    }

    @Throws(IOException::class, ParseException::class)
    @JvmStatic
    fun <T> downloadStringCached(
        url: String,
        cacheName: String,
        parseCallback: ParseCallback<T>
    ): T {
        val cacheDestination = File(HyperUtils.DIR_CACHE, "string_cache/$cacheName")
        if (cacheDestination.isFile &&
            cacheDestination.canRead() &&
            System.currentTimeMillis() < (cacheDestination.lastModified() + 86400000)
        ) {
            try {
                val cachedString = HyperUtils.read(FileInputStream(cacheDestination))
                return parseCallback.process(cachedString)
            } catch (e: IOException) {
                Log.i("DownloadUtils", "Failed to read the cached file", e)
            } catch (e: ParseException) {
                Log.i("DownloadUtils", "Failed to parse the cached file", e)
            }
        }
        val urlContent = downloadString(url)
        val parseResult = parseCallback.process(urlContent)

        val tryWriteCache = if (cacheDestination.exists()) {
            cacheDestination.canWrite()
        } else {
            FileUtils.ensureParentDirectorySilently(cacheDestination)
        }

        if (tryWriteCache) {
            try {
                HyperUtils.write(cacheDestination, urlContent)
            } catch (e: IOException) {
                Log.i("DownloadUtils", "Failed to cache the string", e)
            }
        }
        return parseResult
    }

    @Throws(IOException::class)
    private fun <T> downloadFile(downloadFunction: Callable<T>): T {
        try {
            return downloadFunction.call()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun verifyFile(file: File, sha1: String): Boolean {
        return file.exists() && HashUtils.compareSHA1(file, sha1)
    }

    @Throws(IOException::class)
    @JvmStatic
    fun <T> ensureSha1(outputFile: File, sha1: String?, downloadFunction: Callable<T>) {
        if (sha1 == null) {
            if (outputFile.exists()) return
            else {
                downloadFile(downloadFunction)
                return
            }
        }

        var attempts = 0
        var fileOkay = verifyFile(outputFile, sha1)
        while (attempts < 5 && !fileOkay) {
            attempts++
            downloadFile(downloadFunction)
            fileOkay = verifyFile(outputFile, sha1)
        }
        if (!fileOkay) throw SHA1VerificationException("SHA1 verification failed after 5 download attempts")
    }

    @Suppress("unused")
    @JvmStatic
    fun getContentLength(url: String): Long {
        try {
            val urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.requestMethod = "HEAD"
            urlConnection.doInput = false
            urlConnection.doOutput = false
            urlConnection.connect()
            val responseCode = urlConnection.responseCode
            if (responseCode in 200..299) return urlConnection.contentLength.toLong()
        } catch (e: IOException) {
            Log.w("DownloadUtils", "Failed to get content length", e)
        }
        return -1
    }

    @JvmStatic
    fun pingInjectorUrl(url: String): Boolean {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            conn.responseCode == 200
        } catch (_: IOException) {
            false
        }
    }

    interface ParseCallback<T> {
        @Throws(ParseException::class)
        fun process(input: String): T
    }

    class ParseException(e: Exception) : Exception(e)

    class SHA1VerificationException(message: String) : IOException(message)
}
