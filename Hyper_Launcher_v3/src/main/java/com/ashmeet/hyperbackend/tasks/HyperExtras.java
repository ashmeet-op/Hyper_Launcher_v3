package com.ashmeet.hyperbackend.tasks;

import com.ashmeet.hyperbackend.JVersionList;
import com.ashmeet.hyperbackend.extra.ExtraConstants;
import com.ashmeet.hyperbackend.extra.ExtraCore;
import com.ashmeet.hyperbackend.instances.Instance;

import java.io.File;

public class HyperExtras {
    public static String normalizeVersionId(String versionString) {
        JVersionList versionList = (JVersionList) ExtraCore.getValue(ExtraConstants.RELEASE_TABLE);
        if(versionList == null || versionList.versions == null) return versionString;
        if(Instance.VERSION_LATEST_RELEASE.equals(versionString)) versionString = versionList.latest.get("release");
        if(Instance.VERSION_LATEST_SNAPSHOT.equals(versionString)) versionString = versionList.latest.get("snapshot");
        return versionString;
    }

    public static JVersionList.Version getListedVersion(String normalizedVersionString) {
        JVersionList versionList = (JVersionList) ExtraCore.getValue(ExtraConstants.RELEASE_TABLE);
        if(versionList == null || versionList.versions == null) return null;
        for(JVersionList.Version version : versionList.versions) {
            if(version.id != null && version.id.equals(normalizedVersionString)) return version;
        }
        return null;
    }

    public interface DoneListener{
        void onDownloadDone(File[] classpath);
        void onDownloadFailed(Throwable throwable);
    }
}
