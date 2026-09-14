package com.ashmeet.hyperlauncher.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.ashmeet.hyperlauncher.R
import java.io.File

class FatalErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }
        val storageAllow = extras.getBoolean("storageAllow", false)
        val throwable = extras.getSerializable("throwable") as? Throwable
        val stackTrace = if (throwable != null) Tools.printToString(throwable) else "<null>"
        val strSavePath = extras.getString("savePath")
        val errHeader = if (storageAllow) {
            "Crash stack trace saved to $strSavePath."
        } else {
            "Storage permission is required to save crash stack trace!"
        }

        val finalLogs = "$errHeader\n\n$stackTrace"

        val composeView = ComposeView(this)
        LauncherComposeHelper.ensureViewTreeOwners(composeView)
        LauncherComposeHelper.setExitContent(
            composeView,
            getString(R.string.error_fatal),
            finalLogs,
            {
                // Sharing stack trace instead of log file here
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, finalLogs)
                startActivity(Intent.createChooser(intent, getString(R.string.main_share_logs)))
                Unit
            },
            {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("error", finalLogs)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Error copied to clipboard", Toast.LENGTH_SHORT).show()
                Unit
            },
            {
                startActivity(Intent(this, LauncherActivity::class.java))
                finish()
                Unit
            },
            {
                if (strSavePath != null) {
                    Tools.openPath(this, File(strSavePath), false)
                }
                Unit
            }
        )
        setContentView(composeView)
    }

    companion object {
        @JvmStatic
        fun showError(ctx: Context, savePath: String?, storageAllow: Boolean, th: Throwable?) {
            val fatalErrorIntent = Intent(ctx, FatalErrorActivity::class.java)
            fatalErrorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            fatalErrorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            fatalErrorIntent.putExtra("throwable", th)
            fatalErrorIntent.putExtra("savePath", savePath)
            fatalErrorIntent.putExtra("storageAllow", storageAllow)
            ctx.startActivity(fatalErrorIntent)
        }
    }
}
