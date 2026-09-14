package com.ashmeet.hyperlauncher.fragments.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.fragments.instances.InstanceEditorFragment
import com.ashmeet.hyperlauncher.fragments.instances.InstanceSelectionFragment
import com.ashmeet.hyperlauncher.fragments.home.about.AboutFragment
import com.ashmeet.hyperlauncher.screens.home.MainMenuFragmentCompose
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.activity.CustomControlsActivity
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.utils.FileUtils

class MainMenuFragment : Fragment() {

    private val mModInstallerLauncher = registerForActivityResult(OpenDocumentWithExtension("jar")) { data ->
        if (data != null) Tools.launchModInstaller(requireContext(), data)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    MainMenuFragmentCompose(
                        onAboutClick = {
                            Tools.swapFragment(
                                requireActivity(),
                                AboutFragment::class.java,
                                AboutFragment.TAG,
                                null
                            )
                        },
                        onSocialMediaClick = {
                            Tools.openURL(
                                requireActivity(),
                                getString(R.string.social_media_invite)
                            )
                        },
                        onCustomControlsClick = {
                            startActivity(
                                Intent(
                                    requireContext(),
                                    CustomControlsActivity::class.java
                                )
                            )
                        },
                        onInstallJarClick = { runInstallerWithConfirmation() },
                        onShareLogsClick = { Tools.shareLog(requireContext()) },
                        onOpenFilesClick = { openGameDirectory(requireContext()) },
                        onEditProfileClick = {
                            Tools.swapFragment(
                                requireActivity(),
                                InstanceEditorFragment::class.java,
                                InstanceEditorFragment.TAG,
                                null
                            )
                        },
                        onPlayClick = { ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true) },
                        onVersionSpinnerClick = {
                            Tools.swapFragment(
                                requireActivity(),
                                InstanceSelectionFragment::class.java,
                                InstanceSelectionFragment.TAG,
                                null
                            )
                        },
                        onAccountManagerClick = {
                            ExtraCore.setValue(
                                ExtraConstants.SELECT_AUTH_METHOD,
                                true
                            )
                        }
                    )
                }
            }
        }
    }

    private fun openGameDirectory(context: Context) {
        val instance = Instances.loadSelectedInstance()
        if (instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show()
            return
        }
        val gameDirectory = instance.gameDirectory
        if (FileUtils.ensureDirectorySilently(gameDirectory)) {
            Tools.openPath(context, gameDirectory, false)
        } else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
    }

    private fun runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val TAG = "MainMenuFragment"
    }
}