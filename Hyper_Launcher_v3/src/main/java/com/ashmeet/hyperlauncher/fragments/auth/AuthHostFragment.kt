package com.ashmeet.hyperlauncher.fragments.auth

import android.os.Bundle
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentManager
import com.ashmeet.hyperlauncher.screens.auth.AuthLayout
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R

class AuthHostFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (childFragmentManager.backStackEntryCount > 0) {
                    if (view?.findViewById<View>(R.id.container_fragment_auth) == null) return
                    childFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val fm = childFragmentManager
                var backStackCount by remember { mutableStateOf(fm.backStackEntryCount) }

                DisposableEffect(fm) {
                    val listener = FragmentManager.OnBackStackChangedListener {
                        backStackCount = fm.backStackEntryCount
                    }
                    fm.addOnBackStackChangedListener(listener)
                    onDispose {
                        fm.removeOnBackStackChangedListener(listener)
                    }
                }

                PojavTheme {
                    AuthLayout(
                        title = translatedText("Login"),
                        onBack = if (backStackCount > 0) {
                            { requireActivity().onBackPressedDispatcher.onBackPressed() }
                        } else null,
                        onFragmentViewCreated = {
                            val fm = childFragmentManager
                            if (fm.findFragmentById(R.id.container_fragment_auth) == null) {
                                fm.beginTransaction()
                                    .replace(
                                        R.id.container_fragment_auth,
                                        SelectAuthFragment::class.java,
                                        null,
                                        SelectAuthFragment.TAG
                                    )
                                    .commitAllowingStateLoss()
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "AUTH_HOST_FRAGMENT"
    }
}
