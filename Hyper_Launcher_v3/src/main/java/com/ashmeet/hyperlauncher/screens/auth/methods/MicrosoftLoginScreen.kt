package com.ashmeet.hyperlauncher.screens.auth.methods

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ashmeet.hyperlauncher.theme.PojavTheme

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MicrosoftLoginScreen(
    authUrl: String,
    trackedUrl: String,
    isFullScreen: Boolean = false,
    onCompletion: (String) -> Unit,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isFullScreen) Modifier else Modifier.clip(RoundedCornerShape(24.dp)))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = object : WebViewClient() {
                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                    if (url.startsWith(trackedUrl)) {
                                        onCompletion(url)
                                        return true
                                    }
                                    @Suppress("DEPRECATION")
                                    return super.shouldOverrideUrlLoading(view, url)
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    if (url.startsWith(trackedUrl)) {
                                        onCompletion(url)
                                    }
                                }
                            }

                            CookieManager.getInstance().removeAllCookies(null)
                            clearHistory()
                            clearCache(true)

                            loadUrl(authUrl)
                            onWebViewCreated(this)
                        }
                    }
                )
            }
        }
}

@Preview(
    showBackground = true,
    device = "spec:width=800dp,height=400dp,dpi=420",
)
@Composable
fun MicrosoftLoginScreenPreview() {
    PojavTheme {
        MicrosoftLoginScreen(
            authUrl = "https://login.live.com/",
            trackedUrl = "https://login.live.com/oauth20_desktop.srf",
            onCompletion = {}
        )
    }
}
