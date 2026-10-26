package com.ashmeet.hyperlauncher.fragments.instances

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.instances.InstanceDirectoryScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.HyperUtils

class InstanceDirectoryFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    InstanceDirectoryScreen(
                        onBack = { HyperUtils.removeCurrentFragment(requireActivity()) }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "InstanceDirectoryFragment"
    }
}
