package com.ashmeet.hyperlauncher.fragments.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.auth.methods.MicrosoftLoginScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

class MicrosoftLoginFragment : Fragment() {

    private var mWebView: WebView? = null

    private val mTrackedUrl = "ms-xal-00000000402b5328"
    private val mAuthUrl = "https://login.live.com/oauth20_authorize.srf" +
            "?client_id=00000000402b5328" +
            "&response_type=code" +
            "&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL" +
            "&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"
    private val mExtraCoreConstant = ExtraConstants.MICROSOFT_LOGIN_TODO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canGoBack()) {
                    goBack()
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
                PojavTheme {
                    MicrosoftLoginScreen(
                        authUrl = mAuthUrl,
                        trackedUrl = mTrackedUrl,
                        onCompletion = { fullUrl -> handleCompletion(fullUrl) },
                        onWebViewCreated = { mWebView = it }
                    )
                }
            }
        }
    }

    private fun handleCompletion(fullUrl: String) {
        val activity = activity ?: return
        val uri = Uri.parse(fullUrl)
        val error = uri.getQueryParameter("error")
        val code = uri.getQueryParameter("code")

        if (code == null) {
            activity.onBackPressedDispatcher.onBackPressed()
            if ("access_denied" != error) {
                val errorMessage = uri.getQueryParameter("error_description")
                    ?: uri.getQueryParameter("error")
                    ?: getString(R.string.oauth_unknown_error)
                Tools.dialog(activity, getString(R.string.global_error), errorMessage)
            }
            return
        }

        ExtraCore.setValue(mExtraCoreConstant, code)
        Toast.makeText(activity, R.string.oauth_web_complete, Toast.LENGTH_SHORT).show()
        Tools.backToMainMenu(activity)
    }

    fun canGoBack(): Boolean = mWebView?.canGoBack() == true
    fun goBack() { mWebView?.goBack() }

    companion object {
        const val TAG = "MICROSOFT_LOGIN_FRAGMENT"
    }
}
