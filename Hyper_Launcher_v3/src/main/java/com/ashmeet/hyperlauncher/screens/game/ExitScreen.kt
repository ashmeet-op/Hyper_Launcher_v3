package com.ashmeet.hyperlauncher.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.components.HyperOutlinedTextField
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.GeminiCrashAnalyzer
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import kotlinx.coroutines.launch
import net.ashmeet.hyperlauncher.R

fun cleanMarkdownText(raw: String): String {
    return raw
        .replace(Regex("#{1,6}\\s*"), "") // remove markdown headers like ### or ##
        .replace(Regex("\\*\\*|__"), "") // remove bold markers
        .replace(Regex("[*_]"), "") // remove italic markers
        .replace(Regex("`{1,3}"), "") // remove code backticks
        .replace(Regex("^- ", RegexOption.MULTILINE), "• ") // convert bullets
        .trim()
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExitScreen(
    title: String,
    logs: String,
    onShareClick: () -> Unit,
    onRestartClick: () -> Unit,
    onOpenCrashReport: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val ignoreNotch = if (isPreview) true else LauncherPreferences.PREF_IGNORE_NOTCH

    val crashReportPath = remember(logs) {
        val marker = "#@!@# Game crashed! Crash report saved to: #@!@# "
        val index = logs.indexOf(marker)
        if (index != -1) {
            logs.substring(index + marker.length).trim().substringBefore("\n")
        } else null
    }

    var showApiKeyDialog by remember { mutableStateOf(value = false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(value = false) }
    var aiResult by remember { mutableStateOf<String?>(null) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var viewingAiResult by remember { mutableStateOf(value = false) }

    val coroutineScope = rememberCoroutineScope()

    val runAiInspection = {
        val apiKey = GeminiCrashAnalyzer.getApiKey(context)
        if (apiKey.isBlank()) {
            showApiKeyDialog = true
        } else {
            isAnalyzing = true
            viewingAiResult = true
            aiError = null
            coroutineScope.launch {
                val result = GeminiCrashAnalyzer.analyze(context, logs)
                isAnalyzing = false
                result.fold(
                    onSuccess = { answer ->
                        aiResult = cleanMarkdownText(answer)
                        viewingAiResult = true
                    },
                    onFailure = { error ->
                        if ((error.message == "API_KEY_INVALID") || (error.message == "API_KEY_MISSING")) {
                            showApiKeyDialog = true
                        } else {
                            aiError = error.localizedMessage ?: "Unknown error occurred during AI analysis."
                        }
                    }
                )
            }
        }
    }
    

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val layoutModifier = if (ignoreNotch) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        }

        Row(
            modifier = layoutModifier
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SecondaryTabRow(
                        selectedTabIndex = if (viewingAiResult) 1 else 0,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = !viewingAiResult,
                            onClick = { viewingAiResult = false },
                            text = { Text("Game Logs", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = viewingAiResult,
                            onClick = {
                                if (aiResult == null && !isAnalyzing) {
                                    runAiInspection()
                                } else if (aiResult != null) {
                                    viewingAiResult = true
                                }
                            },
                            text = { Text("AI Analysis", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        if (isAnalyzing) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator()
                            }
                        } else if (viewingAiResult) {
                            if (aiResult != null) {
                                val aiScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(aiScrollState)
                                ) {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + expandVertically()
                                    ) {
                                        Text(
                                            text = aiResult ?: "",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(onClick = { runAiInspection() }, shape = CircleShape) {
                                        Text("Run AI Inspection")
                                    }
                                }
                            }
                        } else {
                            val logScrollState = rememberScrollState()
                            Text(
                                text = logs,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(logScrollState),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Right side: Action Panel
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (crashReportPath != null) {
                    Button(
                        onClick = { runAiInspection() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = translatedText("Inspect with AI"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { onOpenCrashReport(crashReportPath) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape
                    ) {
                        Text(
                            text = translatedText("Crash Report"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape
                ) {
                    Text(
                        text = translatedText(stringResource(R.string.main_share_logs)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRestartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translatedText(stringResource(R.string.global_restart)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // API Key Input Dialog
    if (showApiKeyDialog) {
        HyperAlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(text = translatedText("Gemini API Key Required")) },
            text = {
                Column {
                    Text(
                        text = translatedText("Please enter your Gemini API key to use AI crash inspection."),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    HyperOutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmText = "Save & Inspect",
            onConfirm = {
                if (apiKeyInput.isNotBlank()) {
                    GeminiCrashAnalyzer.saveApiKey(context, apiKeyInput)
                    showApiKeyDialog = false
                    apiKeyInput = ""
                    runAiInspection()
                }
            },
            dismissText = "Cancel",
            onDismiss = { showApiKeyDialog = false }
        )
    }

    // AI Error Dialog
    if (aiError != null) {
        HyperAlertDialog(
            onDismissRequest = { aiError = null },
            title = { Text(text = translatedText("AI Inspection Failed")) },
            text = {
                Text(
                    text = aiError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            },
            confirmText = "OK",
            onConfirm = { aiError = null }
        )
    }
}

@Preview(showBackground = true, device = "spec:width=800dp,height=400dp,dpi=420")
@Composable
fun ExitScreenPreview() {
    PojavTheme {
        ExitScreen(
            title = translatedText("Game exited with code 1"),
            logs = "[10:57:55] [main/INFO]: Loading Minecraft...\n".repeat(20),
            onShareClick = {},
            onRestartClick = {}
        )
    }
}
