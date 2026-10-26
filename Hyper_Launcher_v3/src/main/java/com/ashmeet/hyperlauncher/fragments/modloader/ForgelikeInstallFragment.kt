package com.ashmeet.hyperlauncher.fragments.modloader

import android.content.Context
import com.ashmeet.hyperlauncher.screens.modloader.ModloaderVersionGroup
import com.ashmeet.hyperlauncher.screens.modloader.ModloaderVersionItem
import com.ashmeet.hyperlauncher.tracker.TrackerLayout
import com.ashmeet.hyperbackend.instances.Instances
import com.ashmeet.hyperbackend.modloaders.ForgelikeUtils
import com.ashmeet.hyperbackend.modloaders.ModloaderListenerProxy
import java.io.File
import java.io.IOException

abstract class ForgelikeInstallFragment(private val mUtils: ForgelikeUtils, mFragmentTag: String) :
    ModVersionListFragment<List<String>>(mFragmentTag) {

    @Throws(IOException::class)
    override fun loadVersionList(): List<String> {
        return mUtils.downloadVersions()
    }

    override fun mapToGroups(data: List<String>): List<ModloaderVersionGroup<Any>> {
        val gameVersions = mutableListOf<String>()
        val loaderVersions = mutableListOf<MutableList<String>>()

        for (version in data) {
            if (mUtils.shouldSkipVersion(version)) continue
            val gameVersion = mUtils.processVersionString(version)
            val gameVersionIndex = gameVersions.indexOf(gameVersion)
            val versionList: MutableList<String>
            if (gameVersionIndex != -1) {
                versionList = loaderVersions[gameVersionIndex]
            } else {
                versionList = mutableListOf()
                gameVersions.add(gameVersion)
                loaderVersions.add(versionList)
            }
            versionList.add(version)
        }

        if (mUtils.isVersionOrderInversed) {
            for (versionList in loaderVersions) {
                versionList.reverse()
            }
            loaderVersions.reverse()
            gameVersions.reverse()
        }

        val groups = mutableListOf<ModloaderVersionGroup<Any>>()
        for (i in gameVersions.indices) {
            val items = mutableListOf<ModloaderVersionItem<Any>>()
            for (v in loaderVersions[i]) {
                items.add(ModloaderVersionItem(v, v as Any))
            }
            groups.add(ModloaderVersionGroup(gameVersions[i], items))
        }
        return groups
    }

    override fun createDownloadTask(selectedVersion: Any, listenerProxy: ModloaderListenerProxy): Runnable {
        return Runnable { createInstance(selectedVersion as String, listenerProxy) }
    }

    override fun onDownloadFinished(context: Context, downloadedFile: File?) {}

    private fun createInstance(selectedVersion: String, listenerProxy: ModloaderListenerProxy) {
        try {
            TrackerLayout.setTracker(TrackerLayout.INSTALL_MODPACK, 0)
            val instanceInstaller = mUtils.createInstaller(selectedVersion)
            Instances.createInstance({ instance ->
                instance.name = mUtils.name
                instance.icon = mUtils.iconName
                instance.installer = instanceInstaller
            }, selectedVersion)
            TrackerLayout.clearTracker(TrackerLayout.INSTALL_MODPACK)
            instanceInstaller.start()
            listenerProxy.onDownloadFinished(null)
        } catch (e: IOException) {
            listenerProxy.onDownloadError(e)
        }
    }
}
