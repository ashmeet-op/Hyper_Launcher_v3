package com.ashmeet.hyperlauncher.components.list

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ashmeet.hyperlauncher.components.switch.DefaultSwitch
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.ModMetadataReader
import com.ashmeet.hyperlauncher.utils.WorldMetadataReader
import com.ashmeet.hyperlauncher.utils.drawable.rememberDrawablePainter
import com.ashmeet.hyperlauncher.utils.installer.ModrinthProject
import com.ashmeet.hyperlauncher.utils.installer.ModrinthVersion
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.instances.DisplayInstance
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthService
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileListItem(
    modifier: Modifier = Modifier,
    file: File,
    selectedInstance: Instance?,
    instanceVersion: String?,
    instanceLoader: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onOpenInFiles: () -> Unit,
    onRefresh: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val scope = rememberCoroutineScope()

    var modMeta by remember { mutableStateOf<ModMetadataReader.ModMetadata?>(null) }
    var worldMeta by remember { mutableStateOf<WorldMetadataReader.WorldMetadata?>(null) }

    var updateAvailable by remember { mutableStateOf<ModrinthVersion?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            val mMeta = ModMetadataReader.getMetadata(file)
            val wMeta = WorldMetadataReader.getMetadata(file)
            withContext(Dispatchers.Main) {
                modMeta = mMeta
                worldMeta = wMeta
            }

            if (mMeta != null && mMeta.name != null && mMeta.name!!.lowercase().contains("hyper client")) {
                withContext(Dispatchers.Main) { isCheckingUpdate = true }
                try {
                    val versions = ModrinthService.getProjectVersions("hyperclient")
                    val compatible = versions.filter { v ->
                        instanceVersion != null && v.gameVersions.contains(instanceVersion) &&
                                (instanceLoader == null || v.loaders.any { it.equals(instanceLoader, ignoreCase = true) })
                    }
                    if (compatible.isNotEmpty()) {
                        val latest = compatible.first()
                        val currentVersion = selectedInstance?.hyperClientVersion ?: mMeta.version
                        if (currentVersion == null || latest.name != currentVersion) {
                            withContext(Dispatchers.Main) { updateAvailable = latest }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InstanceDirectory", "Failed to check update", e)
                } finally {
                    withContext(Dispatchers.Main) { isCheckingUpdate = false }
                }
            }
        }
    }

    val animatedAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatedAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                alpha = animatedAlpha.value
            }
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                val iconBitmap = modMeta?.icon ?: worldMeta?.icon
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (file.isDirectory) Icons.Rounded.Folder else Icons.AutoMirrored.Rounded.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modMeta?.name ?: worldMeta?.worldName ?: file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    color = if (file.name.endsWith(".disabled")) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                val subtitle = when {
                    worldMeta != null -> "${worldMeta?.gameMode} • ${dateFormat.format(Date(file.lastModified()))}"
                    file.isDirectory -> "Directory"
                    else -> "${file.length() / 1024} KB • ${dateFormat.format(Date(file.lastModified()))}"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (file.name == "hyper_instance.json" || file.name == "mojo_instance.json" || file.name == "mj_instance.json") {
                val tooltipState = rememberTooltipState()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Critical metadata file") } },
                    state = tooltipState
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = translatedText("Metadata warning"),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(24.dp)
                            .clickable { scope.launch { tooltipState.show() } }
                    )
                }
            }

            if (file.isFile && (file.name.endsWith(".jar") || file.name.endsWith(".jar.disabled"))) {
                val isEnabled = !file.name.endsWith(".disabled")
                val isHyperClient = modMeta?.name?.lowercase()?.contains("hyper client") == true

                if (isHyperClient) {
                    IconButton(
                        onClick = {
                            if (updateAvailable == null) return@IconButton
                            isUpdating = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val url = URL(updateAvailable!!.downloadUrl)
                                    val fileName = updateAvailable!!.downloadUrl.substringAfterLast("/")
                                    val tempFile = File(file.parentFile, fileName + ".tmp")

                                    url.openStream().use { input ->
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                    if (file.exists()) file.delete()
                                    val finalFile = File(file.parentFile, fileName)
                                    tempFile.renameTo(finalFile)

                                    selectedInstance?.let {
                                        it.hyperClientVersion = updateAvailable!!.name
                                        it.maybeWrite()
                                    }

                                    withContext(Dispatchers.Main) {
                                        onRefresh()
                                        updateAvailable = null
                                    }
                                } catch (e: Exception) {
                                    Log.e("InstanceDirectory", "Failed to update mod", e)
                                } finally {
                                    withContext(Dispatchers.Main) { isUpdating = false }
                                }
                            }
                        },
                        enabled = !isUpdating,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (isUpdating) {
                            LoadingIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Update,
                                contentDescription = translatedText("Update available"),
                                tint = if (updateAvailable != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { alpha = if (updateAvailable != null) 1f else 0.3f }
                            )
                        }
                    }
                }

                DefaultSwitch(
                    checked = isEnabled,
                    onCheckedChange = {
                        val newFile = if (isEnabled) {
                            File(file.parentFile, file.name + ".disabled")
                        } else {
                            File(file.parentFile, file.name.removeSuffix(".disabled"))
                        }
                        if (file.renameTo(newFile)) {
                            onRefresh()
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = translatedText("Options"),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = translatedText(stringResource(R.string.global_edit)),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = translatedText("Open in Files"),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenInFiles()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = translatedText(stringResource(R.string.global_delete)),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun FileListItemPreview() {
    PojavTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FileListItem(
                file = File("example_mod.jar"),
                selectedInstance = null,
                instanceVersion = null,
                instanceLoader = null,
                onClick = {},
                onDelete = {},
                onRename = {},
                onOpenInFiles = {},
                onRefresh = {}
            )
        }
    }
}

@Composable
fun InstanceListItem(
    modifier: Modifier = Modifier,
    instance: DisplayInstance,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddShortcut: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(instance) {
        InstanceIconProvider.fetchIcon(context.resources, instance)
    }

    val displayName = if (instance.name.isNullOrBlank()) "UNNAMED" else instance.name
    var menuExpanded by remember { mutableStateOf(value = false) }

    val isInPreview = LocalInspectionMode.current
    val animatedAlpha = remember { Animatable(if (isInPreview) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!isInPreview) {
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                alpha = animatedAlpha.value
            }
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberDrawablePainter(icon),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )
                Text(
                    text = instance.versionId ?: "Unknown Version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = translatedText("Options"),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = translatedText(stringResource(R.string.global_edit)),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Shortcut,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = translatedText("Add Shortcut"),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onAddShortcut()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = translatedText(stringResource(R.string.global_name)),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = translatedText(stringResource(R.string.global_delete)),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InstanceListItemPreview() {
    InstanceListItemPreviewContent(darkTheme = false)
}

@Composable
fun InstanceListItemPreviewContent(darkTheme: Boolean) {
    val dummyInstance = Gson().fromJson(
        """{"name": "Fabric Instance", "versionId": "1.20.1", "icon": "fabric"}""",
        DisplayInstance::class.java
    )
    PojavTheme(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InstanceListItem(
                instance = dummyInstance,
                isSelected = true,
                onClick = {},
                onEdit = {},
                onRename = {},
                onDelete = {},
                onAddShortcut = {}
            )
            InstanceListItem(
                instance = dummyInstance,
                isSelected = false,
                onClick = {},
                onEdit = {},
                onRename = {},
                onDelete = {},
                onAddShortcut = {}
            )
        }
    }
}

@Composable
fun ProjectItemView(
    project: ModrinthProject,
    onClick: () -> Unit
) {
    val animatedAlpha = remember { Animatable(0f) }
    LaunchedEffect(project.id) {
        animatedAlpha.snapTo(0f)
        animatedAlpha.animateTo(1f, animationSpec = tween(400))
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .graphicsLayer { alpha = animatedAlpha.value },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectIcon(project)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = translatedText(project.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )
                Text(
                    text = translatedText(project.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectIcon(project: ModrinthProject, size: Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(project.iconUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                LoadingIndicator(
                    modifier = Modifier.size(size * 0.5f),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            error = {
                Icon(
                    imageVector = Icons.Rounded.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.6f),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        )
    }
}
