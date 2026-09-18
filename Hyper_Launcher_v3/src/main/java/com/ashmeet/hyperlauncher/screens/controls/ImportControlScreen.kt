package com.ashmeet.hyperlauncher.screens.controls

import androidx.compose.foundation.Image
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.ashmeet.hyperlauncher.components.HyperOutlinedTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.button.MineButton
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R

@Composable
fun ImportControlScreen(
    initialFileName: String,
    onImport: (String) -> Unit
) {
    var fileName by remember { mutableStateOf(initialFileName) }

    PojavTheme {
        Box(modifier = Modifier.fillMaxSize()) {

            Image(
                painter = painterResource(id = R.drawable.ic_setting_sign_in_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.85f)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = translatedText("File name:"),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    HyperOutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("File name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    MineButton(
                        text = translatedText(stringResource(R.string.import_control_import_button)),
                        onClick = { onImport(fileName) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
