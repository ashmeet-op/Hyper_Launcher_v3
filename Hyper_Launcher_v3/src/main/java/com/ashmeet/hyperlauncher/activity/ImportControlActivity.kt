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
import net.kdt.pojavlaunch.BaseActivity
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

/**
 * An activity dedicated to importing control files.
 */
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
            // Return early, no initialization needed.
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

    /**
     * Override the previous loaded intent
     * @param intent the intent used to replace the old one.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mHasIntentChanged = true
    }

    /**
     * Update all over again if the intent changed.
     */
    @Suppress("DEPRECATION")
    override fun onPostResume() {
        super.onPostResume()
        if (!Tools.checkStorageInteractive(this)) {
            // Don't try to read the file as when this check fails, external storage paths
            // are no longer valid (likely unmounted).
            // checkStorageInteractive() will finish this activity for us.
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

        // Import and verify thread
        // Kill the app if the file isn't valid.
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

    /**
     * Start the import.
     */
    fun startImport() {
        val fileName = trimFileName(mFileName)
        // Step 1 check for suffixes.
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

    /**
     * Copy a the file from the Intent data with a provided name into the controlmap folder.
     */
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

    /**
     * Tries to get an Uri from the various sources
     */
    private fun getUriData() {
        mUriData = intent.data
        if (mUriData != null) return
        try {
            mUriData = intent.clipData?.getItemAt(0)?.uri
        } catch (_: Exception) {
        }
    }

    companion object {
        /**
         * Tell if the clean version of the filename is valid.
         * @param fileName the string to test
         * @return whether the filename is valid
         */
        private fun isFileNameValid(fileName: String): Boolean {
            val trimmedName = trimFileName(fileName)

            if (trimmedName.isEmpty()) return false
            return !FileUtils.exists(Tools.CTRLMAP_PATH + "/" + trimmedName + ".json")
        }

        /**
         * Remove or undesirable chars from the string
         * @param fileName The string to trim
         * @return The trimmed string
         */
        private fun trimFileName(fileName: String): String {
            return fileName
                .replace(".json", "")
                .replace("%..".toRegex(), "/")
                .replace("/", "")
                .replace("\\", "")
                .trim()
        }

        /**
         * Verify if the control file is valid
         * @return Whether the control file is valid
         */
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
