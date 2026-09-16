package com.ashmeet.hyperlauncher.screens.instances

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.button.MineButton
import com.ashmeet.hyperlauncher.components.menu.HyperDropdownTextField
import com.ashmeet.hyperlauncher.components.switch.DefaultSwitch
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.drawable.rememberDrawablePainter
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.multirt.Runtime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstanceEditorScreen(
    instanceName: String,
    onInstanceNameChange: (String) -> Unit,
    versionId: String,
    onSelectVersion: () -> Unit,
    controlLayout: String,
    onSelectControl: () -> Unit,
    sharedData: Boolean,
    onSharedDataChange: (Boolean) -> Unit,
    jvmArgs: String,
    onJvmArgsChange: (String) -> Unit,
    selectedRuntime: Runtime?,
    runtimes: List<Runtime>,
    onRuntimeSelected: (Runtime) -> Unit,
    selectedRenderer: String,
    renderers: List<String>,
    rendererDisplayNames: List<String>,
    onRendererSelected: (String) -> Unit,
    instanceIcon: Drawable?,
    onChangeIcon: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            FlexibleBottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                scrollBehavior = scrollBehavior
            ) {
                MineButton(
                    text = translatedText(stringResource(R.string.global_delete)),
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    height = 40.dp,
                    shape = CircleShape
                )

                MineButton(
                    text = translatedText(stringResource(R.string.global_save)),
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    height = 40.dp,
                    shape = CircleShape
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { onChangeIcon() },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Image(
                        painter = rememberDrawablePainter(instanceIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = instanceName,
                    onValueChange = onInstanceNameChange,
                    label = { Text(translatedText(stringResource(R.string.profiles_profile_name))) },
                    placeholder = { Text(translatedText(stringResource(R.string.unnamed))) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = versionId,
                        onValueChange = {},
                        label = { Text(translatedText(stringResource(R.string.profiles_profile_version))) },
                        placeholder = { Text(translatedText(stringResource(R.string.version_select_hint))) },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                             IconButton(onClick = onSelectVersion) {
                                 Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                             }
                        }
                    )
                    MineButton(
                        text = translatedText(stringResource(R.string.global_select)),
                        onClick = onSelectVersion,
                        modifier = Modifier.height(40.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = controlLayout,
                        onValueChange = {},
                        label = { Text(translatedText(stringResource(R.string.default_control))) },
                        placeholder = { Text(translatedText(stringResource(R.string.use_global_default))) },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = onSelectControl) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                    )
                    MineButton(
                        text = translatedText(stringResource(R.string.global_select)),
                        onClick = onSelectControl,
                        modifier = Modifier.height(40.dp)
                    )
                }

                ListItem(
                    headlineContent = { Text(translatedText(stringResource(R.string.instance_shared_data))) },
                    supportingContent = {
                        Text(stringResource(if (sharedData) R.string.instance_shared_data_on else R.string.instance_shared_data_off))
                    },
                    trailingContent = {
                        DefaultSwitch(
                            checked = sharedData,
                            onCheckedChange = onSharedDataChange
                        )
                    },
                    modifier = Modifier.clickable { onSharedDataChange(!sharedData) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                OutlinedTextField(
                    value = jvmArgs,
                    onValueChange = onJvmArgsChange,
                    label = { Text(translatedText(stringResource(R.string.pvc_jvmArgs))) },
                    placeholder = { Text(translatedText(stringResource(R.string.use_global_default))) },
                    modifier = Modifier.fillMaxWidth()
                )

                HyperDropdownTextField(
                    label = translatedText(stringResource(R.string.pedit_java_runtime)),
                    items = runtimes,
                    selectedItem = selectedRuntime,
                    itemLabel = {
                        if (runtimes.indexOf(it) == runtimes.size - 1) it.name
                        else "${it.name.replace(".tar.xz", "")} - ${it.versionString ?: translatedText(stringResource(R.string.multirt_runtime_corrupt))}"
                    },
                    onItemSelected = onRuntimeSelected
                )

                HyperDropdownTextField(
                    label = translatedText(stringResource(R.string.pedit_renderer)),
                    items = renderers,
                    selectedItem = selectedRenderer,
                    itemLabel = {
                        val index = renderers.indexOf(it)
                        if (index != -1 && index < rendererDisplayNames.size) rendererDisplayNames[index]
                        else it
                    },
                    onItemSelected = onRendererSelected
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
