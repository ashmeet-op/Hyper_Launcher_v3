package com.ashmeet.hyperlauncher.screens.game

import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingScreen(
    text: String,
    warning: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (warning.isNotEmpty()) {
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { LauncherComposeHelper.setLoadingVisible(false) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
