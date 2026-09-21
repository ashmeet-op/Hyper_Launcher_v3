package com.ashmeet.hyperlauncher.fragments.settings

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.MiscSettingsScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.kdt.pojavlaunch.tasks.DataMigrator

class LauncherPreferenceMiscellaneousFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mIsMicPermissionGranted by mutableStateOf(false)

    private val mRecordAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        mIsMicPermissionGranted = isGranted
        if (!isGranted) {
            Toast.makeText(requireContext(), R.string.notification_permission_toast, Toast.LENGTH_SHORT).show()
        }
    }

    private val mDataMigrationLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            DataMigrator(requireActivity(), uri).migrateData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mIsMicPermissionGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    MiscSettingsScreen(
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        isMicrophonePermissionGranted = mIsMicPermissionGranted,
                        onMicrophoneAccessClick = {
                            mRecordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onRunDataMigrationClick = {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.migration_progress_warning_title)
                                .setMessage(R.string.migration_progress_warning_summary)
                                .setPositiveButton(R.string.global_yes) { _, _ ->
                                    mDataMigrationLauncher.launch(null)
                                }
                                .setNegativeButton(R.string.global_no, null)
                                .show()
                        }
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
