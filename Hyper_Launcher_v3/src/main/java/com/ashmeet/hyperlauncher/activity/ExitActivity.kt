//package name is changed cuz of some JNI issues, DONT TOUCH IT!
package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.ashmeet.hyperlauncher.R
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

@Keep
class ExitActivity : AppCompatActivity() {

    @SuppressLint("StringFormatInvalid") // invalid on some translations but valid on most, cant fix that atm
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var code = -1
        var isSignal = false
        intent.extras?.let {
            code = it.getInt("code", -1)
            isSignal = it.getBoolean("isSignal", false)
        }

        val title = if (isSignal) getString(R.string.mcn_abort_title) else getString(R.string.mcn_exit_title, code)

        val logs = try {
            Tools.read(File(Tools.DIR_GAME_HOME, "latestlog.txt"))
        } catch (e: IOException) {
            "Failed to read logs: ${e.message}"
        }

        val composeView = ComposeView(this)
        LauncherComposeHelper.ensureViewTreeOwners(composeView)
        LauncherComposeHelper.setExitContent(
            composeView,
            title,
            logs,
            onShareClick = { Tools.shareLog(this) },
            onCopyClick = {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("logs", logs)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onRestartClick = { Tools.restartLauncherActivity(this) },
            onOpenCrashReport = { path -> Tools.openPath(this, File(path), false) }
        )
        setContentView(composeView)
    }

    companion object {
        @JvmStatic
        @Suppress("unused") // used by native jre_launcher_new
        fun showExitMessage(ctx: Context?, code: Int, isSignal: Boolean) {
            if (!isSignal && code == 0) {
                ctx?.let { Tools.restartLauncherActivity(it) }
                exitProcess(0)
            }

            val lock = Any()
            Tools.runOnUiThread {
                val i = Intent(ctx, ExitActivity::class.java)
                i.putExtra("code", code)
                i.putExtra("isSignal", isSignal)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx?.startActivity(i)
                synchronized(lock) {
                    (lock as Object).notify()
                }
            }
            synchronized(lock) {
                try {
                    (lock as Object).wait()
                } catch (e: InterruptedException) {
                    Log.e("ExitActivity", "Waiting on lock failed: $e")
                }
            }
            exitProcess(0)
        }
    }
}
