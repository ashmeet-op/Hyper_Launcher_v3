package com.ashmeet.hyperlauncher.screens.instances

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.ashmeet.hyperlauncher.components.button.MineButton
import com.ashmeet.hyperlauncher.components.layout.ScreenLayout
import com.ashmeet.hyperlauncher.components.list.InstanceListItem
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.google.gson.Gson
import net.kdt.pojavlaunch.instances.DisplayInstance
import net.kdt.pojavlaunch.instances.Instances
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstanceSelectionScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onImportModpack: () -> Unit,
    onEditInstance: (DisplayInstance) -> Unit,
    onRenameInstance: (DisplayInstance, onRefresh: () -> Unit) -> Unit,
    onDeleteInstance: (DisplayInstance, onRefresh: () -> Unit) -> Unit,
    onAddShortcut: (DisplayInstance) -> Unit,
) {
    var instances by remember { mutableStateOf<List<DisplayInstance>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val loadInstances = {
        isLoading = true
        PojavApplication.sExecutorService.execute {
            try {
                val loaded = Instances.loadDisplay()
                instances = loaded.list
                selectedIndex = loaded.selectedIndex
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    LaunchedEffect(refreshKey) {
        loadInstances()
    }

    InstanceSelectionContent(
        instances = instances,
        selectedIndex = selectedIndex,
        isLoading = isLoading,
        onRefresh = { loadInstances() },
        onBack = onBack,
        onCreateNew = onCreateNew,
        onImportModpack = onImportModpack,
        onEditInstance = onEditInstance,
        onRenameInstance = { instance -> onRenameInstance(instance) { refreshKey++ } },
        onDeleteInstance = { instance -> onDeleteInstance(instance) { refreshKey++ } },
        onAddShortcut = onAddShortcut,
        onSelectInstance = { instance, index ->
            Instances.setSelectedInstance(instance)
            selectedIndex = index
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun InstanceSelectionContent(
    instances: List<DisplayInstance>,
    selectedIndex: Int,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onImportModpack: () -> Unit,
    onEditInstance: (DisplayInstance) -> Unit,
    onRenameInstance: (DisplayInstance) -> Unit,
    onDeleteInstance: (DisplayInstance) -> Unit,
    onAddShortcut: (DisplayInstance) -> Unit,
    onSelectInstance: (DisplayInstance, Int) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            focusManager.clearFocus()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val searchTextFieldState = rememberTextFieldState(searchQuery)
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(searchTextFieldState.text) {
        searchQuery = searchTextFieldState.text.toString()
    }

    val filteredInstances = remember(instances, selectedTab, searchQuery) {
        val base = when (selectedTab) {
            1 -> instances.filter { isVanilla(it.versionId) }
            2 -> instances.filter { !isVanilla(it.versionId) }
            else -> instances
        }
        if (searchQuery.isBlank()) base
        else base.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    ScreenLayout(
        onBack = {
            if (isSearchActive) {
                isSearchActive = false
                searchTextFieldState.edit { replace(0, length, "") }
            } else {
                onBack()
            }
        },
        onRefresh = onRefresh,
        isSearchActive = isSearchActive,
        onImportModpack = {
            isSearchActive = !isSearchActive
            if (!isSearchActive) {
                searchTextFieldState.edit { replace(0, length, "") }
            }
        },
        fabMenuContent = { onDismiss ->
            FloatingActionButtonMenuItem(
                onClick = {
                    onDismiss()
                    onCreateNew()
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(text = translatedText("Create New")) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    onDismiss()
                    onImportModpack()
                },
                icon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                text = { Text(text = translatedText("Import Modpack")) },
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
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            state = searchTextFieldState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            label = { Text("Search instances...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            interactionSource = interactionSource,
                            shape = SearchBarDefaults.inputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            onKeyboardAction = {
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
                        val tabs = listOf("All", "Vanilla", "Modded")
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                interactionSource = remember { MutableInteractionSource() },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else if (filteredInstances.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = translatedText("instance not found"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    MineButton(
                        text = translatedText("Create"),
                        onClick = onCreateNew,
                        icon = rememberVectorPainter(Icons.Rounded.Add),
                        tintIcon = true
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
                    items = filteredInstances,
                    key = { it.mInstanceRoot.absolutePath }
                ) { instance ->
                    val actualIndex = instances.indexOf(instance)
                    val isSelected = actualIndex == selectedIndex

                    InstanceListItem(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = tween(300),
                                fadeOutSpec = tween(300),
                                placementSpec = tween(300)
                            ),
                        instance = instance,
                        isSelected = isSelected,
                        onClick = {
                            onSelectInstance(instance, actualIndex)
                        },
                        onEdit = { onEditInstance(instance) },
                        onRename = { onRenameInstance(instance) },
                        onDelete = { onDeleteInstance(instance) },
                        onAddShortcut = { onAddShortcut(instance) }
                    )
                }
            }
        }
    }
}

private fun isVanilla(versionId: String?): Boolean {
    if (versionId == null) return true
    val lower = versionId.lowercase()
    return !lower.contains("fabric") &&
           !lower.contains("forge") &&
           !lower.contains("quilt") &&
           !lower.contains("optifine") &&
           !lower.contains("neoforge") &&
           !lower.contains("bta")
}

@Preview(showBackground = true, device = "spec:width=800dp,height=400dp,orientation=landscape")
@Composable
fun InstanceSelectionScreenPreview() {
    val gson = Gson()
    val instances = listOf(
        gson.fromJson("""{"name": "1.20.1 Vanilla", "versionId": "1.20.1", "icon": "default"}""", DisplayInstance::class.java),
        gson.fromJson("""{"name": "Fabric Modpack", "versionId": "1.19.2-fabric", "icon": "fabric"}""", DisplayInstance::class.java),
        gson.fromJson("""{"name": "Forge World", "versionId": "1.16.5-forge", "icon": "forge"}""", DisplayInstance::class.java)
    ).onEach { it.mInstanceRoot = File("/tmp/${it.name}") }

    PojavTheme {
        InstanceSelectionContent(
            instances = instances,
            selectedIndex = 0,
            isLoading = false,
            onRefresh = {},
            onBack = {},
            onCreateNew = {},
            onImportModpack = {},
            onEditInstance = {},
            onRenameInstance = {},
            onDeleteInstance = {},
            onAddShortcut = {},
            onSelectInstance = { _, _ -> }
        )
    }
}
