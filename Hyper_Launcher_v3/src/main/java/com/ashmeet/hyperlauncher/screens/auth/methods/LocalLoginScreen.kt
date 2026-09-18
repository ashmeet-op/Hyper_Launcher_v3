package com.ashmeet.hyperlauncher.screens.auth.methods

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.button.MineButton
import com.ashmeet.hyperlauncher.components.dialog.SimpleAlertDialog
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import java.io.File
import java.io.FileOutputStream

@Composable
fun LocalLoginScreen(
    onLoginClick: (String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var selectedSkinPath by remember { mutableStateOf<String?>(null) }
    var selectedCapePath by remember { mutableStateOf<String?>(null) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    fun validateImage(path: String, isSkin: Boolean): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        val w = options.outWidth
        val h = options.outHeight

        return if (isSkin) {
            (w > 0 && w % 64 == 0) && (h == w || h == w / 2)
        } else {
            ((w > 0 && w % 64 == 0) && h == w / 2) || ((w > 0 && w % 22 == 0) && h == (w * 17) / 22)
        }
    }

    val skinPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentWithExtension("image/png")
    ) { uri ->
        uri?.let {
            val skinFile = File(context.cacheDir, "skin_import_temp_${System.currentTimeMillis()}.png")
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(skinFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val path = skinFile.absolutePath
                if (validateImage(path, true)) {
                    selectedSkinPath = path
                } else {
                    errorDialogMessage = "Invalid skin dimensions. Standard sizes are 64x32 or 64x64 (or their multiples)."
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val capePickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentWithExtension("image/png")
    ) { uri ->
        uri?.let {
            val capeFile = File(context.cacheDir, "cape_import_temp_${System.currentTimeMillis()}.png")
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(capeFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val path = capeFile.absolutePath
                if (validateImage(path, false)) {
                    selectedCapePath = path
                } else {
                    errorDialogMessage = "Invalid cape dimensions. Standard sizes are 64x32 or 22x17 (or their multiples)."
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {

            var hasAttemptedLogin by remember { mutableStateOf(false) }
            val isUsernameValid = remember(username) {
                username.length in 3..16 && username.matches(Regex("^[a-zA-Z0-9_]*$"))
            }
            val isError = (username.isNotEmpty() || hasAttemptedLogin) && !isUsernameValid

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = translatedText("Username")) },
                placeholder = { Text(text = translatedText("Username")) },
                supportingText = if (isError) {
                    {
                        Text(
                            text = translatedText(stringResource(R.string.local_login_bad_username_text)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else null,
                isError = isError,
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { skinPickerLauncher.launch(null) },
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    if (selectedSkinPath != null) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = "Change skin",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FilledTonalButton(
                    onClick = { capePickerLauncher.launch(null) },
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    if (selectedCapePath != null) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = "Change cape",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            MineButton(
                onClick = {
                    hasAttemptedLogin = true
                    if (isUsernameValid) {
                        onLoginClick(username, selectedSkinPath, selectedCapePath)
                    }
                },
                text = translatedText(stringResource(R.string.login_online_login_label)),
                modifier = Modifier.fillMaxWidth(),
                isUppercase = false
            )

            errorDialogMessage?.let { message ->
                SimpleAlertDialog(
                    title = translatedText("Invalid Image"),
                    text = translatedText(message),
                    confirmText = translatedText("OK"),
                    onConfirm = { errorDialogMessage = null },
                    onDismiss = { errorDialogMessage = null }
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=800dp,height=400dp,dpi=420",
)
@Composable
fun LocalLoginScreenPreview() {
    PojavTheme {
        LocalLoginScreen(onLoginClick = { _, _, _ -> })
    }
}
