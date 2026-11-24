package dev.horizon.android

import android.app.NativeActivity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ashmeet.hyperbackend.CallbackBridge
import com.ashmeet.hyperbackend.Logger
import com.ashmeet.hyperbackend.customcontrols.ControlButtonMenuListener
import com.ashmeet.hyperbackend.customcontrols.ControlData
import com.ashmeet.hyperbackend.customcontrols.ControlDrawerData
import com.ashmeet.hyperbackend.customcontrols.ControlJoystickData
import com.ashmeet.hyperbackend.customcontrols.ControlLayout
import com.ashmeet.hyperbackend.customcontrols.CustomControls
import com.ashmeet.hyperbackend.customcontrols.EditorExitable
import com.ashmeet.hyperbackend.customcontrols.buttons.ControlInterface
import com.ashmeet.hyperbackend.customcontrols.keyboard.TouchCharInput
import com.ashmeet.hyperbackend.customcontrols.mouse.GyroControl
import com.ashmeet.hyperbackend.customcontrols.mouse.HotbarView
import com.ashmeet.hyperbackend.game.GameCursorView
import com.ashmeet.hyperbackend.game.GameView
import com.ashmeet.hyperbackend.game.platform.Platform
import com.ashmeet.hyperbackend.instances.Instance
import com.ashmeet.hyperbackend.instances.Instances
import com.ashmeet.hyperbackend.lifecycle.ContextExecutor
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog
import com.ashmeet.hyperlauncher.fragments.dialog.QuickSettingSideDialog
import com.ashmeet.hyperlauncher.screens.game.LoggerView
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_ENABLE_GYRO
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_MOUSESCALE
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_SUSTAINED_PERFORMANCE
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_USE_ALTERNATE_SURFACE
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.SideDialogUtils
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.ashmeet.hyperlauncher.R
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference

@Suppress("ConvertToExplicitBackingFields")
open class HorizonActivity : NativeActivity(),
    ControlButtonMenuListener,
    EditorExitable,
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val mViewModelStore = ViewModelStore()
    private val mSavedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = mViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    companion object {
        private const val TAG = "HorizonActivity"
        @JvmField
        var touchCharInput: TouchCharInput? = null
        @JvmField
        var weakCursor: WeakReference<GameCursorView>? = null

        @JvmStatic
        var isActivityActive: Boolean = false
            private set

        @JvmStatic
        var mForcedPanningHeight: Int = 0

        @JvmStatic
        var mImeHeight: Int = 0
    }

    private var launcherGLView: GameView? = null
    private var loggerView: LoggerView? = null
    private var mGyroControl: GyroControl? = null
    private var mControlLayout: ControlLayout? = null
    private var mHotbarView: HotbarView? = null
    private var mMainComposeView: ComposeView? = null

    var instance: Instance? = null
    private var mQuickSettingSideDialog: QuickSettingSideDialog? = null
    private var mEditControlSideDialog: EditControlSideDialog? = null
    private var mDrawerController: LauncherComposeHelper.DrawerController? = null
    var isInEditor: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        loadNativeLibrary()
        mSavedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate(savedInstanceState)

        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeViewModelStoreOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)

        instance = Instances.loadSelectedInstance()
        if (instance == null) {
            Toast.makeText(this, R.string.instance_dir_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Platform.setPlatformLibrary(com.ashmeet.hyperbackend.game.platform.backend.HorizonBackend())
        Platform.grabStateChanged(HorizonInput.isGrabbed)
        setupWindowFlags()
        initLayout()

        launcherGLView?.let { Platform.initialize(this, it) }
        mGyroControl = GyroControl(this)

        if (PREF_USE_ALTERNATE_SURFACE) {
            window.setBackgroundDrawable(null)
        } else {
            window.setBackgroundDrawable(Color.BLACK.toDrawable())
        }

        window.setSustainedPerformanceMode(PREF_SUSTAINED_PERFORMANCE)

        val androidCompat = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
        if (androidCompat) {
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val glView = launcherGLView
            val surface = glView?.mSurface
            val cursor = glView?.mCursorView
            if (surface == null || cursor == null) return@setOnApplyWindowInsetsListener insets

            val animSurface = surface.animate().setDuration(100)
            val animCursor = cursor.animate().setDuration(100)

            if (!insets.isVisible(WindowInsetsCompat.Type.ime())) {
                animSurface.translationY(0f).start()
                animCursor.translationY(0f).start()
                mImeHeight = 0
                updateImeInset(0)
                if (androidCompat) {
                    view.postDelayed({
                        val controller = WindowCompat.getInsetsController(window, view)
                        controller.let { c ->
                            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            c.hide(WindowInsetsCompat.Type.systemBars())
                        }
                    }, 150)
                }
                return@setOnApplyWindowInsetsListener insets
            }

            if (mForcedPanningHeight == 0 && !LauncherPreferences.PREF_KEYBOARD_AUTOPANNING) {
                return@setOnApplyWindowInsetsListener insets
            }

            mImeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            updateImeInset(mImeHeight)

            val translationY = if (mForcedPanningHeight == 0) {
                HyperUtils.getTranslationFromCursorY(
                    (Platform.cursorY * glView.cursorRatioY + 100).toInt(),
                    glView.height,
                    mImeHeight,
                    0
                )
            } else {
                if (mForcedPanningHeight == -1) mImeHeight else Math.clamp((mImeHeight - mForcedPanningHeight).toLong(), 0, mImeHeight.toLong()).toInt()
            }

            animSurface.translationY((-translationY).toFloat()).start()
            animCursor.translationY((-translationY).toFloat()).start()
            return@setOnApplyWindowInsetsListener insets
        }

        mControlLayout?.setModifiable(false)
        ContextExecutor.setActivity(this)
    }

    protected fun initLayout() {
        bindValues()
        mControlLayout?.setMenuListener(this)

        launcherGLView?.mCursorView?.setCursorScale(PREF_MOUSESCALE)
        launcherGLView?.mCursorView?.let { weakCursor = WeakReference(it) }

        mMainComposeView = ComposeView(this).apply {
            LauncherComposeHelper.ensureViewTreeOwners(this)
            clipChildren = false
            clipToPadding = false
        }
        setContentView(mMainComposeView!!)

        mMainComposeView?.let { composeView ->
            LauncherComposeHelper.setBaseMainContent(
                composeView,
                isInEditor,
                mControlLayout,
                loggerView!!,
                launcherGLView,
                true,
                { },
                { controller -> mDrawerController = controller },
                { action -> onAction(action) }
            )
        }

        mControlLayout?.setOnControlEditListener(object : ControlLayout.OnControlEditListener {
            override fun onEditControl(button: ControlInterface) {
                if (mEditControlSideDialog == null) {
                    mEditControlSideDialog = EditControlSideDialog()
                }
                mEditControlSideDialog?.setCurrentlyEditedButton(button)
                val btnView = button.controlView
                val isLeft = btnView.x + btnView.width / 2f < (mControlLayout?.width ?: 0) / 2f
                SideDialogUtils.show(mEditControlSideDialog!!, isLeft)
            }

            override fun onDisappearLayer(): Boolean {
                return mEditControlSideDialog?.disappearLayer() == true
            }
        })

        try {
            val latestLogFile = File(HyperUtils.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile()) {
                throw IOException("Failed to create a new log file")
            }
            Logger.begin(latestLogFile.absolutePath)
            Logger.appendToLog("--------- Starting Horizon instance with HorizonActivity!")
            title = "HorizonLauncher (${instance?.versionId ?: "horizon"})"
        } catch (e: Throwable) {
            HyperUtils.showError(this, e, true)
        }
    }

    private fun loadControls() {
        try {
            instance?.launchControls?.let { mControlLayout?.loadLayout(it) }
        } catch (e: IOException) {
            try {
                Log.w(TAG, "Unable to load the control file, loading default now", e)
                mControlLayout?.loadLayout(HyperUtils.CTRLDEF_FILE)
            } catch (ioException: IOException) {
                HyperUtils.showError(this, ioException)
            }
        } catch (th: Throwable) {
            HyperUtils.showError(this, th)
        }
        mControlLayout?.toggleControlVisible()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mControlLayout?.post {
            HyperUtils.getDisplayMetrics(this)
            loadControls()
        }
    }

    private fun bindValues() {
        val ctx = this
        mControlLayout = ControlLayout(ctx).apply {
            id = R.id.main_control_layout
            clipChildren = false
            clipToPadding = false
        }

        launcherGLView = GameView(ctx).apply {
            id = R.id.main_game_render_view
            clipChildren = false
            clipToPadding = false
        }

        val cursorView = GameCursorView(ctx).apply {
            id = R.id.main_cursorview
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            isFocusable = false
            translationZ = HyperUtils.dpToPx(1f)
        }
        launcherGLView?.addView(cursorView)
        launcherGLView?.mCursorView = cursorView

        loggerView = LoggerView(ctx).apply {
            id = R.id.mainLoggerView
            visibility = View.GONE
        }

        touchCharInput = TouchCharInput(ctx).apply {
            id = R.id.mainTouchCharInput
            layoutParams = FrameLayout.LayoutParams(HyperUtils.dpToPx(1f).toInt(), HyperUtils.dpToPx(1f).toInt())
        }

        var imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
        imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        touchCharInput?.imeOptions = imeOptions
        touchCharInput?.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT

        mHotbarView = HotbarView(ctx).apply {
            id = R.id.hotbar_view
            layoutParams = FrameLayout.LayoutParams(0, 0)
        }

        mControlLayout?.addView(launcherGLView)
        mControlLayout?.addView(touchCharInput)
        mControlLayout?.addView(mHotbarView)

        LauncherComposeHelper.setLoadingText(getString(R.string.loading_screen_title))
        LauncherComposeHelper.setLoadingWarning(getString(R.string.loading_screen_warning))
        LauncherComposeHelper.setLoadingVisible(true)

        Platform.setCursorImplementor(cursorView)
    }

    private fun onAction(action: Int) {
        if (isInEditor) {
            when (action) {
                0 -> mControlLayout?.addControlButton(ControlData("New"))
                1 -> mControlLayout?.addDrawer(ControlDrawerData())
                2 -> mControlLayout?.addJoystickButton(ControlJoystickData())
                3 -> mControlLayout?.openLoadDialog()
                4 -> mControlLayout?.openSaveDialog(this)
                5 -> mControlLayout?.openSetDefaultDialog()
                6 -> mControlLayout?.openExitDialog(this)
            }
        } else {
            when (action) {
                0 -> HyperUtils.dialogForceClose(this)
                1 -> loggerView?.visibility = View.VISIBLE
                2 -> HyperUtils.dialogSendCustomKey(this)
                3 -> openQuickSettings()
                4 -> openCustomControls()
            }
        }
    }

    private fun openCustomControls() {
        isInEditor = true
        mMainComposeView?.let { composeView ->
            LauncherComposeHelper.setBaseMainContent(
                composeView,
                isInEditor,
                mControlLayout,
                loggerView!!,
                launcherGLView,
                true,
                { },
                { controller -> mDrawerController = controller },
                { action -> onAction(action) }
            )
        }
        mControlLayout?.setModifiable(true)
    }

    private fun openQuickSettings() {
        if (mQuickSettingSideDialog == null) {
            mQuickSettingSideDialog = object : QuickSettingSideDialog() {
                override fun onResolutionChanged() {
                    launcherGLView?.refreshSize()
                    mHotbarView?.onResolutionChanged()
                }

                override fun onGyroStateChanged() {
                    mGyroControl?.updateOrientation()
                    if (PREF_ENABLE_GYRO) mGyroControl?.enable()
                    else mGyroControl?.disable()
                }

                override fun onButtonTransparencyChanged() {
                    mControlLayout?.updateButtonOpacity()
                }

                override fun onShowFpsChanged() {}

                override fun onForceClose() {
                    HyperUtils.dialogForceClose(this@HorizonActivity)
                }

                override fun onViewOutput() {
                    loggerView?.visibility = View.VISIBLE
                }

                override fun onCustomKey() {
                    HyperUtils.dialogSendCustomKey(this@HorizonActivity)
                }
            }
        }
        SideDialogUtils.show(mQuickSettingSideDialog!!, true)
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
        ContextExecutor.setActivity(this)
        if (PREF_ENABLE_GYRO) mGyroControl?.enable()
        Platform.PLATFORM.setHovered(true)
        Platform.setPlatformLibrary(com.ashmeet.hyperbackend.game.platform.backend.HorizonBackend())
        try {
            updateMouseSettings(PREF_MOUSESCALE, true)
            Log.d(TAG, "Engine State: ${getEngineDiagnostics()}")
        } catch (_: Throwable) {}
        applyImmersiveFlags()
    }

    override fun onPause() {
        isActivityActive = false
        ContextExecutor.clearActivity()
        mGyroControl?.disable()
        if (Platform.isGrabbing()) {
            CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE)
            try {
                HorizonInput.key(com.ashmeet.hyperbackend.LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt(), true)
                HorizonInput.key(com.ashmeet.hyperbackend.LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt(), false)
            } catch (_: Throwable) {}
        }
        mQuickSettingSideDialog?.cancel()
        Platform.PLATFORM.setHovered(false)
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        Platform.PLATFORM.setVisible(true)
    }

    override fun onStop() {
        Platform.PLATFORM.setVisible(false)
        super.onStop()
    }

    override fun onDestroy() {
        isActivityActive = false
        Platform.PLATFORM.setVisible(false)
        launcherGLView?.onSurfaceDestroyed()
        ContextExecutor.clearActivity()
        super.onDestroy()
    }

    override fun onPostResume() {
        super.onPostResume()
        launcherGLView?.let { glView ->
            HyperUtils.MAIN_HANDLER.postDelayed({ glView.refreshSize() }, 500)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mGyroControl?.updateOrientation()
        mControlLayout?.requestLayout()
        mControlLayout?.post {
            launcherGLView?.refreshSize()
            mControlLayout?.refreshControlButtonPositions()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isInEditor) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                mControlLayout?.askToExit(this)
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        if (HorizonInput.isKeybindCaptureActive) {
            val glfwKey = com.ashmeet.hyperbackend.game.platform.backend.HorizonBackend.toGlfwKeycode(event.keyCode)
            HorizonInput.key(glfwKey, event.action == KeyEvent.ACTION_DOWN)
            return true
        }

        if (event.unicodeChar != 0 && event.action == KeyEvent.ACTION_DOWN && !event.isSystem) {
            sendTextInput(event.unicodeChar.toChar().toString())
        }

        val handled = launcherGLView?.processKeyEvent(event) == true
        if (!handled) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && touchCharInput?.isEnabled != true) {
                if (event.action == KeyEvent.ACTION_UP) {
                    CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE)
                    try {
                        HorizonInput.key(com.ashmeet.hyperbackend.LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt(), true)
                        HorizonInput.key(com.ashmeet.hyperbackend.LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt(), false)
                    } catch (_: Throwable) {}
                }
                return true
            }
        }
        return handled
    }

    override fun dispatchTrackballEvent(ev: MotionEvent): Boolean {
        val captured = checkCaptureDispatchConditions(ev)
        return if (captured) {
            launcherGLView?.dispatchCapturedPointerEvent(ev) == true
        } else {
            super.dispatchTrackballEvent(ev)
        }
    }

    private fun checkCaptureDispatchConditions(event: MotionEvent): Boolean {
        val src = event.source
        return (src and InputDevice.SOURCE_MOUSE_RELATIVE) != 0 || (src and InputDevice.SOURCE_MOUSE) != 0
    }

    override fun onClickedMenu() {
        mDrawerController?.open()
    }

    override fun exitEditor() {
        try {
            mControlLayout?.loadLayout(null as CustomControls?)
            mControlLayout?.setModifiable(false)
            System.gc()
            instance?.launchControls?.let { mControlLayout?.loadLayout(it) }
        } catch (e: Exception) {
            HyperUtils.showError(this, e)
        }

        isInEditor = false
        mMainComposeView?.let { composeView ->
            LauncherComposeHelper.setBaseMainContent(
                composeView,
                isInEditor,
                mControlLayout,
                loggerView!!,
                launcherGLView,
                true,
                { },
                { controller -> mDrawerController = controller },
                { action -> onAction(action) }
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveFlags()
    }

    private fun loadNativeLibrary() {
        try {
            System.loadLibrary("horizon_android")
            Log.i(TAG, "Successfully loaded horizon_android native library. Info: ${HorizonInput.graphicsInfo()} [Renderer: ${HorizonInput.rendererState()}, Vulkan: ${HorizonInput.vulkanDriverState()}]")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load horizon_android library", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading horizon_android library", e)
        }
    }

    private fun setupWindowFlags() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (!isInEditor && HorizonInput.sLoaded) {
            val action = ev.actionMasked
            val isRelative = checkCaptureDispatchConditions(ev) || HorizonInput.isGrabbed
            when (action) {
                MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                    if (isRelative) {
                        HorizonInput.cursorSilently(ev.x, ev.y)
                    } else {
                        HorizonInput.cursor(ev.x, ev.y)
                    }
                    HorizonInput.cursorShape()
                    HorizonInput.menuCursor()
                }
                MotionEvent.ACTION_SCROLL -> {
                    val vScroll = ev.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    val hScroll = ev.getAxisValue(MotionEvent.AXIS_HSCROLL)
                    HorizonInput.scroll(hScroll, vScroll)
                }
                MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE -> {
                    val pressed = action == MotionEvent.ACTION_BUTTON_PRESS
                    val glfwBtn = when (ev.actionButton) {
                        MotionEvent.BUTTON_PRIMARY -> 0
                        MotionEvent.BUTTON_SECONDARY -> 1
                        MotionEvent.BUTTON_TERTIARY -> 2
                        else -> 0
                    }
                    HorizonInput.mouseButton(glfwBtn, pressed)
                    HorizonInput.takeMenuMouseButton()
                }
            }
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInEditor && HorizonInput.sLoaded) {
            val action = event.actionMasked
            HorizonInput.cursor(event.x, event.y)
            if (action == MotionEvent.ACTION_DOWN) {
                HorizonInput.mouseButton(0, true)
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                HorizonInput.mouseButton(0, false)
            }
        }
        return super.onTouchEvent(event)
    }

    fun updateImeInset(bottomInsetPx: Int) {
        HorizonInput.setImeViewportBottomInset(bottomInsetPx)
    }

    fun sendTextInput(text: String?) {
        HorizonInput.text(text)
    }

    fun updateMouseSettings(mouseScale: Float, keyRepeat: Boolean) {
        HorizonInput.setMouseScale(mouseScale)
        HorizonInput.setMenuMouseScale(mouseScale)
        HorizonInput.setKeyRepeat(keyRepeat)
        HorizonInput.setMenuMouseCapture(true, -1f, -1f)
        HorizonInput.setMenuMouseClicksToHost(true)
    }

    fun getEngineDiagnostics(): String {
        return "FPS: ${HorizonInput.fps()}, GFX: ${HorizonInput.graphicsInfo()}, Renderer: ${HorizonInput.rendererState()}, Vulkan: ${HorizonInput.vulkanDriverState()}"
    }

    @Suppress("KotlinJniMissingFunction")
    external fun nativeDocumentResult(requestCode: Int, resultCode: Int, data: String?)

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            nativeDocumentResult(requestCode, resultCode, data?.dataString ?: data?.data?.toString())
        } catch (_: Throwable) {}
    }

    private fun applyImmersiveFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
}
