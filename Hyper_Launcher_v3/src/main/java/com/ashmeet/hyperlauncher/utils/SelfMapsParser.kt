package com.ashmeet.hyperlauncher.utils

import java.io.FileInputStream
import java.io.IOException
import java.util.Scanner

class SelfMapsParser(private val mCallback: Callback) {

    @Throws(IOException::class, NumberFormatException::class)
    fun run() {
        FileInputStream("/proc/self/maps").use { fileInputStream ->
            val scanner = Scanner(fileInputStream)
            while (scanner.hasNextLine()) {
                if (!forEachLine(scanner.nextLine())) break
            }
        }
    }

    @Throws(NumberFormatException::class)
    private fun forEachLine(line: String): Boolean {
        val firstSpaceIndex = line.indexOf(' ')
        if (firstSpaceIndex == -1) return true
        val addresses = line.substring(0, firstSpaceIndex)
        val addressArray = addresses.split("-")
        if (addressArray.size < 2) return true
        val begin = addressArray[0].toLong(16)
        val end = addressArray[1].toLong(16)
        return mCallback.process(begin, end, line)
    }

    fun interface Callback {
        fun process(startAddress: Long, endAddress: Long, wholeLine: String?): Boolean
    }
}
