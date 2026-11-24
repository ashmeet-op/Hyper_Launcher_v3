package com.ashmeet.hyperlauncher.utils.helper

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ashmeet.hyperbackend.customcontrols.ControlLayout
import com.ashmeet.hyperbackend.game.GameView
import com.ashmeet.hyperlauncher.screens.game.LoggerView

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
        fun onCreated(view: ViewGroup)
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
        if (activity is LifecycleOwner) {
            view.setViewTreeLifecycleOwner(activity)
        }
        if (activity is ViewModelStoreOwner) {
            view.setViewTreeViewModelStoreOwner(activity)
        }
        if (activity is SavedStateRegistryOwner) {
            view.setViewTreeSavedStateRegistryOwner(activity)
        }
        var curr: Context? = view.context
        while (curr is ContextWrapper) {
            if (curr is LifecycleOwner) {
                view.setViewTreeLifecycleOwner(curr)
            }
            if (curr is ViewModelStoreOwner) {
                view.setViewTreeViewModelStoreOwner(curr)
            }
            if (curr is SavedStateRegistryOwner) {
                view.setViewTreeSavedStateRegistryOwner(curr)
            }
            curr = curr.baseContext
        }
        if (view is ComposeView) {
            view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
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
        logs: String,
        onShareClick: () -> Unit,
        onRestartClick: () -> Unit,
        onOpenCrashReport: (String) -> Unit
    ) {
        ExitComposeHelper.setExitContent(composeView,
            logs, onShareClick, onRestartClick, onOpenCrashReport)
    }

    @JvmStatic
    fun setContent(
        activity: FragmentActivity,
        onSettingsClick: Runnable,
        onContentInstallerClick: Runnable,
        onInstanceDirectoryClick: Runnable,
        onRecordingsGalleryClick: Runnable,
        onFragmentViewCreated: OnFragmentViewCreatedListener
    ) {
        LauncherMainComposeHelper.setContent(
            activity,
            onSettingsClick,
            onContentInstallerClick,
            onInstanceDirectoryClick,
            onRecordingsGalleryClick,
            onFragmentViewCreated
        )
    }

    @JvmStatic
    @JvmOverloads
    fun setBaseMainContent(
        composeView: ComposeView,
        isInEditor: Boolean,
        controlLayout: ControlLayout? = null,
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
