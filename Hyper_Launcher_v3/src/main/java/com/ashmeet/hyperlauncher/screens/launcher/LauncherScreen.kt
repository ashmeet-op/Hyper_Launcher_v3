package com.ashmeet.hyperlauncher.screens.launcher

import android.content.SharedPreferences
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.ashmeet.hyperlauncher.components.layout.LauncherBackground
import com.ashmeet.hyperlauncher.components.layout.ProgressLayout
import com.ashmeet.hyperlauncher.components.spinner.AccountSpinnerCompose
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.ashmeet.hyperlauncher.utils.drawable.MaterialIconUtil
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener

@Composable
fun PojavLauncherScreen(
    settingsIconRes: Int,
    isFileManagerVisible: Boolean,
    onSettingsClick: () -> Unit,
    onContentInstallerClick: () -> Unit,
    onInstanceDirectoryClick: () -> Unit,
    onFragmentViewCreated: (FrameLayout) -> Unit
) {
    var taskCount by remember { mutableIntStateOf(ProgressKeeper.getTaskCount()) }
    var launcherBgPath by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH) }
    var launcherBlurredElementsEnabled by remember { mutableStateOf(LauncherPreferences.PREF_BLURRED_ELEMENTS_ENABLED) }

    DisposableEffect(Unit) {
        val listener = TaskCountListener { count ->
            taskCount = count
            false
        }
        val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "launcher_background_path" -> launcherBgPath = LauncherPreferences.prefs.getString("launcher_background_path", null)
                "blurred_elements_enabled" -> launcherBlurredElementsEnabled = LauncherPreferences.prefs.getBoolean("blurred_elements_enabled", false)
            }
        }
        ProgressKeeper.addTaskCountListener(listener)
        LauncherPreferences.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        onDispose {
            ProgressKeeper.removeTaskCountListener(listener)
            LauncherPreferences.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (launcherBgPath != null) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LauncherBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .zIndex(1f)
                ) {
                    if (launcherBlurredElementsEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(16.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (launcherBgPath != null) Color.Transparent else MaterialTheme.colorScheme.surface)
                        )
                    }

                    AccountSpinnerCompose(
                        modifier = Modifier.fillMaxSize(),
                        hideDivider = taskCount > 0,
                        containerColor = Color.Transparent
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                    ) {
                        if (isFileManagerVisible) {
                            IconButton(
                                onClick = onContentInstallerClick,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = translatedText("Content Installer"),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = onInstanceDirectoryClick,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = translatedText("Instance Directory"),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = MaterialIconUtil.getIconVector(settingsIconRes),
                                contentDescription = translatedText("Settings"),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (taskCount > 0) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = if (launcherBgPath != null) Color.Transparent else MaterialTheme.colorScheme.surface
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { context ->
                            FrameLayout(context).apply {
                                id = R.id.container_fragment
                                onFragmentViewCreated(this)
                            }
                        },
                        update = {},
                        modifier = Modifier.fillMaxSize()
                    )

                    ProgressLayout(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "File Manager Visible")
@Composable
fun PojavLauncherScreenPreview() {
    PojavTheme {
        PojavLauncherScreen(
            settingsIconRes = MaterialIconUtil.ICON_SETTINGS,
            isFileManagerVisible = true,
            onSettingsClick = {},
            onContentInstallerClick = {},
            onInstanceDirectoryClick = {},
            onFragmentViewCreated = {}
        )
    }
}

@Preview(showBackground = true, name = "File Manager Hidden")
@Composable
fun PojavLauncherScreenHiddenPreview() {
    PojavTheme {
        PojavLauncherScreen(
            settingsIconRes = MaterialIconUtil.ICON_SETTINGS,
            isFileManagerVisible = false,
            onSettingsClick = {},
            onContentInstallerClick = {},
            onInstanceDirectoryClick = {},
            onFragmentViewCreated = {}
        )
    }
}
