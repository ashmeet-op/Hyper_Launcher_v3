package com.ashmeet.hyperlauncher.tracker

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashmeet.hyperbackend.progresskeeper.ProgressKeeper
import com.ashmeet.hyperbackend.progresskeeper.ProgressListener
import com.ashmeet.hyperbackend.progresskeeper.TaskCountListener
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R


@Stable
class TaskProgressState(
    val key: String,
    initialProgress: Int = 0,
    initialMessage: String = ""
) {
    var progress by mutableIntStateOf(initialProgress)
    var message by mutableStateOf(initialMessage)
}

private val OBSERVED_PROGRESS_KEYS = listOf(
    "unpack_runtime", "download_minecraft", "download_verlist",
    "authenticate", "install_modpack", "extract_components",
    "extract_single_files", "instance_install", "data_migration",
    "copy_files", "download_content", "download_translations"
)

@Composable
fun TrackerView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var taskCount by remember { mutableIntStateOf(ProgressKeeper.getTaskCount()) }
    val activeTasks = remember { mutableStateMapOf<String, TaskProgressState>() }
    val activeTaskList by remember {
        derivedStateOf { activeTasks.values.toList() }
    }

    DisposableEffect(Unit) {
        val taskCountListener = TaskCountListener { tc ->
            taskCount = tc
            false
        }
        ProgressKeeper.addTaskCountListener(taskCountListener)

        val listeners = OBSERVED_PROGRESS_KEYS.map { key ->
            val listener = object : ProgressListener {
                private var lastUpdate = 0L

                override fun onProgressStarted() {
                    activeTasks[key] = TaskProgressState(key)
                }

                @SuppressLint("LocalContextGetResourceValueCall")
                override fun onProgressUpdated(progress: Int, resid: Int, vararg va: Any?) {
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate < 50 && progress != 100) return
                    lastUpdate = now

                    val args = if (va.size == 1 && va[0] is Array<*>) {
                        (va[0] as Array<*>).map { it }.toTypedArray()
                    } else {
                        va
                    }

                    val msg: String = if (resid != -1) {
                        try {
                            context.getString(resid, *args)
                        } catch (e: Exception) {
                            Log.e("TrackerView", "Error formatting string resource $resid", e)
                            context.getString(resid)
                        }
                    } else {
                        val rawVa = if (va.size == 1 && va[0] is Array<*>) {
                            (va[0] as Array<*>).firstOrNull()?.toString()
                        } else {
                            va.firstOrNull()?.toString()
                        }
                        rawVa ?: ""
                    }

                    val existing = activeTasks[key]
                    if (existing != null) {
                        if (existing.progress != progress) existing.progress = progress
                        if (existing.message != msg) existing.message = msg
                    } else {
                        activeTasks[key] = TaskProgressState(key, progress, msg)
                    }
                }

                override fun onProgressEnded() {
                    activeTasks.remove(key)
                }
            }
            ProgressKeeper.addListener(key, listener)
            key to listener
        }

        onDispose {
            ProgressKeeper.removeTaskCountListener(taskCountListener)
            listeners.forEach { (key, listener) ->
                ProgressKeeper.removeListener(key, listener)
            }
        }
    }

    TrackerContent(
        taskCount = taskCount,
        activeTasks = activeTaskList,
        modifier = modifier
    )
}

@Composable
fun TrackerContent(
    taskCount: Int,
    activeTasks: List<TaskProgressState>,
    modifier: Modifier = Modifier
) {
    var isCollapsed by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = taskCount > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isCollapsed = !isCollapsed },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = translatedText(stringResource(R.string.progresslayout_tasks_in_progress, taskCount)),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { isCollapsed = !isCollapsed },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Rounded.ArrowDropDown else Icons.Rounded.ArrowDropUp,
                            contentDescription = if (isCollapsed) "Expand" else "Collapse",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = !isCollapsed) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        activeTasks.forEach { task ->
                            TaskItem(task)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: TaskProgressState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (task.progress >= 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${task.progress}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        TaskIndicator(task)
    }
}

@Composable
fun TaskIndicator(task: TaskProgressState) {
    val isIndeterminate by remember { derivedStateOf { task.progress < 0 } }
    val animatedProgress by animateFloatAsState(
        targetValue = if (task.progress >= 0) task.progress / 100f else 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "Smooth Progress"
    )

    if (!isIndeterminate) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    } else {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Preview
@Composable
fun TrackerPreview() {
    PojavTheme {
        TrackerContent(
            taskCount = 2,
            activeTasks = listOf(
                TaskProgressState("download_minecraft", 50, "Downloading Minecraft..."),
                TaskProgressState("install_modpack", -1, "Installing Modpack...")
            )
        )
    }
}
