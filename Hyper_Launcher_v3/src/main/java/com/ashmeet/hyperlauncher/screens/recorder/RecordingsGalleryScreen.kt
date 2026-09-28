package com.ashmeet.hyperlauncher.screens.recorder

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ashmeet.hyperlauncher.components.dialog.SimpleAlertDialog
import com.ashmeet.hyperlauncher.recorder.RecordingItem
import com.ashmeet.hyperlauncher.recorder.RecordingManager
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ashmeet.hyperlauncher.R
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsGalleryScreen(
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var launcherBgPath by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH) }
    val hasBackground = launcherBgPath != null
    val backgroundTransparency = if (hasBackground) 0.5f else 1f

    var recordings by remember { mutableStateOf<List<RecordingItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<RecordingItem?>(null) }
    var itemToDelete by remember { mutableStateOf<RecordingItem?>(null) }

    fun refreshList() {
        scope.launch {
            recordings = RecordingManager.getRecordings(context)
            if (selectedItem != null && !recordings.any { it.file.absolutePath == selectedItem?.file?.absolutePath }) {
                selectedItem = recordings.firstOrNull()
            } else if (selectedItem == null && recordings.isNotEmpty()) {
                selectedItem = recordings.first()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 3.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (recordings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = translatedText("No recorded game clips found."),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = translatedText("Use Quick Settings or Drawer button to start recording."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column: Video List
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(32.dp),
                        color = if (hasBackground) MaterialTheme.colorScheme.surface.copy(alpha = backgroundTransparency)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        tonalElevation = 0.dp
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recordings, key = { it.file.absolutePath }) { item ->
                                RecordingListItemCard(
                                    item = item,
                                    isSelected = item.file.absolutePath == selectedItem?.file?.absolutePath,
                                    onClick = { selectedItem = item },
                                    onDeleteClick = { itemToDelete = item }
                                )
                            }
                        }
                    }

                    // Right Column: Built-in Media Player & Actions
                    Surface(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(32.dp),
                        color = if (hasBackground) MaterialTheme.colorScheme.surface.copy(alpha = backgroundTransparency)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        tonalElevation = 0.dp
                    ) {
                        if (selectedItem != null) {
                            BuiltInMediaPlayerView(
                                item = selectedItem!!,
                                onDelete = { itemToDelete = selectedItem }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translatedText("Select a video clip to play"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        val fileToDelete = itemToDelete!!.file
        SimpleAlertDialog(
            title = translatedText("Delete Recording?"),
            text = "${translatedText("Are you sure you want to delete")} \"${fileToDelete.name}\"? ${translatedText("This action cannot be undone.")}",
            confirmText = translatedText("Delete"),
            dismissText = translatedText("Cancel"),
            isDestructive = true,
            onConfirm = {
                val deleted = RecordingManager.deleteRecording(fileToDelete)
                if (deleted) {
                    Toast.makeText(context, "Deleted ${fileToDelete.name}", Toast.LENGTH_SHORT).show()
                }
                itemToDelete = null
                refreshList()
            },
            onDismiss = { itemToDelete = null }
        )
    }
}

@Composable
fun RecordingListItemCard(
    item: RecordingItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var thumbnail by remember(item.file.absolutePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.file.absolutePath) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(item.file.absolutePath)
                val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                thumbnail = bitmap
            } catch (e: Exception) {
                Log.w("RecordingCard", "Failed to load thumbnail for ${item.name}", e)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Box
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.formattedDuration,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.formattedSize} • ${item.formattedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun BuiltInMediaPlayerView(
    item: RecordingItem,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isPrepared by remember(item.file.absolutePath) { mutableStateOf(false) }
    var isPlaying by remember(item.file.absolutePath) { mutableStateOf(false) }
    var currentPosMs by remember(item.file.absolutePath) { mutableLongStateOf(0L) }
    var totalDurationMs by remember(item.file.absolutePath) { mutableLongStateOf(item.durationMs) }
    var isMuted by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(1.0f) }

    var videoWidth by remember(item.file.absolutePath) { mutableIntStateOf(0) }
    var videoHeight by remember(item.file.absolutePath) { mutableIntStateOf(0) }

    var textureSurface by remember { mutableStateOf<Surface?>(null) }
    var mediaPlayer by remember(item.file.absolutePath) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(item.file.absolutePath) {
        val player = MediaPlayer().apply {
            try {
                setDataSource(item.file.absolutePath)
                setOnPreparedListener { mp ->
                    isPrepared = true
                    totalDurationMs = mp.duration.toLong().coerceAtLeast(0L)
                    if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                        videoWidth = mp.videoWidth
                        videoHeight = mp.videoHeight
                    }
                    val vol = if (isMuted) 0f else volumeLevel
                    mp.setVolume(vol, vol)
                }
                setOnVideoSizeChangedListener { _, width, height ->
                    if (width > 0 && height > 0) {
                        videoWidth = width
                        videoHeight = height
                    }
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosMs = totalDurationMs
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayer", "MediaPlayer error: what=$what, extra=$extra")
                    isPrepared = false
                    isPlaying = false
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("MediaPlayer", "Failed to prepare video source", e)
            }
        }
        mediaPlayer = player

        onDispose {
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            } catch (e: Exception) {
                Log.w("MediaPlayer", "Error releasing player", e)
            }
            mediaPlayer = null
            isPrepared = false
            isPlaying = false
        }
    }

    LaunchedEffect(item.file.absolutePath) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(item.file.absolutePath)
                val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

                var w = wStr?.toIntOrNull() ?: 0
                var h = hStr?.toIntOrNull() ?: 0
                val rotation = rotationStr?.toIntOrNull() ?: 0

                if (rotation == 90 || rotation == 270) {
                    val tmp = w
                    w = h
                    h = tmp
                }
                if (w > 0 && h > 0) {
                    withContext(Dispatchers.Main) {
                        videoWidth = w
                        videoHeight = h
                    }
                }
            } catch (e: Exception) {
                Log.w("BuiltInMediaPlayerView", "Failed metadata extraction", e)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(mediaPlayer, textureSurface) {
        val mp = mediaPlayer
        val surface = textureSurface
        if (mp != null && surface != null && surface.isValid) {
            try {
                mp.setSurface(surface)
            } catch (e: Exception) {
                Log.e("MediaPlayer", "Failed to setSurface", e)
            }
        }
    }

    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        currentPosMs = mp.currentPosition.toLong()
                    }
                } catch (e: Exception) {
                    Log.e("MediaPlayer", "Error reading position", e)
                }
            }
            delay(200.milliseconds)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Video Preview Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (videoWidth > 0 && videoHeight > 0) {
                val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
                val containerAspect = maxWidth.value / maxHeight.value

                val (finalWidth, finalHeight) = if (videoAspect > containerAspect) {
                    maxWidth to (maxWidth / videoAspect)
                } else {
                    (maxHeight * videoAspect) to maxHeight
                }

                Box(
                    modifier = Modifier
                        .size(finalWidth, finalHeight)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                        val oldSurface = textureSurface
                                        textureSurface = Surface(st)
                                        oldSurface?.release()
                                    }
                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                        textureSurface?.release()
                                        textureSurface = null
                                        return true
                                    }
                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                    val oldSurface = textureSurface
                                    textureSurface = Surface(st)
                                    oldSurface?.release()
                                }
                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                    textureSurface?.release()
                                    textureSurface = null
                                    return true
                                }
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (!isPlaying) {
                IconButton(
                    onClick = {
                        if (!isPrepared) return@IconButton
                        mediaPlayer?.let { mp ->
                            try {
                                if (!mp.isPlaying) {
                                    if (currentPosMs >= totalDurationMs) {
                                        mp.seekTo(0)
                                        currentPosMs = 0
                                    }
                                    mp.start()
                                    isPlaying = true
                                }
                            } catch (e: Exception) {
                                Log.e("MediaPlayer", "Error starting playback", e)
                            }
                        }
                    },
                    enabled = isPrepared,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = if (isPrepared) 0.6f else 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPrepared) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Timeline Seek Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMs(currentPosMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = if (totalDurationMs > 0) (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                onValueChange = { frac ->
                    if (!isPrepared) return@Slider
                    val seekTarget = (frac * totalDurationMs).toLong()
                    currentPosMs = seekTarget
                    try {
                        mediaPlayer?.seekTo(seekTarget.toInt())
                    } catch (e: Exception) {
                        Log.e("MediaPlayer", "Error seeking", e)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = formatMs(totalDurationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Controls & Volume Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (!isPrepared) return@IconButton
                        mediaPlayer?.let { mp ->
                            try {
                                if (mp.isPlaying) {
                                    mp.pause()
                                    isPlaying = false
                                } else {
                                    if (currentPosMs >= totalDurationMs) {
                                        mp.seekTo(0)
                                        currentPosMs = 0
                                    }
                                    mp.start()
                                    isPlaying = true
                                }
                            } catch (e: Exception) {
                                Log.e("MediaPlayer", "Error toggling play/pause", e)
                            }
                        }
                    },
                    enabled = isPrepared
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        val vol = if (isMuted) 0f else volumeLevel
                        if (isPrepared) {
                            try {
                                mediaPlayer?.setVolume(vol, vol)
                            } catch (e: Exception) {
                                Log.e("MediaPlayer", "Error setting volume", e)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isMuted || volumeLevel == 0f) Icons.AutoMirrored.Rounded.VolumeMute else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Mute",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Slider(
                    value = if (isMuted) 0f else volumeLevel,
                    onValueChange = { vol ->
                        volumeLevel = vol
                        isMuted = vol == 0f
                        if (isPrepared) {
                            try {
                                mediaPlayer?.setVolume(vol, vol)
                            } catch (e: Exception) {
                                Log.e("MediaPlayer", "Error setting volume", e)
                            }
                        }
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(90.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { saveToGallery(context, item.file) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = translatedText("Save to Gallery"),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                IconButton(
                    onClick = { openExternal(context, item.file) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = translatedText("Open External"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { shareClip(context, item.file) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = translatedText("Share"),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = translatedText("Delete"),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun saveToGallery(context: Context, file: File) {
    try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/HyperLauncher")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("video/mp4"), null)
            Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Could not save to gallery", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("SaveToGallery", "Failed to save video to gallery", e)
        Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}

private fun getUriForFile(context: Context, file: File): Uri {
    return DocumentsContract.buildDocumentUri(
        context.getString(R.string.storageProviderAuthorities),
        file.absolutePath
    )
}

private fun openExternal(context: Context, file: File) {
    try {
        val uri = getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Play Video"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open external player: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareClip(context: Context, file: File) {
    try {
        val uri = getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Recording"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share video: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
