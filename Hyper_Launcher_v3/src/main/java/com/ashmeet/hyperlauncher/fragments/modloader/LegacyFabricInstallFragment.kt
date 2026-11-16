package com.ashmeet.hyperlauncher.fragments.modloader

import com.ashmeet.hyperbackend.modloaders.FabriclikeUtils

class LegacyFabricInstallFragment : FabriclikeInstallFragment(FabriclikeUtils.LEGACY_FABRIC_UTILS, TAG) {
    companion object {
        const val TAG = "LegacyFabricInstallFragment"
    }
}
