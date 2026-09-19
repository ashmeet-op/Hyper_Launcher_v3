package com.ashmeet.hyperlauncher.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ashmeet.hyperlauncher.components.dialog.SimpleAlertDialog
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.Tools
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.tasks.AsyncAssetManager

class TestStorageActivity : BaseActivity() {
    private val REQUEST_STORAGE_REQUEST_CODE = 1
    private var mPermsRequired = false
    private var mPermsDialogShown = false
    private var mShowRerequestDialog by mutableStateOf(false)

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPermsDialogShown = false
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29 && !isStorageAllowed(this)) {
            mPermsRequired = true
            setContentView(ComposeView(this).apply {
                setContent {
                    PojavTheme {
                        if (mShowRerequestDialog) {
                            SimpleAlertDialog(
                                title = getString(R.string.global_error),
                                text = getString(R.string.toast_permission_denied),
                                confirmText = getString(android.R.string.ok),
                                onConfirm = {
                                    mShowRerequestDialog = false
                                    requestStoragePermission()
                                },
                                onDismiss = { finish() }
                            )
                        }
                    }
                }
            })
        } else {
            exit()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mPermsRequired) return
        if (!mPermsDialogShown) {
            requestStoragePermission()
        } else {
            showRerequestDialog()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    private fun showRerequestDialog() {
        mShowRerequestDialog = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPermsRequired = false
                exit()
            } else {
                mPermsDialogShown = true
                showRerequestDialog()
            }
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_STORAGE_REQUEST_CODE
        )
    }

    private fun exit() {
        if (!Tools.checkStorageRoot(this)) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            return
        }
        //Initialize constants (implicitly) and preferences after we confirm that we have storage.
        LauncherPreferences.loadPreferences(this)
        AsyncAssetManager.unpackComponents(this)
        AsyncAssetManager.unpackSingleFiles(this)

        val intent = Intent(this, LauncherActivity::class.java)
        getIntent().extras?.let {
            intent.putExtras(it)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        @JvmStatic
        fun isStorageAllowed(context: Context): Boolean {
            //Getting the permission status
            val result1 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val result2 = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)

            //If permission is granted returning true
            return result1 == PackageManager.PERMISSION_GRANTED &&
                    result2 == PackageManager.PERMISSION_GRANTED
        }
    }
}
