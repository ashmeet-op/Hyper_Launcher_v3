package com.ashmeet.hyperlauncher.screens.instances

import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.layout.ScreenLayout
import com.ashmeet.hyperlauncher.components.list.InstanceListItem

import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.google.gson.Gson
import com.ashmeet.hyperlauncher.activity.PojavApplication
import net.kdt.pojavlaunch.instances.DisplayInstance
import net.kdt.pojavlaunch.instances.Instances
import java.io.File

@Composable
fun InstanceSelectionScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onImportModpack: () -> Unit,
    onEditInstance: (DisplayInstance) -> Unit,
    onRenameInstance: (DisplayInstance, onRefresh: () -> Unit) -> Unit,
    onDeleteInstance: (DisplayInstance, onRefresh: () -> Unit) -> Unit,
    onAddShortcut: (DisplayInstance) -> Unit
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
        }
    )
}

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
    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredInstances = remember(instances, selectedTab) {
        when (selectedTab) {
            1 -> instances.filter { isVanilla(it.versionId) }
            2 -> instances.filter { !isVanilla(it.versionId) }
            else -> instances
        }
    }

    ScreenLayout(
        onBack = onBack,
        onRefresh = onRefresh,
        onCreateNew = onCreateNew,
        onImportModpack = onImportModpack,
        header = {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredInstances.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = translatedText("No instances found"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh")
                    }
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
