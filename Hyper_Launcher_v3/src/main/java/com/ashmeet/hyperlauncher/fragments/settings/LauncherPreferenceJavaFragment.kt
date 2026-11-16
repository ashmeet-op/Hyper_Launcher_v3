package com.ashmeet.hyperlauncher.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ashmeet.hyperlauncher.screens.settings.JavaSettingsScreen
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.HyperUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ashmeet.hyperbackend.contracts.OpenDocumentWithExtension
import com.ashmeet.hyperlauncher.runtime.RuntimeUtils
import com.ashmeet.hyperlauncher.runtime.Runtime

class LauncherPreferenceJavaFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val mVmInstallLauncher: ActivityResultLauncher<Any?> =
        registerForActivityResult(OpenDocumentWithExtension("xz")) { data ->
            if (data != null) {
                context?.let { HyperUtils.installRuntimeFromUri(it, data) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val deviceRam = HyperUtils.getTotalDeviceMemory(requireContext())
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
                RuntimeUtils.removeRuntimeNamed(runtime.name)
                withContext(Dispatchers.Main) {
                    if (LauncherPreferences.PREF_DEFAULT_RUNTIME == runtime.name) {
                        val remaining = RuntimeUtils.getRuntimes()
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
                    context?.let { HyperUtils.showError(it, e) }
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
