package dev.horizon.android

import android.app.NativeActivity
import android.content.Intent
import android.content.res.Configuration
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
import androidx.annotation.Keep
import androidx.compose.ui.platform.ComposeView
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
import com.ashmeet.hyperbackend.game.platform.backend.HorizonBackend
import com.ashmeet.hyperbackend.instances.Instances
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog
import com.ashmeet.hyperlauncher.fragments.dialog.QuickSettingSideDialog
import com.ashmeet.hyperlauncher.screens.game.LoggerView
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_ENABLE_GYRO
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.SideDialogUtils
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.ashmeet.hyperlauncher.R
import java.lang.ref.WeakReference

@Suppress("KotlinJniMissingFunction")
@Keep
class HorizonActivity : NativeActivity(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, ControlButtonMenuListener, EditorExitable {

    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var loggerView: LoggerView? = null
    private var controlLayout: ControlLayout? = null
    private var launcherGLView: GameView? = null
    private var touchCharInput: TouchCharInput? = null
    private var hotbarView: HotbarView? = null
    private var gyroControl: GyroControl? = null
    private var mainComposeView: ComposeView? = null
    private var drawerController: LauncherComposeHelper.DrawerController? = null
    private var quickSettingSideDialog: QuickSettingSideDialog? = null
    private var editControlSideDialog: EditControlSideDialog? = null
    private var isInEditor = false
    private val instance = Instances.loadSelectedInstance()

    companion object {
        var weakCursor: WeakReference<GameCursorView>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeViewModelStoreOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        initLayout()

        Platform.initialize(this, launcherGLView!!)
        Platform.setPlatformLibrary(HorizonBackend())
        Platform.grabStateChanged(true)

        gyroControl = GyroControl(this)

        LauncherComposeHelper.setLoadingVisible(false)
    }

    private fun initLayout() {
        controlLayout = ControlLayout(this).apply {
            id = R.id.main_control_layout
            clipChildren = false
            clipToPadding = false
            setMenuListener(this@HorizonActivity)
        }

        launcherGLView = GameView(this).apply {
            id = R.id.main_game_render_view
            clipChildren = false
            clipToPadding = false
        }

        val cursorView = GameCursorView(this).apply {
            id = R.id.main_cursorview
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = false
            translationZ = HyperUtils.dpToPx(1f)
        }
        launcherGLView?.addView(cursorView)
        launcherGLView?.cursorView = cursorView
        launcherGLView?.let { weakCursor = WeakReference(cursorView) }

        loggerView = LoggerView(this).apply {
            id = R.id.mainLoggerView
            visibility = View.GONE
        }

        touchCharInput = TouchCharInput(this).apply {
            id = R.id.mainTouchCharInput
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

        hotbarView = HotbarView(this).apply {
            id = R.id.hotbar_view
            layoutParams = FrameLayout.LayoutParams(0, 0)
        }

        controlLayout?.addView(launcherGLView)
        controlLayout?.addView(touchCharInput)
        controlLayout?.addView(hotbarView)

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

        setContentView(mainComposeView!!)

        LauncherComposeHelper.setBaseMainContent(
            mainComposeView!!,
            isInEditor,
            controlLayout!!,
            loggerView!!,
            launcherGLView,
            true,
            { },
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
                }
            }

            override fun onDisappearLayer(): Boolean {
                return editControlSideDialog?.disappearLayer() ?: true
            }
        })

        launcherGLView?.mCursorView?.setCursorScale(LauncherPreferences.PREF_MOUSESCALE)

        controlLayout?.post {
            HyperUtils.getDisplayMetrics(this)
            loadControls()
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
                0 -> HyperUtils.dialogForceClose(this)
                1 -> openLogOutput()
                2 -> dialogSendCustomKey()
                3 -> openQuickSettings()
                4 -> openCustomControls()
            }
        }
    }

    private fun openCustomControls() {
        isInEditor = true
        mainComposeView?.let { view ->
            LauncherComposeHelper.setBaseMainContent(
                view,
                isInEditor,
                controlLayout!!,
                loggerView!!,
                launcherGLView,
                true,
                { },
                { controller -> drawerController = controller },
                { action -> onAction(action) }
            )
        }
        controlLayout?.setModifiable(true)
    }

    private fun openLogOutput() {
        loggerView?.visibility = View.VISIBLE
    }

    private fun dialogSendCustomKey() {
        HyperUtils.dialogSendCustomKey(this)
    }

    private fun openQuickSettings() {
        if (quickSettingSideDialog == null) {
            quickSettingSideDialog = object : QuickSettingSideDialog() {
                override fun onResolutionChanged() {
                    launcherGLView?.refreshSize()
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
                    HyperUtils.dialogForceClose(this@HorizonActivity)
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
    }

    override fun onClickedMenu() {
        drawerController?.open()
    }

    override fun exitEditor() {
        try {
            controlLayout?.loadLayout(null as CustomControls?)
            controlLayout?.setModifiable(false)
            System.gc()
            val ctrlPath = instance?.launchControls ?: HyperUtils.CTRLDEF_FILE
            controlLayout?.loadLayout(ctrlPath)
        } catch (e: Exception) {
            Log.e("HorizonActivity", "exitEditor failed", e)
        }

        isInEditor = false
        mainComposeView?.let { view ->
            LauncherComposeHelper.setBaseMainContent(
                view,
                isInEditor,
                controlLayout!!,
                loggerView!!,
                launcherGLView,
                true,
                { },
                { controller -> drawerController = controller },
                { action -> onAction(action) }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        Platform.PLATFORM.setVisible(true)
    }

    override fun onResume() {
        super.onResume()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (PREF_ENABLE_GYRO) gyroControl?.enable()
        Platform.PLATFORM.setHovered(true)
    }

    override fun onPause() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
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
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        Platform.PLATFORM.setVisible(false)
        launcherGLView?.onSurfaceDestroyed()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
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
            launcherGLView?.refreshSize()
            controlLayout?.refreshControlButtonPositions()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isInEditor) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    controlLayout?.askToExit(this)
                }
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        var handleEvent = false
        if (launcherGLView?.processKeyEvent(event) == false) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && touchCharInput?.isEnabled == false) {
                if (event.action != KeyEvent.ACTION_UP) return true
                CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE)
                return true
            }
        } else {
            handleEvent = true
        }
        return handleEvent
    }

    override fun dispatchTrackballEvent(ev: MotionEvent): Boolean {
        if (HyperUtils.isAndroid8OrHigher() && checkCaptureDispatchConditions(ev)) {
            return launcherGLView?.dispatchCapturedPointerEvent(ev) == true
        }
        return super.dispatchTrackballEvent(ev)
    }

    private fun checkCaptureDispatchConditions(event: MotionEvent): Boolean {
        val eventSource = event.source
        return (eventSource and InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource and InputDevice.SOURCE_MOUSE) != 0
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            nativeDocumentResult(requestCode, resultCode, data?.dataString)
        } catch (e: Throwable) {
            Log.e("HorizonActivity", "nativeDocumentResult failed", e)
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    @Keep
    external fun nativeDocumentResult(requestCode: Int, resultCode: Int, data: String?)
}
