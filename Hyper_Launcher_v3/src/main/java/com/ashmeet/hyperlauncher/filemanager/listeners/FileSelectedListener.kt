package com.ashmeet.hyperlauncher.filemanager.listeners

import java.io.File

abstract class FileSelectedListener {
    abstract fun onFileSelected(file: File, path: String)
    open fun onFileLongClick(file: File, path: String) {}
}
