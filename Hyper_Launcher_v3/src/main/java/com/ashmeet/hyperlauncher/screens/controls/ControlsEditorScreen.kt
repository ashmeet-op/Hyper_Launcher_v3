package com.ashmeet.hyperlauncher.screens.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.ashmeet.hyperlauncher.components.layout.ActionRow
import com.ashmeet.hyperlauncher.components.layout.LauncherBackground
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog
import com.ashmeet.hyperlauncher.utils.SideDialogUtils
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import kotlinx.coroutines.launch
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface


@Composable
fun ControlsEditorScreen(
    controlLayout: ControlLayout,
    hostViews: Boolean = true,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
) {
    val scope = rememberCoroutineScope()

    var followedButton by remember { mutableStateOf<ControlInterface?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
