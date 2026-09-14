package com.ashmeet.hyperlauncher.fragments.modloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ashmeet.hyperlauncher.screens.modloader.fabric.FabriclikeInstallScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.FabricVersion
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import java.io.File
import java.io.IOException
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthService
import java.net.URL
import android.util.Log
import net.kdt.pojavlaunch.instances.Instance

abstract class FabriclikeInstallFragment(
    private val mFabriclikeUtils: FabriclikeUtils,
    private val mFragmentTag: String
) : Fragment(), ModloaderDownloadListener {
    private val mExtraTag: String = mFragmentTag + "_proxy"

    private var gameVersions by mutableStateOf<List<FabricVersion>>(emptyList())
    private var loaderVersions by mutableStateOf<List<FabricVersion>>(emptyList())
    private var isLoading by mutableStateOf(false)
    private var isInstalling by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    FabriclikeInstallScreen(
                        loaderName = mFabriclikeUtils.name,
                        isLoading = isLoading,
                        isInstalling = isInstalling,
                        gameVersions = gameVersions,
                        loaderVersions = loaderVersions,
                        onInstall = { gameVersion, loaderVersion, isHyperClientEnabled, hyperClientVersionId ->
                            performInstallation(gameVersion, loaderVersion, isHyperClientEnabled, hyperClientVersionId)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val proxy = getListenerProxy()
        if (proxy != null) {
            isInstalling = true
            proxy.attachListener(this)
        }
        loadVersions()
    }

    private fun loadVersions() {
        isLoading = true
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val gv = mFabriclikeUtils.downloadGameVersions()
                val lv = mFabriclikeUtils.downloadLoaderVersions()
                withContext(Dispatchers.Main) {
                    gameVersions = gv?.toList() ?: emptyList()
                    loaderVersions = lv?.toList() ?: emptyList()
                    isLoading = false
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Tools.showError(requireContext(), e)
                }
            }
        }
    }

    private fun performInstallation(gameVersion: String, loaderVersion: String, isHyperClientEnabled: Boolean, hyperClientVersionId: String?) {
        if (ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return
        }
        val proxy = ModloaderListenerProxy()
        proxy.attachListener(this)
        setListenerProxy(proxy)
        isInstalling = true

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val versionId = mFabriclikeUtils.install(gameVersion, loaderVersion)
                if (versionId == null) {
                    withContext(Dispatchers.Main) {
                        getListenerProxy()?.onDataNotAvailable()
                    }
                    return@launch
                }
                val instance = Instances.createInstance({ i ->
                    if (isHyperClientEnabled && mFabriclikeUtils.name.lowercase() == "fabric") {
                        i.name = "Hyper Client"
                        i.icon = "default"
                        i.hyperClientVersion = hyperClientVersionId
                    } else {
                        i.name = mFabriclikeUtils.name
                        i.icon = mFabriclikeUtils.iconName
                    }
                    i.versionId = versionId
                }, versionId)

                if (isHyperClientEnabled && mFabriclikeUtils.name.lowercase() == "fabric") {
                    installHyperClientMods(instance, gameVersion, hyperClientVersionId)
                }

                withContext(Dispatchers.Main) {
                    getListenerProxy()?.onDownloadFinished(null)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Tools.showErrorRemote(e)
                }
            }
        }
    }

    private suspend fun installHyperClientMods(instance: Instance, gameVersion: String, hyperClientVersionId: String?) {
        val modsFolder = File(instance.gameDirectory, "mods")
        modsFolder.mkdirs()

        val modsToInstall = listOf("hyperclient", "fabric-api", "modmenu")
        modsToInstall.forEach { modId ->
            try {
                val versions = ModrinthService.getProjectVersions(modId)
                val compatibleVersion = if (modId == "hyperclient" && hyperClientVersionId != null) {
                    versions.find { it.id == hyperClientVersionId }
                } else {
                    versions.firstOrNull { v ->
                        v.gameVersions.contains(gameVersion) && v.loaders.any { it.equals("fabric", ignoreCase = true) }
                    }
                }

                if (compatibleVersion != null) {
                    val url = URL(compatibleVersion.downloadUrl)
                    val fileName = compatibleVersion.downloadUrl.substringAfterLast("/")
                    val destFile = File(modsFolder, fileName)

                    url.openStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FabriclikeInstall", "Failed to install mod $modId", e)
            }
        }
    }

    override fun onDownloadFinished(downloadedFile: File?) {
        Tools.runOnUiThread {
            getListenerProxy()?.detachListener()
            setListenerProxy(null)
            isInstalling = false
            parentFragmentManager.popBackStackImmediate()
        }
    }

    override fun onDataNotAvailable() {
        Tools.runOnUiThread {
            val context = requireContext()
            getListenerProxy()?.detachListener()
            setListenerProxy(null)
            isInstalling = false
            Tools.dialog(
                context,
                context.getString(R.string.global_error),
                context.getString(R.string.fabric_dl_cant_read_meta, mFabriclikeUtils.name)
            )
        }
    }

    override fun onDownloadError(e: Exception) {
        Tools.runOnUiThread {
            getListenerProxy()?.detachListener()
            setListenerProxy(null)
            isInstalling = false
            Tools.showError(requireContext(), e)
        }
    }

    private fun getListenerProxy(): ModloaderListenerProxy? {
        return ExtraCore.getValue(mExtraTag) as? ModloaderListenerProxy
    }

    private fun setListenerProxy(listenerProxy: ModloaderListenerProxy?) {
        ExtraCore.setValue(mExtraTag, listenerProxy)
    }
}
