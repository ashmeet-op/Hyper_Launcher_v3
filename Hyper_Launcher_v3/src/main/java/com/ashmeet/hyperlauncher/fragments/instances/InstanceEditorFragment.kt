package com.ashmeet.hyperlauncher.fragments.instances

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import com.ashmeet.hyperlauncher.fragments.dialog.DeleteConfirmDialogFragment
import com.ashmeet.hyperlauncher.fragments.selection.FileSelectorFragment
import com.ashmeet.hyperlauncher.profiles.VersionSelectorDialog
import com.ashmeet.hyperlauncher.screens.instances.InstanceEditorScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.RendererCompatUtil
import com.ashmeet.hyperlauncher.utils.Tools
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.utils.CropperUtils
import java.io.IOException

class InstanceEditorFragment : Fragment(), CropperUtils.CropperReceiver {

    private var mInstance: Instance? = null
    private var mInstanceName by mutableStateOf("")
    private var mVersionId by mutableStateOf("")
    private var mControlLayout by mutableStateOf("")
    private var mSharedData by mutableStateOf(false)
    private var mJvmArgs by mutableStateOf("")
    private var mSelectedRuntime by mutableStateOf<Runtime?>(null)
    private var mSelectedRenderer by mutableStateOf("")
    private var mInstanceIcon by mutableStateOf<Drawable?>(null)

    private var mInitialInstanceName = ""
    private var mInitialVersionId = ""
    private var mInitialControlLayout = ""
    private var mInitialSharedData = false
    private var mInitialJvmArgs = ""
    private var mInitialRuntimeName: String? = null
    private var mInitialRenderer: String? = null
    private var mIconChanged by mutableStateOf(false)

    private var mRuntimes: List<Runtime> = emptyList()
    private var mRenderNames: List<String> = emptyList()
    private var mRenderDisplayNames: List<String> = emptyList()

    private var mRecommendedIconSize = 256
    private lateinit var mCropperLauncher: ActivityResultLauncher<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCropperLauncher = CropperUtils.registerCropper(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val selectedInstance = Instances.loadSelectedInstance()
        if (selectedInstance == null) {
            Toast.makeText(requireContext(), R.string.no_instance, Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return View(requireContext())
        }

        if (mInstance == null) {
            loadValues(selectedInstance)
        }

        return ComposeView(requireContext()).apply {
            setContent {
                val value = remember { ExtraCore.consumeValue(ExtraConstants.FILE_SELECTOR) as? String }
                LaunchedEffect(value) {
                    if (value != null) {
                        mControlLayout = value
                    }
                }

                val hasChanges = mInstanceName != mInitialInstanceName ||
                        mVersionId != mInitialVersionId ||
                        mControlLayout != mInitialControlLayout ||
                        mSharedData != mInitialSharedData ||
                        mJvmArgs != mInitialJvmArgs ||
                        mSelectedRuntime?.name != mInitialRuntimeName ||
                        mSelectedRenderer != (mInitialRenderer ?: "default") ||
                        mIconChanged

                PojavTheme {
                    InstanceEditorScreen(
                        instanceName = mInstanceName,
                        onInstanceNameChange = { mInstanceName = it },
                        versionId = mVersionId,
                        onSelectVersion = { openVersionSelector() },
                        controlLayout = mControlLayout,
                        onSelectControl = { openControlSelector() },
                        sharedData = mSharedData,
                        onSharedDataChange = { mSharedData = it },
                        jvmArgs = mJvmArgs,
                        onJvmArgsChange = { mJvmArgs = it },
                        selectedRuntime = mSelectedRuntime,
                        runtimes = mRuntimes,
                        onRuntimeSelected = { mSelectedRuntime = it },
                        selectedRenderer = mSelectedRenderer,
                        renderers = mRenderNames + "default",
                        rendererDisplayNames = mRenderDisplayNames + getString(R.string.global_default),
                        onRendererSelected = { mSelectedRenderer = it },
                        instanceIcon = mInstanceIcon,
                        onChangeIcon = {
                            mRecommendedIconSize = 256
                            CropperUtils.startCropper(mCropperLauncher)
                        },
                        hasChanges = hasChanges,
                        onSave = { save() },
                        onDelete = { delete() },
                        onBack = { parentFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }

    private fun loadValues(instance: Instance) {
        mInstance = instance
        mInstanceIcon = InstanceIconProvider.fetchIcon(resources, instance)

        val runtimes = MultiRTUtils.getRuntimes().toMutableList()
        if (runtimes.none { it.name == "<Default>" }) {
            runtimes.add(Runtime("<Default>"))
        }
        mRuntimes = runtimes

        val jvmIndex = if (instance.selectedRuntime != null) {
            mRuntimes.indexOfFirst { it.name == instance.selectedRuntime }
        } else -1

        mSelectedRuntime = if (jvmIndex != -1) mRuntimes[jvmIndex] else mRuntimes.last()

        val renderersList = RendererCompatUtil.getCompatibleRenderers(requireContext())
        mRenderNames = renderersList.rendererIds.toList()
        mRenderDisplayNames = renderersList.rendererDisplayNames.toList()

        mSelectedRenderer = instance.renderer ?: "default"
        if (mSelectedRenderer != "default" && !mRenderNames.contains(mSelectedRenderer)) {
            mSelectedRenderer = "default"
        }

        mInstanceName = instance.name ?: ""
        mVersionId = instance.versionId ?: ""
        if (mControlLayout.isEmpty()) {
            mControlLayout = instance.controlLayout ?: ""
        }
        mSharedData = instance.sharedData
        mJvmArgs = instance.jvmArgs ?: ""

        mInitialInstanceName = mInstanceName
        mInitialVersionId = mVersionId
        mInitialControlLayout = mControlLayout
        mInitialSharedData = mSharedData
        mInitialJvmArgs = mJvmArgs
        mInitialRuntimeName = mSelectedRuntime?.name
        mInitialRenderer = instance.renderer
    }

    private fun openVersionSelector() {
        VersionSelectorDialog.open(requireActivity(), false) { id, _ ->
            mVersionId = id
        }
    }

    private fun openControlSelector() {
        val bundle = Bundle(3).apply {
            putBoolean(FileSelectorFragment.BUNDLE_SELECT_FOLDER, false)
            putString(FileSelectorFragment.BUNDLE_ROOT_PATH, Tools.CTRLMAP_PATH)
        }
        Tools.swapFragment(requireActivity(), FileSelectorFragment::class.java, FileSelectorFragment.TAG, bundle)
    }

    private fun save() {
        val instance = mInstance ?: return
        instance.versionId = mVersionId
        instance.controlLayout = mControlLayout.ifEmpty { null }
        instance.name = mInstanceName
        instance.jvmArgs = mJvmArgs.ifEmpty { null }
        instance.sharedData = mSharedData

        instance.selectedRuntime = if (mSelectedRuntime?.name == "<Default>" || mSelectedRuntime?.versionString == null) {
            null
        } else {
            mSelectedRuntime?.name
        }

        instance.renderer = if (mSelectedRenderer == "default") null else mSelectedRenderer

        try {
            InstanceIconProvider.dropIcon(instance)
            instance.write()
            mInitialInstanceName = mInstanceName
            mInitialVersionId = mVersionId
            mInitialControlLayout = mControlLayout
            mInitialSharedData = mSharedData
            mInitialJvmArgs = mJvmArgs
            mInitialRuntimeName = instance.selectedRuntime
            mInitialRenderer = instance.renderer
            mIconChanged = false

            ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, true)
            Toast.makeText(requireContext(), R.string.global_save, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        } catch (e: IOException) {
            Tools.showErrorRemote(e)
        }
    }

    private fun delete() {
        val dialogFragment = DeleteConfirmDialogFragment()
        dialogFragment.show(childFragmentManager, "delete_dialog_confirm")
    }

    override fun getAspectRatio(): Float = 1f
    override fun getTargetMaxSide(): Int = mRecommendedIconSize

    override fun onCropped(contentBitmap: Bitmap) {
        mInstanceIcon = contentBitmap.toDrawable(resources)
        try {
            mInstance?.encodeNewIcon(contentBitmap)
            mIconChanged = true
        } catch (e: IOException) {
            Tools.showErrorRemote(e)
        }
    }

    override fun onFailed(exception: Exception) {
        Tools.showErrorRemote(exception)
    }

    companion object {
        const val TAG = "InstanceEditorFragment"
    }
}
