package com.ashmeet.hyperlauncher.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import com.ashmeet.hyperlauncher.screens.activity.MissingStorageScreen

class MissingStorageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MissingStorageScreen()
        }
    }
}