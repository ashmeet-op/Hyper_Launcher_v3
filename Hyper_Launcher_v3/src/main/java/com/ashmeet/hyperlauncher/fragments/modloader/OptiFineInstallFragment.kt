package com.ashmeet.hyperlauncher.fragments.modloader

import android.content.Context
import com.ashmeet.hyperlauncher.screens.modloader.ModloaderVersionGroup
import com.ashmeet.hyperlauncher.screens.modloader.ModloaderVersionItem
import com.ashmeet.hyperlauncher.tracker.TrackerLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.modloaders.OptiFineDownloadTask
import net.kdt.pojavlaunch.modloaders.OptiFineUtils
import java.io.File
import java.io.IOException

class OptiFineInstallFragment : ModVersionListFragment<OptiFineUtils.OptiFineVersions>(TAG) {
    companion object {
        const val TAG = "OptiFineInstallFragment"
    }

    override fun getTitleText(): Int = R.string.of_dl_select_version
    override fun getNoDataMsg(): Int = R.string.of_dl_failed_to_scrape

    @Throws(IOException::class)
    override fun loadVersionList(): OptiFineUtils.OptiFineVersions {
        return OptiFineUtils.downloadOptiFineVersions()
    }

    override fun mapToGroups(versionList: OptiFineUtils.OptiFineVersions): List<ModloaderVersionGroup<Any>> {
        val groups = mutableListOf<ModloaderVersionGroup<Any>>()
        for (i in versionList.gameVersions.indices) {
            val items = mutableListOf<ModloaderVersionItem<Any>>()
            for (v in versionList.optifineVersions[i]) {
                items.add(ModloaderVersionItem(v.versionName, v as Any))
            }
            groups.add(ModloaderVersionGroup(versionList.gameVersions[i], items))
        }
        return groups
    }

    private fun createInstance(version: OptiFineUtils.OptiFineVersion, listenerProxy: ModloaderListenerProxy) {
        try {
            TrackerLayout.setTracker(TrackerLayout.INSTALL_MODPACK, 0)
            OptiFineDownloadTask(version).prepareForInstall()
            val instanceInstaller = OptiFineUtils.createInstaller(version)
            Instances.createInstance({ instance ->
                instance.name = "OptiFine"
                instance.icon = "optifine"
                instance.installer = instanceInstaller
                instance.sharedData = true
            }, "OptiFine")
            TrackerLayout.clearTracker(TrackerLayout.INSTALL_MODPACK)
            instanceInstaller.start()
            listenerProxy.onDownloadFinished(null)
        } catch (e: Exception) {
            listenerProxy.onDownloadError(e)
        }
    }

    override fun createDownloadTask(selectedVersion: Any, listenerProxy: ModloaderListenerProxy): Runnable {
        return Runnable { createInstance(selectedVersion as OptiFineUtils.OptiFineVersion, listenerProxy) }
    }

    override fun onDownloadFinished(context: Context, downloadedFile: File?) {}
}
