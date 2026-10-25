package com.ashmeet.hyperbackend.vulkan;

import android.os.Build;
import android.util.Log;

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;
import com.ashmeet.hyperlauncher.utils.Tools;
import com.ashmeet.hyperbackend.utils.FileUtils;
import com.ashmeet.hyperbackend.utils.JSONUtils;
import com.ashmeet.hyperbackend.utils.ZipUtils;
import com.ashmeet.hyperbackend.vulkan.driver.AdrenoDriver;
import com.ashmeet.hyperbackend.vulkan.driver.BuiltinDriver;
import com.ashmeet.hyperbackend.vulkan.driver.DisplayDriver;
import com.ashmeet.hyperbackend.vulkan.driver.Driver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * AdrenoTools driver package manager
 */
public class DriverManager {
    private static final String METADATA_FILENAME = "meta.json";
    public static final String TAG = "DriverManager";
    public static final BuiltinDriver DEFAULT_DRIVER = new BuiltinDriver();
    // Must be in the exec place, /sdcard/... is noexec
    private static final File packagesPath = new File(Tools.DIR_DATA, "vulkan-drivers");
    private static Boolean supported = null;

    private DriverManager() {}

    /**
     * Checks if DriverManager i.e. custom drivers are supported on the current device
     * @return True if requirements are met, false otherwise
     */
    public static boolean isSupportedByDevice(){
        if (supported == null) {
            supported = Build.VERSION.SDK_INT >= 28;
        }
        return supported;
    }

    /**
     * Loads and returns a preferred driver. Returns a default driver if there is no preferred driver or it has failed loading
     * @return A driver
     */
    public static Driver getPreferredDriver() {
        String hash = getPreferredDriverHash();
        Driver driver = hash == null ? DEFAULT_DRIVER : loadDriver(hash);
        if(driver == null) {
            Log.e(TAG, "Failed to load preferred driver package " + hash);
            return DEFAULT_DRIVER;
        }
        return driver;
    }

    /**
     * Checks if the provided driver is selected as the default/preferred
     * @param driver A driver to check
     * @return True if the provided driver is used
     */
    public static boolean isPreferredDriver(Driver driver){
        String pkg = getPreferredDriverHash();
        // selected default package
        if(pkg == null && (driver == null || driver instanceof BuiltinDriver))
            return true;
        if(pkg != null && driver != null) {
            return pkg.equals(driver.getHash());
        }
        return false;
    }

    /**
     * Gets the hash string of a preferred driver
     * @return A hash
     */
    public static String getPreferredDriverHash() {
        String driverPref = LauncherPreferences.PREF_DRIVER;
        if (driverPref == null || driverPref.equals("default") || driverPref.isEmpty()) {
            return LauncherPreferences.getPREF_VULKAN_DRIVER();
        }
        return driverPref;
    }

    /**
     * Gets an absolute path to the Vulkan library of the preferred driver
     * @return A path
     */
    public static String getPreferredDriverLibraryPath(){
        Driver driver = getPreferredDriver();
        if(driver instanceof BuiltinDriver) return driver.getMainLibrary(); // Default driver should be in the library path already
        File path = new File(packagesPath,  (driver.getHash() + "/" + driver.getMainLibrary()));
        if(path.exists()) return path.getAbsolutePath();

        // Invalid driver
        Log.e(TAG, "Unable to resolve Vulkan library: path " + path.getAbsolutePath() + " is not a file or does not exist");
        return DEFAULT_DRIVER.getMainLibrary();
    }

    /**
     * Gets an absolute path to the root directory of the preferred driver
     * @return A path
     */
    public static String getPreferredDriverRootPath(){
        Driver driver = getPreferredDriver();
        if(driver instanceof BuiltinDriver) return null; // Already in the library path
        File path = new File(packagesPath,  driver.getHash());
        if(path.exists())
            return path.getAbsolutePath();
        else return null;
    }

    /**
     * Sets the preferred driver
     * @param driver A driver to set
     */
    public static void setPreferredDriver(Driver driver){
        if(driver == null || driver instanceof BuiltinDriver) driver = DEFAULT_DRIVER;
        String hash = driver.getHash(); // Builtin will return null
        LauncherPreferences.setPREF_VULKAN_DRIVER(hash);
        if (LauncherPreferences.getPrefs() != null) {
            LauncherPreferences.getPrefs().edit().putString("driver", hash != null ? hash : "default").apply();
        }
    }

    public static File getDriverRoot(Driver driver) {
        if(driver instanceof BuiltinDriver) return null;
        File root = new File(packagesPath, driver.getHash());
        if(root.exists() && root.isDirectory()) return root;
        else return null;
    }

    private static boolean ensureRootExists(){
        if(!packagesPath.exists())
            return packagesPath.mkdirs();
        return true;
    }

    /**
     * Gets a list of hashes of the drivers that are installed in the driver path
     * @return A list of hashes
     */
    public static List<String> getDriverPaths(){
        if(!packagesPath.exists())
            return Collections.emptyList();
        File[] files = packagesPath.listFiles(File::isDirectory);
        if (files == null) return Collections.emptyList();
        List<String> packages = new ArrayList<>();
        for(File dir : files) {
            if(!new File(dir, METADATA_FILENAME).exists())
                continue;
            packages.add(dir.getName());
        }
        return packages;
    }

    /**
     * Gets a list of the installed drivers + the default (built-in) driver
     * @return A list of drivers
     */
    public static List<Driver> getDrivers(){
        List<Driver> drivers = new ArrayList<>();
        drivers.add(DEFAULT_DRIVER);
        if(!packagesPath.exists())
            return drivers;
        File[] files = packagesPath.listFiles(File::isDirectory);
        if (files == null) return drivers;
        for(File dir : files) {
            File metadata = new File(dir, METADATA_FILENAME);
            if(!metadata.exists())
                continue;
            AdrenoDriver driver = AdrenoDriver.fromJson(metadata);
            if(driver != null)
                drivers.add(driver);
        }
        return drivers;
    }

    /**
     * Checks if the driver installed
     * @param hash A hash of the driver
     * @return True if the driver is installed
     */
    public static boolean driverExists(String hash){
        return packagesPath.exists() && new File(packagesPath, hash + "/" + METADATA_FILENAME).exists();
    }

    /**
     * Loads a driver from the installed path
     * @param hash A driver hash
     * @return Driver if the load succeeded, null otherwise
     */
    public static AdrenoDriver loadDriver(String hash) {
        File metadata = new File(packagesPath, hash + "/" + METADATA_FILENAME);
        if (!metadata.exists())
            return null;
        return AdrenoDriver.fromJson(metadata);
    }

    public static DisplayDriver loadDisplayDriver(String hash) {
        File metadata = new File(packagesPath, hash + "/" + METADATA_FILENAME);
        if (!metadata.exists()) return null;
        try {
            return JSONUtils.readFromFile(metadata, DisplayDriver.class);
        } catch (IOException e) {
            Log.i(TAG, "Failed to read metadata from " + metadata.getAbsolutePath() + "!");
            return null;
        }
    }

    /**
     * Checks if the driver package is valid by path.
     * @param path A path to a driver package
     * @return true if the provided zip file is a valid package
     */
    public static boolean validateDriver(File path){
        try(ZipFile zf = new ZipFile(path)){
            AdrenoDriver driver = AdrenoDriver.fromJson(ZipUtils.getEntryStream(zf, METADATA_FILENAME));
            return driver != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the driver package is valid in the provided input stream.
     * @param stream An input stream containing a zip data
     * @return driver hash if valid or null if invalid
     */
    public static String validateDriver(InputStream stream){
        try (ZipInputStream zip = new ZipInputStream(stream)) {
            ZipEntry entry;
            do {
                entry = zip.getNextEntry();
            } while(entry != null && !METADATA_FILENAME.equals(entry.getName()));
            if(entry == null) return null;
            AdrenoDriver driver = AdrenoDriver.fromJson(zip);
            return driver != null ? driver.getHash() : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Install a driver from the provided path
     */
    public static AdrenoDriver installDriver(File path, boolean overwrite){
        ensureRootExists();
        try(ZipFile zf = new ZipFile(path)){
            AdrenoDriver driver = AdrenoDriver.fromJson(ZipUtils.getEntryStream(zf, METADATA_FILENAME));
            if(driver == null) return null;
            String hash = driver.getHash();
            if(driverExists(hash)){
                if(!overwrite) {
                    Log.w(TAG, "Driver package already installed and overwrite is not requested");
                    return null;
                }
                else {
                    removeDriver(hash);
                }
            }
            File installPath = new File(packagesPath, hash);
            ZipUtils.zipExtract(zf, "", installPath);
            Log.i(TAG, "Installed driver package " + driver.getName() + " into " + installPath.getAbsolutePath());
            return driver;
        } catch (Exception e) {
            Log.e(TAG, "Malformed driver package file at " + path.getAbsolutePath());
            return null;
        }
    }

    public static void installDriverContent(String hash, InputStream is, boolean overwrite) throws IOException {
        ensureRootExists();
        try(ZipInputStream zis = new ZipInputStream(is)){
            if(driverExists(hash)){
                if(!overwrite) {
                    Log.w(TAG, "Driver package already installed and overwrite is not requested");
                    return;
                }
                else {
                    removeDriver(hash);
                }
            }
            File installPath = new File(packagesPath, hash);
            FileUtils.ensureDirectory(installPath);
            ZipUtils.zipStreamExtract(zis, installPath);
            Log.i(TAG, "Installed driver package " + hash + " into " + installPath.getAbsolutePath());
            loadDriver(hash);
        }
    }

    /**
     * Removes a driver by its name (hash)
     * @param name A driver hash
     * @return true if the remove succeeded, false otherwise
     */
    public static boolean removeDriver(String name) {
        if(!packagesPath.exists())
            return false;
        File path = new File(packagesPath, name);
        if (!path.isDirectory())
            return false;
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(path);
            setPreferredDriver(null);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to remove driver package " + name);
            return false;
        }
    }
}
