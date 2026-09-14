package net.kdt.pojavlaunch.awt

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.ashmeet.hyperlauncher.activity.BaseActivity
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.ashmeet.hyperlauncher.screens.activity.JavaGuiLauncherScreen
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.Tools
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.*
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput
import net.kdt.pojavlaunch.game.platform.Platform
import net.kdt.pojavlaunch.game.platform.backend.AWTBackend
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.utils.MathUtils
import net.kdt.pojavlaunch.utils.SingleTapConfirm
import net.kdt.pojavlaunch.utils.jre.JavaRunner
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.jar.JarFile

class AWTActivity : BaseActivity() {

    private var mIsTrusted = false
    private lateinit var mGestureDetector: GestureDetector
    
    // States for Compose
    private var isMouseEnabled by mutableStateOf(false)
    private var isLoggerVisible by mutableStateOf(false)
    private var mousePosition by mutableStateOf(Offset.Zero)

    private var prevX = 0f
    private var prevY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val latestLogFile = File(Tools.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw IOException("Failed to create a new log file")
            Logger.begin(latestLogFile.absolutePath)
        } catch (e: IOException) {
            Tools.showError(this, e, true)
        }

        CallbackBridge.windowWidth = AWTView.AWT_CANVAS_WIDTH
        CallbackBridge.windowHeight = AWTView.AWT_CANVAS_HEIGHT

        Platform.PLATFORM = AWTBackend()
        Platform.initializeMinimal(applicationContext)

        mGestureDetector = GestureDetector(this, SingleTapConfirm())

        setContent {
            JavaGuiLauncherScreen(
                onForceClose = { Tools.dialogForceClose(this) },
                onOpenLogOutput = { isLoggerVisible = true },
                onToggleVirtualMouse = { enabled ->
                    isMouseEnabled = enabled
                    Toast.makeText(this, 
                        if (enabled) R.string.control_mouseon else R.string.control_mouseoff,
                        Toast.LENGTH_SHORT).show()
                },
                onToggleKeyboard = {
                    findViewById<TouchCharInput>(R.id.awt_touch_char)?.switchKeyboardState()
                },
                onPerformCopy = { performCopy() },
                onPerformPaste = { performPaste() },
                onMouseEvent = { button, isDown ->
                    Platform.PLATFORM.sendMouseEvent(button, if (isDown) 1 else 0, CallbackBridge.getCurrentMods())
                },
                onMoveWindow = { dx, dy ->
                    AWTBridge.nativeMoveWindow(dx, dy)
                },
                modifier = Modifier.fillMaxSize(),
                isMouseEnabled = isMouseEnabled,
                isLoggerVisible = isLoggerVisible,
                mousePosition = mousePosition,
                onAwtViewTouch = { v, event -> handleAwtViewTouch(v, event) },
                onTouchpadTouch = { v, event -> handleTouchpadTouch(v, event) }
            )
        }

        intent.extras?.let { extras ->
            mIsTrusted = extras.getBoolean("trusted", false)
            val javaArgs = extras.getStringArrayList("javaArgs")
            val resourceUri = extras.getParcelable<Uri>("modUri")
            val jarPath = extras.getString("modPath")

            if (jarPath != null) {
                startModInstaller(File(jarPath), javaArgs)
            } else {
                PojavApplication.sExecutorService.execute { startModInstallerWithUri(resourceUri, javaArgs) }
            }
            if (extras.getBoolean("openLogOutput", false)) isLoggerVisible = true
        } ?: finish()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Tools.dialogForceClose(this@AWTActivity)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        window.decorView.systemUiVisibility = uiOptions
    }

    private fun handleTouchpadTouch(v: View, event: MotionEvent): Boolean {
        val action = event.actionMasked
        val x = event.x
        val y = event.y
        
        var mouseX = mousePosition.x
        var mouseY = mousePosition.y

        if (mGestureDetector.onTouchEvent(event)) {
            sendScaledMousePosition(mouseX, mouseY)
            CallbackBridge.performClick(MotionEvent.BUTTON_PRIMARY)
        } else {
            if (action == MotionEvent.ACTION_MOVE) {
                mouseX = (mouseX + x - prevX).coerceIn(0f, v.width.toFloat())
                mouseY = (mouseY + y - prevY).coerceIn(0f, v.height.toFloat())
                mousePosition = Offset(mouseX, mouseY)
                sendScaledMousePosition(mouseX, mouseY)
            }
        }

        prevX = x
        prevY = y
        return true
    }

    private fun handleAwtViewTouch(v: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (mGestureDetector.onTouchEvent(event)) {
            sendScaledMousePosition(x + v.x, y)
            CallbackBridge.performClick(MotionEvent.BUTTON_PRIMARY)
            return true
        }

        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            sendScaledMousePosition(x + v.x, y)
        }
        return true
    }

    private fun sendScaledMousePosition(x: Float, y: Float) {
        val textureView = findViewById<View>(R.id.installmod_surfaceview) ?: return
        val clampedX = x.coerceIn(0f, textureView.width.toFloat())
        val clampedY = y.coerceIn(0f, textureView.height.toFloat())

        Platform.cursorX = MathUtils.map(clampedX, 0f, textureView.width.toFloat(), 0f, CallbackBridge.windowWidth.toFloat()).toDouble()
        Platform.cursorY = MathUtils.map(clampedY, 0f, textureView.height.toFloat(), 0f, CallbackBridge.windowHeight.toFloat()).toDouble()
        Platform.PLATFORM.sendMousePosition()
    }

    private fun performCopy() {
        CallbackBridge.setModifiers(KeyEvent.KEYCODE_CTRL_LEFT, true)
        Platform.PLATFORM.sendKeyEvent(KeyEvent.KEYCODE_CTRL_LEFT, 1, CallbackBridge.getCurrentMods())
        CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_C)
        Platform.PLATFORM.sendKeyEvent(KeyEvent.KEYCODE_CTRL_LEFT, 0, CallbackBridge.getCurrentMods())
        CallbackBridge.setModifiers(KeyEvent.KEYCODE_CTRL_LEFT, false)
    }

    private fun performPaste() {
        CallbackBridge.setModifiers(KeyEvent.KEYCODE_CTRL_LEFT, true)
        Platform.PLATFORM.sendKeyEvent(KeyEvent.KEYCODE_CTRL_LEFT, 1, CallbackBridge.getCurrentMods())
        CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_V)
        Platform.PLATFORM.sendKeyEvent(KeyEvent.KEYCODE_CTRL_LEFT, 0, CallbackBridge.getCurrentMods())
        CallbackBridge.setModifiers(KeyEvent.KEYCODE_CTRL_LEFT, false)
    }

    private fun startModInstallerWithUri(uri: Uri?, javaArgs: List<String>?) {
        if (uri == null) {
            startModInstaller(null, javaArgs)
            return
        }
        try {
            val cacheFile = File(cacheDir, "mod-installer-temp")
            contentResolver.openInputStream(uri)?.use { contentStream ->
                FileOutputStream(cacheFile).use { fileOutputStream ->
                    IOUtils.copy(contentStream, fileOutputStream)
                }
            }
            startModInstaller(cacheFile, javaArgs)
        } catch (e: IOException) {
            Tools.showError(this, e, true)
        }
    }

    private fun startModInstaller(modFile: File?, javaArgs: List<String>?) {
        Thread({ runModInstaller(modFile, javaArgs) }, "JREMainThread").start()
    }

    private fun runModInstaller(modFile: File?, javaArgs: List<String>?) {
        if (modFile == null) return
        val props = try {
            JarFileProperties.read(modFile)
        } catch (e: IOException) {
            Log.i("JavaGUILauncherActivity", "Failed to read JarFileProperties", e)
            null
        }
        if (props == null) {
            finalErrorDialog(getString(R.string.execute_jar_failed_to_read_file))
            return
        }
        val selectedRuntime = selectRuntime(props.minJavaVersion) ?: return
        launchJavaRuntime(selectedRuntime, javaArgs, modFile, props.mainClass)
    }

    private fun selectRuntime(javaVersion: Int): Runtime? {
        if (javaVersion == -1) {
            finalErrorDialog(getString(R.string.execute_jar_failed_to_read_file))
            return null
        }
        val nearestRuntime = MultiRTUtils.getNearestJreName(javaVersion)
        if (nearestRuntime == null) {
            finalErrorDialog(getString(R.string.multirt_nocompatiblert, javaVersion))
            return null
        }
        return MultiRTUtils.forceReread(nearestRuntime)
    }

    private fun launchJavaRuntime(runtime: Runtime, javaArgs: List<String>?, modFile: File, mainClass: String) {
        JREUtils.redirectAndPrintJRELog()
        try {
            val javaArgList = mutableListOf<String>()
            javaArgs?.let { javaArgList.addAll(it) }

            if (LauncherPreferences.PREF_JAVA_SANDBOX && !mIsTrusted) {
                javaArgList.add(0, "-Xbootclasspath/a:" + Tools.DIR_DATA + "/security/pro-grade.jar")
                javaArgList.add(1, "-Djava.security.manager=net.sourceforge.prograde.sm.ProGradeJSM")
                javaArgList.add(2, "-Djava.security.policy=" + Tools.DIR_DATA + "/security/java_sandbox.policy")
            }

            Logger.appendToLog("Info: Java arguments: $javaArgList")

            JavaRunner.nativeSetupExit(applicationContext)
            JavaRunner.startJvm(runtime, javaArgList, listOf(modFile.absolutePath), mainClass, emptyList())

            JREUtils.launchJavaVM(this, runtime, null, javaArgList, LauncherPreferences.PREF_CUSTOM_JAVA_ARGS)
        } catch (th: Throwable) {
            Tools.showError(this, th, true)
        }
    }

    private fun finalErrorDialog(msg: CharSequence) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(R.string.global_error)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }

    private class JarFileProperties(val mainClass: String, val minJavaVersion: Int) {
        companion object {
            fun read(file: File): JarFileProperties? {
                JarFile(file).use { jarFile ->
                    val manifest = jarFile.manifest ?: return null
                    val mainAttrs = manifest.mainAttributes ?: return null
                    val mainClass = mainAttrs.getValue("Main-Class") ?: return null
                    val javaVersion = getJavaVersion(jarFile, mainClass)
                    return JarFileProperties(mainClass, javaVersion)
                }
            }

            private fun getJavaVersion(jarFile: JarFile, mainClass: String): Int {
                val path = mainClass.trim().replace('.', '/') + ".class"
                val entry = jarFile.getEntry(path) ?: return -1
                val bytes = ByteArray(8)
                jarFile.getInputStream(entry).use { it.read(bytes) }
                val buffer = ByteBuffer.wrap(bytes)
                if (buffer.int != 0xCAFEBABE.toInt()) return -1
                buffer.short // minor
                val major = buffer.short
                return classVersionToJavaVersion(major.toInt())
            }

            private fun classVersionToJavaVersion(majorVersion: Int): Int {
                return if (majorVersion < 46) 2 else majorVersion - 44
            }
        }
    }
}
