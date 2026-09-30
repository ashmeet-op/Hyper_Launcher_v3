package com.ashmeet.hyperlauncher.recorder


import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(
        val durationSeconds: Long,
        val startTimeMillis: Long,
        val outputFile: File
    ) : RecordingState()
}

data class RecordingItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val timestampMillis: Long
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb)
            else String.format(Locale.US, "%.0f KB", kb)
        }

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestampMillis))
        }
}

object RecordingManager {
    private const val TAG = "RecordingManager"

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun getRecordingsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @Synchronized
    fun startRecording(context: Context, width: Int = 0, height: Int = 0): Boolean {
        if (_recordingState.value is RecordingState.Recording) {
            Log.w(TAG, "Recording is already active")
            return false
        }

        try {
            val dm = context.resources.displayMetrics
            var recordWidth = if (width > 0) width else dm.widthPixels
            var recordHeight = if (height > 0) height else dm.heightPixels

            if (width <= 0 || height <= 0) {
                if (recordWidth < recordHeight) {
                    val tmp = recordWidth
                    recordWidth = recordHeight
                    recordHeight = tmp
                }
            }


            recordWidth = if (recordWidth % 2 == 0) recordWidth else recordWidth - 1
            recordHeight = if (recordHeight % 2 == 0) recordHeight else recordHeight - 1

            val recordingsDir = getRecordingsDir(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFile = File(recordingsDir, "GameRecord_$timeStamp.mp4")
            currentOutputFile = outputFile

            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            } catch (e: Exception) {
                Log.w(TAG, "Audio source MIC unavailable, recording video only: ${e.message}")
            }

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.setVideoEncodingBitRate(12000000)
            recorder.setVideoFrameRate(60)
            recorder.setVideoSize(recordWidth, recordHeight)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            try {
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioSamplingRate(44100)
                recorder.setAudioEncodingBitRate(128000)
            } catch (_: Exception) {

            }

            recorder.prepare()

            val inputSurface = recorder.surface
            SurfaceRecorderHook.setRecorderSurface(inputSurface, recordWidth, recordHeight)

            recorder.start()
            mediaRecorder = recorder

            val startTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording(0L, startTime, outputFile)

            timerJob?.cancel()
            timerJob = scope.launch {
                var seconds = 0L
                while (true) {
                    delay(1000.milliseconds)
                    seconds++
                    val currentState = _recordingState.value
                    if (currentState is RecordingState.Recording) {
                        _recordingState.value = currentState.copy(durationSeconds = seconds)
                    } else {
                        break
                    }
                }
            }

            Toast.makeText(context, "Game recording started!", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Recording started -> ${outputFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanupRecorder()
            Toast.makeText(context, "Failed to start recording: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    @Synchronized
    fun stopRecording(context: Context): File? {
        val currentState = _recordingState.value
        if (currentState !is RecordingState.Recording) {
            return null
        }

        timerJob?.cancel()
        timerJob = null

        val savedFile = currentState.outputFile
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            cleanupRecorder()
        }

        _recordingState.value = RecordingState.Idle

        if (savedFile.exists() && savedFile.length() > 0) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(savedFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )
            val durationText = formatDuration(currentState.durationSeconds)
            Toast.makeText(
                context,
                "Recording saved ($durationText): ${savedFile.name}",
                Toast.LENGTH_LONG
            ).show()
            Log.i(TAG, "Recording saved successfully: ${savedFile.absolutePath}")
            return savedFile
        } else {
            if (savedFile.exists()) savedFile.delete()
            Toast.makeText(context, "Recording failed or file was empty", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    fun toggleRecording(context: Context, width: Int = 0, height: Int = 0) {
        if (_recordingState.value is RecordingState.Recording) {
            stopRecording(context)
        } else {
            startRecording(context, width, height)
        }
    }

    private fun cleanupRecorder() {
        SurfaceRecorderHook.setRecorderSurface(null, 0, 0)
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        }
        mediaRecorder = null
        currentOutputFile = null
    }

    suspend fun getRecordings(context: Context): List<RecordingItem> = withContext(Dispatchers.IO) {
        val dir = getRecordingsDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(".mp4", ignoreCase = true) } ?: emptyArray()

        files.map { file ->
            var duration = 0L
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = time?.toLongOrNull() ?: 0L
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract metadata for ${file.name}", e)
            }

            RecordingItem(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                durationMs = duration,
                timestampMillis = file.lastModified()
            )
        }.sortedByDescending { it.timestampMillis }
    }

    fun deleteRecording(file: File): Boolean {
        return if (file.exists()) {
            file.delete()
        } else false
    }

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
