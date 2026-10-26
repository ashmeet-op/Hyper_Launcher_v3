package com.ashmeet.hyperbackend.customcontrols.gamepad;

import android.util.Log;

import com.google.gson.JsonParseException;

import com.ashmeet.hyperlauncher.utils.HyperUtils;
import com.ashmeet.hyperbackend.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class GamepadMapStore {
    private static final File STORE_FILE = new File(HyperUtils.DIR_DATA, "gamepad_map.json");
    private static GamepadMapStore sMapStore;
    private GamepadMap mInMenuMap;
    private GamepadMap mInGameMap;
    private static GamepadMapStore createDefault() {
        GamepadMapStore mapStore = new GamepadMapStore();
        mapStore.mInGameMap = GamepadMap.getDefaultGameMap();
        mapStore.mInMenuMap = GamepadMap.getDefaultMenuMap();
        return mapStore;
    }

    private static void loadIfNecessary() {
        if(sMapStore == null) return;
        load();
    }

    public static void load() {
        GamepadMapStore mapStore = null;
        if(STORE_FILE.exists() && STORE_FILE.canRead()) {
            try {
                String storeFileContent = HyperUtils.read(STORE_FILE);
                mapStore = HyperUtils.GLOBAL_GSON.fromJson(storeFileContent, GamepadMapStore.class);
            } catch (JsonParseException | IOException e) {
                Log.w("GamepadMapStore", "Map store failed to load!", e);
            }
        }
        if(mapStore == null) mapStore = createDefault();
        sMapStore = mapStore;
    }

    public static void save() throws IOException {
        if(sMapStore == null) throw new RuntimeException("Must load map store first!");
        FileUtils.ensureParentDirectory(STORE_FILE);
        String jsonData = HyperUtils.GLOBAL_GSON.toJson(sMapStore);
        HyperUtils.write(STORE_FILE, jsonData);
    }

    public static GamepadMap getGameMap() {
        loadIfNecessary();
        return sMapStore.mInGameMap;
    }

    public static GamepadMap getMenuMap() {
        loadIfNecessary();
        return sMapStore.mInMenuMap;
    }
}
