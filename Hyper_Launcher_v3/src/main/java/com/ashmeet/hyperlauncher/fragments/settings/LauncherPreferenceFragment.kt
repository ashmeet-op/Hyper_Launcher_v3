package com.ashmeet.hyperlauncher.fragments.settings

import android.Manifest
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.fragments.home.about.AboutFragment
import com.ashmeet.hyperlauncher.screens.settings.MainSettingsScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.activity.LauncherActivity
import com.ashmeet.hyperlauncher.utils.Tools

open class LauncherPreferenceFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    MainSettingsScreen(
                        onNavigateToVideo = { navigateTo(LauncherPreferenceVideoFragment::class.java) },
                        onNavigateToControl = { navigateTo(LauncherPreferenceControlFragment::class.java) },
                        onNavigateToJava = { navigateTo(LauncherPreferenceJavaFragment::class.java) },
                        onNavigateToMisc = { navigateTo(LauncherPreferenceMiscellaneousFragment::class.java) },
                        onNavigateToExperimental = { navigateTo(LauncherPreferenceExperimentalFragment::class.java) },
                        onNavigateToDeveloper = { navigateTo(LauncherPreferenceDeveloperFragment::class.java) },
                        onNavigateToAppearance = { navigateTo(LauncherPreferenceAppearanceFragment::class.java) },
                        onNavigateToAbout = { navigateTo(AboutFragment::class.java) },
                        showNotificationRequest = !getLauncherActivity().checkForPermission(33, Manifest.permission.POST_NOTIFICATIONS),
                        onNotificationRequestClick = { getLauncherActivity().askForPermission(33, Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }
            }
        }
    }

    protected fun navigateTo(fragmentClass: Class<out Fragment>) {
        Tools.swapFragment(requireActivity(), fragmentClass, null, null)
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

    protected fun getLauncherActivity(): LauncherActivity {
        return activity as LauncherActivity
    }
}
