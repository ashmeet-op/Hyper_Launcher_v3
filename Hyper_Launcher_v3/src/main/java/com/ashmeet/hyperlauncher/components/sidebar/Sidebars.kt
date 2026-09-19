package com.ashmeet.hyperlauncher.components.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ashmeet.hyperlauncher.components.list.ProjectIcon
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SingleChoiceDialog
import com.ashmeet.hyperlauncher.utils.installer.ContentInstallerType
import com.ashmeet.hyperlauncher.utils.installer.ContentSource
import com.ashmeet.hyperlauncher.utils.installer.ModrinthProject
import com.ashmeet.hyperlauncher.utils.installer.ModrinthVersion
import com.ashmeet.hyperlauncher.utils.installer.cleanMcVersion
import com.ashmeet.hyperlauncher.utils.installer.cleanLoaderName
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.ashmeet.hyperlauncher.profiles.VersionSelectorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsSidebar(
    project: ModrinthProject,
    versions: List<ModrinthVersion> = emptyList()
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        ProjectIcon(project, size = 100.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = translatedText(project.title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = translatedText(project.description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            if (project.gallery.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = translatedText("Images"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    HorizontalMultiBrowseCarousel(
                        state = rememberCarouselState { project.gallery.size },
                        preferredItemWidth = 240.dp,
                        itemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    ) { index ->
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(project.gallery[index])
                                .crossfade(true)
                                .build(),
                            contentDescription = translatedText("Gallery image $index"),
                            modifier = Modifier
                                .fillMaxSize()
                                .maskClip(MaterialTheme.shapes.extraLarge),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Version Info Section
            if (versions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = translatedText("Version Information"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                    Column {
                        val latestVersion = versions.firstOrNull()?.name ?: "Unknown"
                        
                        SettingsActionItem(
                            title = translatedText("Latest Version"),
                            summary = latestVersion,
                            icon = Icons.Rounded.Info,
                            onClick = {}
                        )
                    }
                }
            }

            // Links Section
            val links = remember(project) {
                listOfNotNull(
                    project.websiteUrl?.let { "Website" to it to Icons.Rounded.Language },
                    project.wikiUrl?.let { "Wiki" to it to Icons.AutoMirrored.Rounded.MenuBook },
                    project.sourceUrl?.let { "Source Code" to it to Icons.Rounded.Code },
                    project.issuesUrl?.let { "Issues" to it to Icons.Rounded.BugReport },
                    project.discordUrl?.let { "Discord" to it to Icons.Rounded.Description }
                )
            }

            if (links.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = translatedText("Links"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                    Column {
                        links.forEach { (data, icon) ->
                            val (label, url) = data
                            SettingsActionItem(
                                title = translatedText(label),
                                summary = url,
                                icon = icon,
                                onClick = {
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SearchFiltersSidebar(
    instanceVersion: String?,
    instanceLoader: String?,
    selectedVersion: String?,
    selectedLoader: String?,
    selectedSource: ContentSource = ContentSource.MODRINTH,
    showLoaderFilter: Boolean = true,
    selectedType: ContentInstallerType = ContentInstallerType.MODS,
    onVersionChange: (String?) -> Unit,
    onLoaderChange: (String?) -> Unit,
    onSourceChange: (ContentSource) -> Unit,
    onImportContent: (ContentInstallerType) -> Unit
) {
    val context = LocalContext.current
    var showLoaderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsCard(
                position = CardPosition.TOP,
                useSurface = true
            ) {
                FilterSourceItem(
                    currentSource = selectedSource,
                    onSourceChange = onSourceChange
                )
            }

            SettingsCard(
                position = if (showLoaderFilter) CardPosition.MIDDLE else CardPosition.BOTTOM,
                useSurface = true
            ) {
                val displayVersion = remember(selectedVersion, instanceVersion) {
                    val v = selectedVersion ?: instanceVersion
                    if (v == null || v == "Any") "Any" else cleanMcVersion(v)
                }
                SettingsActionItem(
                    title = translatedText("Game Version"),
                    summary = displayVersion,
                    icon = Icons.Rounded.Event,
                    onClick = {
                        VersionSelectorDialog.open(context, true) { id, _ ->
                            onVersionChange(id)
                        }
                    }
                )
            }

            if (showLoaderFilter) {
                SettingsCard(
                    position = CardPosition.BOTTOM,
                    useSurface = true
                ) {
                    val displayLoader = remember(selectedLoader, instanceLoader) {
                        val l = selectedLoader ?: instanceLoader
                        if (l == null || l == "any") "Any" else cleanLoaderName(l)
                    }
                    SettingsActionItem(
                        title = translatedText("Loader"),
                        summary = displayLoader,
                        icon = Icons.Rounded.Settings,
                        onClick = { showLoaderDialog = true }
                    )
                }
            }
        }

        if (selectedType == ContentInstallerType.MODPACKS) {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsCard(
                    position = CardPosition.SINGLE,
                    useSurface = true
                ) {
                    SettingsActionItem(
                        title = translatedText("Import Modpack"),
                        summary = translatedText("Install a local modpack file (.zip, .mrpack)"),
                        icon = Icons.Rounded.FileUpload,
                        onClick = { onImportContent(ContentInstallerType.MODPACKS) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showLoaderDialog) {
        val loaders = listOf("Any", "Fabric", "Forge", "Quilt", "NeoForge")
        val loaderValues = listOf(null, "fabric", "forge", "quilt", "neoforge")
        SingleChoiceDialog(
            title = translatedText("Select Loader"),
            options = loaders,
            optionValues = loaderValues.map { it ?: "any" },
            selectedValue = selectedLoader ?: "any",
            onValueChange = { value ->
                onLoaderChange(if (value == "any") null else value)
                showLoaderDialog = false
            },
            onDismiss = { showLoaderDialog = false }
        )
    }
}

@Composable
fun FilterSourceItem(
    currentSource: ContentSource,
    onSourceChange: (ContentSource) -> Unit
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) {
        AlertDialog(
            onDismissRequest = { isShowingDialog = false },
            title = { Text(translatedText("Select Source")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContentSource.entries.forEach { source ->
                        Surface(
                            onClick = {
                                onSourceChange(source)
                                isShowingDialog = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (currentSource == source)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            tonalElevation = if (currentSource == source) 4.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (source == ContentSource.MODRINTH) Icons.Rounded.Language else Icons.Rounded.Extension,
                                    contentDescription = null,
                                    tint = if (currentSource == source)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = source.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (currentSource == source) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentSource == source)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentSource == source) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    SettingsActionItem(
        title = translatedText("Source"),
        summary = currentSource.displayName,
        icon = Icons.Rounded.Language,
        warningTooltip = if (currentSource == ContentSource.CURSEFORGE) "CurseForge support is experimental and may be unstable." else null,
        onClick = { isShowingDialog = true }
    )
}
