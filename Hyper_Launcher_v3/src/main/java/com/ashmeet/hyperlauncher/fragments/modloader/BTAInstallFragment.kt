package com.ashmeet.hyperlauncher.fragments.modloader

import android.content.Context
import com.ashmeet.hyperlauncher.screens.modloader.ModloaderVersionGroup
import com.ashmeet.hyperlauncher.screens.modloader.ModloaderVersionItem
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperbackend.modloaders.BTADownloadTask
import com.ashmeet.hyperbackend.modloaders.BTAUtils
import com.ashmeet.hyperbackend.modloaders.ModloaderListenerProxy
import java.io.File
import java.io.IOException

class BTAInstallFragment : ModVersionListFragment<BTAUtils.BTAVersionList>(TAG) {
    companion object {
        const val TAG = "BTAInstallFragment"
    }

    override fun getTitleText(): Int = R.string.select_bta_version
    override fun getNoDataMsg(): Int = R.string.modloader_dl_failed_to_load_list

    @Throws(IOException::class)
    override fun loadVersionList(): BTAUtils.BTAVersionList {
        return BTAUtils.downloadVersionList()
    }

    override fun mapToGroups(versionList: BTAUtils.BTAVersionList): List<ModloaderVersionGroup<Any>> {
        val groups = mutableListOf<ModloaderVersionGroup<Any>>()
        if (versionList.testedVersions.isNotEmpty()) {
            groups.add(createGroup(R.string.bta_installer_available_versions, versionList.testedVersions))
        }
        if (versionList.untestedVersions.isNotEmpty()) {
            groups.add(createGroup(R.string.bta_installer_untested_versions, versionList.untestedVersions))
        }
        if (versionList.nightlyVersions.isNotEmpty()) {
            groups.add(createGroup(R.string.bta_installer_nightly_versions, versionList.nightlyVersions))
        }
        return groups
    }

    private fun createGroup(titleRes: Int, versions: List<BTAUtils.BTAVersion>): ModloaderVersionGroup<Any> {
        val items = mutableListOf<ModloaderVersionItem<Any>>()
        for (version in versions) {
            items.add(ModloaderVersionItem(version.versionName, version as Any))
        }
        return ModloaderVersionGroup(getString(titleRes), items)
    }

    override fun createDownloadTask(selectedVersion: Any, listenerProxy: ModloaderListenerProxy): Runnable {
        return BTADownloadTask(listenerProxy, selectedVersion as BTAUtils.BTAVersion)
    }

    override fun onDownloadFinished(context: Context, downloadedFile: File?) {}
}
