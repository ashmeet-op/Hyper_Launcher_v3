package com.ashmeet.hyperlauncher.components.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.utils.installer.ContentInstallerType
import com.ashmeet.hyperlauncher.utils.installer.ModrinthVersion
import com.ashmeet.hyperlauncher.utils.installer.isMcVersionCompatible
import com.ashmeet.hyperlauncher.utils.installer.isLoaderCompatible
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VersionList(
    projectVersions: List<ModrinthVersion>,
    availableProjectMCVersions: List<String>,
    selectedProjectMCVersion: String?,
    instanceVersion: String?,
    instanceLoader: String?,
    selectedType: ContentInstallerType,
    isLoading: Boolean = false,
    onProjectMCVersionClick: (String) -> Unit,
    onVersionClick: (ModrinthVersion) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading && projectVersions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else if (selectedProjectMCVersion == null) {
            Text(
                text = translatedText("Select Game Version"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            val lazyListState = rememberLazyListState()
            LaunchedEffect(availableProjectMCVersions, instanceVersion) {
                if (availableProjectMCVersions.isNotEmpty()) {
                    val compatibleIndex = availableProjectMCVersions.indexOfFirst { v ->
                        instanceVersion != null && isMcVersionCompatible(instanceVersion, v)
                    }
                    if (compatibleIndex > 5) {
                        delay(200.milliseconds)
                        lazyListState.animateScrollToItem(compatibleIndex)
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableProjectMCVersions, key = { it }) { v ->
                    val isCompatible = instanceVersion != null && isMcVersionCompatible(instanceVersion, v)
                    SubVersionItemView(
                        text = v,
                        isCompatible = isCompatible,
                        onClick = { onProjectMCVersionClick(v) }
                    )
                }
            }
        } else {
            val filteredVersions = remember(projectVersions, selectedProjectMCVersion) {
                projectVersions.filter { it.gameVersions.contains(selectedProjectMCVersion) }
            }

            val lazyListState = rememberLazyListState()
            LaunchedEffect(filteredVersions, instanceVersion, instanceLoader) {
                if (filteredVersions.isNotEmpty()) {
                    val compatibleIndex = filteredVersions.indexOfFirst { version ->
                        instanceVersion != null && version.gameVersions.any { isMcVersionCompatible(instanceVersion, it) } &&
                                isLoaderCompatible(instanceLoader, version.loaders)
                    }
                    if (compatibleIndex > 5) {
                        delay(200.milliseconds)
                        lazyListState.animateScrollToItem(compatibleIndex)
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredVersions, key = { it.id }) { version ->
                    val isMCCompatible = instanceVersion != null && version.gameVersions.any { isMcVersionCompatible(instanceVersion, it) }
                    val isLoaderCompatible = selectedType == ContentInstallerType.RESOURCEPACKS ||
                            selectedType == ContentInstallerType.SHADERS ||
                            isLoaderCompatible(instanceLoader, version.loaders)

                    VersionItemView(
                        version = version,
                        isCompatible = isMCCompatible && isLoaderCompatible,
                        isLoaderCompatible = isLoaderCompatible,
                        onClick = { onVersionClick(version) }
                    )
                }
            }
        }
    }
}
