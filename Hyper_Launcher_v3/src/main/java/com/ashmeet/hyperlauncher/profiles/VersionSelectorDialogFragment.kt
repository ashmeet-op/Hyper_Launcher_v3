package com.ashmeet.hyperlauncher.profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import com.ashmeet.hyperlauncher.components.text.LegacyMigratedComponentsBridge
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

class VersionSelectorDialogFragment : DialogFragment() {

    private var listener: VersionSelectorListener? = null

    fun setListener(listener: VersionSelectorListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val hideCustomVersions = arguments?.getBoolean("hideCustomVersions") ?: false

        val jVersionList = ExtraCore.getValue(ExtraConstants.RELEASE_TABLE) as? JVersionList
        val versionArray = jVersionList?.versions ?: emptyArray()
        val adapter = VersionListAdapter(versionArray, hideCustomVersions, context)

        val groups = mutableListOf<String>()
        val groupData = mutableListOf<List<String>>()
        for (i in 0 until adapter.groupCount) {
            groups.add(adapter.getGroupName(i))
            groupData.add(adapter.getGroupChildren(i))
        }

        return ComposeView(context).apply {
            LauncherComposeHelper.ensureViewTreeOwners(this)

            LegacyMigratedComponentsBridge.setVersionSelectorContent(
                this,
                groups,
                groupData,
                onDismiss = { dismiss() }
            ) { groupIdx, childIdx ->
                val version = adapter.getChild(groupIdx, childIdx)
                listener?.onVersionSelected(version, adapter.isSnapshotSelected(groupIdx))
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "VersionSelectorDialogFragment"
    }
}
