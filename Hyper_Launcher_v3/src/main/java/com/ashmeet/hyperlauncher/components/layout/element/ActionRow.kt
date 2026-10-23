package com.ashmeet.hyperlauncher.components.layout.element

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import com.ashmeet.hyperlauncher.utils.translation.translatedText

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import kotlin.math.roundToInt

@Composable
fun ActionRow(
    followedButton: ControlInterface?,
    onDelete: () -> Unit,
    onClone: () -> Unit,
    onAddSub: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (followedButton == null) return

    val view = followedButton.controlView
    val density = LocalDensity.current

    var x by remember(followedButton) { mutableFloatStateOf(view.x) }
    var y by remember(followedButton) { mutableFloatStateOf(view.y) }
    var width by remember(followedButton) { mutableIntStateOf(view.width) }
    var height by remember(followedButton) { mutableIntStateOf(view.height) }

    var rowWidth by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(followedButton) {
        val listener = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            x = v.x
            y = v.y
            width = v.width
            height = v.height
        }
        view.addOnLayoutChangeListener(listener)


        while(true) {
            if (x != view.x || y != view.y || width != view.width || height != view.height) {
                x = view.x
                y = view.y
                width = view.width
                height = view.height
            }
            delay(16)
        }
    }

    val rowHeight = with(density) { 40.dp.toPx() }
    val parent = view.parent as? ViewGroup
    val parentWidth = parent?.width ?: 0

    var side = 1
    val futureY = y - rowHeight
    if (futureY < 0) {
        side = 3
    }

    val finalY = if (side == 1) y - rowHeight else y + height

    Surface(
        modifier = modifier
            .onGloballyPositioned {
                rowWidth = it.size.width.toFloat()
            }
            .offset {
                val finalX = (x + width / 2f - rowWidth / 2f).coerceIn(0f, (parentWidth - rowWidth).coerceAtLeast(0f))
                IntOffset(finalX.roundToInt(), finalY.roundToInt())
            }
            .wrapContentSize(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(translatedText(stringResource(R.string.global_delete)), style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onClone,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(translatedText(stringResource(R.string.global_clone)), style = MaterialTheme.typography.labelMedium)
            }
            if (followedButton is ControlDrawer) {
                Button(
                    onClick = onAddSub,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(translatedText(stringResource(R.string.customctrl_addsubbutton)), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
