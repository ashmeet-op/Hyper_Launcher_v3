package com.ashmeet.hyperlauncher.profiles

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

object VersionSelectorDialog {
    @JvmStatic
    fun open(context: Context, hideCustomVersions: Boolean, listener: VersionSelectorListener) {
        var activity: FragmentActivity? = null
        if (context is FragmentActivity) {
            activity = context
        }
        
        if (activity == null) return

        val dialog = VersionSelectorDialogFragment()
        val bundle = Bundle()
        bundle.putBoolean("hideCustomVersions", hideCustomVersions)
        dialog.arguments = bundle
        dialog.setListener(listener)
        dialog.show(activity.supportFragmentManager, VersionSelectorDialogFragment.TAG)
    }
}
