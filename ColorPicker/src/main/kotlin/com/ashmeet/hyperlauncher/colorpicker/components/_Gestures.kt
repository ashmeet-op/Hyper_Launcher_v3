package com.ashmeet.hyperlauncher.colorpicker.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize


internal fun Modifier.progressBar(
    widthPx: Float,
    heightPx: Float,
    onOffsetChanged: (Offset) -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    onChangeFinished: () -> Unit
): Modifier = this
    .pointerInput(widthPx, heightPx) {
        detectDragGestures(
            onDragStart = { offset ->
                onOffsetChanged(Offset(offset.x.coerceIn(0f, widthPx), heightPx / 2f))
            },
            onDrag = { change, _ ->
                val x = change.position.x.coerceIn(0f, widthPx)
                onOffsetChanged(Offset(x, heightPx / 2f))
                change.consume()
            },
            onDragEnd = onChangeFinished
        )
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { offset ->
                val x = offset.x.coerceIn(0f, widthPx)
                onOffsetChanged(Offset(x, heightPx / 2f))
                onChangeFinished()
            }
        )
    }
    .onSizeChanged { size ->
        onSizeChanged(size)
    }


internal fun Modifier.verticalProgressBar(
    widthPx: Float,
    heightPx: Float,
    onOffsetChanged: (Offset) -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    onChangeFinished: () -> Unit
): Modifier = this
    .pointerInput(widthPx, heightPx) {
        detectDragGestures(
            onDragStart = { offset ->
                onOffsetChanged(Offset(widthPx / 2f, offset.y.coerceIn(0f, heightPx)))
            },
            onDrag = { change, _ ->
                val y = change.position.y.coerceIn(0f, heightPx)
                onOffsetChanged(Offset(widthPx / 2f, y))
                change.consume()
            },
            onDragEnd = onChangeFinished
        )
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { offset ->
                val y = offset.y.coerceIn(0f, heightPx)
                onOffsetChanged(Offset(widthPx / 2f, y))
                onChangeFinished()
            }
        )
    }
    .onSizeChanged { size ->
        onSizeChanged(size)
    }



internal fun Modifier.squarePicker(
    widthPx: Float,
    heightPx: Float,
    onOffsetChanged: (Offset) -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    onChangeFinished: () -> Unit
): Modifier = this
    .pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset ->
                val x = offset.x.coerceIn(0f, widthPx)
                val y = offset.y.coerceIn(0f, heightPx)
                onOffsetChanged(Offset(x, y))
            },
            onDrag = { change, _ ->
                val x = change.position.x.coerceIn(0f, widthPx)
                val y = change.position.y.coerceIn(0f, heightPx)
                onOffsetChanged(Offset(x, y))
                change.consume()
            },
            onDragEnd = onChangeFinished
        )
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { offset ->
                val x = offset.x.coerceIn(0f, widthPx)
                val y = offset.y.coerceIn(0f, heightPx)
                onOffsetChanged(Offset(x, y))
                onChangeFinished()
            }
        )
    }
    .onSizeChanged { size ->
        onSizeChanged(size)
    }