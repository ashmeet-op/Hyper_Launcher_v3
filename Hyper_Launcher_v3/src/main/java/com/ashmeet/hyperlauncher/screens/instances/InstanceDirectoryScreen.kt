package com.ashmeet.hyperlauncher.screens.instances

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.components.HyperSearchBar
import com.ashmeet.hyperlauncher.components.layout.ScreenLayout
import com.ashmeet.hyperlauncher.components.list.FileListItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.TextInputDialog
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun InstanceDirectoryScreen(
    onBack: () -> Unit,
) {
    val isPreview = LocalInspectionMode.current
    val selectedInstance = remember {
        if (isPreview) null else Instances.loadSelectedInstance()
    }
    val instanceRoot = remember(selectedInstance) {
        selectedInstance?.gameDirectory
    }

    InstanceDirectoryContent(
        instanceRoot = instanceRoot,
        selectedInstance = selectedInstance,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstanceDirectoryContent(
    instanceRoot: File?,
    selectedInstance: net.kdt.pojavlaunch.instances.Instance?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentDir by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    val searchTextFieldState = rememberTextFieldState(searchQuery)
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(searchTextFieldState.text) {
        searchQuery = searchTextFieldState.text.toString()
    }

    val instanceVersion = remember(selectedInstance) {
        selectedInstance?.let {
            if (it.versionId == "latest_release" || it.versionId == "latest_snapshot") {
                return@let null
            }

            val v = try {
                Tools.getVersionInfo(it.versionId)
            } catch (_: Exception) {
                null
            }
            if (v != null && v.inheritsFrom != null) return@let v.inheritsFrom

            val id = it.versionId
            if (id.contains("-")) {
                val lastPart = id.substringAfterLast("-")
                if (lastPart.contains(".") && lastPart.any { char -> char.isDigit() }) {
                    return@let lastPart
                }
            }

            val regex = Regex("""1\.\d+(\.\d+)*(?:-?[a-zA-Z\d]+)?|\d+w\d+[a-z]""")
            regex.findAll(id).lastOrNull()?.value ?: id
        }
    }

    val instanceLoader = remember(selectedInstance) {
        selectedInstance?.let {
            val vId = it.versionId.lowercase()
            when {
                vId.contains("fabric") -> "fabric"
                vId.contains("forge") -> "forge"
                vId.contains("quilt") -> "quilt"
                vId.contains("neoforge") -> "neoforge"
                vId.contains("optifine") -> "optifine"
                else -> null
            }
        }
    }

    val loadFiles = { dir: File ->
        isLoading = true
        PojavApplication.sExecutorService.execute {
            try {
                val list = dir.listFiles()?.asSequence()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
                files = list
                currentDir = dir
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = Tools.getFileName(context, uri)
            if (fileName != null) {
                currentDir?.let { destDir ->
                    val destFile = File(destDir, fileName)
                    PojavApplication.sExecutorService.execute {
                        try {
                            val progressKey = "copy_files"
                            ProgressKeeper.submitProgress(progressKey, 0, -1, "Importing $fileName...")

                            context.contentResolver.openInputStream(uri)?.use { input ->
                                destFile.outputStream().use { output ->
                                    val totalSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                                    var bytesCopied = 0L
                                    val buffer = ByteArray(8192)
                                    var bytes = input.read(buffer)
                                    while (bytes >= 0) {
                                        output.write(buffer, 0, bytes)
                                        bytesCopied += bytes
                                        if (totalSize > 0) {
                                            val progress = ((bytesCopied * 100) / totalSize).toInt()
                                            ProgressKeeper.submitProgress(progressKey, progress, -1, "Importing $fileName...")
                                        }
                                        bytes = input.read(buffer)
                                    }
                                }
                            }

                            ProgressKeeper.submitProgress(progressKey, -1, -1)
                            loadFiles(destDir)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ProgressKeeper.submitProgress("copy_files", -1, -1)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedTab) {
        val root = instanceRoot ?: return@LaunchedEffect
        when (selectedTab) {
            0 -> loadFiles(root)
            1 -> loadFiles(File(root, "mods"))
            2 -> loadFiles(File(root, "saves"))
            3 -> loadFiles(File(root, "resourcepacks"))
        }
        isSearchActive = false
        searchTextFieldState.edit { replace(0, length, "") }
    }

    if (showNewFolderDialog) {
        TextInputDialog(
            title = translatedText("New Folder"),
            initialValue = "",
            onConfirm = { name ->
                showNewFolderDialog = false
                if (name.isNotBlank()) {
                    currentDir?.let {
                        val newDir = File(it, name)
                        if (newDir.mkdirs()) {
                            loadFiles(it)
                        }
                    }
                }
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    if (fileToRename != null) {
        TextInputDialog(
            title = translatedText("Rename"),
            initialValue = fileToRename?.name ?: "",
            onConfirm = { newName ->
                val target = fileToRename
                fileToRename = null
                if (newName.isNotBlank() && target != null) {
                    val dest = File(target.parentFile, newName)
                    if (target.renameTo(dest)) {
                        currentDir?.let { loadFiles(it) }
                    }
                }
            },
            onDismiss = { fileToRename = null }
        )
    }

    if (fileToDelete != null) {
        val target = fileToDelete!!
        HyperAlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(translatedText("Delete ${if (target.isDirectory) "Folder" else "File"}?")) },
            text = { Text(translatedText("Are you sure you want to delete \"${target.name}\"? This action cannot be undone.")) },
            confirmText = "Delete",
            onConfirm = {
                fileToDelete = null
                PojavApplication.sExecutorService.execute {
                    if (target.isDirectory) {
                        try {
                            FileUtils.deleteDirectory(target)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        target.delete()
                    }
                    currentDir?.let { loadFiles(it) }
                }
            },
            dismissText = "Cancel",
            onDismiss = { fileToDelete = null },
            isDestructive = true
        )
    }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    ScreenLayout(
        onBack = {
            if (isSearchActive) {
                isSearchActive = false
                searchTextFieldState.edit { replace(0, length, "") }
            } else {
                val parent = currentDir?.parentFile
                if (currentDir == instanceRoot || parent == null || instanceRoot == null) {
                    onBack()
                } else if (currentDir?.absolutePath?.startsWith(instanceRoot.absolutePath) == true) {
                    loadFiles(parent)
                } else {
                    onBack()
                }
            }
        },
        onRefresh = { currentDir?.let { loadFiles(it) } },
        onImportModpack = {
            isSearchActive = !isSearchActive
            if (!isSearchActive) {
                searchTextFieldState.edit { replace(0, length, "") }
            }
        },
        isSearchActive = isSearchActive,
        fabMenuContent = { onDismiss ->
            FloatingActionButtonMenuItem(
                onClick = {
                    onDismiss()
                    showNewFolderDialog = true
                },
                icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                text = { Text(text = translatedText("New Folder")) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    onDismiss()
                    importFileLauncher.launch("*/*")
                },
                icon = { Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, contentDescription = null) },
                text = { Text(text = translatedText("Import File")) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        header = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    (scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialScale = 0.9f
                    ) + fadeIn()) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "search_transition"
            ) { active ->
                if (active) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(200.milliseconds)
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .semantics { isTraversalGroup = true }
                    ) {
                        HyperSearchBar(
                            state = searchTextFieldState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .semantics { traversalIndex = 0f }
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            label = "Search files...",
                            focusRequester = focusRequester,
                            interactionSource = interactionSource,
                            onSearchAction = {
                                isSearchActive = false
                            }
                        )
                    }
                } else {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = @Composable {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier
                                    .tabIndicatorOffset(selectedTab)
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                height = 4.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        val tabs = listOf("All", "Mods", "Saves", "Packs")
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                interactionSource = remember { MutableInteractionSource() },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        AnimatedContent(
            targetState = isLoading to currentDir,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "content_transition",
            modifier = Modifier.weight(1f)
        ) { (loading, _) ->
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (filteredFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No files found for \"$searchQuery\"" else "This folder is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val lazyListState = rememberLazyListState()
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredFiles,
                        key = { it.absolutePath }
                    ) { file ->
                        FileListItem(
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(300),
                                fadeOutSpec = tween(300),
                                placementSpec = tween(300)
                            ),
                            file = file,
                            selectedInstance = selectedInstance,
                            instanceVersion = instanceVersion,
                            instanceLoader = instanceLoader,
                            onClick = {
                                if (file.isDirectory) {
                                    loadFiles(file)
                                    searchTextFieldState.edit { replace(0, length, "") }
                                    isSearchActive = false
                                } else {
                                    Tools.openPath(context, file, false)
                                }
                            },
                            onDelete = { fileToDelete = file },
                            onRename = { fileToRename = file },
                            onOpenInFiles = {
                                Tools.openPath(context, file, false)
                            },
                            onRefresh = { currentDir?.let { loadFiles(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun InstanceDirectoryScreenPreview() {
    PojavTheme {
        InstanceDirectoryContent(
            instanceRoot = null,
            selectedInstance = null,
            onBack = {}
        )
    }
}

