package com.ashmeet.hyperlauncher.fragments.recorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.recorder.RecordingsGalleryScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.Tools

class RecordingsGalleryFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    RecordingsGalleryScreen(
                        onBack = { Tools.removeCurrentFragment(requireActivity()) }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "RecordingsGalleryFragment"
    }
}
