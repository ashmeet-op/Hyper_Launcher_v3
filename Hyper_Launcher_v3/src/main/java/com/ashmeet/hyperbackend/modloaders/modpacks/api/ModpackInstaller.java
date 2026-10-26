package com.ashmeet.hyperbackend.modloaders.modpacks.api;

import android.util.Log;

import com.ashmeet.hyperlauncher.tracker.TrackerLayout;

import net.ashmeet.hyperlauncher.R;
import com.ashmeet.hyperlauncher.utils.HyperUtils;
import com.ashmeet.hyperbackend.instances.InstanceInstaller;
import com.ashmeet.hyperbackend.instances.Instances;
import com.ashmeet.hyperbackend.instances.Instance;
import com.ashmeet.hyperbackend.modloaders.modpacks.api.modloader.LoaderInstaller;
import com.ashmeet.hyperbackend.modloaders.modpacks.imagecache.ModIconCache;
import com.ashmeet.hyperbackend.modloaders.modpacks.models.ModDetail;
import com.ashmeet.hyperbackend.progresskeeper.DownloaderProgressWrapper;
import com.ashmeet.hyperbackend.utils.DownloadUtils;
import com.ashmeet.hyperbackend.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Callable;

public class ModpackInstaller {

    public static LoaderInstaller installModpack(String modpackName, String title, File modpackFile, String icon, InstallFunction installFunction) throws IOException {

        LoaderInstaller loaderInstaller;
        Instance instance = Instances.createInstance(i-> i.name = title, modpackName.substring(0, Math.min(16,modpackName.length())));
        try {

            loaderInstaller = installFunction.installModpack(modpackFile, instance.getGameDirectory());

            if(loaderInstaller == null) throw new IOException("Unknown modpack mod loader information");

            if(loaderInstaller.requiresGuiInstallation()) {
                InstanceInstaller instanceInstaller = loaderInstaller.createInstaller();
                if(instanceInstaller == null) throw new IOException("Failed to prepare data for instance installation");
                instance.installer = instanceInstaller;
            } else {
                String versionId = loaderInstaller.installHeadlessly();
                if(versionId == null) throw new IOException("Unknown mod loader version");
                instance.versionId = versionId;
            }
            instance.write();
            ModIconCache.writeInstanceImage(instance, icon);

            Instances.setSelectedInstance(instance);
            if(loaderInstaller.requiresGuiInstallation()) {
                instance.installer.start();
            }
        } catch (IOException e) {
            Instances.removeInstance(instance);
            throw e;
        } finally {
            modpackFile.delete();
            TrackerLayout.clearTracker(TrackerLayout.INSTALL_MODPACK);
        }

        return loaderInstaller;
    }

    public static LoaderInstaller downloadModpack(ModDetail modDetail, int selectedVersion, InstallFunction installFunction) throws IOException {
        String versionUrl = modDetail.versionUrls[selectedVersion];
        String versionHash = modDetail.versionHashes[selectedVersion];
        String modpackName = (modDetail.title.toLowerCase(Locale.ROOT) + " " + modDetail.versionNames[selectedVersion])
                .trim().replaceAll("[\\\\/:*?\"<>| \\t\\n]", "_" );
        String name = modDetail.title;
        String icon = modDetail.getIconCacheTag();

        if (modDetail.imageUrl != null && !modDetail.imageUrl.isEmpty()) {
            File cacheDir = ModIconCache.getImageCachePath();
            File iconFile = new File(cacheDir, icon + ".ca");
            if (!iconFile.exists()) {
                try {
                    FileUtils.ensureDirectorySilently(cacheDir);
                    DownloadUtils.downloadFile(modDetail.imageUrl, iconFile);
                } catch (IOException e) {
                    Log.e("ModpackInstaller", "Failed to download modpack icon", e);
                }
            }
        }

        if (versionHash != null) {
            modpackName += "_" + versionHash;
        }

        if (modpackName.length() > 255){
            modpackName = modpackName.substring(0,255);
        }

        File modpackFile = new File(HyperUtils.DIR_CACHE, modpackName + ".cf");

        byte[] downloadBuffer = new byte[8192];
        try {
            DownloadUtils.ensureSha1(modpackFile, versionHash, (Callable<Void>) () -> {
                DownloadUtils.downloadFileMonitored(versionUrl, modpackFile, downloadBuffer,
                        new DownloaderProgressWrapper(R.string.modpack_download_downloading_metadata,
                                TrackerLayout.INSTALL_MODPACK
                        )
                );
                return null;
            });
        } catch (IOException e) {
            modpackFile.delete();
            throw e;
        }

        return installModpack(modpackName, name, modpackFile, icon, installFunction);
    }

    public interface InstallFunction {
        LoaderInstaller installModpack(File modpackFile, File instanceDestination) throws IOException;
    }
}
