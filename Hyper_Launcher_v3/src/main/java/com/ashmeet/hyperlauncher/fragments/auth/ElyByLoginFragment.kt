package com.ashmeet.hyperlauncher.fragments.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.webkit.WebView
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.screens.auth.methods.MicrosoftLoginScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

class ElyByLoginFragment : Fragment() {

    private var mWebView: WebView? = null

    private val mTrackedUrl = "internalredirect"
    private val mAuthUrl = "https://account.ely.by/oauth2/v1" +
            "?client_id=mojolauncher2" +
            "&redirect_uri=internalredirect%3A%2F%2Fcomplete" +
            "&response_type=code" +
            "&scope=account_info%20offline_access%20minecraft_server_session"
    private val mExtraCoreConstant = ExtraConstants.ELYBY_LOGIN_TODO

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    MicrosoftLoginScreen(
                        authUrl = mAuthUrl,
                        trackedUrl = mTrackedUrl,
                        isFullScreen = true,
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
        const val TAG = "ELYBY_LOGIN_FRAGMENT"
    }
}
