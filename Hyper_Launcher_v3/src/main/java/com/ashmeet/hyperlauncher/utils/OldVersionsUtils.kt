package com.ashmeet.hyperlauncher.utils

import android.util.Log
import com.ashmeet.hyperbackend.JVersionList
import com.ashmeet.hyperlauncher.extra.ExtraConstants
import com.ashmeet.hyperlauncher.extra.ExtraCore

object OldVersionsUtils {

    @JvmStatic
    fun selectOpenGlVersion(version: JVersionList.Version) {
        val creationTime = version.time
        if (!HyperUtils.isValidString(creationTime)) {
            ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, "2")
            return
        }

        val creationDate = DateUtils.parseReleaseDate(creationTime)
        val openGlVersion = if (DateUtils.dateBefore(creationDate, 2011, 6, 8)) "1" else "2"
        Log.i("GL_SELECT", openGlVersion)
        ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, openGlVersion)
    }
}
