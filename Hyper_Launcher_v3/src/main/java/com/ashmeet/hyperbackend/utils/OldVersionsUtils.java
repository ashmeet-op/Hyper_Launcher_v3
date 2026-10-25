package com.ashmeet.hyperbackend.utils;

import android.util.Log;

import com.ashmeet.hyperbackend.JVersionList;
import com.ashmeet.hyperlauncher.utils.Tools;
import com.ashmeet.hyperbackend.extra.ExtraConstants;
import com.ashmeet.hyperbackend.extra.ExtraCore;

import com.ashmeet.hyperlauncher.utils.DateUtils;

import java.util.Date;


public class OldVersionsUtils {

    public static void selectOpenGlVersion(JVersionList.Version version){

        String creationTime = version.time;
        if(!Tools.isValidString(creationTime)){
            ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, "2");
            return;
        }

        Date creationDate = DateUtils.parseReleaseDate(creationTime);
        String openGlVersion =  DateUtils.dateBefore(creationDate, 2011, 6, 8) ? "1" : "2";
        Log.i("GL_SELECT", openGlVersion);
        ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, openGlVersion);
    }
}
