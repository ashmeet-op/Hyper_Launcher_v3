package com.ashmeet.hyperlauncher.fragments.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.auth.skin.TextureSelectionScreen
import com.ashmeet.hyperlauncher.screens.auth.skin.TextureType
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

class TextureSelectionFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val typeName = arguments?.getString(ARG_TEXTURE_TYPE) ?: TextureType.SKIN.name
        val textureType = try { TextureType.valueOf(typeName) } catch (_: Exception) { TextureType.SKIN }

        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    TextureSelectionScreen(
                        type = textureType,
                        account = try { Accounts.getCurrent() } catch (_: Exception) { null },
                        onApplied = {
                            ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "TEXTURE_SELECTION_FRAGMENT"
        const val ARG_TEXTURE_TYPE = "texture_type"

        fun newInstance(type: TextureType): TextureSelectionFragment {
            return TextureSelectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXTURE_TYPE, type.name)
                }
            }
        }
    }
}
