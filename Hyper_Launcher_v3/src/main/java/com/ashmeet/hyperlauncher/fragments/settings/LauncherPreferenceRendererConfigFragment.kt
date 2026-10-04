package com.ashmeet.hyperlauncher.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.settings.RendererConfigScreen
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme

class LauncherPreferenceRendererConfigFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    RendererConfigScreen(
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
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
