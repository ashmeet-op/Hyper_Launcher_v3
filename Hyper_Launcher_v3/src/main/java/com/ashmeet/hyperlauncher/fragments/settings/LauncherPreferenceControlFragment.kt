package com.ashmeet.hyperlauncher.fragments.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import fr.spse.gamepad_remapper.Remapper
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.CustomControlsActivity
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.fragments.controls.GamepadMapperFragment
import com.ashmeet.hyperlauncher.screens.settings.ControlSettingsScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

class LauncherPreferenceControlFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val isGyroAvailable = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null

        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    ControlSettingsScreen(
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onNavigateToCustomControls = { startActivity(Intent(requireContext(), CustomControlsActivity::class.java)) },
                        onNavigateToGamepadMapper = { Tools.swapFragment(requireActivity(), GamepadMapperFragment::class.java, GamepadMapperFragment.TAG, null) },
                        onWipeController = {
                            Remapper.wipePreferences(requireContext())
                            Toast.makeText(requireContext(), R.string.preference_controller_map_wiped, Toast.LENGTH_SHORT).show()
                        },
                        isGyroAvailable = isGyroAvailable
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
