package com.ashmeet.hyperlauncher.profiles

import android.app.Dialog
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import com.ashmeet.hyperlauncher.components.text.LegacyMigratedComponentsBridge
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

class VersionSelectorDialogFragment : DialogFragment() {

    private var listener: VersionSelectorListener? = null

    fun setListener(listener: VersionSelectorListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val hideCustomVersions = arguments?.getBoolean("hideCustomVersions") ?: false
        val builder = MaterialAlertDialogBuilder(context)

        val jVersionList = ExtraCore.getValue(ExtraConstants.RELEASE_TABLE) as? JVersionList
        val versionArray = jVersionList?.versions ?: emptyArray()
        val adapter = VersionListAdapter(versionArray, hideCustomVersions, context)

        val groups = mutableListOf<String>()
        val groupData = mutableListOf<List<String>>()
        for (i in 0 until adapter.groupCount) {
            groups.add(adapter.getGroupName(i))
            groupData.add(adapter.getGroupChildren(i))
        }

        val composeView = ComposeView(context)
        // ensureViewTreeOwners is still good to have, though DialogFragment handles it mostly
        LauncherComposeHelper.ensureViewTreeOwners(composeView)

        LegacyMigratedComponentsBridge.setVersionSelectorContent(
            composeView,
            groups,
            groupData
        ) { groupIdx, childIdx ->
            val version = adapter.getChild(groupIdx, childIdx)
            listener?.onVersionSelected(version, adapter.isSnapshotSelected(groupIdx))
            dismiss()
        }

        builder.setView(composeView)
        return builder.create()
    }

    companion object {
        const val TAG = "VersionSelectorDialogFragment"
    }
}
