package dev.horizon.android

import android.app.NativeActivity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
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
import com.ashmeet.hyperbackend.customcontrols.handleview.DrawerPullButton
import com.ashmeet.hyperbackend.customcontrols.keyboard.TouchCharInput
import com.ashmeet.hyperbackend.customcontrols.mouse.GyroControl
import com.ashmeet.hyperbackend.customcontrols.mouse.HotbarView
import com.ashmeet.hyperbackend.customcontrols.mouse.PointerTracker
import com.ashmeet.hyperbackend.game.GameActivity
import com.ashmeet.hyperbackend.game.GameView
import com.ashmeet.hyperbackend.game.platform.Platform
import com.ashmeet.hyperbackend.game.platform.backend.HorizonBackend
import com.ashmeet.hyperbackend.instances.Instances
import com.ashmeet.hyperbackend.lifecycle.ContextExecutor
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog
import com.ashmeet.hyperlauncher.fragments.dialog.QuickSettingSideDialog
import com.ashmeet.hyperlauncher.fragments.dialog.SideDialogView
import com.ashmeet.hyperlauncher.screens.game.LoggerView
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_ENABLE_GYRO
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.KeycodeUtils
import com.ashmeet.hyperlauncher.utils.SideDialogUtils
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.ashmeet.hyperlauncher.R

@Keep
open class HorizonActivity : NativeActivity(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    ControlButtonMenuListener,
    EditorExitable {

    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private val lifecycleRegistry = LifecycleRegistry(this)

    @Suppress("ConvertToExplicitBackingFields")
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var loggerView: LoggerView? = null

    @JvmField
    var controlLayout: ControlLayout? = null
    private var touchCharInput: TouchCharInput? = null
    private var hotbarView: HotbarView? = null
    private var gyroControl: GyroControl? = null
    private var mainComposeView: ComposeView? = null
    private var overlayView: View? = null
    private var drawerController: LauncherComposeHelper.DrawerController? = null
    private var quickSettingSideDialog: QuickSettingSideDialog? = null
    private var editControlSideDialog: EditControlSideDialog? = null
    private var isInEditor = false

    private var cursorTouchPointerId = MotionEvent.INVALID_POINTER_ID
    private val activeCameraPointerIds = mutableSetOf<Int>()
    private val instance = Instances.loadSelectedInstance()

    private val pointerTracker = PointerTracker()
    private var isDraggingMouse = false
    private var mLastGrabState = false
    private val grabStateRunnable = object : Runnable {
        override fun run() {
            weakCursor?.get()?.onCursorChanged()
            controlLayout?.postDelayed(this, 100)
        }
    }

    companion object {
        const val EXTRA_ENGINE_HOME = "dev.horizon.android.extra.ENGINE_HOME"
        const val EXTRA_GAME_DIR = "dev.horizon.android.extra.GAME_DIR"
        const val EXTRA_SESSION_FILE = "dev.horizon.android.extra.SESSION_FILE"
        const val EXTRA_QUICK_PLAY_WORLD = "dev.horizon.android.extra.QUICK_PLAY_WORLD"
        const val EXTRA_QUICK_PLAY_SERVER = "dev.horizon.android.extra.QUICK_PLAY_SERVER"
        const val EXTRA_HOST_CONTROLS = "dev.horizon.android.extra.HOST_CONTROLS"
        const val EXTRA_RENDERER = "dev.horizon.android.extra.RENDERER"
        const val EXTRA_VULKAN_DRIVER_NAME = "dev.horizon.android.extra.VULKAN_DRIVER_NAME"
        const val EXTRA_VULKAN_DRIVER_DIR = "dev.horizon.android.extra.VULKAN_DRIVER_DIR"
        const val EXTRA_VULKAN_DRIVER_LIBRARY = "dev.horizon.android.extra.VULKAN_DRIVER_LIBRARY"
        const val EXTRA_VULKAN_DRIVER_TURNIP = "dev.horizon.android.extra.VULKAN_DRIVER_TURNIP"

        const val EXTRA_ACCOUNT_NAME = "dev.horizon.android.extra.ACCOUNT_NAME"
        const val EXTRA_ACCOUNT_UUID = "dev.horizon.android.extra.ACCOUNT_UUID"
        const val EXTRA_ACCOUNT_TOKEN = "dev.horizon.android.extra.ACCOUNT_TOKEN"
        const val EXTRA_ACCOUNT_TYPE = "dev.horizon.android.extra.ACCOUNT_TYPE"
        const val EXTRA_SKIN_PATH = "dev.horizon.android.extra.SKIN_PATH"
        const val EXTRA_CAPE_PATH = "dev.horizon.android.extra.CAPE_PATH"

        private const val ENV_HOME = "HYPER_HORIZON_HOME"
        private const val ENV_GAME_DIR = "HYPER_HORIZON_GAME_DIR"
        private const val ENV_SESSION = "HYPER_HORIZON_SESSION"
        private const val ENV_QUICK_PLAY_WORLD = "HYPER_HORIZON_QUICK_PLAY_WORLD"
        private const val ENV_QUICK_PLAY_SERVER = "HYPER_HORIZON_QUICK_PLAY_SERVER"
        private const val ENV_AUTOPLAY = "HYPER_HORIZON_AUTOPLAY"
        private const val ENV_HOST_CONTROLS = "HYPER_HORIZON_HOST_CONTROLS"
        private const val ENV_RENDERER = "HYPER_HORIZON_RENDERER"
        private const val ENV_VULKAN_DRIVER = "HYPER_HORIZON_VULKAN_DRIVER"
        private const val ENV_LOCK_DIR = "HYPER_HORIZON_LOCK_DIR"
        private const val ENV_ACCOUNT_NAME = "HYPER_HORIZON_ACCOUNT_NAME"
        private const val ENV_ACCOUNT_UUID = "HYPER_HORIZON_ACCOUNT_UUID"
        private const val ENV_ACCOUNT_TOKEN = "HYPER_HORIZON_ACCOUNT_TOKEN"
        private const val ENV_ACCOUNT_TYPE = "HYPER_HORIZON_ACCOUNT_TYPE"
        private const val ENV_SKIN_PATH = "HYPER_HORIZON_SKIN_PATH"
        private const val ENV_CAPE_PATH = "HYPER_HORIZON_CAPE_PATH"
        private const val ENV_TELEMETRY_URL = "HORIZON_TELEMETRY_URL"
        private const val TAG = "HyperHorizon"

        @JvmField
        var touchCharInput: TouchCharInput? = null

        @JvmStatic
        var isActivityActive: Boolean = false
            private set

        @JvmStatic
        var mImeHeight: Int = 0

        private var weakCursor: java.lang.ref.WeakReference<HorizonCursorView>? = null


        @JvmStatic
        fun toggleMouse(ctx: android.content.Context) {
            val cursorView = weakCursor?.get() ?: return
            val toastString: Int
            when (cursorView.visibility) {
                View.VISIBLE -> {
                    toastString = R.string.control_mouseoff
                    cursorView.visibility = View.GONE
                }
                else -> {
                    toastString = R.string.control_mouseon
                    cursorView.visibility = View.VISIBLE
                }
            }
            if (toastString != 0) {
                Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show()
            }
        }

        init {
            if (Architecture.is64BitsDevice()) {
                try {
                    System.loadLibrary("horizon_android")
                } catch (e: Throwable) {
                    Log.e("HorizonActivity", "Failed to load libhorizon_android.so", e)
                }
            } else {
                Log.e(HyperUtils.APP_NAME, "device not supported bruh")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.enableEdgeToEdge(window)
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate(savedInstanceState)

        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeViewModelStoreOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        hideSystemUI()
        LauncherPreferences.loadPreferences(this)
        updateWindowSize()

        setupHorizonHandoffEnvironment()

        try {
            val latestLogFile = java.io.File(HyperUtils.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile()) {
                throw java.io.IOException("Failed to create a new log file")
            }
            Logger.begin(latestLogFile.absolutePath)
            Logger.appendToLog("--------- Starting Horizon Activity with Launcher Debug!")
        } catch (e: Exception) {
            Log.e("HorizonActivity", "Failed to initialize logger", e)
        }

        initLayout()
        controlLayout?.post(grabStateRunnable)

        Platform.setPlatformLibrary(HorizonBackend())
        mLastGrabState = HorizonInput.isGrabbed
        HorizonInput.setKeyRepeat(true)
        gyroControl = GyroControl(this)
        ContextExecutor.setActivity(this)

        LauncherComposeHelper.setLoadingVisible(false)
    }

    @Keep
    @Suppress("unused")
    fun onGrabStateChanged(grabbing: Boolean) {
        Platform.grabStateChanged(grabbing)
        updateOverlayFocusableState()
    }

    private fun extra(intent: Intent?, key: String): String? {
        if (intent == null) return null
        val value = intent.getStringExtra(key)
        return if (value.isNullOrBlank()) null else value.trim()
    }

    private fun setEnv(key: String, value: String?) {
        try {
            if (value == null) {
                android.system.Os.unsetenv(key)
            } else {
                android.system.Os.setenv(key, value, true)
            }
        } catch (error: Throwable) {
            Log.w("HorizonActivity", "Could not set $key", error)
        }
    }

    private fun prepareVulkanDriver(intent: Intent?): String? {
        val name = extra(intent, EXTRA_VULKAN_DRIVER_NAME)
        val dir = extra(intent, EXTRA_VULKAN_DRIVER_DIR)
        val library = extra(intent, EXTRA_VULKAN_DRIVER_LIBRARY)
        val turnip = intent?.getBooleanExtra(EXTRA_VULKAN_DRIVER_TURNIP, false) ?: false
        val custom = name != null && dir != null && library != null
                && java.io.File(library).isFile && java.io.File(dir).isDirectory
        val libraryName = if (custom) java.io.File(library).name else null
        val supportDir = applicationInfo?.nativeLibraryDir
        setEnv(ENV_VULKAN_DRIVER, if (custom) "custom" else null)
        setEnv("HYPER_USE_SYSTEM_VULKAN", if (custom) "0" else "1")
        setEnv("HYPER_LOAD_CUSTOM_VULKAN", if (custom) "1" else null)
        setEnv("DRIVER_PATH", if (custom) dir else null)
        setEnv("HYPER_CUSTOM_VULKAN_DRIVER_DIR", if (custom) dir else null)
        setEnv("HYPER_CUSTOM_VULKAN_SUPPORT_DIR", if (custom) supportDir else null)
        setEnv("HYPER_CUSTOM_VULKAN_DRIVER", if (custom) library else null)
        setEnv("HYPER_CUSTOM_VULKAN_DRIVER_NAME", libraryName)
        setEnv("HYPER_TURNIP_DRIVER_DIR", if (custom && turnip) dir else null)
        setEnv("HYPER_TURNIP_DRIVER_LIBRARY", if (custom && turnip) library else null)
        setEnv("HYPER_LOAD_TURNIP", if (custom && turnip) "1" else null)
        setEnv("HYPER_USE_CUSTOM_TURNIP", if (custom && turnip) "1" else null)
        setEnv("HYPER_VULKAN_DRIVER_REPORT", if (custom) name else null)

        val cache = cacheDir
        setEnv("TMPDIR", cache?.absolutePath)

        val aliasDir = if (custom && cache != null) java.io.File(cache, "horizon-vulkan-loader") else null
        if (aliasDir != null) {
            aliasDir.mkdirs()
            val stale = java.io.File(aliasDir, "libvulkan.so")
            if (stale.isFile && !stale.delete()) {
                Log.w(TAG, "Could not delete the previous Vulkan loader alias $stale")
            }
        }
        setEnv("HYPER_KOPPER_FORCE_NAMESPACE_VULKAN", if (aliasDir != null) "1" else null)
        setEnv("HYPER_KOPPER_VULKAN_ALIAS_DIR", aliasDir?.absolutePath)
        if (custom) Log.i(TAG, "Vulkan driver for Horizon: $name ($library)")
        return if (custom) name else null
    }

    private fun setupHorizonHandoffEnvironment() {
        val intent = intent
        var home = extra(intent, EXTRA_ENGINE_HOME)
        if (home == null) {
            home = java.io.File(filesDir, "horizon").absolutePath
        }
        java.io.File(home).mkdirs()

        val gameDir = extra(intent, EXTRA_GAME_DIR) ?: HyperUtils.DIR_GAME_HOME
        val hostControls = intent?.getBooleanExtra(EXTRA_HOST_CONTROLS, true) ?: true

        setEnv(ENV_HOME, home)
        setEnv(ENV_GAME_DIR, gameDir)
        setEnv(ENV_SESSION, extra(intent, EXTRA_SESSION_FILE))
        setEnv(ENV_QUICK_PLAY_WORLD, extra(intent, EXTRA_QUICK_PLAY_WORLD))
        setEnv(ENV_QUICK_PLAY_SERVER, extra(intent, EXTRA_QUICK_PLAY_SERVER))
        setEnv(ENV_AUTOPLAY, "1")
        setEnv(ENV_HOST_CONTROLS, if (hostControls) "1" else null)
        setEnv(ENV_ACCOUNT_NAME, extra(intent, EXTRA_ACCOUNT_NAME))
        setEnv(ENV_ACCOUNT_UUID, extra(intent, EXTRA_ACCOUNT_UUID))
        setEnv(ENV_ACCOUNT_TOKEN, extra(intent, EXTRA_ACCOUNT_TOKEN))
        setEnv(ENV_ACCOUNT_TYPE, extra(intent, EXTRA_ACCOUNT_TYPE))
        setEnv(ENV_SKIN_PATH, extra(intent, EXTRA_SKIN_PATH))
        setEnv(ENV_CAPE_PATH, extra(intent, EXTRA_CAPE_PATH))
        setEnv(ENV_TELEMETRY_URL, "off")
        setEnv("HORIZON_NO_HEARTBEAT", "1")

        val lockDir = java.io.File(cacheDir, "horizon-world-locks")
        lockDir.mkdirs()
        setEnv(ENV_LOCK_DIR, lockDir.absolutePath)
        setEnv(ENV_RENDERER, extra(intent, EXTRA_RENDERER))
        prepareVulkanDriver(intent)
    }

    @Keep
    fun quit() {
        Log.i("HorizonActivity", "Horizon native loop finished; removing task")
        try {
            finishAndRemoveTask()
        } catch (_: Throwable) {
            finish()
        }
    }

    protected fun initLayout() {

        controlLayout = ControlLayout(this).apply {
            id = R.id.main_control_layout
            clipChildren = false
            clipToPadding = false
            setMenuListener(this@HorizonActivity)
        }

        loggerView = LoggerView(this).apply {
            id = R.id.mainLoggerView
            visibility = View.GONE
        }

        touchCharInput = TouchCharInput(this).apply {
            id = R.id.mainTouchCharInput
            isHorizonMode = true
            layoutParams = FrameLayout.LayoutParams(
                HyperUtils.dpToPx(1f).toInt(),
                HyperUtils.dpToPx(1f).toInt()
            )
            var opts = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
            opts = opts or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            imeOptions = opts
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                    InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        }
        GameActivity.touchCharInput = touchCharInput
        HorizonActivity.touchCharInput = touchCharInput

        hotbarView = HotbarView(this).apply {
            id = R.id.hotbar_view
            layoutParams = FrameLayout.LayoutParams(0, 0)
        }

        val scale4x = LauncherPreferences.PREF_MOUSESCALE * 4f
        val cursorView = HorizonCursorView(this).apply {
            id = R.id.main_cursorview
            visibility = if (LauncherPreferences.PREF_VIRTUAL_MOUSE_START) View.VISIBLE else View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = false
            translationZ = HyperUtils.dpToPx(1f)
            setCursorScale(scale4x)
        }
        HorizonInput.setMouseScale(scale4x)
        HorizonInput.setMenuMouseScale(scale4x)

        controlLayout?.addView(touchCharInput)
        controlLayout?.addView(hotbarView)
        controlLayout?.addView(cursorView)
        Platform.setCursorImplementor(cursorView)
        weakCursor = java.lang.ref.WeakReference(cursorView)

        mainComposeView = ComposeView(this).apply {
            clipChildren = false
            clipToPadding = false
        }
        mainComposeView?.let { view ->
            LauncherComposeHelper.ensureViewTreeOwners(view)
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }

        val overlay = object : FrameLayout(this) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                this@HorizonActivity.dispatchTouchEvent(ev)
                return super.dispatchTouchEvent(ev)
            }

            override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
                this@HorizonActivity.dispatchGenericMotionEvent(ev)
                return super.dispatchGenericMotionEvent(ev)
            }

            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                return this@HorizonActivity.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
            }
        }.apply {
            clipChildren = false
            clipToPadding = false
            isFocusable = true
            isFocusableInTouchMode = true
            addView(
                mainComposeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        LauncherComposeHelper.ensureViewTreeOwners(overlay)
        overlay.setViewTreeLifecycleOwner(this)
        overlay.setViewTreeViewModelStoreOwner(this)
        overlay.setViewTreeSavedStateRegistryOwner(this)
        overlayView = overlay

        window.decorView.post {
            if (!isFinishing && !isDestroyed && overlayView?.parent == null) {
                val params = WindowManager.LayoutParams().apply {
                    type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
                    token = window.decorView.windowToken
                    format = PixelFormat.TRANSLUCENT
                    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                try {
                    windowManager.addView(overlayView, params)
                } catch (e: Throwable) {
                    Log.e("HorizonActivity", "Failed to add mainComposeView overlay", e)
                }
            }
        }

        LauncherComposeHelper.setBaseMainContent(
            mainComposeView!!,
            isInEditor,
            controlLayout!!,
            loggerView!!,
            null,
            true,
            { updateOverlayFocusableState() },
            { controller -> drawerController = controller },
            { action -> onAction(action) }
        )

        controlLayout?.setOnControlEditListener(object : ControlLayout.OnControlEditListener {
            override fun onEditControl(button: ControlInterface) {
                if (editControlSideDialog == null) {
                    editControlSideDialog = EditControlSideDialog()
                }
                editControlSideDialog?.setCurrentlyEditedButton(button)
                button.controlView?.let { ctrlView ->
                    SideDialogUtils.show(
                        editControlSideDialog!!,
                        ctrlView.x + ctrlView.width / 2f < (controlLayout?.width ?: 0) / 2f
                    )
                    updateOverlayFocusableState()
                }
            }

            override fun onDisappearLayer(): Boolean {
                val result = editControlSideDialog?.disappearLayer() ?: true
                updateOverlayFocusableState()
                return result
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val animOverlay = overlayView?.animate()?.setDuration(100)
            if (!insets.isVisible(WindowInsetsCompat.Type.ime())) {
                animOverlay?.translationY(0f)?.start()
                GameActivity.mImeHeight = 0
                mImeHeight = 0
                HorizonInput.setImeViewportBottomInset(0)
                if (touchCharInput?.isEnabled == true) {
                    touchCharInput?.disable()
                }
            } else {
                val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                GameActivity.mImeHeight = imeHeight
                mImeHeight = imeHeight
                val translationY = imeHeight.toFloat()
                animOverlay?.translationY(-translationY)?.start()
                HorizonInput.setImeViewportBottomInset(imeHeight)
            }
            insets
        }

        controlLayout?.post {
            HyperUtils.getDisplayMetrics(this)
            loadControls()
            if (instance?.horizonUseHorizonControls == true) {
                controlLayout?.visibility = View.GONE
            }
        }
    }

    private fun loadControls() {
        try {
            val ctrlPath = instance?.launchControls ?: HyperUtils.CTRLDEF_FILE
            controlLayout?.loadLayout(ctrlPath)
        } catch (_: Exception) {
            try {
                controlLayout?.loadLayout(HyperUtils.CTRLDEF_FILE)
            } catch (ioException: Exception) {
                Log.e("HorizonActivity", "Failed to load default controls", ioException)
            }
        }
        controlLayout?.toggleControlVisible()
    }

    private fun updateWindowSize() {
        val dm = resources.displayMetrics
        CallbackBridge.windowWidth = dm.widthPixels
        CallbackBridge.windowHeight = dm.heightPixels
        CallbackBridge.windowRate = dm.widthPixels.toFloat() / dm.heightPixels
    }

    private fun updateOverlayFocusableState(forceFocusable: Boolean? = null) {
        val overlay = overlayView ?: return
        val params = overlay.layoutParams as? WindowManager.LayoutParams ?: return
        val isDrawerOpen = drawerController?.isOpen() == true
        val isDialogActive = SideDialogUtils.activeDialog?.isDisplaying == true
        val isLoggerVisible = loggerView?.visibility == View.VISIBLE
        val isKeyboardActive = touchCharInput?.isEnabled == true
        val isFocusableNeeded = forceFocusable ?: (isDrawerOpen || isDialogActive || isLoggerVisible || isInEditor || isKeyboardActive)

        val newFlags = if (isFocusableNeeded) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        if (params.flags != newFlags) {
            params.flags = newFlags
            try {
                windowManager.updateViewLayout(overlay, params)
            } catch (e: Throwable) {
                Log.e("HorizonActivity", "Failed to update overlay view layout params", e)
            }
        }
    }

    private fun forceCloseApp() {
        try {
            HyperUtils.restartLauncherActivity(this)
            HyperUtils.fullyExit()
        } catch (_: Throwable) {
            finish()
        }
    }

    private inner class CustomKeySideDialog : SideDialogView() {
        init {
            setTitle(R.string.control_customkey)
            dialogWidth = 280.dp
        }

        @Composable
        override fun DialogContent() {
            val keyNames = remember { KeycodeUtils.generateKeyName() }
            val scrollState = rememberScrollState()

            Surface(
                color = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    keyNames.forEachIndexed { index, name ->
                        Surface(
                            onClick = {
                                KeycodeUtils.execKeyIndex(index)
                                disappear(true)
                                updateOverlayFocusableState()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onAction(action: Int) {
        if (isInEditor) {
            when (action) {
                -1 -> {}
                0 -> controlLayout?.addControlButton(ControlData("New"))
                1 -> controlLayout?.addDrawer(ControlDrawerData())
                2 -> controlLayout?.addJoystickButton(ControlJoystickData())
                3 -> controlLayout?.openLoadDialog()
                4 -> controlLayout?.openSaveDialog(this)
                5 -> controlLayout?.openSetDefaultDialog()
                6 -> controlLayout?.openExitDialog(this)
            }
        } else {
            when (action) {
                0 -> forceCloseApp()
                1 -> openLogOutput()
                2 -> dialogSendCustomKey()
                3 -> openQuickSettings()
                4 -> openCustomControls()
            }
        }
    }

    private fun ensureUngrabbed() {
        if (Platform.isGrabbing()) {
            CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE)
        }
    }

    private fun openCustomControls() {
        ensureUngrabbed()
        isInEditor = true
        mainComposeView?.let { view ->
            LauncherComposeHelper.setBaseMainContent(
                view,
                isInEditor,
                controlLayout!!,
                loggerView!!,
                null,
                true,
                { updateOverlayFocusableState() },
                { controller -> drawerController = controller },
                { action -> onAction(action) }
            )
        }
        controlLayout?.setModifiable(true)
        updateOverlayFocusableState()
    }

    private fun openLogOutput() {
        ensureUngrabbed()
        loggerView?.visibility = View.VISIBLE
        updateOverlayFocusableState()
    }

    private fun dialogSendCustomKey() {
        ensureUngrabbed()
        SideDialogUtils.show(CustomKeySideDialog(), true)
        updateOverlayFocusableState()
    }

    private fun openQuickSettings() {
        ensureUngrabbed()
        if (quickSettingSideDialog == null) {
            quickSettingSideDialog = object : QuickSettingSideDialog() {
                override fun onResolutionChanged() {
                    updateWindowSize()
                    hotbarView?.onResolutionChanged()
                }

                override fun onGyroStateChanged() {
                    gyroControl?.updateOrientation()
                    if (PREF_ENABLE_GYRO) {
                        gyroControl?.enable()
                    } else {
                        gyroControl?.disable()
                    }
                }

                override fun onButtonTransparencyChanged() {
                    controlLayout?.updateButtonOpacity()
                }

                override fun onShowFpsChanged() {}

                override fun onForceClose() {
                    forceCloseApp()
                }

                override fun onViewOutput() {
                    openLogOutput()
                }

                override fun onCustomKey() {
                    dialogSendCustomKey()
                }
            }
        }
        SideDialogUtils.show(quickSettingSideDialog!!, true)
        updateOverlayFocusableState()
    }

    override fun onClickedMenu() {
        ensureUngrabbed()
        drawerController?.open()
        updateOverlayFocusableState(true)
    }

    override fun exitEditor() {
        try {
            controlLayout?.loadLayout(null as CustomControls?)
            controlLayout?.setModifiable(false)
            System.gc()
            val ctrlPath = instance?.launchControls ?: HyperUtils.CTRLDEF_FILE
            controlLayout?.loadLayout(ctrlPath)
        } catch (e: Exception) {
            Log.e("HorizonActivity", "exit failed", e)
        }

        isInEditor = false
        mainComposeView?.let { view ->
            LauncherComposeHelper.setBaseMainContent(
                view,
                isInEditor,
                controlLayout!!,
                loggerView!!,
                null,
                true,
                { updateOverlayFocusableState() },
                { controller -> drawerController = controller },
                { action -> onAction(action) }
            )
        }
        updateOverlayFocusableState()
    }

    override fun onStart() {
        super.onStart()
        isActivityActive = true
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        Platform.PLATFORM.setVisible(true)
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
        LauncherPreferences.loadPreferences(this)
        val scale4x = LauncherPreferences.PREF_MOUSESCALE * 4f
        weakCursor?.get()?.setCursorScale(scale4x)
        HorizonInput.setMouseScale(scale4x)
        HorizonInput.setMenuMouseScale(scale4x)
        ContextExecutor.setActivity(this)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (PREF_ENABLE_GYRO) gyroControl?.enable()
        Platform.PLATFORM.setHovered(true)
        Platform.setPlatformLibrary(HorizonBackend())
        hideSystemUI()
    }

    override fun onPause() {
        isActivityActive = false
        ContextExecutor.clearActivity()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        gyroControl?.disable()
        if (Platform.isGrabbing()) {
            CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE)
        }
        quickSettingSideDialog?.cancel()
        Platform.PLATFORM.setHovered(false)
        super.onPause()
    }

    override fun onStop() {
        Platform.PLATFORM.setVisible(false)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        controlLayout?.removeCallbacks(grabStateRunnable)
        isActivityActive = false
        if (overlayView?.parent != null) {
            try {
                windowManager.removeViewImmediate(overlayView)
            } catch (e: Throwable) {
                Log.e("HorizonActivity", "Failed to remove View overlay", e)
            }
        }
        Platform.PLATFORM.setVisible(false)
        ContextExecutor.clearActivity()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        gyroControl?.updateOrientation()
        controlLayout?.requestLayout()
        controlLayout?.post {
            controlLayout?.refreshControlButtonPositions()
        }
    }

    private fun isTouchOnControlButton(x: Float, y: Float): Boolean {
        val layout = controlLayout
        if (layout != null && layout.isVisible) {
            val buttons = layout.buttonChildren
            if (buttons != null) {
                for (button in buttons) {
                    val ctrlView = button.controlView ?: continue
                    if (ctrlView.isVisible) {
                        val location = IntArray(2)
                        ctrlView.getLocationOnScreen(location)
                        val left = location[0]
                        val top = location[1]
                        val right = left + ctrlView.width
                        val bottom = top + ctrlView.height
                        if (x >= left.toFloat() && x <= right.toFloat() && y >= top.toFloat() && y <= bottom.toFloat()) {
                            return true
                        }
                    }
                }
            }
        }
        return isTouchOnDrawerPullButton(overlayView, x, y)
    }

    private fun isTouchOnDrawerPullButton(view: View?, x: Float, y: Float): Boolean {
        if (view == null || !view.isVisible) return false
        if (view is DrawerPullButton) {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val left = location[0]
            val top = location[1]
            val right = left + view.width
            val bottom = top + view.height
            if (x >= left.toFloat() && x <= right.toFloat() && y >= top.toFloat() && y <= bottom.toFloat()) {
                return true
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (isTouchOnDrawerPullButton(view.getChildAt(i), x, y)) {
                    return true
                }
            }
        }
        return false
    }

    @Suppress("unused")
    fun notifyKeyboardState(active: Boolean) {
        updateOverlayFocusableState(if (active) true else null)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        updateOverlayFocusableState()
        if (touchCharInput?.isEnabled == true) {
            return super.dispatchTouchEvent(ev)
        }
        val isGrabbing = Platform.isGrabbing()
        if (!isGrabbing && !isInEditor && SideDialogUtils.activeDialog?.isDisplaying != true && drawerController?.isOpen() != true) {
            CallbackBridge.setModifiers(KeyEvent.KEYCODE_UNKNOWN, false)
            val action = ev.actionMasked
            val pointerCount = ev.pointerCount
            for (i in 0 until pointerCount) {
                val pointerId = ev.getPointerId(i)
                val x = ev.getX(i)
                val y = ev.getY(i)
                val isControl = isTouchOnControlButton(x, y)
                val isDownAction = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                val isUpAction = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP

                if (isDownAction && i == ev.actionIndex) {
                    if (!isControl) {
                        activeCameraPointerIds.add(pointerId)
                    }
                }

                val shouldSend = activeCameraPointerIds.contains(pointerId) || !isControl
                if (shouldSend) {
                    val act = when (action) {
                        MotionEvent.ACTION_POINTER_DOWN -> if (i == ev.actionIndex) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_MOVE
                        MotionEvent.ACTION_POINTER_UP -> if (i == ev.actionIndex) MotionEvent.ACTION_UP else MotionEvent.ACTION_MOVE
                        else -> action
                    }
                    try {
                        val cursorView = weakCursor?.get()
                        val isMouseOn = cursorView?.visibility == View.VISIBLE

                        if (isMouseOn) {
                            val mouseSpeed = LauncherPreferences.PREF_MOUSESPEED.let { if (it <= 0) 1f else it }
                            when (act) {
                                MotionEvent.ACTION_DOWN -> {
                                    pointerTracker.startTracking(ev)
                                    isDraggingMouse = false
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    pointerTracker.trackEvent(ev)
                                    val vector = pointerTracker.motionVector
                                    val dx = vector[0] * mouseSpeed
                                    val dy = vector[1] * mouseSpeed
                                    if (kotlin.math.abs(dx) > 0.5f || kotlin.math.abs(dy) > 0.5f) {
                                        isDraggingMouse = true
                                    }
                                    val newX = (Platform.cursorX + dx).coerceIn(0.0, GameView.getWindowWidth().toDouble())
                                    val newY = (Platform.cursorY + dy).coerceIn(0.0, GameView.getWindowHeight().toDouble())
                                    Platform.setCursorPosition(newX, newY)
                                    HorizonInput.cursor(newX.toFloat(), newY.toFloat())
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    pointerTracker.cancelTracking()
                                    if (!isDraggingMouse) {
                                        HorizonInput.cursor(Platform.cursorX.toFloat(), Platform.cursorY.toFloat())
                                        CallbackBridge.sendMouseButton(MotionEvent.BUTTON_PRIMARY, true)
                                        controlLayout?.postDelayed({
                                            try {
                                                HorizonInput.cursor(Platform.cursorX.toFloat(), Platform.cursorY.toFloat())
                                                CallbackBridge.sendMouseButton(MotionEvent.BUTTON_PRIMARY, false)
                                            } catch (_: Throwable) {
                                            }
                                        }, 50)
                                    }
                                    isDraggingMouse = false
                                }
                            }
                        } else {
                            HorizonInput.cursor(x, y)
                            if (act == MotionEvent.ACTION_DOWN) {
                                CallbackBridge.sendMouseButton(MotionEvent.BUTTON_PRIMARY, true)
                            } else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                                CallbackBridge.sendMouseButton(MotionEvent.BUTTON_PRIMARY, false)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e(HyperUtils.APP_NAME, "Failed to send touch input", e)
                    }
                }

                if (isUpAction && i == ev.actionIndex) {
                    activeCameraPointerIds.remove(pointerId)
                }
            }
        } else {
            cursorTouchPointerId = MotionEvent.INVALID_POINTER_ID
            activeCameraPointerIds.clear()
        }
        val superResult = super.dispatchTouchEvent(ev)
        return activeCameraPointerIds.isNotEmpty() || superResult
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        CallbackBridge.setModifiers(event)
        if (isInEditor) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    controlLayout?.askToExit(this)
                }
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK && touchCharInput?.isEnabled == true) {
            if (event.action == KeyEvent.ACTION_UP) {
                touchCharInput?.disable()
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK && touchCharInput?.isEnabled == false) {
            if (event.action == KeyEvent.ACTION_UP) {
                CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE)
            }
            return true
        }
        if ((event.flags and KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    touchCharInput?.let { input ->
                        val text = input.text
                        if (text != null && !text.endsWith("\n")) {
                            input.append("\n")
                        }
                    }
                }
                return true
            }
            touchCharInput?.dispatchKeyEvent(event)
            return true
        }
        val isDown = event.action == KeyEvent.ACTION_DOWN
        Platform.PLATFORM.sendKeyEvent(event.keyCode, isDown, CallbackBridge.getCurrentMods())
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTrackballEvent(ev: MotionEvent): Boolean {
        return super.dispatchTrackballEvent(ev)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            nativeDocumentResult(requestCode, resultCode, data?.dataString)
        } catch (e: Throwable) {
            Log.e("HorizonActivity", "nativeDocumentResult failed", e)
        }
    }

    private fun hideSystemUI() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } catch (e: Throwable) {
            Log.e("HorizonActivity", "Failed to hide system UI", e)
            HyperUtils.setInsetsMode(this, noSystemBars = true, ignoreNotch = LauncherPreferences.PREF_IGNORE_NOTCH)
        }
    }

    @Suppress("KotlinJniMissingFunction")
    @Keep
    external fun nativeDocumentResult(requestCode: Int, resultCode: Int, data: String?)
}
