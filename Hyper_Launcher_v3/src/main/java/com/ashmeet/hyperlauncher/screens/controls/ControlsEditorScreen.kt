package com.ashmeet.hyperlauncher.screens.controls

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ashmeet.hyperlauncher.components.layout.ActionRow
import com.ashmeet.hyperlauncher.components.layout.LauncherBackground
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.components.HyperOutlinedTextField
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.SideDialogUtils
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.kdt.pickafile.FileListView
import com.kdt.pickafile.FileSelectedListener
import kotlinx.coroutines.launch
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import java.io.File


@Composable
fun ControlsEditorScreen(
    controlLayout: ControlLayout,
    hostViews: Boolean = true,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    activeAction: Int? = null,
    onActionConsumed: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var followedButton by remember { mutableStateOf<ControlInterface?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSetDefaultDialog by remember { mutableStateOf(false) }
    var saveLayoutName by remember { mutableStateOf(controlLayout.mLayoutFileName ?: "") }

    LaunchedEffect(activeAction) {
        when (activeAction) {
            3 -> {
                showLoadDialog = true
                onActionConsumed()
            }
            4 -> {
                saveLayoutName = controlLayout.mLayoutFileName ?: ""
                showSaveDialog = true
                onActionConsumed()
            }
            5 -> {
                showSetDefaultDialog = true
                onActionConsumed()
            }
        }
    }

    val editDialog = remember(controlLayout) {
        EditControlSideDialog()
    }

    LaunchedEffect(controlLayout) {
        controlLayout.setOnControlEditListener(object : ControlLayout.OnControlEditListener {
            override fun onEditControl(button: ControlInterface) {
                followedButton = button
                editDialog.setCurrentlyEditedButton(button)
                SideDialogUtils.show(editDialog, button.controlView.x + button.controlView.width / 2f < controlLayout.width / 2f)
            }

            override fun onDisappearLayer(): Boolean {
                followedButton = null
                return editDialog.disappearLayer()
            }
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hostViews) {
            LauncherBackground()
            AndroidView(
                factory = { controlLayout },
                modifier = Modifier.fillMaxSize()
            )
        }

        ActionRow(
            followedButton = followedButton,
            onDelete = {
                showDeleteConfirm = true
            },
            onClone = {
                followedButton?.cloneButton()
                controlLayout.removeEditWindow()
                followedButton = null
            },
            onAddSub = {
                if (followedButton is ControlDrawer) {
                    controlLayout.addSubButton(followedButton as ControlDrawer, ControlData())
                }
            }
        )

        if (showDeleteConfirm) {
            HyperAlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(text = translatedText(stringResource(R.string.global_delete))) },
                text = { Text(text = translatedText("Are you sure you want to delete this button?")) },
                confirmText = stringResource(R.string.global_delete),
                onConfirm = {
                    followedButton?.removeButton()
                    followedButton = null
                    showDeleteConfirm = false
                },
                dismissText = stringResource(android.R.string.cancel),
                onDismiss = { showDeleteConfirm = false },
                isDestructive = true
            )
        }

        if (showLoadDialog) {
            HyperAlertDialog(
                onDismissRequest = { showLoadDialog = false },
                title = { Text(text = translatedText(stringResource(R.string.global_load))) },
                text = {
                    AndroidView(
                        factory = { ctx ->
                            FileListView(ctx, null, arrayOf("json")).apply {
                                if (Build.VERSION.SDK_INT < 29) {
                                    listFileAt(File(Tools.CTRLMAP_PATH))
                                } else {
                                    lockPathAt(File(Tools.CTRLMAP_PATH))
                                }
                                setFileSelectedListener(object : FileSelectedListener() {
                                    override fun onFileSelected(file: File, path: String) {
                                        try {
                                            controlLayout.loadLayout(path)
                                        } catch (e: Exception) {
                                            Tools.showError(ctx, e)
                                        }
                                        showLoadDialog = false
                                    }
                                })
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    )
                },
                dismissText = stringResource(android.R.string.cancel),
                onDismiss = { showLoadDialog = false }
            )
        }

        if (showSaveDialog) {
            HyperAlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text(text = translatedText(stringResource(R.string.global_save))) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HyperOutlinedTextField(
                            value = saveLayoutName,
                            onValueChange = { saveLayoutName = it },
                            label = { Text(translatedText("Layout Name")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmText = stringResource(R.string.global_save),
                onConfirm = {
                    if (saveLayoutName.isBlank()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            val jsonPath = controlLayout.saveToDirectory(saveLayoutName)
                            Toast.makeText(context, "${context.getString(R.string.global_save)}: $jsonPath", Toast.LENGTH_SHORT).show()
                            showSaveDialog = false
                        } catch (e: Throwable) {
                            Tools.showError(context, e, false)
                        }
                    }
                },
                dismissText = stringResource(android.R.string.cancel),
                onDismiss = { showSaveDialog = false }
            )
        }

        if (showSetDefaultDialog) {
            HyperAlertDialog(
                onDismissRequest = { showSetDefaultDialog = false },
                title = { Text(text = translatedText(stringResource(R.string.customctrl_selectdefault))) },
                text = {
                    AndroidView(
                        factory = { ctx ->
                            FileListView(ctx, null, arrayOf("json")).apply {
                                lockPathAt(File(Tools.CTRLMAP_PATH))
                                setFileSelectedListener(object : FileSelectedListener() {
                                    override fun onFileSelected(file: File, path: String) {
                                        try {
                                            LauncherPreferences.DEFAULT_PREF?.edit()?.putString("defaultCtrl", path)?.apply()
                                            LauncherPreferences.PREF_DEFAULTCTRL_PATH = path
                                            controlLayout.loadLayout(path)
                                            Toast.makeText(ctx, "Default layout set to ${file.name}", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Tools.showError(ctx, e)
                                        }
                                        showSetDefaultDialog = false
                                    }
                                })
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    )
                },
                dismissText = stringResource(android.R.string.cancel),
                onDismiss = { showSetDefaultDialog = false }
            )
        }

        if (drawerState.targetValue != DrawerValue.Closed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.01f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch { drawerState.close() }
                    }
            )
        }
    }
}
