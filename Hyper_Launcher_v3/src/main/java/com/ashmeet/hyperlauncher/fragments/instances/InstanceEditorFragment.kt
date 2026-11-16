package com.ashmeet.hyperlauncher.fragments.instances

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import com.ashmeet.hyperbackend.game.renderer.RendererCache
import com.ashmeet.hyperbackend.instances.Instance
import com.ashmeet.hyperbackend.instances.InstanceIconProvider
import com.ashmeet.hyperbackend.instances.Instances
import com.ashmeet.hyperbackend.utils.CropperUtils
import com.ashmeet.hyperlauncher.extra.ExtraConstants
import com.ashmeet.hyperlauncher.extra.ExtraCore
import com.ashmeet.hyperlauncher.fragments.dialog.DeleteConfirmDialogFragment
import com.ashmeet.hyperlauncher.profiles.VersionSelectorDialog
import com.ashmeet.hyperlauncher.runtime.Runtime
import com.ashmeet.hyperlauncher.runtime.RuntimeUtils
import com.ashmeet.hyperlauncher.screens.instances.InstanceEditorScreen
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.horizon.HorizonUtils
import net.ashmeet.hyperlauncher.R
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

    private var mHorizonRenderScale by mutableFloatStateOf(1.0f)
    private var mHorizonOrientation by mutableStateOf("sensor_landscape")
    private var mHorizonInputHost by mutableStateOf("keyboard_mouse")
    private var mHorizonUseHorizonControls by mutableStateOf(false)

    private var mInitialInstanceName = ""
    private var mInitialVersionId = ""
    private var mInitialControlLayout = ""
    private var mInitialSharedData = false
    private var mInitialJvmArgs = ""
    private var mInitialRuntimeName: String? = null
    private var mInitialRenderer: String? = null
    private var mInitialHorizonRenderScale = 1.0f
    private var mInitialHorizonOrientation = "sensor_landscape"
    private var mInitialHorizonInputHost = "keyboard_mouse"
    private var mInitialHorizonUseHorizonControls = false
    private var mIconChanged by mutableStateOf(false)

    private var mRuntimes: List<Runtime> = emptyList()
    private var mRenderNames: List<String> = emptyList()
    private var mRenderDisplayNames: List<String> = emptyList()

    private var mRecommendedIconSize = 256
    private lateinit var mCropperLauncher: ActivityResultLauncher<*>
    private var mIsNewInstance by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCropperLauncher = CropperUtils.registerCropper(this, this)
        mIsNewInstance = arguments?.getBoolean("is_new_instance", false) == true
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

        if (mInstance == null || mInstance?.mInstanceRoot != selectedInstance.mInstanceRoot) {
            loadValues(selectedInstance)
        } else if (mControlLayout.isEmpty() && !selectedInstance.controlLayout.isNullOrEmpty()) {
            mControlLayout = selectedInstance.controlLayout ?: ""
        }

        return ComposeView(requireContext()).apply {
            setContent {
                val hasChanges = mIsNewInstance ||
                        mInstanceName != mInitialInstanceName ||
                        mVersionId != mInitialVersionId ||
                        mControlLayout != mInitialControlLayout ||
                        mSharedData != mInitialSharedData ||
                        mJvmArgs != mInitialJvmArgs ||
                        mSelectedRuntime?.name != mInitialRuntimeName ||
                        mSelectedRenderer != (mInitialRenderer ?: "default") ||
                        mHorizonRenderScale != mInitialHorizonRenderScale ||
                        mHorizonOrientation != mInitialHorizonOrientation ||
                        mHorizonInputHost != mInitialHorizonInputHost ||
                        mHorizonUseHorizonControls != mInitialHorizonUseHorizonControls ||
                        mIconChanged

                val isHorizon = HorizonUtils.isHorizonInstance(mInstance) ||
                        HorizonUtils.isHorizonInstance(mVersionId, mSelectedRuntime?.name)

                PojavTheme {
                    InstanceEditorScreen(
                        instanceName = mInstanceName,
                        onInstanceNameChange = { mInstanceName = it },
                        versionId = mVersionId,
                        onSelectVersion = { openVersionSelector() },
                        controlLayout = mControlLayout,
                        onControlLayoutChange = { mControlLayout = it },
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
                        isHorizon = isHorizon,
                        horizonRenderScale = mHorizonRenderScale,
                        onHorizonRenderScaleChange = { mHorizonRenderScale = it },
                        horizonOrientation = mHorizonOrientation,
                        onHorizonOrientationChange = { mHorizonOrientation = it },
                        horizonInputHost = mHorizonInputHost,
                        onHorizonInputHostChange = { mHorizonInputHost = it },
                        horizonUseHorizonControls = mHorizonUseHorizonControls,
                        onHorizonUseHorizonControlsChange = { mHorizonUseHorizonControls = it },
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

        val runtimes = mutableListOf(Runtime("auto"))
        runtimes.addAll(RuntimeUtils.getRuntimes())
        if (runtimes.none { it.name == "<Default>" }) {
            runtimes.add(Runtime("<Default>"))
        }
        mRuntimes = runtimes

        val jvmIndex = if (instance.selectedRuntime != null) {
            mRuntimes.indexOfFirst { it.name == instance.selectedRuntime }
        } else -1

        mSelectedRuntime = if (jvmIndex != -1) mRuntimes[jvmIndex] else mRuntimes.find { it.name == "<Default>" } ?: mRuntimes.last()

        val renderersList = RendererCache.getCompatibleRenderers(requireContext())
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

        mHorizonRenderScale = instance.horizonRenderScale ?: 1.0f
        mHorizonOrientation = instance.horizonOrientation ?: "sensor_landscape"
        mHorizonInputHost = instance.horizonInputHost ?: "keyboard_mouse"
        mHorizonUseHorizonControls = instance.horizonUseHorizonControls

        mInitialInstanceName = mInstanceName
        mInitialVersionId = mVersionId
        mInitialControlLayout = mControlLayout
        mInitialSharedData = mSharedData
        mInitialJvmArgs = mJvmArgs
        mInitialRuntimeName = mSelectedRuntime?.name
        mInitialRenderer = instance.renderer
        mInitialHorizonRenderScale = mHorizonRenderScale
        mInitialHorizonOrientation = mHorizonOrientation
        mInitialHorizonInputHost = mHorizonInputHost
        mInitialHorizonUseHorizonControls = mHorizonUseHorizonControls
    }

    private fun openVersionSelector() {
        VersionSelectorDialog.open(requireActivity(), false) { id, _ ->
            mVersionId = id
        }
    }

    private fun save() {
        val instance = mInstance ?: return
        instance.versionId = mVersionId
        instance.controlLayout = mControlLayout.ifEmpty { null }
        instance.name = mInstanceName
        instance.jvmArgs = mJvmArgs.ifEmpty { null }
        instance.sharedData = mSharedData
        instance.horizonRenderScale = mHorizonRenderScale
        instance.horizonOrientation = mHorizonOrientation
        instance.horizonInputHost = mHorizonInputHost
        instance.horizonUseHorizonControls = mHorizonUseHorizonControls

        instance.selectedRuntime = if (mSelectedRuntime?.name == "<Default>" || (mSelectedRuntime?.versionString == null && mSelectedRuntime?.name != "auto")) {
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
            mInitialHorizonRenderScale = mHorizonRenderScale
            mInitialHorizonOrientation = mHorizonOrientation
            mInitialHorizonInputHost = mHorizonInputHost
            mInitialHorizonUseHorizonControls = mHorizonUseHorizonControls
            mIconChanged = false
            mIsNewInstance = false

            ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, true)
            Toast.makeText(requireContext(), R.string.global_save, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        } catch (e: IOException) {
            HyperUtils.showErrorRemote(e)
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
            HyperUtils.showErrorRemote(e)
        }
    }

    override fun onFailed(exception: Exception) {
        HyperUtils.showErrorRemote(exception)
    }

    companion object {
        const val TAG = "InstanceEditorFragment"
    }
}
