package com.ashmeet.hyperlauncher.profiles

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.ashmeet.hyperlauncher.utils.Tools
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.utils.FilteredSubList
import java.io.File

class VersionListAdapter(versionList: Array<JVersionList.Version>, private val hideCustomVersions: Boolean, ctx: Context) : BaseExpandableListAdapter() {
    private val mLayoutInflater: LayoutInflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val mGroups: Array<String>
    private val mInstalledVersions: Array<String>?
    private val mData: Array<List<*>>
    private val mSnapshotListPosition: Int

    init {
        val releaseList = FilteredSubList(versionList) { item: JVersionList.Version -> item.type == "release" }
        val snapshotList = FilteredSubList(versionList) { item: JVersionList.Version -> item.type == "snapshot" }
        val betaList = FilteredSubList(versionList) { item: JVersionList.Version -> item.type == "old_beta" }
        val alphaList = FilteredSubList(versionList) { item: JVersionList.Version -> item.type == "old_alpha" }

        // Query installed versions
        mInstalledVersions = File(Tools.DIR_GAME_NEW + "/versions").list()
        mInstalledVersions?.sort()

        if (!areInstalledVersionsAvailable()) {
            mGroups = arrayOf(
                ctx.getString(R.string.mcl_setting_veroption_release),
                ctx.getString(R.string.mcl_setting_veroption_snapshot),
                ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            )
            mData = arrayOf(releaseList, snapshotList, betaList, alphaList)
            mSnapshotListPosition = 1
        } else {
            mGroups = arrayOf(
                ctx.getString(R.string.mcl_setting_veroption_installed),
                ctx.getString(R.string.mcl_setting_veroption_release),
                ctx.getString(R.string.mcl_setting_veroption_snapshot),
                ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            )
            mData = arrayOf(mInstalledVersions!!.toList(), releaseList, snapshotList, betaList, alphaList)
            mSnapshotListPosition = 2
        }
    }

    fun getGroupName(groupPosition: Int): String {
        return mGroups[groupPosition]
    }

    fun getGroupChildren(groupPosition: Int): List<String> {
        val children = mutableListOf<String>()
        for (i in 0 until getChildrenCount(groupPosition)) {
            children.add(getChild(groupPosition, i))
        }
        return children
    }

    override fun getGroupCount(): Int {
        return mGroups.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return mData[groupPosition].size
    }

    override fun getGroup(groupPosition: Int): Any {
        return mData[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): String {
        if (isInstalledVersionSelected(groupPosition)) {
            return mInstalledVersions!![childPosition]
        }
        return (mData[groupPosition][childPosition] as JVersionList.Version).id
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        }
        (view as TextView).text = mGroups[groupPosition]
        return view
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        }
        (view as TextView).text = getChild(groupPosition, childPosition)
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    fun isSnapshotSelected(groupPosition: Int): Boolean {
        return groupPosition == mSnapshotListPosition
    }

    private fun areInstalledVersionsAvailable(): Boolean {
        if (hideCustomVersions) return false
        return !mInstalledVersions.isNullOrEmpty()
    }

    private fun isInstalledVersionSelected(groupPosition: Int): Boolean {
        return groupPosition == 0 && areInstalledVersionsAvailable()
    }
}
