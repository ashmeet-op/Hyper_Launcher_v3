package com.ashmeet.hyperlauncher.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ashmeet.hyperlauncher.fragments.dialog.SideDialogView


object SideDialogUtils {
    var activeDialog by mutableStateOf<SideDialogView?>(null)
        @JvmStatic get
        @JvmStatic set


    @JvmStatic
    fun show(dialog: SideDialogView, fromRight: Boolean) {
        activeDialog = dialog
        dialog.appear(fromRight)
    }


    @JvmStatic
    fun dismiss() {
        activeDialog?.disappear(true)
        activeDialog = null
    }
}