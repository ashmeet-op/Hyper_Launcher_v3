package com.ashmeet.hyperbackend.modloaders.modpacks.api.modloader;

import com.ashmeet.hyperbackend.instances.InstanceInstaller;
import java.io.IOException;

public interface LoaderInstaller {
    boolean requiresGuiInstallation();
    InstanceInstaller createInstaller() throws IOException;
    String installHeadlessly() throws IOException;
    String getVersionId();
}
