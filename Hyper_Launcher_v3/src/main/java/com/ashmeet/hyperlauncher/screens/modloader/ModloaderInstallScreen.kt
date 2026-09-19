package com.ashmeet.hyperlauncher.screens.modloader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.translation.translatedText
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
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
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
                        contentPadding = PaddingValues(top = 80.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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

            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                tonalElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = translatedText("Back"),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (isDownloading || isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            ListItem(
                modifier = Modifier.clickable { expanded = !expanded },
                leadingContent = null,
                trailingContent = {
                Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null
                )
                },
                overlineContent = null,
                supportingContent = null,
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                elevation = ListItemDefaults.elevation(),
                content = {
                Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
                )
                },
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                ) {
                    group.versions.forEach { version ->
                        ListItem(
                            modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = enabled) {
                            onVersionSelected(version.data)
                            },
                            leadingContent = null,
                            trailingContent = null,
                            overlineContent = null,
                            supportingContent = null,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            elevation = ListItemDefaults.elevation(),
                            content = {
                            Text(
                            text = version.name,
                            style = MaterialTheme.typography.bodyLarge
                            )
                            },
                        )
                    }
                }
            }
        }
    }
}
