package com.ashmeet.hyperbackend.vulkan.driver;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.ashmeet.hyperbackend.utils.JSONUtils;
import com.ashmeet.hyperbackend.vulkan.DriverManager;

// AdrenoTools package driver
public class AdrenoDriver extends DisplayDriver {
    public String description;
    public String libraryName;
    public int schemaVersion;
    public int minApi;

    public AdrenoDriver() {}

    public AdrenoDriver(String name, String description, String author, String driverVersion, String libraryName, int schemaVersion, int minApi) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.driverVersion = driverVersion;
        this.libraryName = libraryName;
        this.schemaVersion = schemaVersion;
        this.minApi = minApi;
    }

    public static AdrenoDriver fromJson(File metadata) {
        AdrenoDriver adrenoDriver;
        try {
            adrenoDriver = JSONUtils.readFromFile(metadata, AdrenoDriver.class);
        } catch (IOException e) {
            Log.i(DriverManager.TAG, "Failed to read metadata from " + metadata.getAbsolutePath() + "!");
            return null;
        }
        return adrenoDriver;
    }

    public static AdrenoDriver fromJson(InputStream is) throws IOException {
        return JSONUtils.readFromStream(is, AdrenoDriver.class);
    }

    @Override
    public String getMainLibrary() {
        return libraryName;
    }
}
