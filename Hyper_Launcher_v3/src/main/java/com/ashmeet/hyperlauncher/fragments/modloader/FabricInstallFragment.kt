package com.ashmeet.hyperlauncher.fragments.modloader

import com.ashmeet.hyperbackend.modloaders.FabriclikeUtils

class FabricInstallFragment : FabriclikeInstallFragment(FabriclikeUtils.FABRIC_UTILS, TAG) {
    companion object {
        const val TAG = "FabricInstallFragment"
    }
}
