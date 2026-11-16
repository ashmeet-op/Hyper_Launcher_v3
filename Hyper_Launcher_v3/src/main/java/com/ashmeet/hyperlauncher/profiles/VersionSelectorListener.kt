package com.ashmeet.hyperlauncher.profiles

fun interface VersionSelectorListener {
    fun onVersionSelected(versionId: String, isSnapshot: Boolean)
}
