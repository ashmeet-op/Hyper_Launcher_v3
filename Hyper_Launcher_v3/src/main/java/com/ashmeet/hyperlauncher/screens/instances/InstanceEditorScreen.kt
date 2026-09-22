package com.ashmeet.hyperlauncher.screens.instances

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SingleChoiceDialog
import com.ashmeet.hyperlauncher.screens.settings.preferences.TextInputDialog
import com.ashmeet.hyperlauncher.utils.drawable.rememberDrawablePainter
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.multirt.Runtime

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
    hasChanges: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showJvmArgsDialog by remember { mutableStateOf(false) }
    var showRuntimeDialog by remember { mutableStateOf(false) }
    var showRendererDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScreenWrapper(
            title = translatedText("Profile Editor"),
            onBack = onBack,
            addTopGap = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsCard(position = CardPosition.TOP, useSurface = true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberDrawablePainter(instanceIcon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = translatedText("Current Icon"),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = translatedText("Tap below to change"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("Change Icon"),
                        summary = translatedText("Choose a new image for this profile"),
                        icon = Icons.Rounded.AddPhotoAlternate,
                        onClick = onChangeIcon
                    )
                }
                SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText(stringResource(R.string.profiles_profile_name)),
                        summary = instanceName.ifEmpty { translatedText(stringResource(R.string.unnamed)) },
                        icon = Icons.Rounded.Title,
                        onClick = { showNameDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsCard(position = CardPosition.TOP, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText(stringResource(R.string.profiles_profile_version)),
                        summary = versionId.ifEmpty { translatedText(stringResource(R.string.version_select_hint)) },
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = onSelectVersion
                    )
                }
                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText(stringResource(R.string.default_control)),
                        summary = controlLayout.ifEmpty { translatedText(stringResource(R.string.use_global_default)) },
                        icon = Icons.Rounded.Settings,
                        onClick = onSelectControl
                    )
                }
                SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                    SettingsSwitchItem(
                        title = translatedText(stringResource(R.string.instance_shared_data)),
                        summary = stringResource(if (sharedData) R.string.instance_shared_data_on else R.string.instance_shared_data_off),
                        checked = sharedData,
                        onCheckedChange = onSharedDataChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsCard(position = CardPosition.TOP, useSurface = true) {
                    val corruptText = translatedText(stringResource(R.string.multirt_runtime_corrupt))
                    val currentSummary = selectedRuntime?.let { 
                        if (it.name == "auto") {
                            translatedText(stringResource(R.string.multirt_auto))
                        } else if (it.name == "<Default>") {
                            it.name
                        } else {
                            "${it.name.replace(".tar.xz", "")} - ${it.versionString ?: corruptText}"
                        }
                    } ?: translatedText(stringResource(R.string.global_default))

                    SettingsActionItem(
                        title = translatedText(stringResource(R.string.pedit_java_runtime)),
                        summary = currentSummary,
                        icon = Icons.Rounded.Memory,
                        onClick = { showRuntimeDialog = true }
                    )
                }
                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    val index = renderers.indexOf(selectedRenderer)
                    val currentSummary = if (index != -1 && index < rendererDisplayNames.size) rendererDisplayNames[index] else selectedRenderer

                    SettingsActionItem(
                        title = translatedText(stringResource(R.string.pedit_renderer)),
                        summary = currentSummary,
                        icon = Icons.Rounded.Settings,
                        onClick = { showRendererDialog = true }
                    )
                }
                SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText(stringResource(R.string.pvc_jvmArgs)),
                        summary = jvmArgs.ifEmpty { translatedText(stringResource(R.string.use_global_default)) },
                        icon = Icons.Rounded.Terminal,
                        onClick = { showJvmArgsDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.global_delete)),
                    icon = Icons.Rounded.Delete,
                    tintIcon = true,
                    onClick = onDelete
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        AnimatedVisibility(
            visible = hasChanges,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FloatingActionButton(
                onClick = onSave,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = translatedText(stringResource(R.string.global_save)))
            }
        }

        if (showNameDialog) {
            TextInputDialog(
                title = translatedText(stringResource(R.string.profiles_profile_name)),
                initialValue = instanceName,
                onConfirm = {
                    onInstanceNameChange(it)
                    showNameDialog = false
                },
                onDismiss = { showNameDialog = false }
            )
        }

        if (showJvmArgsDialog) {
            TextInputDialog(
                title = translatedText(stringResource(R.string.pvc_jvmArgs)),
                initialValue = jvmArgs,
                onConfirm = {
                    onJvmArgsChange(it)
                    showJvmArgsDialog = false
                },
                onDismiss = { showJvmArgsDialog = false }
            )
        }

        if (showRuntimeDialog) {
            val corruptText = translatedText(stringResource(R.string.multirt_runtime_corrupt))
            val runtimeOptions = runtimes.map { 
                if (it.name == "auto") {
                    translatedText(stringResource(R.string.multirt_auto))
                } else if (it.name == "<Default>") {
                    it.name
                } else {
                    "${it.name.replace(".tar.xz", "")} - ${it.versionString ?: corruptText}"
                }
            }
            SingleChoiceDialog(
                title = translatedText(stringResource(R.string.pedit_java_runtime)),
                options = runtimeOptions,
                optionValues = runtimes.map { it.name },
                selectedValue = selectedRuntime?.name ?: "",
                onValueChange = { name ->
                    runtimes.find { it.name == name }?.let { onRuntimeSelected(it) }
                    showRuntimeDialog = false
                },
                onDismiss = { showRuntimeDialog = false }
            )
        }

        if (showRendererDialog) {
            SingleChoiceDialog(
                title = translatedText(stringResource(R.string.pedit_renderer)),
                options = rendererDisplayNames,
                optionValues = renderers,
                selectedValue = selectedRenderer,
                onValueChange = {
                    onRendererSelected(it)
                    showRendererDialog = false
                },
                onDismiss = { showRendererDialog = false }
            )
        }

    }
}
