package com.ashmeet.hyperlauncher.screens.game

import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.customcontrols.handleview.DrawerPullButton

@Composable
fun GameBasemainScreen(
    drawerState: DrawerState? = null,
    showLoading: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (drawerState != null) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        val button = DrawerPullButton(ctx).apply {
                            val density = ctx.resources.displayMetrics.density
                            val p = (4 * density).toInt()
                            setPadding(p, p, p, p)
                            elevation = 10 * density
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                scope.launch { drawerState.open() }
                            }
                        }
                        addView(button)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(101f)
            )
        }

        if (showLoading) {
            AnimatedVisibility(
                visible = LauncherComposeHelper.isLoading,
                exit = fadeOut(animationSpec = tween(300))
            ) {
                LoadingScreen(
                    text = LauncherComposeHelper.loadingText,
                    warning = LauncherComposeHelper.loadingWarning
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(
    text: String,
    warning: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = warning,
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
