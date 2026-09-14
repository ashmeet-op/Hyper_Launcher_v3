package com.ashmeet.hyperlauncher.profiles

interface VersionSelectorListener {
    fun onVersionSelected(versionId: String, isSnapshot: Boolean)
}
