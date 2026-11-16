package com.ashmeet.hyperbackend.modloaders.modpacks.api.modloader;

import com.ashmeet.hyperbackend.instances.InstanceInstaller;
import com.ashmeet.hyperbackend.modloaders.FabriclikeUtils;
import java.io.IOException;

public class FabriclikeLoaderInstaller implements LoaderInstaller {
    private final FabriclikeUtils utils;
    private final String mcVersion;
    private final String loaderVersion;

    public FabriclikeLoaderInstaller(FabriclikeUtils utils, String mcVersion, String loaderVersion) {
        this.utils = utils;
        this.mcVersion = mcVersion;
        this.loaderVersion = loaderVersion;
    }

    @Override
    public boolean requiresGuiInstallation() {
        return false;
    }

    @Override
    public InstanceInstaller createInstaller() throws IOException {
        return null;
    }

    @Override
    public String installHeadlessly() throws IOException {
        return utils.install(mcVersion, loaderVersion);
    }

    @Override
    public String getVersionId() {
        if (utils == FabriclikeUtils.FABRIC_UTILS) {
            return "fabric-loader-" + loaderVersion + "-" + mcVersion;
        }
        if (utils == FabriclikeUtils.QUILT_UTILS) {
            return "quilt-loader-" + loaderVersion + "-" + mcVersion;
        }
        if (utils == FabriclikeUtils.LEGACY_FABRIC_UTILS) {
            return "legacy-fabric-loader-" + loaderVersion + "-" + mcVersion;
        }
        return null;
    }
}
