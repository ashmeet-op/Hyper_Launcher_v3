package com.ashmeet.hyperlauncher.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.edit
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.JavaSettingsScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.Runtime

class LauncherPreferenceJavaFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val mVmInstallLauncher: ActivityResultLauncher<Any?> =
        registerForActivityResult(OpenDocumentWithExtension("xz")) { data ->
            if (data != null) {
                context?.let { Tools.installRuntimeFromUri(it, data) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val deviceRam = Tools.getTotalDeviceMemory(requireContext())
        val maxRAM = if (Architecture.is32BitsDevice() || deviceRam < 2048) {
            minOf(1024, deviceRam)
        } else {
            deviceRam - if (deviceRam < 3064) 800 else 1024
        }

        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    JavaSettingsScreen(
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onAddRuntime = { mVmInstallLauncher.launch(null) },
                        onDeleteRuntime = { runtime, onDeleted -> deleteRuntime(runtime, onDeleted) },
                        maxRam = maxRAM
                    )
                }
            }
        }
    }

    private fun deleteRuntime(runtime: Runtime, onDeleted: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MultiRTUtils.removeRuntimeNamed(runtime.name)
                withContext(Dispatchers.Main) {
                    if (LauncherPreferences.PREF_DEFAULT_RUNTIME == runtime.name) {
                        val remaining = MultiRTUtils.getRuntimes()
                        if (remaining.isNotEmpty()) {
                            val newDefault = remaining[0].name
                            LauncherPreferences.prefs.edit { putString("defaultRuntime", newDefault) }
                            LauncherPreferences.loadPreferences(context)
                        }
                    }
                    onDeleted()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    context?.let { Tools.showError(it, e) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LauncherPreferences.prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        LauncherPreferences.prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        LauncherPreferences.loadPreferences(context)
    }
}
