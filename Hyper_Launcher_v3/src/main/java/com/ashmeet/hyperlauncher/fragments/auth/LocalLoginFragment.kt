package com.ashmeet.hyperlauncher.fragments.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.fragments.home.MainMenuFragment
import com.ashmeet.hyperlauncher.screens.auth.methods.LocalLoginScreen
import com.ashmeet.hyperlauncher.skin.SkinManager
import com.ashmeet.hyperlauncher.skin.androidSkinAnalyzerFacade
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import java.io.File
import java.util.regex.Pattern

class LocalLoginFragment : Fragment() {

    private val mUsernameValidationPattern = Pattern.compile("^[a-zA-Z0-9_]*$")
    private val mSkinManager = SkinManager(androidSkinAnalyzerFacade)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    LocalLoginScreen(
                        onLoginClick = { username, skinPath, capePath ->
                            if (!checkUsername(username)) {
                                val context = requireContext()
                                Tools.dialog(
                                    context,
                                    context.getString(R.string.local_login_bad_username_title),
                                    context.getString(R.string.local_login_bad_username_text)
                                )
                            } else {
                                val skinFile = skinPath?.let { File(it) }
                                val capeFile = capePath?.let { File(it) }
                                try {
                                    val prepared = mSkinManager.prepareAccount(username, skinFile, capeFile)
                                    Accounts.create { acc ->
                                        acc.username = prepared.username
                                        acc.profileId = prepared.profileId
                                        acc.skinPath = prepared.skinPath
                                        acc.capePath = prepared.capePath
                                        acc.skinModel = prepared.skinModel
                                    }.also {
                                        Accounts.setCurrent(it)
                                        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
                                    }
                                    Tools.swapFragment(requireActivity(), MainMenuFragment::class.java, MainMenuFragment.TAG, null)
                                } catch (e: Exception) {
                                    Tools.showError(requireContext(), e)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun checkUsername(username: String): Boolean {
        val matcher = mUsernameValidationPattern.matcher(username)
        return !(username.isEmpty() || username.length < 3 || username.length > 16 || !matcher.find())
    }

    companion object {
        const val TAG = "LOCAL_LOGIN_FRAGMENT"
    }
}
