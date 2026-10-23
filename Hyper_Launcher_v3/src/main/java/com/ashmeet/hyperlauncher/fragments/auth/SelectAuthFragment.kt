package com.ashmeet.hyperlauncher.fragments.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.auth.methods.SelectAuthMethodScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.tracker.TrackerLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper

class SelectAuthFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    SelectAuthMethodScreen(
                        onMicrosoftClick = {
                            launchAuthFragment(
                                MicrosoftLoginFragment::class.java,
                                MicrosoftLoginFragment.TAG
                            )
                        },
                        onElyByClick = {
                            launchAuthFragment(
                                ElyByLoginFragment::class.java,
                                ElyByLoginFragment.TAG
                            )
                        },
                        onLocalClick = {
                            launchAuthFragment(
                                LocalLoginFragment::class.java,
                                LocalLoginFragment.TAG
                            )
                        }
                    )
                }
            }
        }
    }

    private fun launchAuthFragment(fragmentClass: Class<out Fragment>, fragmentTag: String) {
        if (ProgressKeeper.hasProgressKey(TrackerLayout.AUTHENTICATE)) {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
            return
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_enter, R.anim.fade_exit, R.anim.fade_pop_enter, R.anim.fade_pop_exit)
            .replace(R.id.container_fragment_auth, fragmentClass, null, fragmentTag)
            .addToBackStack(fragmentTag)
            .commit()
    }

    companion object {
        const val TAG = "AUTH_SELECT_FRAGMENT"
    }
}