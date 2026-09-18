package com.ashmeet.hyperlauncher.components.rail

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.ashmeet.hyperlauncher.R

@Composable
fun SideNavigationRail(
    isEditor: Boolean,
    onAction: (Int) -> Unit,
    isExport: Boolean = false
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = WindowInsets(0.dp),
        modifier = Modifier
            .fillMaxHeight()
            .width(240.dp),
        header = {
            Column(horizontalAlignment = Alignment.Start) {
                SidebarRailButton(
                    icon = Icons.Rounded.Close,
                    label = translatedText(stringResource(R.string.close)),
                    onClick = { onAction(-1) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            if (isEditor) {
                SidebarRailButton(
                    icon = Icons.Rounded.Add,
                    label = translatedText(stringResource(R.string.customctrl_addbutton)),
                    onClick = { onAction(0) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.Search,
                    label = translatedText(stringResource(R.string.customctrl_addbutton_drawer)),
                    onClick = { onAction(1) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.VideogameAsset,
                    label = translatedText(stringResource(R.string.customctrl_addbutton_joystick)),
                    onClick = { onAction(2) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.Refresh,
                    label = translatedText(stringResource(R.string.global_load)),
                    onClick = { onAction(3) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.Description,
                    label = translatedText(stringResource(R.string.global_save)),
                    onClick = { onAction(4) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.Settings,
                    label = translatedText(stringResource(R.string.customctrl_selectdefault)),
                    onClick = { onAction(5) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = if (isExport) Icons.Rounded.Share else Icons.Rounded.Close,
                    label = stringResource(if (isExport) R.string.customctrl_export else R.string.customctrl_editor_exit),
                    onClick = { onAction(6) },
                    isExpanded = true
                )
            } else {
                SidebarRailButton(
                    icon = Icons.Rounded.Close,
                    label = translatedText(stringResource(R.string.control_forceclose)),
                    onClick = { onAction(0) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.Description,
                    label = translatedText(stringResource(R.string.control_viewout)),
                    onClick = { onAction(1) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.Keyboard,
                    label = translatedText(stringResource(R.string.control_customkey)),
                    onClick = { onAction(2) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.Settings,
                    label = translatedText(stringResource(R.string.quick_setting_title)),
                    onClick = { onAction(3) },
                    isExpanded = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                SidebarRailButton(
                    icon = Icons.Rounded.Build,
                    label = translatedText(stringResource(R.string.mcl_option_customcontrol)),
                    onClick = { onAction(4) },
                    isExpanded = true
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

    NavigationRail(
        containerColor = Color.Transparent,
        windowInsets = WindowInsets(0.dp),
        modifier = Modifier.fillMaxHeight(),
        header = {
            SidebarRailButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                label = "Back",
                onClick = onBack
            )
        }
    ) {
        Spacer(modifier = Modifier.weight(1f))

        val fabMenuStartColor = MaterialTheme.colorScheme.secondary
        val fabMenuEndColor = MaterialTheme.colorScheme.surface
        val fabMenuIconStartColor = MaterialTheme.colorScheme.onSecondary
        val fabMenuIconEndColor = MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier.size(56.dp)
        ) {
            FloatingActionButtonMenu(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .wrapContentSize(align = Alignment.BottomCenter, unbounded = true)
                    .offset(y = 16.dp),
                expanded = fabMenuExpanded,
                horizontalAlignment = Alignment.CenterHorizontally,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { 
                            fabMenuExpanded = !fabMenuExpanded
                        },
                        modifier = Modifier.size(56.dp),
                        containerSize = { 56.dp },
                        contentAlignment = Alignment.Center,
                        containerColor = { progress ->
                            lerp(fabMenuStartColor, fabMenuEndColor, progress)
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val imageVector = if (fabMenuExpanded) Icons.Rounded.Close else Icons.Rounded.Add
                            Icon(
                                imageVector = imageVector,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .animateIcon(
                                        checkedProgress = { checkedProgress },
                                        color = { progress ->
                                            lerp(fabMenuIconStartColor, fabMenuIconEndColor, progress)
                                        }
                                    )
                            )
                        }
                    }
                }
            ) {
                fabMenuContent { fabMenuExpanded = false }
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
