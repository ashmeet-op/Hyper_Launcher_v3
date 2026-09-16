package com.ashmeet.hyperlauncher.screens.modloader

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import net.ashmeet.hyperlauncher.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ModloaderInstallScreen(
    title: String,
    isLoading: Boolean,
    isDownloading: Boolean,
    loadError: Exception?,
    versionGroups: List<ModloaderVersionGroup<T>>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onVersionSelected: (T) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                if (!isLoading && !isDownloading) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = translatedText(stringResource(R.string.global_retry))
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outline
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading && versionGroups.isEmpty()) {
                    LoadingIndicator()
                } else if (loadError != null && versionGroups.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = translatedText(stringResource(R.string.modloader_dl_failed_to_load_list)),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text(translatedText(stringResource(R.string.global_retry)))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(versionGroups) { group ->
                            ModloaderVersionGroupItem(
                                group = group,
                                enabled = !isDownloading,
                                onVersionSelected = onVersionSelected
                            )
                        }
                    }
                }
            }

            if (isDownloading || isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

data class ModloaderVersionGroup<T>(
    val name: String,
    val versions: List<ModloaderVersionItem<T>>
)

data class ModloaderVersionItem<T>(
    val name: String,
    val data: T
)

@Composable
private fun <T> ModloaderVersionGroupItem(
    group: ModloaderVersionGroup<T>,
    enabled: Boolean,
    onVersionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        ListItem(
            headlineContent = {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null
                )
            },
            modifier = Modifier.clickable { expanded = !expanded },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                group.versions.forEach { version ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = version.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        modifier = Modifier.clickable(enabled = enabled) {
                            onVersionSelected(version.data)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}
