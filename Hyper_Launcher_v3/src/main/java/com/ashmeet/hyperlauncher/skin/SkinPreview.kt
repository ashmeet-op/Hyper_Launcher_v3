package com.ashmeet.hyperlauncher.skin

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileInputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SkinPreview(
    modifier: Modifier = Modifier,
    skinUrl: String? = null,
    capeUrl: String? = null,
    showSkin: Boolean = true,
    backEquipment: String = "cape",
    animation: String = "NewIdle",
    model: String = "default",
    azimuth: Float = 0f,
    onLoadingStateChanged: (Boolean) -> Unit = {},
    onWebViewCreated: (WebView) -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer))
        return
    }

    var isPageLoaded by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            onLoadingStateChanged(true)
            WebView(context).apply {
                stopLoading()
                loadUrl("about:blank")

                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.textZoom = 100
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = false
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                overScrollMode = View.OVER_SCROLL_NEVER
                setBackgroundColor(0)

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val url = request?.url ?: return null
                        if (url.host == "local-skin.pojavlauncher.net") {
                            val path = url.getQueryParameter("path")
                            if (path != null) {
                                val file = File(path)
                                if (file.exists()) {
                                    return try {
                                        WebResourceResponse("image/png", "UTF-8", FileInputStream(file))
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        isPageLoaded = true
                        onLoadingStateChanged(false)
                        val finalSkinUrl = if (!showSkin) {
                            "none"
                        } else if (skinUrl?.startsWith("file://") == true) {
                            val path = skinUrl.substring(7)
                            val file = File(path)
                            if (file.exists()) "https://local-skin.pojavlauncher.net/texture?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                            else "steve.png"
                        } else if (!skinUrl.isNullOrEmpty()) {
                            skinUrl
                        } else {
                            "steve.png"
                        }

                        val finalCapeUrl = if (capeUrl?.startsWith("file://") == true) {
                            val path = capeUrl.substring(7)
                            val file = File(path)
                            if (file.exists()) "https://local-skin.pojavlauncher.net/texture?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                            else ""
                        } else if (!capeUrl.isNullOrEmpty()) {
                            capeUrl
                        } else {
                            ""
                        }

                        val dist = if (showSkin) 190 else 80
                        view?.evaluateJavascript("loadSkin('$finalSkinUrl', '$model'); loadCape('$finalCapeUrl'); setBackEquipment('$backEquipment'); setShowSkin(${showSkin}); startAnim('$animation'); setAzimuthAndPitch($azimuth, 10, $dist);", null)
                    }
                }

                val encodedUrl = try { URLEncoder.encode(if (showSkin) skinUrl ?: "" else "", StandardCharsets.UTF_8.toString()) } catch (_: Exception) { "" }
                val finalUrl = "file:///android_asset/skinview.html" + (if (encodedUrl.isNotEmpty()) "?skin=$encodedUrl&model=$model" else "")
                loadUrl(finalUrl)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            if (isPageLoaded) {
                val skin = if (!showSkin) {
                    "none"
                } else if (skinUrl?.startsWith("file://") == true) {
                    val path = skinUrl.substring(7)
                    val file = File(path)
                    if (file.exists()) "https://local-skin.pojavlauncher.net/texture?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    else "steve.png"
                } else if (!skinUrl.isNullOrEmpty()) {
                    skinUrl
                } else {
                    "steve.png"
                }

                val cape = if (capeUrl?.startsWith("file://") == true) {
                    val path = capeUrl.substring(7)
                    val file = File(path)
                    if (file.exists()) "https://local-skin.pojavlauncher.net/texture?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    else ""
                } else if (!capeUrl.isNullOrEmpty()) {
                    capeUrl
                } else {
                    ""
                }

                val dist = if (showSkin) 190 else 80
                webView.evaluateJavascript("loadSkin('$skin', '$model');", null)
                webView.evaluateJavascript("loadCape('$cape');", null)
                webView.evaluateJavascript("setShowSkin(${showSkin});", null)
                webView.evaluateJavascript("setBackEquipment('$backEquipment');", null)
                webView.evaluateJavascript("startAnim('$animation');", null)
                webView.evaluateJavascript("setAzimuthAndPitch($azimuth, 10, $dist);", null)
                webView.evaluateJavascript("resize();", null)
            }
        }
    )
}
