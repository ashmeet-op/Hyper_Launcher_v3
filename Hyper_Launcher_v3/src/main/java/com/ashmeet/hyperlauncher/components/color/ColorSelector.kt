@file:JvmName("ColorSelectorKt")

package com.ashmeet.hyperlauncher.components.color

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.colorpicker.rememberColorPickerController
import com.ashmeet.hyperlauncher.colorpicker.components.VerticalAlphaBarPicker
import com.ashmeet.hyperlauncher.colorpicker.components.ColorSquarePicker
import com.ashmeet.hyperlauncher.colorpicker.components.VerticalHueBarPicker
import com.ashmeet.hyperlauncher.colorpicker.components.TransparentChecker
import com.ashmeet.hyperlauncher.components.HyperOutlinedTextField

@Composable
fun ColorSelectorContent(
    initialColor: Int,
    alphaEnabled: Boolean,
    lockSatVal: Boolean = false,
    onColorChanged: (Int) -> Unit,
    onClose: () -> Unit
) {
    val controller = rememberColorPickerController(initialColor = Color(initialColor))

    if (lockSatVal) {
        LaunchedEffect(Unit) {
            controller.setSaturation(1f)
            controller.setValue(1f)
        }
    }

    val currentColor by controller.color

    LaunchedEffect(currentColor) {
        onColorChanged(currentColor.toArgb())
    }

    BackHandler {
        onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (lockSatVal) Modifier.pointerInput(Unit) {} else Modifier)
            ) {
                ColorSquarePicker(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
            }


            VerticalHueBarPicker(
                controller = controller,
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
            )

            if (alphaEnabled) {

                VerticalAlphaBarPicker(
                    controller = controller,
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                TransparentChecker(modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(currentColor)
                )
            }

            var hexText by remember(currentColor) {
                mutableStateOf(String.format("%08X", currentColor.toArgb()))
            }

            HyperOutlinedTextField(
                value = hexText,
                onValueChange = { hex ->
                    hexText = hex
                    try {
                        if (hex.length == 8) {
                            val colorInt = android.graphics.Color.parseColor("#$hex")
                            controller.setColor(Color(colorInt))
                        }
                    } catch (e: Exception) {

                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text("HEX") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}
