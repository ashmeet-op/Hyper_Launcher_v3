package net.kdt.pojavlaunch.game.platform;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.InputDevice;
import android.view.Surface;
import android.view.View;

import net.kdt.pojavlaunch.awt.AWTBridge;
import net.kdt.pojavlaunch.game.GameView;
import net.kdt.pojavlaunch.game.GameActivity;
import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.customcontrols.gamepad.DefaultDataProvider;
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad;
import net.kdt.pojavlaunch.game.platform.backend.AWTBackend;
import net.kdt.pojavlaunch.game.platform.backend.DummyBackend;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.game.platform.backend.GLFWBackend;
import net.kdt.pojavlaunch.game.platform.backend.PlatformBackend;
import net.kdt.pojavlaunch.game.platform.backend.SDLBackend;
import net.kdt.pojavlaunch.game.platform.clipboard.AndroidClipboard;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursor;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursorImplementor;
import net.kdt.pojavlaunch.game.platform.input.PlatformGamepad;
import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.game.platform.input.gamepad.GLFWGamepad;
import net.kdt.pojavlaunch.game.platform.input.gamepad.GenericGamepad;
import net.kdt.pojavlaunch.game.platform.input.gamepad.SDLGamepad;

import java.util.ArrayList;
import java.util.List;

import fr.spse.gamepad_remapper.RemapperManager;
import fr.spse.gamepad_remapper.RemapperView;
import git.artdeell.dnbootstrap.glfw.GLFW;
import git.mojo.sdl.SDLActivity;
import git.mojo.sdl.SDLControllerManager;


public class Platform {

    private static final boolean RESET_CURSOR_UNGRAB = true;
    public static PlatformBackend PLATFORM = new DummyBackend();
    public static double cursorX;
    public static double cursorY;
    private static final List<PlatformGrabListener> grabListeners = new ArrayList<>();
    private static PlatformCursorImplementor mCursorImplementor = null;
    private static boolean isGrabbing = false;
    private static Surface mPendingSurface;
    private static PlatformGamepad mPlatformGamepad = null;
    private static PlatformCursor mPlatformCursor = null;
    private static AndroidClipboard mClipboard;
    private static GameView mHostView;
    private static RemapperManager mInputManager;


    public static void initialize(Activity activity, GameView view) {
        Platform.mHostView = view;
        Platform.mInputManager = createRemapperManager(view);
        mClipboard = new AndroidClipboard(activity.getApplicationContext());
        GLFW.setInitCallback(() -> onInit(new GLFWBackend()));
        SDLActivity.setInitCallback(() -> onInit(new SDLBackend()));
        AWTBridge.setEnableCallback(() -> onInit(new AWTBackend()));
        SDLActivity.setClipboard(mClipboard);
        GLFW.setClipboardImpl(mClipboard);



        SDLControllerManager.setEnabledCallback(() -> setPlatformGamepad(new SDLGamepad()));

        GLFW.setGamepadEnableHandler(() -> setPlatformGamepad(new GLFWGamepad(view.getContext(), mInputManager)));
        SDLBackend.initialize(activity);
    }

    public static void initializeMinimal(Context appContext) {
        Platform.mHostView = null;
        Platform.mInputManager = null;
        mClipboard = new AndroidClipboard(appContext);

    }

    private static void onInit(PlatformBackend impl) {

        Platform.setPlatformLibrary(impl);
        Log.i("Platform", "Init backend : " + impl.backendName());
        ContextExecutor.executeActivity(activity -> ((GameActivity) activity).hideLoadingScreen());
        resetCursorPosition();
    }


    public static boolean isGrabbing() {
        return isGrabbing;
    }


    public static void grabStateChanged(boolean grabbing) {
        boolean wasGrabbing = isGrabbing;
        isGrabbing = grabbing;
        Tools.runOnUiThread(() -> {
            if (RESET_CURSOR_UNGRAB && wasGrabbing && !isGrabbing) resetCursorPosition();
            if (mCursorImplementor != null) mCursorImplementor.onGrabState(grabbing);
            for (PlatformGrabListener listener : grabListeners) {
                listener.onGrabState(grabbing);
            }
        });
    }


    public static PlatformGamepad getPlatformGamepad() {
        return mPlatformGamepad;
    }


    public static PlatformCursor getCursor() {
        return mPlatformCursor;
    }


    public static void setCursor(Bitmap bitmap, int xhot, int yhot) {
        mPlatformCursor = bitmap == null ? null : new PlatformCursor(bitmap, xhot, yhot);
        mCursorImplementor.onCursorChanged();
    }


    public static PlatformCursorImplementor getCursorImplementor() {
        return mCursorImplementor;
    }


    public static void setCursorImplementor(PlatformCursorImplementor implementor) {
        mCursorImplementor = implementor;
    }

    private static void setPlatformGamepad(PlatformGamepad gamepad){
        if(mPlatformGamepad != null)
            mPlatformGamepad.onDestroy();
        mPlatformGamepad = gamepad;
    }


    public static void createGenericGamepad(InputDevice device, View touchpadView){
        if(mHostView == null || mInputManager == null) return;
        Gamepad gamepad = new Gamepad(device, DefaultDataProvider.INSTANCE, touchpadView);
        setPlatformGamepad(new GenericGamepad(mHostView.getContext(), mInputManager, gamepad));
    }


    public static void setCursorPosition(double x, double y) {
        cursorX = x;
        cursorY = y;
        clampCursorPosition();
        mCursorImplementor.onCursorPosition();
    }


    public static void clampCursorPosition() {
        cursorX = Math.clamp(cursorX, 0, GameView.getWindowWidth());
        cursorY = Math.clamp(cursorY, 0f, GameView.getWindowHeight());
    }


    public static void resetCursorPosition() {
        cursorX = (double) GameView.getWindowWidth() / 2;
        cursorY = (double) GameView.getWindowHeight() / 2;
    }


    public static void floorCursorPosition(){
        cursorX = Math.floor(cursorX);
        cursorY = Math.floor(cursorY);
    }


    public static void sendCursorPosition() {
        if(mCursorImplementor != null) mCursorImplementor.onCursorPosition();
        if (!isGrabbing) clampCursorPosition();
        else floorCursorPosition();
        PLATFORM.sendMousePosition();
    }


    public static void addGrabListener(PlatformGrabListener pgl) {
        grabListeners.add(pgl);
    }


    public static void updateSurface(Surface surface) {
        mPendingSurface = surface;
        PLATFORM.surfaceCreated(surface);
    }


    public static void setPlatformLibrary(PlatformBackend backend) {
        if(PLATFORM != null) PLATFORM.surfaceDestroyed();
        PLATFORM = backend;

        if (mPendingSurface != null)
            PLATFORM.surfaceCreated(mPendingSurface);
    }


    public static AndroidClipboard getClipboard() {
        return mClipboard;
    }

    private static RemapperManager createRemapperManager(View view){
        return new RemapperManager(view.getContext(), new RemapperView.Builder(null)
                .remapA(true)
                .remapB(true)
                .remapX(true)
                .remapY(true)
                .remapLeftJoystick(true)
                .remapRightJoystick(true)
                .remapStart(true)
                .remapSelect(true)
                .remapLeftShoulder(true)
                .remapRightShoulder(true)
                .remapLeftTrigger(true)
                .remapRightTrigger(true)
                .remapDpad(true));
    }
}