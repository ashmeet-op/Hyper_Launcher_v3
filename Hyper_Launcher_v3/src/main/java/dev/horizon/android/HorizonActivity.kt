package dev.horizon.android

import android.app.NativeActivity
import android.content.Context
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
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.ashmeet.hyperbackend.game.GameActivity
import com.ashmeet.hyperbackend.game.GameCursorView
import com.ashmeet.hyperbackend.game.platform.Platform
import com.ashmeet.hyperbackend.game.platform.backend.HorizonBackend
import com.ashmeet.hyperbackend.instances.Instances
import com.ashmeet.hyperbackend.lifecycle.ContextExecutor
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
    private var touchCharInput: TouchCharInput? = null
    private var hotbarView: HotbarView? = null
    private var gyroControl: GyroControl? = null
    private var mainComposeView: ComposeView? = null
    private var overlayView: View? = null
    private var drawerController: LauncherComposeHelper.DrawerController? = null
    private var quickSettingSideDialog: QuickSettingSideDialog? = null
    private var editControlSideDialog: EditControlSideDialog? = null
    private var isInEditor = false
    private val instance = Instances.loadSelectedInstance()

    companion object {
        var weakCursor: WeakReference<GameCursorView>? = null

        init {
            try {
                System.loadLibrary("horizon_android")
            } catch (e: Throwable) {
                Log.e("HorizonActivity", "Failed to load libhorizon_android.so", e)
            }
        }

        @JvmStatic
        @Suppress("unused")
        fun toggleMouse(ctx: Context) {
            if (Platform.isGrabbing()) return
            val cursorView = HyperUtils.getWeakReference(weakCursor) ?: return
            var toastString = 0
            when (cursorView.visibility) {
                View.GONE, View.INVISIBLE -> {
                    toastString = R.string.control_mouseon
                    cursorView.visibility = View.VISIBLE
                }
                View.VISIBLE -> {
                    toastString = R.string.control_mouseoff
                    cursorView.visibility = View.GONE
                }
            }
            if (toastString != 0) {
                Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

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
        initLayout()

        Platform.setPlatformLibrary(HorizonBackend())
        Platform.grabStateChanged(true)

        gyroControl = GyroControl(this)
        ContextExecutor.setActivity(this)

        LauncherComposeHelper.setLoadingVisible(false)
    }

    private fun initLayout() {
        controlLayout = ControlLayout(this).apply {
            id = R.id.main_control_layout
            clipChildren = false
            clipToPadding = false
            setMenuListener(this@HorizonActivity)
        }

        val cursorView = GameCursorView(this).apply {
            id = R.id.main_cursorview
            visibility = if (LauncherPreferences.PREF_VIRTUAL_MOUSE_START) View.VISIBLE else View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = false
            translationZ = HyperUtils.dpToPx(1f)
        }
        controlLayout?.addView(cursorView)
        weakCursor = WeakReference(cursorView)
        Platform.setCursorImplementor(cursorView)

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
        GameActivity.touchCharInput = touchCharInput

        hotbarView = HotbarView(this).apply {
            id = R.id.hotbar_view
            layoutParams = FrameLayout.LayoutParams(0, 0)
        }

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
                    flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
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

        cursorView.setCursorScale(LauncherPreferences.PREF_MOUSESCALE)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val animCursor = cursorView.animate().setDuration(100)
            if (!insets.isVisible(WindowInsetsCompat.Type.ime())) {
                animCursor.translationY(0f).start()
                GameActivity.mImeHeight = 0
            } else {
                val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                GameActivity.mImeHeight = imeHeight
                if (GameActivity.mForcedPanningHeight != 0 || LauncherPreferences.PREF_KEYBOARD_AUTOPANNING) {
                    val translationY = if (GameActivity.mForcedPanningHeight == 0) {
                        HyperUtils.getTranslationFromCursorY(
                            (Platform.cursorY * 1.0 + 100).toInt(),
                            controlLayout?.height ?: 0,
                            imeHeight,
                            0
                        )
                    } else {
                        if (GameActivity.mForcedPanningHeight == -1) {
                            imeHeight
                        } else {
                            (imeHeight - GameActivity.mForcedPanningHeight).coerceIn(0, imeHeight)
                        }
                    }
                    animCursor.translationY(-translationY.toFloat()).start()
                }
            }
            insets
        }

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
                null,
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
                null,
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
        ContextExecutor.setActivity(this)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (PREF_ENABLE_GYRO) gyroControl?.enable()
        Platform.PLATFORM.setHovered(true)
    }

    override fun onPause() {
        ContextExecutor.clearActivity()
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
        if (overlayView?.parent != null) {
            try {
                windowManager.removeViewImmediate(overlayView)
            } catch (e: Throwable) {
                Log.e("HorizonActivity", "Failed to remove mainComposeView overlay", e)
            }
        }
        Platform.PLATFORM.setVisible(false)
        ContextExecutor.clearActivity()
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
            controlLayout?.refreshControlButtonPositions()
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!isInEditor && SideDialogUtils.activeDialog?.isDisplaying != true && drawerController?.isOpen() != true) {
            CallbackBridge.setModifiers(KeyEvent.KEYCODE_UNKNOWN, false)
            val action = ev.actionMasked
            val pointerCount = ev.pointerCount
            for (i in 0 until pointerCount) {
                val pointerId = ev.getPointerId(i)
                val x = ev.getX(i)
                val y = ev.getY(i)
                if (i == 0) {
                    Platform.cursorX = x.toDouble()
                    Platform.cursorY = y.toDouble()
                    if (weakCursor?.get()?.visibility == View.VISIBLE) {
                        Platform.sendCursorPosition()
                    }
                }
                val act = when (action) {
                    MotionEvent.ACTION_POINTER_DOWN -> if (i == ev.actionIndex) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_MOVE
                    MotionEvent.ACTION_POINTER_UP -> if (i == ev.actionIndex) MotionEvent.ACTION_UP else MotionEvent.ACTION_MOVE
                    else -> action
                }
                try {
                    nativeRegisterTouch(pointerId, act, x, y)
                } catch (_: Throwable) {
                    HorizonInput.registerTouch(pointerId, act, x, y)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
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
        if (event.keyCode == KeyEvent.KEYCODE_BACK && touchCharInput?.isEnabled == false) {
            if (event.action == KeyEvent.ACTION_UP) {
                CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE)
            }
            return true
        }
        val isDown = event.action == KeyEvent.ACTION_DOWN
        Platform.PLATFORM.sendKeyEvent(event.keyCode, isDown, CallbackBridge.getCurrentMods())
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTrackballEvent(ev: MotionEvent): Boolean {
        return super.dispatchTrackballEvent(ev)
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
        HyperUtils.setInsetsMode(this, noSystemBars = true, ignoreNotch = true)
    }

    @Suppress("KotlinJniMissingFunction")
    @Keep
    external fun nativeRegisterTouch(pointerId: Int, action: Int, x: Float, y: Float)

    @Suppress("KotlinJniMissingFunction")
    //I DONT KNOW WHAT IT IS//
    @Keep
    external fun nativeDocumentResult(requestCode: Int, resultCode: Int, data: String?)
}


