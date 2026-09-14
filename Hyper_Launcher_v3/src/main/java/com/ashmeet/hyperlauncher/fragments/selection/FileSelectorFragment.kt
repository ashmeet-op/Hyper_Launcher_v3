package com.ashmeet.hyperlauncher.fragments.selection

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.components.button.MineButton
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.kdt.pickafile.FileListView
import com.kdt.pickafile.FileSelectedListener
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import java.io.File

class FileSelectorFragment : Fragment() {
    companion object {
        const val TAG = "FileSelectorFragment"
        const val BUNDLE_SELECT_FOLDER = "select_folder"
        const val BUNDLE_SHOW_FILE = "show_file"
        const val BUNDLE_SHOW_FOLDER = "show_folder"
        const val BUNDLE_ROOT_PATH = "root_path"
    }

    private var selectFolder = true
    private var showFiles = true
    private var showFolders = true
    private var rootPath: String = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        Tools.DIR_GAME_NEW
    else Environment.getExternalStorageDirectory().absolutePath) ?: Tools.DIR_GAME_HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            selectFolder = bundle.getBoolean(BUNDLE_SELECT_FOLDER, selectFolder)
            showFiles = bundle.getBoolean(BUNDLE_SHOW_FILE, showFiles)
            showFolders = bundle.getBoolean(BUNDLE_SHOW_FOLDER, showFolders)
            rootPath = bundle.getString(BUNDLE_ROOT_PATH) ?: rootPath
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    FileSelectorContent(
                        rootPath = rootPath,
                        selectFolder = selectFolder,
                        showFiles = showFiles,
                        showFolders = showFolders,
                        onFileSelected = { _, path ->
                            ExtraCore.setValue(ExtraConstants.FILE_SELECTOR, removeLockPath(path))
                            Tools.removeCurrentFragment(requireActivity())
                        }
                    ) { path ->
                        ExtraCore.setValue(ExtraConstants.FILE_SELECTOR, removeLockPath(path))
                        Tools.removeCurrentFragment(requireActivity())
                    }
                }
            }
        }
    }

    private fun removeLockPath(path: String): String {
        return path.replace(rootPath, ".")
    }
}

@Composable
fun FileSelectorContent(
    rootPath: String,
    selectFolder: Boolean,
    showFiles: Boolean,
    showFolders: Boolean,
    onFileSelected: (File, String) -> Unit,
    onFolderSelected: (String) -> Unit
) {
    var currentPath by remember { mutableStateOf(".") }
    var fileListViewRef by remember { mutableStateOf<FileListView?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = currentPath,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AndroidView(
                factory = { context ->
                    FileListView(context).apply {
                        setShowFiles(showFiles)
                        setShowFolders(showFolders)
                        lockPathAt(File(rootPath))
                        setDialogTitleListener { title ->
                            currentPath = title.replace(rootPath, ".")
                        }
                        setFileSelectedListener(
                            object : FileSelectedListener() {
                                override fun onFileSelected(file: File, path: String) {
                                    onFileSelected(file, path)
                                }
                            }
                        )
                        refreshPath()
                        fileListViewRef = this
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MineButton(
                    text = translatedText(stringResource(R.string.folder_fragment_create)),
                    onClick = {
                        fileListViewRef?.let { flv ->
                            val context = flv.context
                            val editText = EditText(context)
                            AlertDialog.Builder(context)
                                .setTitle(R.string.folder_dialog_insert_name)
                                .setView(editText)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(R.string.folder_dialog_create) { _, _ ->
                                    val folder = File(flv.fullPath, editText.text.toString())
                                    if (folder.mkdir()) {
                                        flv.listFileAt(File(flv.fullPath, editText.text.toString()))
                                    } else {
                                        flv.refreshPath()
                                    }
                                }.show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                if (selectFolder) {
                    MineButton(
                        text = translatedText(stringResource(R.string.folder_fragment_select)),
                        onClick = {
                            fileListViewRef?.let { flv ->
                                onFolderSelected(flv.fullPath.absolutePath)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
