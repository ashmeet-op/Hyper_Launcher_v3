package com.ashmeet.hyperlauncher.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.settings.VideoSettingsScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.utils.GpuUtils
import net.kdt.pojavlaunch.utils.MCOptionUtils
import java.io.File

class LauncherPreferenceVideoFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        updateGraphicsBackendPreference()
        val nativeLibDir = context?.applicationInfo?.nativeLibraryDir ?: Tools.NATIVE_LIB_DIR
        val isAngleAvailable = (nativeLibDir != null && File(nativeLibDir, "libEGL_angle.so").exists()) ||
                File("/system/lib64/libEGL_angle.so").exists() ||
                File("/system/lib/libEGL_angle.so").exists()
        val supportsTurnip = GpuUtils.checkVulkanSupport(requireContext().packageManager) && GpuUtils.getGlInfo().isAdreno
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    VideoSettingsScreen(
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        isAngleAvailable = isAngleAvailable,
                        isZinkPreferSystemDriverVisible = supportsTurnip,
                        onNavigateToRendererConfig = {
                            Tools.swapFragment(requireActivity(), LauncherPreferenceRendererConfigFragment::class.java, null, null)
                        }
                    )
                }
            }
        }
    }

    private fun updateGraphicsBackendPreference() {
        val context = context ?: return
        val selectedInstance = Instances.loadSelectedInstance()
        val gameDir = selectedInstance?.gameDirectory?.absolutePath ?: Tools.DIR_GAME_NEW
        if (gameDir != null) {
            MCOptionUtils.load(gameDir)
            val hasOption = (MCOptionUtils.get("preferredGraphicsBackend") != null) ||
                            (MCOptionUtils.get("beckend") != null) ||
                            (MCOptionUtils.get("backend") != null)
            if (hasOption) {
                LauncherPreferences.prefs.edit { putString("preferredGraphicsBackend", "minecraft") }
            } else {
                if (LauncherPreferences.prefs.getString("preferredGraphicsBackend", null) == "minecraft") {
                    LauncherPreferences.prefs.edit { remove("preferredGraphicsBackend") }
                }
            }
            LauncherPreferences.loadPreferences(context)
        }
    }

    override fun onResume() {
        super.onResume()
        updateGraphicsBackendPreference()
        LauncherPreferences.DEFAULT_PREF?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        LauncherPreferences.DEFAULT_PREF?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        LauncherPreferences.loadPreferences(context)
    }
}
