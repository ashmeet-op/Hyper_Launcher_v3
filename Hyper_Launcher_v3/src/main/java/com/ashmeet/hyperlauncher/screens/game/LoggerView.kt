package com.ashmeet.hyperlauncher.screens.game

import android.content.Context
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperbackend.Logger
import com.ashmeet.hyperlauncher.utils.LoggerProxy

@Stable
data class LogLine(val text: String, val color: Color)

class LoggerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var _isOutputOn = mutableStateOf(true)

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == VISIBLE) {
            _isOutputOn.value = true
        }
    }

    @Composable
    override fun Content() {
        LoggerViewCompose(
            isOutputOn = _isOutputOn.value,
            onOutputToggle = { _isOutputOn.value = it },
            onDismiss = { visibility = GONE }
        )
    }
}

private fun parseLog(text: String): LogLine {
    val lowerText = text.lowercase()

    val color = when {
        lowerText.contains("error") -> Color(0xFFF44336)
        lowerText.contains("warn") -> Color(0xFFFFB300)
        lowerText.contains("success") -> Color(0xFF4CAF50)
        else -> Color.Unspecified
    }


    val cleanText = text
        .replace(Regex("^.*?<.*?:"), "")
        .replace(Regex("^.*?:.*?>"), "")
        .replace(Regex("<.*?>"), "")
        .trim()

    return LogLine(cleanText, color)
}

@Composable
fun LoggerViewCompose(
    isOutputOn: Boolean,
    onOutputToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logs = remember { mutableStateListOf<LogLine>() }
    var isAutoScrollOn by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val handler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(isOutputOn) {
        if (isOutputOn) {
            val listener = Logger.eventLogListener { text ->
                val parsed = parseLog(text ?: "")
                handler.post {
                    logs.add(parsed)
                    if (logs.size > 1500) {
                        repeat(100) { if (logs.isNotEmpty()) logs.removeAt(0) }
                    }
                }
            }
            LoggerProxy.addListener(listener)

            onDispose {
                LoggerProxy.removeListener(listener)
            }
        } else {
            logs.clear()
            onDispose { }
        }
    }

    LaunchedEffect(logs.size, isAutoScrollOn) {
        if (isAutoScrollOn && logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.50f))
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = translatedText(stringResource(R.string.log_view_label_log_output)),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                IconToggleButton(
                    checked = isAutoScrollOn,
                    onCheckedChange = { isAutoScrollOn = it },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerticalAlignBottom,
                        contentDescription = stringResource(
                            id = if (isAutoScrollOn) R.string.log_view_button_scroll_on
                                 else R.string.log_view_button_scroll_off
                        ),
                        tint = if (isAutoScrollOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconToggleButton(
                    checked = isOutputOn,
                    onCheckedChange = { onOutputToggle(it) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = stringResource(
                            id = if (isOutputOn) R.string.log_view_button_output_on
                                 else R.string.log_view_button_output_off
                        ),
                        tint = if (isOutputOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = translatedText("Close"),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(logs) { line ->
                Text(
                    text = line.text,
                    color = if (line.color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else line.color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
