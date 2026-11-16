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
import com.ashmeet.hyperbackend.game.GameActivity
import com.ashmeet.hyperbackend.game.platform.Platform
import com.ashmeet.hyperbackend.game.platform.backend.HorizonBackend
import com.ashmeet.hyperbackend.instances.Instances
import com.ashmeet.hyperbackend.lifecycle.ContextExecutor
import com.ashmeet.hyperbackend.utils.KeycodeUtils
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog
import com.ashmeet.hyperlauncher.fragments.dialog.QuickSettingSideDialog
import com.ashmeet.hyperlauncher.fragments.dialog.SideDialogView
import com.ashmeet.hyperlauncher.screens.game.LoggerView
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_ENABLE_GYRO
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.SideDialogUtils
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import com.ashmeet.hyperlauncher.utils.horizon.HorizonUtils
import net.ashmeet.hyperlauncher.R
import java.lang.ref.WeakReference
import androidx.core.view.isVisible
import com.ashmeet.hyperlauncher.utils.Architecture

@Keep
class HorizonActivity : NativeActivity(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, ControlButtonMenuListener, EditorExitable {

    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    private var loggerView: LoggerView? = null
    @JvmField
    var controlLayout: ControlLayout? = null
    private var cursorView: HorizonCursorView? = null
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
    private var cursorTouchLastX = 0f
    private var cursorTouchLastY = 0f
    private var cursorTouchStartX = 0f
    private var cursorTouchStartY = 0f
    private var cursorTouchMoved = false
    private val instance = Instances.loadSelectedInstance()
    var mForcedPanningHeight = 0

    @JvmOverloads
    fun showKeyboard(initialText: String? = "", panning: Boolean = false) {
        GameActivity.mForcedPanningHeight = if (panning) -1 else 0
        mForcedPanningHeight = if (panning) -1 else 0
        touchCharInput?.setKeyboardState(true, initialText)
        updateOverlayFocusableState(true)
    }

    fun hideKeyboard() {
        GameActivity.mForcedPanningHeight = 0
        mForcedPanningHeight = 0
        touchCharInput?.setKeyboardState(false)
        updateOverlayFocusableState(false)
    }

    fun getInputText(): String? {
        return touchCharInput?.text?.toString()
    }

    @Keep
    @Suppress("unused")
    fun onGrabStateChanged(grabbing: Boolean) {
        Platform.grabStateChanged(grabbing)
        updateOverlayFocusableState()
    }

    companion object {
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
        LauncherPreferences.loadPreferences(this)
        updateWindowSize()

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

        Platform.setPlatformLibrary(HorizonBackend())
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

        if (instance?.horizonUseHorizonControls != true) {
            cursorView = HorizonCursorView(this).apply {
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
            HorizonInput.weakCursor = WeakReference(cursorView)
            Platform.setCursorImplementor(cursorView)
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

        cursorView?.setCursorScale(LauncherPreferences.PREF_MOUSESCALE * 3f)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val animCursor = cursorView?.animate()?.setDuration(100)
            val animOverlay = overlayView?.animate()?.setDuration(100)
            if (!insets.isVisible(WindowInsetsCompat.Type.ime())) {
                animOverlay?.translationY(0f)?.start()
                animCursor?.translationY(0f)?.start()
                GameActivity.mImeHeight = 0
            } else {
                val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                GameActivity.mImeHeight = imeHeight
                val translationY = imeHeight.toFloat()
                animOverlay?.translationY(-translationY)?.start()
                animCursor?.translationY(-translationY)?.start()
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
                    HorizonUtils.updateRenderScale(this@HorizonActivity, LauncherPreferences.PREF_SCALE_FACTOR)
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
                { updateOverlayFocusableState() },
                { controller -> drawerController = controller },
                { action -> onAction(action) }
            )
        }
        updateOverlayFocusableState()
    }

    override fun onStart() {
        super.onStart()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        Platform.PLATFORM.setVisible(true)
    }

    override fun onResume() {
        super.onResume()
        LauncherPreferences.loadPreferences(this)
        cursorView?.setCursorScale(LauncherPreferences.PREF_MOUSESPEED * 3f)
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

    private fun isTouchOnControlButton(x: Float, y: Float): Boolean {
        val layout = controlLayout ?: return false
        if (layout.visibility != View.VISIBLE) return false
        val buttons = layout.buttonChildren ?: return false
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
        return false
    }

    fun handleSpecialButton(keycode: Int, isDown: Boolean): Boolean {
        return when (keycode) {
            ControlData.SPECIALBTN_KEYBOARD,
            ControlData.SPECIALBTN_KEYBOARDPAN -> {
                if (isDown) HorizonInput.onNativeShowKeyboard(true)
                true
            }
            ControlData.SPECIALBTN_TOGGLECTRL -> {
                if (isDown) controlLayout?.toggleControlVisible()
                true
            }
            ControlData.SPECIALBTN_VIRTUALMOUSE -> {
                if (isDown) HorizonInput.toggleMouse(this)
                true
            }
            ControlData.SPECIALBTN_MOUSEPRI -> {
                HorizonInput.sendMouseButton(0, isDown)
                true
            }
            ControlData.SPECIALBTN_MOUSESEC -> {
                HorizonInput.sendMouseButton(1, isDown)
                true
            }
            ControlData.SPECIALBTN_MOUSEMID -> {
                HorizonInput.sendMouseButton(2, isDown)
                true
            }
            ControlData.SPECIALBTN_SCROLLUP -> {
                if (!isDown) HorizonInput.sendScroll(0f, -1f)
                true
            }
            ControlData.SPECIALBTN_SCROLLDOWN -> {
                if (!isDown) HorizonInput.sendScroll(0f, 1f)
                true
            }
            ControlData.SPECIALBTN_MENU -> {
                if (isDown) drawerController?.open()
                true
            }
            else -> false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val isGrabbing = Platform.isGrabbing()
        val cursorVisible = !isGrabbing && (HorizonInput.weakCursor?.get()?.visibility == View.VISIBLE)
        if (!isInEditor && SideDialogUtils.activeDialog?.isDisplaying != true && drawerController?.isOpen() != true) {
            CallbackBridge.setModifiers(KeyEvent.KEYCODE_UNKNOWN, false)
            val action = ev.actionMasked

            if (cursorVisible) {
                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        val index = ev.actionIndex
                        val x = ev.getX(index)
                        val y = ev.getY(index)
                        if (!isTouchOnControlButton(x, y)) {
                            if (cursorTouchPointerId == MotionEvent.INVALID_POINTER_ID) {
                                cursorTouchPointerId = ev.getPointerId(index)
                                cursorTouchStartX = x
                                cursorTouchStartY = y
                                cursorTouchLastX = x
                                cursorTouchLastY = y
                                cursorTouchMoved = false
                            }
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (cursorTouchPointerId != MotionEvent.INVALID_POINTER_ID) {
                            val index = ev.findPointerIndex(cursorTouchPointerId)
                            if (index >= 0) {
                                val x = ev.getX(index)
                                val y = ev.getY(index)
                                val dx = x - cursorTouchLastX
                                val dy = y - cursorTouchLastY
                                val touchSlop = HyperUtils.dpToPx(8f)
                                cursorTouchMoved = cursorTouchMoved ||
                                    (x - cursorTouchStartX) * (x - cursorTouchStartX) +
                                    (y - cursorTouchStartY) * (y - cursorTouchStartY) > touchSlop * touchSlop
                                val speed = LauncherPreferences.PREF_MOUSESPEED
                                Platform.cursorX += dx * speed
                                Platform.cursorY += dy * speed
                                cursorTouchLastX = x
                                cursorTouchLastY = y
                                Platform.sendCursorPosition()
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        val pointerId = ev.getPointerId(ev.actionIndex)
                        if (pointerId == cursorTouchPointerId) {
                            if (!cursorTouchMoved) {
                                HorizonInput.sendMouseButton(0, true)
                                HorizonInput.sendMouseButton(0, false)
                            }
                            cursorTouchPointerId = MotionEvent.INVALID_POINTER_ID
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        cursorTouchPointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                cursorTouchPointerId = MotionEvent.INVALID_POINTER_ID
                activeCameraPointerIds.clear()
            }

            if (!cursorVisible) {
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
                            nativeRegisterTouch(pointerId, act, x, y)
                        } catch (_: Throwable) {
                            HorizonInput.registerTouch(pointerId, act, x, y)
                        }
                    }

                    if (isUpAction && i == ev.actionIndex) {
                        activeCameraPointerIds.remove(pointerId)
                    }
                }
            }
        } else {
            cursorTouchPointerId = MotionEvent.INVALID_POINTER_ID
            activeCameraPointerIds.clear()
        }
        val superResult = super.dispatchTouchEvent(ev)
        return cursorVisible && cursorTouchPointerId != MotionEvent.INVALID_POINTER_ID || superResult
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
    @Keep
    external fun nativeDocumentResult(requestCode: Int, resultCode: Int, data: String?)
}
