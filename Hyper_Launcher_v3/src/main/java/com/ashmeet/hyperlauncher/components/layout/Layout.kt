package com.ashmeet.hyperlauncher.components.layout

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ashmeet.hyperlauncher.components.rail.SideRail
import com.ashmeet.hyperlauncher.components.rail.SideRailPortrait
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.blur.backgroundGlass

@Composable
fun ScreenLayout(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onImportModpack: () -> Unit = {},
    isSearchActive: Boolean = false,
    sideRailExtra: @Composable (BoxScope.() -> Unit)? = null,
    fabMenuContent: @Composable FloatingActionButtonMenuScope.(() -> Unit) -> Unit = {},
    header: @Composable () -> Unit = {},
    sidebar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        val blur = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR > 0) LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR else 25
        val glassColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

        if (isPortrait) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (sidebar == null) 0.dp else 0.dp)
                        .backgroundGlass(blur, glassColor, enabled = LauncherPreferences.PREF_LIQUID_GLASS_ELEMENTS_ENABLED),
                    shape = RoundedCornerShape(32.dp),
                    color = glassColor,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    focusManager.clearFocus()
                                })
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            header()
                            content()
                        }
                    }
                }

                if (sidebar != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .backgroundGlass(blur, glassColor, enabled = LauncherPreferences.PREF_LIQUID_GLASS_ELEMENTS_ENABLED),
                        shape = RoundedCornerShape(24.dp),
                        color = glassColor,
                        tonalElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            sidebar()
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
                    SideRailPortrait(
                        onRefresh = onRefresh,
                        onImportModpack = onImportModpack,
                        onBack = onBack,
                        isSearchActive = isSearchActive,
                        fabMenuContent = fabMenuContent
                    )
                    sideRailExtra?.invoke(this)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.zIndex(1f)) {
                    SideRail(
                        onRefresh = onRefresh,
                        onImportModpack = onImportModpack,
                        onBack = onBack,
                        isSearchActive = isSearchActive,
                        fabMenuContent = fabMenuContent
                    )
                    sideRailExtra?.invoke(this)
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp, end = if (sidebar == null) 16.dp else 0.dp)
                        .backgroundGlass(blur, glassColor, enabled = LauncherPreferences.PREF_LIQUID_GLASS_ELEMENTS_ENABLED),
                    shape = RoundedCornerShape(32.dp),
                    color = glassColor,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    focusManager.clearFocus()
                                })
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            header()
                            content()
                        }
                    }
                }

                if (sidebar != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
                            .backgroundGlass(blur, glassColor, enabled = LauncherPreferences.PREF_LIQUID_GLASS_ELEMENTS_ENABLED),
                        shape = RoundedCornerShape(32.dp),
                        color = glassColor,
                        tonalElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            sidebar()
                        }
                    }
                }
            }
        }
    }
}
