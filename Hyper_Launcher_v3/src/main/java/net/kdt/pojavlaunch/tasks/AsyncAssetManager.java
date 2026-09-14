package net.kdt.pojavlaunch.tasks;


import static com.ashmeet.hyperlauncher.utils.Architecture.archAsString;
import static com.ashmeet.hyperlauncher.utils.Architecture.archAsStringAndroid;
import static com.ashmeet.hyperlauncher.utils.Architecture.getDeviceArchitecture;
import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AsyncAssetManager {

    private AsyncAssetManager(){}

    /**
     * Attempt to install the java 8 runtime, if necessary
     * @param am App context
     */
    public static void unpackRuntime(AssetManager am) {
        /* Check if JRE is included */
        String rt_version = null;
        String current_rt_version = MultiRTUtils.readInternalRuntimeVersion("Internal");
        try {
            rt_version = Tools.read(am.open("components/jre/version"));
        } catch (IOException e) {
            Log.e("JREAuto", "JRE was not included on this APK.", e);
        }
        String exactJREName = MultiRTUtils.getExactJreName(8);
        if(current_rt_version == null && exactJREName != null && !exactJREName.equals("Internal")/*this clause is for when the internal runtime is goofed*/) return;
        if(rt_version == null) return;
        if(rt_version.equals(current_rt_version)) return;

        // Install the runtime in an async manner, hope for the best
        String finalRt_version = rt_version;
        sExecutorService.execute(() -> {

            try {
                MultiRTUtils.installRuntimeNamedBinpack(
                        am.open("components/jre/universal.tar.xz"),
                        am.open("components/jre/bin-" + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz"),
                        "Internal", finalRt_version);
                MultiRTUtils.postPrepare("Internal");
            }catch (IOException e) {
                Log.e("JREAuto", "Internal JRE unpack failed", e);
            }
        });
    }

    /** Unpack single files, with no regard to version tracking */
    public static void unpackSingleFiles(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_SINGLE_FILES, 0);
        sExecutorService.execute(() -> {
            try {
                Tools.copyAssetFile(ctx, "options.txt", Tools.DIR_GAME_NEW, false);

                // This is disgusting, but am lazy. We probably won't be getting any updates to
                // controlmap till rewrite anyway so this is fine.
                try (InputStream is = ctx.getAssets().open("default.json")) {
                    String assetSha1 = new String(org.apache.commons.codec.binary.Hex.encodeHex(org.apache.commons.codec.digest.DigestUtils.sha1(is)));
                    if (!Tools.compareSHA1(new File(Tools.CTRLDEF_FILE), assetSha1)) {
                        Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, "new_default.json" , false);
                    } else if (!new File(Tools.CTRLMAP_PATH+"/new_default.json").exists())
                    Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, false);
                }

                Tools.copyAssetFile(ctx, "launcher_profiles.json", Tools.DIR_GAME_NEW, false);
                Tools.copyAssetFile(ctx,"resolv.conf",Tools.DIR_DATA, false);
            } catch (IOException e) {
                Log.e("AsyncAssetManager", "Failed to unpack critical components !");
            }
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_SINGLE_FILES);
        });
    }

    public static void unpackComponents(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_COMPONENTS, 0);
        sExecutorService.execute(() -> {
            tryUnpackComponent(ctx, "caciocavallo", false);
            tryUnpackComponent(ctx, "caciocavallo17", false);
            
            try {
                // Since the Java module system doesn't allow multiple JARs to declare the same module,
                // we repack them to a single file here
                unpackLwjglNatives(ctx);
            } catch (IOException e) {
                Log.e("AsyncAssetManager", "Failed to unpack LWJGL natives !", e);
            }

            tryUnpackComponent(ctx, "lwjgl3/3.3.3", false);
            tryUnpackComponent(ctx, "lwjgl3/3.4.1", false);
            tryUnpackComponent(ctx, "security", true);
            tryUnpackComponent(ctx, "arc_dns_injector", true);
            tryUnpackComponent(ctx, "MioLibPatcher", true);
            tryUnpackComponent(ctx, "forge_installer", true);
            tryUnpackComponent(ctx, "authlib-injector", true);

            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_COMPONENTS);
        });
    }

    private static String readInstalledComponentVersion(File componentRoot) {
        File localVersionFile = new File(componentRoot, "version");
        if (!localVersionFile.exists()) return null;
        try(FileInputStream fileInputStream = new FileInputStream(localVersionFile)) {
            return IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
        }catch (IOException ignored) {}
        return null;
    }

    private static String readBuiltinComponentVersion(AssetManager assetManager, String componentName) {
        String componentVersionLocation = "components/"+componentName+"/version";
        try (InputStream inputStream = assetManager.open(componentVersionLocation)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }catch (IOException ignored) {}
        return null;
    }

    private static void tryUnpackComponent(Context ctx, String component, boolean privateDirectory) {
        try {
            unpackComponent(ctx, component, privateDirectory);
        }catch (IOException e) {
            Log.e("AssetUnpacker", "Failed to unpack component "+component, e);
        }
    }

    // Piggybacks off of the java modules extracting later to use their version files for update checks
    // This is indeed prone to breaking.
    private static void unpackLwjglNatives(Context ctx) throws IOException {
        AssetManager am = ctx.getAssets();
        String rootDir = Tools.DIR_DATA;
        String sArch = archAsStringAndroid(getDeviceArchitecture());

        String[] lwjglVersions = {"3.3.3", "3.4.1"};
        for (String lwjglVer : lwjglVersions) {
            String componentVersionLocation = "components/lwjgl3/" + lwjglVer + "/version";
            try (InputStream is = am.open(componentVersionLocation)) {
                File versionFile = new File(Tools.DIR_GAME_HOME + String.format("/lwjgl3/%s/version", lwjglVer));
                String pathToLwjglNatives = String.format("lwjgl-%s-natives/", lwjglVer) + sArch;

                boolean shouldUpdate = true;
                if (versionFile.exists()) {
                    FileInputStream fis = new FileInputStream(versionFile);
                    String release1 = Tools.read(is);
                    String release2 = Tools.read(fis);
                    if (release1.equals(release2))
                        shouldUpdate = false;
                }

                if (shouldUpdate) {
                    Log.i("UnpackLwjgl", lwjglVer + " was installed manually, or does not exist, unpacking new...");
                    String[] fileList = am.list("components/" + pathToLwjglNatives);
                    if (fileList != null) {
                        for (String fileName : fileList) {
                            Tools.copyAssetFile(ctx, "components/" + pathToLwjglNatives + "/" + fileName, rootDir + "/" + pathToLwjglNatives, true);
                        }
                    }
                } else {
                    Log.i("UnpackLwjgl", lwjglVer + " is up-to-date with the launcher, continuing...");
                }
            } catch (IOException e) {
                Log.w("UnpackLwjgl", "LWJGL version " + lwjglVer + " not found in assets, skipping.");
            }
        }
    }

    private static void unpackComponent(Context ctx, String component, boolean privateDirectory) throws IOException {
        AssetManager am = ctx.getAssets();
        String rootDir = privateDirectory ? Tools.DIR_DATA : Tools.DIR_GAME_HOME;
        File componentTarget = new File(rootDir, component);
        String installedVersion = readInstalledComponentVersion(componentTarget);
        String builtinVersion = readBuiltinComponentVersion(am, component);
        String componentSource = "components/" + component;
        String[] fileList = am.list(componentSource);

        boolean needsUpdate = installedVersion == null || !installedVersion.equals(builtinVersion);
        
        // Even if the version matches, check if the files actually exist and are not empty
        if (!needsUpdate && fileList != null) {
            for (String fileName : fileList) {
                if (fileName.equals("version")) continue;
                File f = new File(componentTarget, fileName);
                if (!f.exists() || f.length() == 0) {
                    needsUpdate = true;
                    Log.w("AssetUnpacker", "Component file " + fileName + " is missing or empty, forcing re-unpack");
                    break;
                }
            }
        }

        if(!needsUpdate) {
            Log.i("AssetUnpacker", "Component "+component+" is up-to-date, continuing...");
            return;
        }
        Log.i("AssetUnpacker", "Updating "+component);

        if(componentTarget.exists()) {
            FileUtils.deleteDirectory(componentTarget);
        }
        if(!componentTarget.mkdirs()) {
            throw new IOException("Failed to create directory for "+component);
        }

        if (fileList != null) {
            for (String fileName : fileList) {
                if (fileName.equals("version")) continue;
                String sourcePath = componentSource + "/" + fileName;
                Tools.copyAssetFile(ctx, sourcePath, componentTarget.getAbsolutePath(), true);
            }
        }

        // Always write the version file separately after extracting everything else, to improve
        // reliability.
        if (builtinVersion != null) {
            Tools.write(new File(componentTarget, "version"), builtinVersion);
        }
    }

    public static void extractDefaultSettings(Context context, File gamedir)  {
        try {
            String gameDirPath = gamedir.getAbsolutePath();
            Tools.copyAssetFile(context, "options.txt", gameDirPath, false);
        }catch (IOException e) {
            Tools.showError(context, e);
        }
    }
}
