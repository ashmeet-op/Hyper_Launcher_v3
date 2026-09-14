package com.ashmeet.hyperlauncher.activity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.platform.ComposeView
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import com.google.gson.JsonSyntaxException
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.EditorExitable
import net.kdt.pojavlaunch.utils.CropperUtils
import java.io.IOException

class CustomControlsActivity : BaseActivity(), EditorExitable, CropperUtils.CropperReceiver {
    private lateinit var mControlLayout: ControlLayout
    private var mCropperReceiver: CropperUtils.CropperReceiver? = null
    private lateinit var mCropperLauncher: ActivityResultLauncher<*>

    override fun shouldEnableEdgeToEdge(): Boolean {
        return LauncherPreferences.PREF_FULLSCREEN_LAUNCHER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCropperLauncher = CropperUtils.registerCropper(this, this)

        val composeView = ComposeView(this)
        LauncherComposeHelper.ensureViewTreeOwners(composeView)
        setContentView(composeView)

        mControlLayout = ControlLayout(this)
        mControlLayout.setModifiable(true)

        LauncherComposeHelper.setControlsEditorContent(composeView, mControlLayout) { position ->
            when (position) {
                0 -> mControlLayout.addControlButton(ControlData("New"))
                1 -> mControlLayout.addDrawer(ControlDrawerData())
                2 -> mControlLayout.addJoystickButton(ControlJoystickData())
                3 -> mControlLayout.openLoadDialog()
                4 -> mControlLayout.openSaveDialog(this)
                5 -> mControlLayout.openSetDefaultDialog()
                6 -> { // Saving the currently shown control
                    try {
                        val contentUri = DocumentsContract.buildDocumentUri(
                            getString(R.string.storageProviderAuthorities),
                            mControlLayout.saveToDirectory(mControlLayout.mLayoutFileName)
                        )

                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            type = "application/json"
                        }
                        startActivity(shareIntent)

                        val sendIntent = Intent.createChooser(shareIntent, mControlLayout.mLayoutFileName)
                        startActivity(sendIntent)
                    } catch (e: Exception) {
                        Tools.showError(this, e)
                    }
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mControlLayout.post {
            try {
                mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH)
            } catch (e: IOException) {
                Tools.showError(this, e)
            } catch (e: JsonSyntaxException) {
                Tools.showError(this, e)
            }
        }
    }

    fun startCropping(cropperReceiver: CropperUtils.CropperReceiver) {
        mCropperReceiver = cropperReceiver
        CropperUtils.startCropper(mCropperLauncher)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        mControlLayout.askToExit(this)
    }

    override fun exitEditor() {
        super.onBackPressed()
    }

    override fun getAspectRatio(): Float {
        return mCropperReceiver?.aspectRatio ?: 1f
    }

    override fun getTargetMaxSide(): Int {
        return mCropperReceiver?.targetMaxSide ?: 128
    }

    override fun onCropped(contentBitmap: Bitmap?) {
        mCropperReceiver?.onCropped(contentBitmap)
    }

    override fun onFailed(exception: Exception?) {
        mCropperReceiver?.onFailed(exception)
    }
}
