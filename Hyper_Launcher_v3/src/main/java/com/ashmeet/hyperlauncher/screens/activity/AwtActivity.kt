package com.ashmeet.hyperlauncher.screens.activity

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ashmeet.hyperlauncher.screens.game.LoggerView
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperbackend.awt.AWTView
import com.ashmeet.hyperbackend.customcontrols.keyboard.TouchCharInput


@Composable
fun LegacyControlButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onPress: ((Boolean) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .then(
                if (onPress != null) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> onPress(true)
                                    PointerEventType.Release, PointerEventType.Exit -> onPress(false)
                                }
                            }
                        }
                    }
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun JavaGuiLauncherScreen(
    onForceClose: () -> Unit,
    onOpenLogOutput: () -> Unit,
    onToggleVirtualMouse: (Boolean) -> Unit,
    onToggleKeyboard: () -> Unit,
    onPerformCopy: () -> Unit,
    onPerformPaste: () -> Unit,
    onMouseEvent: (Int, Boolean) -> Unit,
    onMoveWindow: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    isMouseEnabled: Boolean = false,
    isLoggerVisible: Boolean = false,
    onAwtViewTouch: (View, MotionEvent) -> Boolean = { _, _ -> false },
    onTouchpadTouch: (View, MotionEvent) -> Boolean = { _, _ -> false },
    mousePosition: Offset = Offset.Zero
) {
    val mouseScale = LauncherPreferences.PREF_MOUSESCALE

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    AWTView(context).apply {
                        id = R.id.installmod_surfaceview
                        setOnTouchListener { v, event -> onAwtViewTouch(v, event) }
                    }
                },
                modifier = Modifier.aspectRatio(1024f / 768f)
            )
        }

        if (isMouseEnabled) {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        id = R.id.main_touchpad
                        setOnTouchListener { v, event -> onTouchpadTouch(v, event) }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_mouse_pointer_arrow),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(mousePosition.x.dp, mousePosition.y.dp)
                        .size(
                            (18 * mouseScale).dp,
                            (27 * mouseScale).dp
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegacyControlButton(
                text = stringResource(id = R.string.control_forceclose),
                onClick = onForceClose
            )
            LegacyControlButton(
                text = stringResource(id = R.string.control_viewout),
                onClick = onOpenLogOutput
            )
            LegacyControlButton(
                text = stringResource(id = R.string.control_mouse),
                onClick = {
                    onToggleVirtualMouse(!isMouseEnabled)
                }
            )
        }


        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegacyControlButton(
                text = "Keyboard",
                onClick = onToggleKeyboard
            )
            LegacyControlButton(
                text = "Copy",
                onClick = onPerformCopy
            )
            LegacyControlButton(
                text = "Paste",
                onClick = onPerformPaste
            )
        }


        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegacyControlButton(
                text = stringResource(id = R.string.control_primary),
                onPress = { isDown -> onMouseEvent(MotionEvent.BUTTON_PRIMARY, isDown) }
            )
            LegacyControlButton(
                text = stringResource(id = R.string.control_secondary),
                onPress = { isDown -> onMouseEvent(MotionEvent.BUTTON_SECONDARY, isDown) }
            )
        }


        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegacyControlButton(
                    text = "▲",
                    onPress = { isDown -> if (isDown) onMoveWindow(0, -10) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegacyControlButton(
                        text = "◀",
                        onPress = { isDown -> if (isDown) onMoveWindow(-10, 0) }
                    )
                    LegacyControlButton(
                        text = "▼",
                        onPress = { isDown -> if (isDown) onMoveWindow(0, 10) }
                    )
                    LegacyControlButton(
                        text = "▶",
                        onPress = { isDown -> if (isDown) onMoveWindow(10, 0) }
                    )
                }
            }
        }


        if (isLoggerVisible) {
            AndroidView(
                factory = { context ->
                    LoggerView(context).apply {
                        id = R.id.launcherLoggerView
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }


        AndroidView(
            factory = { context ->
                TouchCharInput(context).apply {
                    id = R.id.awt_touch_char
                    layoutParams = ViewGroup.LayoutParams(1, 1)
                }
            }
        )
    }
}
