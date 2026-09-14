package com.ashmeet.hyperlauncher.utils

import net.kdt.pojavlaunch.JVersionList
import java.text.SimpleDateFormat
import java.util.*


object DateUtils {
    private val DEFAULT_DATE: Date by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2011-11-18")!!
    }


    @JvmStatic
    fun parseReleaseDate(releaseTime: String?): Date {
        if (releaseTime.isNullOrBlank()) return DEFAULT_DATE
        return try {
            var time = releaseTime
            val tIndexOf = time.indexOf('T')
            if (tIndexOf != -1) time = time.substring(0, tIndexOf)
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(time) ?: DEFAULT_DATE
        } catch (_: Exception) {
            DEFAULT_DATE
        }
    }


    @JvmStatic
    fun dateBefore(date: Date, year: Int, month: Int, dayOfMonth: Int): Boolean {
        return date.before(GregorianCalendar(year, month, dayOfMonth).time)
    }


    @JvmStatic
    fun getOriginalReleaseDate(gameVersion: JVersionList.Version): Date {
        return try {
            val actualVersion = if (Tools.isValidString(gameVersion.inheritsFrom)) {
                Tools.getVersionInfo(gameVersion.inheritsFrom, true)
            } else {



                Tools.getVersionInfo(gameVersion.id, true)
            }
            parseReleaseDate(actualVersion.releaseTime)
        } catch (_: Exception) {
            DEFAULT_DATE
        }
    }
}
