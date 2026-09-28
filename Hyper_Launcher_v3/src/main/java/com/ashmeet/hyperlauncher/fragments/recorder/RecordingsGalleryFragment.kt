package com.ashmeet.hyperlauncher.fragments.recorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.recorder.RecordingsGalleryScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme

class RecordingsGalleryFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    RecordingsGalleryScreen(
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "RecordingsGalleryFragment"
    }
}
