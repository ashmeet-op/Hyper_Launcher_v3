package com.ashmeet.hyperlauncher.fragments.home.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.home.about.AboutScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    AboutScreen(
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "AboutFragment"
    }
}