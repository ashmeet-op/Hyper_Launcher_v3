package com.ashmeet.hyperlauncher.filemanager.components

import android.content.Context
import android.os.Environment
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.ashmeet.hyperlauncher.filemanager.listeners.DialogTitleListener
import com.ashmeet.hyperlauncher.filemanager.listeners.FileSelectedListener
import com.ashmeet.hyperlauncher.filemanager.utils.SortFileName
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.Tools
import java.io.File
import java.util.Arrays

class FileListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val fileSuffixes: Array<String> = emptyArray()
) : LinearLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?, fileSuffixes: Array<String>) : this(context, attrs, 0, fileSuffixes)

    var fullPath: File = try {
        Environment.getExternalStorageDirectory()
    } catch (_: Exception) {
        File("/")
    }

    private var lockPath: File = File("/")
    private var showFiles: Boolean = true
    private var showFolders: Boolean = true
    private var fileSelectedListener: FileSelectedListener? = null
    private var dialogTitleListener: DialogTitleListener? = null

    private var filesState by mutableStateOf<List<File>>(emptyList())
    private var currentTitleState by mutableStateOf("")

    init {
        orientation = VERTICAL
        val composeView = ComposeView(context).apply {
            setContent {
                PojavTheme {
                    FileListViewContent(
                        files = filesState,
                        currentPath = currentTitleState,
                        onItemClick = { file ->
                            if (file.name == ".." || (filesState.isNotEmpty() && filesState[0] == file && !lockPath.equals(fullPath))) {
                                parentDir()
                            } else if (file.isDirectory) {
                                listFileAt(file)
                            } else {
                                fileSelectedListener?.onFileSelected(file, file.absolutePath)
                            }
                        },
                        onItemLongClick = { file ->
                            if (file.isFile) {
                                fileSelectedListener?.onFileLongClick(file, file.absolutePath)
                            }
                        }
                    )
                }
            }
        }
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        refreshPath()
    }

    fun setFileSelectedListener(listener: FileSelectedListener?) {
        this.fileSelectedListener = listener
    }

    fun setDialogTitleListener(listener: DialogTitleListener?) {
        this.dialogTitleListener = listener
    }

    fun listFileAt(path: File?) {
        if (path == null) return
        PojavApplication.sExecutorService.execute {
            try {
                if (path.exists()) {
                    if (path.isDirectory) {
                        fullPath = path
                        val listFile = path.listFiles()
                        val resultFiles = mutableListOf<File>()
                        if (!path.equals(lockPath)) {
                            resultFiles.add(File(path, ".."))
                        }
                        if (listFile != null && listFile.isNotEmpty()) {
                            Arrays.sort(listFile, SortFileName())
                            for (file in listFile) {
                                if (file.isDirectory) {
                                    if (showFolders && ((!file.name.startsWith(".")) || file.name == ".minecraft")) {
                                        resultFiles.add(file)
                                    }
                                    continue
                                }
                                if (showFiles) {
                                    if (fileSuffixes.isNotEmpty()) {
                                        for (suffix in fileSuffixes) {
                                            if (file.name.endsWith(".$suffix")) {
                                                resultFiles.add(file)
                                                break
                                            }
                                        }
                                    } else {
                                        resultFiles.add(file)
                                    }
                                }
                            }
                        }
                        Tools.runOnUiThread {
                            filesState = resultFiles
                            val absPath = path.absolutePath
                            currentTitleState = absPath
                            dialogTitleListener?.onChangeDialogTitle(absPath)
                        }
                    } else {
                        Tools.runOnUiThread {
                            fileSelectedListener?.onFileSelected(path, path.absolutePath)
                        }
                    }
                } else {
                    Tools.runOnUiThread {
                        Toast.makeText(context, "This folder (or file) doesn't exist", Toast.LENGTH_SHORT).show()
                        refreshPath()
                    }
                }
            } catch (e: Exception) {
                Tools.runOnUiThread {
                    Tools.showError(context, e)
                }
            }
        }
    }

    fun refreshPath() {
        listFileAt(fullPath)
    }

    fun parentDir() {
        if (fullPath.absolutePath != "/") {
            fullPath.parentFile?.let { listFileAt(it) }
        }
    }

    fun lockPathAt(path: File) {
        lockPath = path
        listFileAt(path)
    }

    fun setShowFiles(showFiles: Boolean) {
        this.showFiles = showFiles
    }

    fun setShowFolders(showFolders: Boolean) {
        this.showFolders = showFolders
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListViewContent(
    files: List<File>,
    currentPath: String,
    onItemClick: (File) -> Unit,
    onItemLongClick: (File) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                text = currentPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(files) { file ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onItemClick(file) },
                                onLongClick = { onItemLongClick(file) }
                            ),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Rounded.FolderOpen else Icons.AutoMirrored.Rounded.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
