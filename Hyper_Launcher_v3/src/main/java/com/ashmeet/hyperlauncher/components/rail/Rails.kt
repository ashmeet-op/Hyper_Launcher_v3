package com.ashmeet.hyperlauncher.components.rail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import kotlinx.coroutines.delay
import net.ashmeet.hyperlauncher.R
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SideNavigationRail(
    isEditor: Boolean,
    onAction: (Int) -> Unit,
    isExport: Boolean = false
) {
    NavigationRail(
        containerColor = NavigationRailDefaults.ContainerColor,
        windowInsets = WindowInsets(0.dp),
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavigationRailItem(
                selected = false,
                onClick = { onAction(-1) },
                icon = { Icon(imageVector = Icons.Rounded.ArrowBackIosNew, contentDescription = null , Modifier.rotate(180f)) },
                label = { Text(translatedText(stringResource(R.string.close))) }
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (isEditor) {
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(0) },
                    icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.customctrl_addbutton))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(1) },
                    icon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.customctrl_addbutton_drawer))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(2) },
                    icon = { Icon(imageVector = Icons.Rounded.VideogameAsset, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.customctrl_addbutton_joystick))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(3) },
                    icon = { Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.global_load))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(4) },
                    icon = { Icon(imageVector = Icons.Rounded.Description, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.global_save))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(5) },
                    icon = { Icon(imageVector = Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.customctrl_selectdefault))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(6) },
                    icon = { Icon(imageVector = if (isExport) Icons.Rounded.Share else Icons.Rounded.Close, contentDescription = null) },
                    label = { Text(stringResource(if (isExport) R.string.customctrl_export else R.string.customctrl_editor_exit)) }
                )
            } else {
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(0) },
                    icon = { Icon(imageVector = Icons.Rounded.Close, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.control_forceclose))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(1) },
                    icon = { Icon(imageVector = Icons.Rounded.Description, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.control_viewout))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(2) },
                    icon = { Icon(imageVector = Icons.Rounded.Keyboard, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.control_customkey))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(3) },
                    icon = { Icon(imageVector = Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.quick_setting_title))) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NavigationRailItem(
                    selected = false,
                    onClick = { onAction(4) },
                    icon = { Icon(imageVector = Icons.Rounded.Build, contentDescription = null) },
                    label = { Text(translatedText(stringResource(R.string.mcl_option_customcontrol))) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SideRail(
    onRefresh: () -> Unit,
    onImportModpack: () -> Unit,
    onBack: () -> Unit,
    isSearchActive: Boolean = false,
    fabMenuContent: @Composable FloatingActionButtonMenuScope.(() -> Unit) -> Unit = {}
) {
    var refreshRotationTarget by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val refreshRotation by animateFloatAsState(
        targetValue = refreshRotationTarget,
        animationSpec = tween(durationMillis = 600),
        label = "RefreshRotation"
    )

    var fabMenuExpanded by remember { mutableStateOf(false) }
    var innerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(fabMenuExpanded) {
        if (fabMenuExpanded) {
            innerVisible = true
        }
    }

    LaunchedEffect(innerVisible) {
        if (!innerVisible && fabMenuExpanded) {
            delay(200.milliseconds)
            fabMenuExpanded = false
        }
    }

    NavigationRail(
        containerColor = Color.Transparent,
        windowInsets = WindowInsets(0.dp),
        modifier = Modifier.fillMaxHeight(),
        header = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }

    ) {
        Spacer(modifier = Modifier.weight(1f))

        val secondaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSecondaryColor = MaterialTheme.colorScheme.onPrimary
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        val density = LocalDensity.current
        val popupOffset = remember(density) { with(density) { 60.dp.roundToPx() } }

        Box(
            modifier = Modifier.size(56.dp).zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            ToggleFloatingActionButton(
                checked = fabMenuExpanded,
                onCheckedChange = {
                    if (it) {
                        fabMenuExpanded = true
                    } else {
                        innerVisible = false
                    }
                },
                modifier = Modifier.size(56.dp),
                containerSize = { 56.dp },
                contentAlignment = Alignment.Center,
                containerColor = { progress ->
                    lerp(secondaryColor, surfaceColor, progress)
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                rotationZ = checkedProgress * 45f
                            }
                            .animateIcon(
                                checkedProgress = { checkedProgress },
                                color = { progress ->
                                    lerp(onSecondaryColor, onSurfaceColor, progress)
                                }
                            )
                    )
                }
            }

            if (fabMenuExpanded) {
                Popup(
                    alignment = Alignment.CenterStart,
                    offset = IntOffset(popupOffset, 0),
                    onDismissRequest = { innerVisible = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    Box {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = innerVisible,
                            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0f, transformOrigin = TransformOrigin(0f, 0.5f)),
                            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0f, transformOrigin = TransformOrigin(0f, 0.5f))
                        ) {
                            FloatingActionButtonMenu(
                                expanded = innerVisible,
                                horizontalAlignment = Alignment.Start,
                                button = { Box(Modifier.size(0.dp)) }
                            ) {
                                fabMenuContent { 
                                    innerVisible = false
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        IconButton(
            onClick = {
                refreshRotationTarget += 360f
                onRefresh()
            },
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer(rotationZ = refreshRotation),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        IconToggleButton(
            checked = isSearchActive,
            onCheckedChange = {
                onImportModpack()
            },
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                checkedContainerColor = MaterialTheme.colorScheme.secondary,
                checkedContentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
