package com.ashmeet.hyperlauncher.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.plugins.natives.LibraryPlugin
import com.ashmeet.hyperlauncher.screens.settings.VideoSettingsScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

class LauncherPreferenceVideoFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val isAngleAvailable = LibraryPlugin.discoverPlugin(requireContext(), LibraryPlugin.ID_ANGLE_PLUGIN) != null
        val supportsTurnip = com.ashmeet.hyperlauncher.utils.RendererCompatUtil.checkVulkanSupport(requireContext().packageManager) && net.kdt.pojavlaunch.utils.GLInfoUtils.getGlInfo().isAdreno
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    VideoSettingsScreen(
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        isAngleAvailable = isAngleAvailable,
                        isZinkPreferSystemDriverVisible = supportsTurnip
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
