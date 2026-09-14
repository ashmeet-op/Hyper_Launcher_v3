package com.ashmeet.hyperlauncher.utils.helper

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ashmeet.hyperlauncher.screens.game.LoggerView
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.game.GameView

object LauncherComposeHelper {
    val isLoading get() = LoadingComposeHelper.isLoading
    val loadingText get() = LoadingComposeHelper.loadingText
    val loadingWarning get() = LoadingComposeHelper.loadingWarning

    @JvmStatic
    fun setLoadingVisible(visible: Boolean) {
        LoadingComposeHelper.setLoadingVisible(visible)
    }

    @JvmStatic
    fun setLoadingText(text: String) {
        LoadingComposeHelper.setLoadingText(text)
    }

    @JvmStatic
    fun setLoadingWarning(warning: String) {
        LoadingComposeHelper.setLoadingWarning(warning)
    }

    interface OnFragmentViewCreatedListener {
        fun onCreated(view: FrameLayout)
    }

    interface DrawerController {
        fun open()
        fun close()
        fun toggle()
        fun isOpen(): Boolean
    }

    @JvmStatic
    fun ensureViewTreeOwners(view: View) {
        val activity = findActivity(view.context)
        if (activity != null) {
            view.setViewTreeLifecycleOwner(activity)
            view.setViewTreeViewModelStoreOwner(activity)
            view.setViewTreeSavedStateRegistryOwner(activity)
        }
        if (view is ComposeView) {
            view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
    }

    private fun findActivity(context: Context): FragmentActivity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    @JvmStatic
    fun setSettingsIcon(iconRes: Int) {
        LauncherMainComposeHelper.setSettingsIcon(iconRes)
    }

    @JvmStatic
    fun setFileManagerVisible(visible: Boolean) {
        LauncherMainComposeHelper.setFileManagerVisible(visible)
    }

    @JvmStatic
    fun setExitContent(
        composeView: ComposeView,
        title: String,
        logs: String,
        onShareClick: () -> Unit,
        onCopyClick: () -> Unit,
        onRestartClick: () -> Unit,
        onOpenCrashReport: (String) -> Unit
    ) {
        ExitComposeHelper.setExitContent(composeView, title, logs, onShareClick, onCopyClick, onRestartClick, onOpenCrashReport)
    }

    @JvmStatic
    fun setContent(
        activity: FragmentActivity,
        onSettingsClick: Runnable,
        onContentInstallerClick: Runnable,
        onInstanceDirectoryClick: Runnable,
        onFragmentViewCreated: OnFragmentViewCreatedListener
    ) {
        LauncherMainComposeHelper.setContent(activity, onSettingsClick, onContentInstallerClick, onInstanceDirectoryClick, onFragmentViewCreated)
    }

    @JvmStatic
    fun setBaseMainContent(
        composeView: ComposeView,
        isInEditor: Boolean,
        controlLayout: ControlLayout,
        loggerView: LoggerView,
        gameView: GameView?,
        hostViews: Boolean,
        onDrawerStateChanged: ((Boolean) -> Unit)?,
        onDrawerControllerCreated: (DrawerController) -> Unit,
        onAction: (Int) -> Unit
    ) {
        GameComposeHelper.setBaseMainContent(composeView, isInEditor, controlLayout, loggerView, gameView, hostViews, onDrawerStateChanged, onDrawerControllerCreated, onAction)
    }

    @JvmStatic
    fun setControlsEditorContent(
        composeView: ComposeView,
        controlLayout: ControlLayout,
        onAction: (Int) -> Unit
    ) {
        GameComposeHelper.setControlsEditorContent(composeView, controlLayout, onAction)
    }

    @JvmStatic
    fun setImportControlContent(
        composeView: ComposeView,
        initialFileName: String,
        onImport: (String) -> Unit
    ) {
        ImportComposeHelper.setImportControlContent(composeView, initialFileName, onImport)
    }
}
