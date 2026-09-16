package com.ashmeet.hyperlauncher.components.rail

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun SideRail(
    onCreateNew: () -> Unit,
    onRefresh: () -> Unit,
    onImportModpack: () -> Unit,
    onBack: () -> Unit
) {
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val primaryColor = MaterialTheme.colorScheme.primary

    NavigationRail(
        containerColor = Color.Transparent,
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

        SidebarRailButton(
            icon = Icons.Rounded.Add,
            label = "New",
            onClick = onCreateNew,
            containerColor = primaryColor,
            contentColor = onPrimaryColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        SidebarRailButton(
            icon = Icons.Rounded.Refresh,
            label = "Refresh",
            onClick = onRefresh
        )

        Spacer(modifier = Modifier.height(16.dp))

        SidebarRailButton(
            icon = Icons.Rounded.Search,
            label = "Import",
            onClick = onImportModpack
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
