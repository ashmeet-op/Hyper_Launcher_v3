package com.ashmeet.hyperlauncher.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps
import net.kdt.pojavlaunch.utils.FileUtils
import org.apache.commons.io.IOUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread


@Suppress("IOStreamConstructor")
class ImportControlActivity : BaseActivity() {

    private var mUriData: Uri? = null
    private var mHasIntentChanged = true
    @Volatile
    private var mIsFileVerified = false

    private var mFileName = ""
    private lateinit var mComposeView: ComposeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Tools.getDisplayMetrics(this)
        if (Tools.checkStorageInteractive(this)) {
            Tools.initStorageConstants(applicationContext)
        } else {

            return
        }

        mComposeView = ComposeView(this)
        LauncherComposeHelper.ensureViewTreeOwners(mComposeView)
        setContentView(mComposeView)
        updateComposeContent()
    }

    private fun updateComposeContent() {
        LauncherComposeHelper.setImportControlContent(mComposeView, mFileName) { fileName ->
            mFileName = fileName
            startImport()
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mHasIntentChanged = true
    }


    @Suppress("DEPRECATION")
    override fun onPostResume() {
        super.onPostResume()
        if (!Tools.checkStorageInteractive(this)) {



            return
        }
        if (!mHasIntentChanged) return
        mIsFileVerified = false
        getUriData()
        val uri = mUriData
        if (uri == null) {
            finishAndRemoveTask()
            return
        }
        mFileName = trimFileName(Tools.getFileName(this, uri) ?: "")
        updateComposeContent()
        mHasIntentChanged = false



        thread {
            importControlFile()

            if (verify()) {
                mIsFileVerified = true
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@ImportControlActivity,
                        R.string.import_control_invalid_file,
                        Toast.LENGTH_SHORT
                    ).show()
                    finishAndRemoveTask()
                }
            }
        }
    }


    fun startImport() {
        val fileName = trimFileName(mFileName)

        if (!isFileNameValid(fileName)) {
            Toast.makeText(this, R.string.import_control_invalid_name, Toast.LENGTH_SHORT).show()
            return
        }
        if (!mIsFileVerified) {
            Toast.makeText(this, R.string.import_control_verifying_file, Toast.LENGTH_LONG).show()
            return
        }

        File(Tools.CTRLMAP_PATH, "TMP_IMPORT_FILE.json").renameTo(File(Tools.CTRLMAP_PATH, "$fileName.json"))
        Toast.makeText(applicationContext, R.string.import_control_done, Toast.LENGTH_SHORT).show()
        finishAndRemoveTask()
    }


    private fun importControlFile() {
        val uri = mUriData ?: return
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream: OutputStream = FileOutputStream(File(Tools.CTRLMAP_PATH, "TMP_IMPORT_FILE.json"))
            if (inputStream != null) {
                IOUtils.copy(inputStream, outputStream)
                outputStream.close()
                inputStream.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun getUriData() {
        mUriData = intent.data
        if (mUriData != null) return
        try {
            mUriData = intent.clipData?.getItemAt(0)?.uri
        } catch (_: Exception) {
        }
    }

    companion object {

        private fun isFileNameValid(fileName: String): Boolean {
            val trimmedName = trimFileName(fileName)

            if (trimmedName.isEmpty()) return false
            return !FileUtils.exists(Tools.CTRLMAP_PATH + "/" + trimmedName + ".json")
        }


        private fun trimFileName(fileName: String): String {
            return fileName
                .replace(".json", "")
                .replace("%..".toRegex(), "/")
                .replace("/", "")
                .replace("\\", "")
                .trim()
        }


        private fun verify(): Boolean {
            return try {
                val layout = LayoutBitmaps.load(File(Tools.CTRLMAP_PATH, "TMP_IMPORT_FILE.json"))
                val layoutJobj = JSONObject(layout.mControlsJson)
                layoutJobj.has("version") && layoutJobj.has("mControlDataList")
            } catch (e: Exception) {
                when (e) {
                    is IOException, is JSONException -> {
                        Log.w("ImportControlActivity", "Failed to validate layout", e)
                        false
                    }
                    else -> throw e
                }
            }
        }
    }
}
