package com.ashmeet.hyperlauncher.fragments.modloader

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.modloader.ModloaderInstallScreen
import com.ashmeet.hyperlauncher.screens.modloader.ModloaderVersionGroup
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper

import java.io.File
import java.io.IOException

abstract class ModVersionListFragment<T>(private val mFragmentTag: String) : Fragment(), Runnable, ModloaderDownloadListener {
    private val mExtraTag: String = mFragmentTag + "_proxy"

    private var versionGroups by mutableStateOf<List<ModloaderVersionGroup<Any>>>(emptyList())
    private var isLoading by mutableStateOf(false)
    private var isDownloading by mutableStateOf(false)
    private var loadError by mutableStateOf<Exception?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    ModloaderInstallScreen(
                        title = getString(getTitleText()),
                        isLoading = isLoading,
                        isDownloading = isDownloading,
                        loadError = loadError,
                        versionGroups = versionGroups,
                        onBack = { Tools.removeCurrentFragment(requireActivity()) },
                        onRetry = {
                            loadError = null
                            isLoading = true
                            Thread(this@ModVersionListFragment).start()
                        },
                        onVersionSelected = { onVersionSelected(it) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val taskProxy = getTaskProxy()
        if (taskProxy != null) {
            isDownloading = true
            taskProxy.attachListener(this)
        }
        isLoading = true
        Thread(this).start()
    }

    override fun onStop() {
        getTaskProxy()?.detachListener()
        super.onStop()
    }

    override fun run() {
        try {
            val versions = loadVersionList()
            val groups = mapToGroups(versions)
            Tools.runOnUiThread {
                versionGroups = groups
                isLoading = false
            }
        } catch (e: IOException) {
            Tools.runOnUiThread {
                if (context != null) {
                    loadError = e
                    isLoading = false
                }
            }
        }
    }

    private fun onVersionSelected(selectedVersion: Any) {
        if (ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return
        }
        val taskProxy = ModloaderListenerProxy()
        val downloadTask = createDownloadTask(selectedVersion, taskProxy)
        setTaskProxy(taskProxy)
        taskProxy.attachListener(this)
        isDownloading = true
        Thread(downloadTask).start()
    }

    override fun onDownloadFinished(downloadedFile: File?) {
        Tools.runOnUiThread {
            val context = requireContext()
            getTaskProxy()?.detachListener()
            setTaskProxy(null)
            isDownloading = false
            parentFragmentManager.popBackStackImmediate()
            onDownloadFinished(context, downloadedFile)
        }
    }

    override fun onDataNotAvailable() {
        Tools.runOnUiThread {
            val context = requireContext()
            getTaskProxy()?.detachListener()
            setTaskProxy(null)
            isDownloading = false
            Tools.dialog(
                context,
                context.getString(R.string.global_error),
                context.getString(getNoDataMsg())
            )
        }
    }

    override fun onDownloadError(e: Exception) {
        Tools.runOnUiThread {
            val context = requireContext()
            getTaskProxy()?.detachListener()
            setTaskProxy(null)
            isDownloading = false
            Tools.showError(context, e)
        }
    }

    private fun setTaskProxy(proxy: ModloaderListenerProxy?) {
        ExtraCore.setValue(mExtraTag, proxy)
    }

    private fun getTaskProxy(): ModloaderListenerProxy? {
        return ExtraCore.getValue(mExtraTag) as? ModloaderListenerProxy
    }

    abstract fun getTitleText(): Int
    abstract fun getNoDataMsg(): Int

    @Throws(IOException::class)
    abstract fun loadVersionList(): T
    abstract fun mapToGroups(data: T): List<ModloaderVersionGroup<Any>>
    abstract fun createDownloadTask(selectedVersion: Any, listenerProxy: ModloaderListenerProxy): Runnable
    abstract fun onDownloadFinished(context: Context, downloadedFile: File?)
}
