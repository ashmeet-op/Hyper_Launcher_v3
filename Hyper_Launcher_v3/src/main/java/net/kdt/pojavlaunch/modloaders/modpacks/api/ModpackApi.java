package net.kdt.pojavlaunch.modloaders.modpacks.api;


import android.content.Context;

import com.kdt.mcgui.ProgressLayout;

import com.ashmeet.hyperlauncher.activity.PojavApplication;
import net.ashmeet.hyperlauncher.R;
import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.api.modloader.LoaderInstaller;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;

import java.io.File;
import java.io.IOException;


public interface ModpackApi {


    SearchResult searchMod(SearchFilters searchFilters, SearchResult previousPageResult);


    ModDetail getModDetails(ModItem item);


    default void handleModpackInstallation(Context context, ModDetail modDetail, int selectedVersion) {


        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);
        PojavApplication.sExecutorService.execute(() -> {
            try {
                installModpack(modDetail, selectedVersion);
            }catch (IOException e) {
                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e);
            }
        });
    }

    LoaderInstaller installLocalModpack(String modpackName, File modpackFile, String icon) throws IOException;


    LoaderInstaller installModpack(ModDetail modDetail, int selectedVersion) throws IOException;
}
