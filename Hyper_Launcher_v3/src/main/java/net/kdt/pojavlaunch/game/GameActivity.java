package net.kdt.pojavlaunch.game;


import static com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_ENABLE_GYRO;
import static com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_SUSTAINED_PERFORMANCE;
import static com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_USE_ALTERNATE_SURFACE;
import static com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_VIRTUAL_MOUSE_START;
import static com.ashmeet.hyperlauncher.utils.Tools.dialogForceClose;
import static net.kdt.pojavlaunch.game.platform.Platform.PLATFORM;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.text.InputType;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;


import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog;
import com.ashmeet.hyperlauncher.fragments.dialog.QuickSettingSideDialog;
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper;
import com.ashmeet.hyperlauncher.utils.SideDialogUtils;
import com.ashmeet.hyperlauncher.screens.game.LoggerView;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.BaseActivity;
import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.utils.KeycodeUtils;
import net.kdt.pojavlaunch.Logger;
import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.CustomControls;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput;
import net.kdt.pojavlaunch.customcontrols.mouse.GyroControl;
import net.kdt.pojavlaunch.customcontrols.mouse.HotbarView;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.game.platform.Platform;
import net.kdt.pojavlaunch.game.platform.backend.DummyBackend;

import net.kdt.pojavlaunch.services.GameService;
import net.kdt.pojavlaunch.tasks.AsyncAssetManager;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.MCOptionUtils;
import net.kdt.pojavlaunch.authenticator.accounts.Account;
import com.ashmeet.hyperlauncher.utils.RendererCompatUtil;
import net.kdt.pojavlaunch.utils.jre.GameRunner;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Objects;
import androidx.compose.ui.platform.ComposeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;



public class GameActivity extends BaseActivity implements ControlButtonMenuListener, EditorExitable, ServiceConnection {
    public static final String INTENT_LAUNCH_VERSION = "intent_version";
    public static final String INTENT_LAUNCH_CLASSPATH = "intent_classpath";

    public static TouchCharInput touchCharInput;
    private GameView launcherGLView;
    private static WeakReference<GameCursorView> weakCursor;
    private LoggerView loggerView;
    private GyroControl mGyroControl = null;
    private ControlLayout mControlLayout;
    private HotbarView mHotbarView;
    private View mLoadingScreen;
    private ComposeView mMainComposeView;

    Instance instance;
    Account account;

    private GameService.LocalBinder mServiceBinder;

    private QuickSettingSideDialog mQuickSettingSideDialog;
    private EditControlSideDialog mEditControlSideDialog;
    private LauncherComposeHelper.DrawerController mDrawerController;

    public static int mForcedPanningHeight = 0;
    public static int mImeHeight = 0;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = Instances.loadSelectedInstance();
        account = Accounts.getCurrent();
        if(instance == null) {
            Toast.makeText(this, R.string.instance_dir_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        AsyncAssetManager.extractDefaultSettings(this, instance.getGameDirectory());
        MCOptionUtils.load(instance.getGameDirectory().getAbsolutePath());

        Intent gameServiceIntent = new Intent(this, GameService.class);
        // Start the service a bit early
        ContextCompat.startForegroundService(this, gameServiceIntent);
        initLayout();

        Platform.initialize(this, launcherGLView);

        mGyroControl = new GyroControl(this);

        // Enabling this on TextureView results in a broken white result
        if(PREF_USE_ALTERNATE_SURFACE) getWindow().setBackgroundDrawable(null);
        else getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        // Set the sustained performance mode for available APIs
        getWindow().setSustainedPerformanceMode(PREF_SUSTAINED_PERFORMANCE);

        // This is required on Android 10 for the insets listener
        // https://issuetracker.google.com/issues/266331465
        boolean androidCompat = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q;
        if(androidCompat)
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // Make keyboard pan the activity so the user sees what they're typing
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (view, insets) -> {
            if(launcherGLView.mSurface == null)
                return insets;
            ViewPropertyAnimator animSurface = launcherGLView.mSurface.animate()
                    .setDuration(100);
            ViewPropertyAnimator animCursor = launcherGLView.mCursorView.animate()
                    .setDuration(100);
            if(!insets.isVisible(WindowInsetsCompat.Type.ime())){
                animSurface.translationY(0).start();
                animCursor.translationY(0).start();
                mImeHeight = 0;
                if(androidCompat) {
                    // AndroidX keeps SystemUI visible for some reason after IME session
                    view.postDelayed(() -> {
                        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), view);
                        if (controller != null) {
                            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                            controller.hide(WindowInsetsCompat.Type.systemBars());
                        }
                    }, 150);
                }
                return insets;
            }
            if(mForcedPanningHeight == 0 && !LauncherPreferences.PREF_KEYBOARD_AUTOPANNING)
                return insets;
            mImeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int translationY;
            // Auto-panning (if keyboardPan wasn't clicked)
            if(mForcedPanningHeight == 0) {
                translationY = Tools.getTranslationFromCursorY(
                        (int)(Platform.cursorY * launcherGLView.getCursorRatioY() + 100),
                        launcherGLView.getHeight(),
                        mImeHeight,
                        0
                );
            } else
                translationY = mForcedPanningHeight == -1 ? mImeHeight : Math.clamp(mImeHeight - mForcedPanningHeight, 0, mImeHeight);
            animSurface.translationY(-translationY).start();
            animCursor.translationY(-translationY).start();
            return insets;
        });

        // Recompute the gui scale when options are changed
        MCOptionUtils.MCOptionListener optionListener = MCOptionUtils::getMcScale;
        MCOptionUtils.addMCOptionListener(optionListener);
        mControlLayout.setModifiable(false);

        // Set the activity for the executor. Must do this here, or else Tools.showErrorRemote() may not
        // execute the correct method
        ContextExecutor.setActivity(this);
        //Now, attach to the service. The game will only start when this happens, to make sure that we know the right state.
        bindService(gameServiceIntent, this, 0);
    }

    @SuppressWarnings("deprecation")
    protected void initLayout() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bindValues();
        mControlLayout.setMenuListener(this);

        launcherGLView.mCursorView.setCursorScale(LauncherPreferences.PREF_MOUSESCALE);
        weakCursor = new WeakReference<>(launcherGLView.mCursorView);

        mMainComposeView = new ComposeView(this);
        LauncherComposeHelper.ensureViewTreeOwners(mMainComposeView);
        mMainComposeView.setClipChildren(false);
        mMainComposeView.setClipToPadding(false);
        setContentView(mMainComposeView);

        LauncherComposeHelper.setBaseMainContent(
                mMainComposeView,
                isInEditor,
                mControlLayout,
                loggerView,
                launcherGLView,
                true, // hostViews = true
                isOpen -> kotlin.Unit.INSTANCE,
                controller -> { mDrawerController = controller; return kotlin.Unit.INSTANCE; },
                action -> { onAction(action); return kotlin.Unit.INSTANCE; }
        );

        mControlLayout.setOnControlEditListener(new ControlLayout.OnControlEditListener() {
            @Override
            public void onEditControl(ControlInterface button) {
                if (mEditControlSideDialog == null) {
                    mEditControlSideDialog = new EditControlSideDialog();
                }
                mEditControlSideDialog.setCurrentlyEditedButton(button);
                SideDialogUtils.show(mEditControlSideDialog, button.getControlView().getX() + button.getControlView().getWidth()/2f < mControlLayout.getWidth()/2f);
            }

            @Override
            public boolean onDisappearLayer() {
                if (mEditControlSideDialog != null) {
                    return mEditControlSideDialog.disappearLayer();
                }
                return true;
            }
        });

        try {
            File latestLogFile = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            if(!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw new IOException("Failed to create a new log file");
            Logger.begin(latestLogFile.getAbsolutePath());

            Bundle extras = Objects.requireNonNull(getIntent().getExtras());
            String version = extras.getString(INTENT_LAUNCH_VERSION);
            File[] classpath;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                classpath = extras.getSerializable(INTENT_LAUNCH_CLASSPATH, File[].class);
            } else {
                classpath = (File[]) extras.getSerializable(INTENT_LAUNCH_CLASSPATH);
            }

            setTitle("HyperLauncher (" + version + ")");

            launcherGLView.setSurfaceReadyListener(() -> {
                try {
                    Tools.runOnUiThread(() -> { if(PREF_VIRTUAL_MOUSE_START) launcherGLView.mCursorView.setVisibility(View.VISIBLE); });
                    runCraft(version, classpath);
                }catch (Throwable e){
                    Tools.showErrorRemote(e);
                }
            });
        } catch (Throwable e) {
            Tools.showError(this, e, true);
        }
    }

    private void onAction(int action) {
        if (isInEditor) {
            switch (action) {
                case -1: // Close
                    break;
                case 0: mControlLayout.addControlButton(new ControlData("New")); break;
                case 1: mControlLayout.addDrawer(new ControlDrawerData()); break;
                case 2: mControlLayout.addJoystickButton(new ControlJoystickData()); break;
                case 3: mControlLayout.openLoadDialog(); break;
                case 4: mControlLayout.openSaveDialog(this); break;
                case 5: mControlLayout.openSetDefaultDialog(); break;
                case 6: mControlLayout.openExitDialog(this); break;
            }
        } else {
            switch (action) {
                case 0: dialogForceClose(GameActivity.this); break;
                case 1: openLogOutput(); break;
                case 2: dialogSendCustomKey(); break;
                case 3: openQuickSettings(); break;
                case 4: openCustomControls(); break;
            }
        }
    }

    private void loadControls() {
        try {
            // Load keys
            mControlLayout.loadLayout(instance.getLaunchControls());
        } catch(IOException e) {
            try {
                Log.w("MainActivity", "Unable to load the control file, loading the default now", e);
                mControlLayout.loadLayout(Tools.CTRLDEF_FILE);
            } catch (IOException ioException) {
                Tools.showError(this, ioException);
            }
        } catch (Throwable th) {
            Tools.showError(this, th);
        }
        mControlLayout.toggleControlVisible();
    }

    @Override
    public void onAttachedToWindow() {
        // Post to get the correct display dimensions after layout.
        mControlLayout.post(()->{
            Tools.getDisplayMetrics(this);
            loadControls();
        });
    }

    /** Boilerplate binding */
    private void bindValues(){
        mControlLayout = new ControlLayout(this);
        mControlLayout.setId(R.id.main_control_layout);
        mControlLayout.setClipChildren(false);
        mControlLayout.setClipToPadding(false);

        launcherGLView = new GameView(this);
        launcherGLView.setId(R.id.main_game_render_view);
        launcherGLView.setClipChildren(false);
        launcherGLView.setClipToPadding(false);

        GameCursorView cursorView = new GameCursorView(this);
        cursorView.setId(R.id.main_cursorview);
        cursorView.setVisibility(View.GONE);
        FrameLayout.LayoutParams cursorLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        cursorView.setLayoutParams(cursorLp);
        cursorView.setFocusable(false);
        cursorView.setTranslationZ(Tools.dpToPx(1));
        launcherGLView.addView(cursorView);
        launcherGLView.mCursorView = cursorView;

        loggerView = new LoggerView(this);
        loggerView.setId(R.id.mainLoggerView);
        loggerView.setVisibility(View.GONE);

        touchCharInput = new TouchCharInput(this);
        touchCharInput.setId(R.id.mainTouchCharInput);
        touchCharInput.setLayoutParams(new FrameLayout.LayoutParams((int)Tools.dpToPx(1), (int)Tools.dpToPx(1)));
        
        int imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            imeOptions |= EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        }
        touchCharInput.setImeOptions(imeOptions);
        touchCharInput.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);

        mHotbarView = new HotbarView(this);
        mHotbarView.setId(R.id.hotbar_view);
        mHotbarView.setLayoutParams(new FrameLayout.LayoutParams(0, 0));

        // Setup hierarchy in ControlLayout
        mControlLayout.addView(launcherGLView);
        mControlLayout.addView(touchCharInput);
        mControlLayout.addView(mHotbarView);

        LauncherComposeHelper.setLoadingText(getString(R.string.loading_screen_title));
        LauncherComposeHelper.setLoadingWarning(getString(R.string.loading_screen_warning));
        LauncherComposeHelper.setLoadingVisible(true);

        Platform.setCursorImplementor(cursorView);
    }

    @Override
    public void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        if(PREF_ENABLE_GYRO) mGyroControl.enable();
        PLATFORM.setHovered(true);
    }

    @Override
    protected void onPause() {
        ContextExecutor.clearActivity();
        mGyroControl.disable();
        // Avoid going through the JNI each time.
        if (Platform.isGrabbing()){
            CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE);
        }
        if(mQuickSettingSideDialog != null) {
            mQuickSettingSideDialog.cancel();
        }
        PLATFORM.setHovered(false);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PLATFORM.setVisible(true);
    }

    @Override
    protected void onStop() {
        PLATFORM.setVisible(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContextExecutor.clearActivity();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(mGyroControl != null) mGyroControl.updateOrientation();
        // Layout resize is practically guaranteed on a configuration change, and `onConfigurationChanged`
        // does not implicitly start a layout. So, request a layout and expect the screen dimensions to be valid after the]
        // post.
        if(mControlLayout == null) return;
        mControlLayout.requestLayout();
        mControlLayout.post(()->{
            // Child of mControlLayout, so refreshing size here is correct
            launcherGLView.refreshSize();
            mControlLayout.refreshControlButtonPositions();
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(mLoadingScreen != null && !(PLATFORM instanceof DummyBackend)) hideLoadingScreen();
        if(launcherGLView != null)  // Useful when backing out of the app
            Tools.MAIN_HANDLER.postDelayed(() -> launcherGLView.refreshSize(), 500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // Reload PREF_DEFAULTCTRL_PATH
            // If the storage root got unmounted/unreadable we won't be able to load the file anyway,
            // and MissingStorageActivity will be started.
            if(!Tools.checkStorageRoot(this)) return;
            LauncherPreferences.loadPreferences(getApplicationContext());
            try {
                mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runCraft(String versionId, File[] classpath) throws Throwable {
        String renderer = instance.getLaunchRenderer();
        if(!RendererCompatUtil.checkRendererCompatible(this, renderer)) {
            RendererCompatUtil.RenderersList renderersList = RendererCompatUtil.getCompatibleRenderers(this);
            String firstCompatibleRenderer = renderersList.rendererIds.get(0);
            Log.w("runCraft","Incompatible renderer "+renderer+ " will be replaced with "+firstCompatibleRenderer);
            renderer = firstCompatibleRenderer;
        }
        Logger.appendToLog("--------- Starting game with Launcher Debug!");
        Tools.printLauncherInfo(versionId, instance.getLaunchArgs(), renderer, this);
        JREUtils.redirectAndPrintJRELog();
        GameRunner.launchGame(this, account, instance, versionId, classpath, renderer);
        //Note that we actually stall in the above function, even if the game crashes. But let's be safe.
        Tools.runOnUiThread(()-> mServiceBinder.isActive = false);
    }

    private void dialogSendCustomKey() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.control_customkey);
        dialog.setItems(KeycodeUtils.generateKeyName(), (dInterface, position) -> KeycodeUtils.execKeyIndex(position));
        dialog.show();
    }

    boolean isInEditor;
    private void openCustomControls() {
        isInEditor = true;
        LauncherComposeHelper.setBaseMainContent(
                mMainComposeView,
                isInEditor,
                mControlLayout,
                loggerView,
                launcherGLView,
                true, // hostViews = true
                isOpen -> kotlin.Unit.INSTANCE,
                controller -> { mDrawerController = controller; return kotlin.Unit.INSTANCE; },
                action -> { onAction(action); return kotlin.Unit.INSTANCE; }
        );
        mControlLayout.setModifiable(true);
    }

    private void openLogOutput() {
        loggerView.setVisibility(View.VISIBLE);
    }

    private void openQuickSettings() {
        if(mQuickSettingSideDialog == null) {
            mQuickSettingSideDialog = new QuickSettingSideDialog() {
                @Override
                public void onResolutionChanged() {
                    launcherGLView.refreshSize();
                    mHotbarView.onResolutionChanged();
                }

                @Override
                public void onGyroStateChanged() {
                    mGyroControl.updateOrientation();
                    if (LauncherPreferences.PREF_ENABLE_GYRO) {
                        mGyroControl.enable();
                    } else {
                        mGyroControl.disable();
                    }
                }

                @Override
                public void onButtonTransparencyChanged() {
                    mControlLayout.updateButtonOpacity();
                }

                @Override
                public void onForceClose() {
                    dialogForceClose(GameActivity.this);
                }

                @Override
                public void onViewOutput() {
                    openLogOutput();
                }

                @Override
                public void onCustomKey() {
                    dialogSendCustomKey();
                }
            };
        }
        SideDialogUtils.show(mQuickSettingSideDialog, true);
    }

    public static void toggleMouse(Context ctx) {
        // Avoid going through the JNI each time.
        if (Platform.isGrabbing()) return;
        GameCursorView cursorView = Tools.getWeakReference(weakCursor);
        if(cursorView == null) return;
        int toastString = 0;
        switch (cursorView.getVisibility()) {
            case View.GONE:
            case View.INVISIBLE:
                toastString = R.string.control_mouseon;
                cursorView.setVisibility(View.VISIBLE);
                break;
            case View.VISIBLE:
                toastString = R.string.control_mouseoff;
                cursorView.setVisibility(View.GONE);
                break;
        }

        if(toastString != 0) Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(isInEditor) {
            if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) mControlLayout.askToExit(this);
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        boolean handleEvent;
        if(!(handleEvent = launcherGLView.processKeyEvent(event))) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && !touchCharInput.isEnabled()) {
                if(event.getAction() != KeyEvent.ACTION_UP) return true; // We eat it anyway
                CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE);
                return true;
            }
        }
        return handleEvent;
    }

    public static void switchKeyboardState(boolean panning) {
        if(touchCharInput != null) {
            touchCharInput.switchKeyboardState();
            GameActivity.mForcedPanningHeight = panning ? -1 : 0;
        }
    }

    public void hideLoadingScreen(){
        LauncherComposeHelper.setLoadingText(getString(R.string.loading_screen_booted, PLATFORM.backendName()));
        Tools.MAIN_HANDLER.postDelayed(() -> {
            LauncherComposeHelper.setLoadingVisible(false);
            mLoadingScreen = null;
        }, 1000);
    }

    @Override
    public void onClickedMenu() {
        if (mDrawerController != null) {
            mDrawerController.open();
        }
    }

    @Override
    public void exitEditor() {
        try {
            mControlLayout.loadLayout((CustomControls)null);
            mControlLayout.setModifiable(false);
            System.gc();
            mControlLayout.loadLayout(instance.getLaunchControls());
        } catch (Exception e) {
            Tools.showError(this,e);
        }

        isInEditor = false;
        LauncherComposeHelper.setBaseMainContent(
                mMainComposeView,
                isInEditor,
                mControlLayout,
                loggerView,
                launcherGLView,
                true, // hostViews = true
                isOpen -> kotlin.Unit.INSTANCE,
                controller -> { mDrawerController = controller; return kotlin.Unit.INSTANCE; },
                action -> { onAction(action); return kotlin.Unit.INSTANCE; }
        );
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        GameService.LocalBinder localBinder = (GameService.LocalBinder) service;
        mServiceBinder = localBinder;
        launcherGLView.start(localBinder.isActive);
        localBinder.isActive = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /*
     * Android 14 (or some devices, at least) seems to dispatch the captured mouse events as trackball events
     * due to a bug(?) somewhere(????)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean checkCaptureDispatchConditions(MotionEvent event) {
        int eventSource = event.getSource();
        // On my device, the mouse sends events as a relative mouse device.
        // Not comparing with == here because apparently `eventSource` is a mask that can
        // sometimes indicate multiple sources, like in the case of InputDevice.SOURCE_TOUCHPAD
        // (which is *also* an InputDevice.SOURCE_MOUSE when controlling a cursor)
        return (eventSource & InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource & InputDevice.SOURCE_MOUSE) != 0;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if(Tools.isAndroid8OrHigher() && checkCaptureDispatchConditions(ev))
            return launcherGLView.dispatchCapturedPointerEvent(ev);
        else return super.dispatchTrackballEvent(ev);
    }
}