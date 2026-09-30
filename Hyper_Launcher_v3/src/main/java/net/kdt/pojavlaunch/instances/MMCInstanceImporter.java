package net.kdt.pojavlaunch.instances;

import android.util.Log;
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils;
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils;
import net.kdt.pojavlaunch.modloaders.modpacks.api.modloader.FabriclikeLoaderInstaller;
import net.kdt.pojavlaunch.modloaders.modpacks.api.modloader.ForgelikeLoaderInstaller;
import net.kdt.pojavlaunch.modloaders.modpacks.api.modloader.LoaderInstaller;
import net.kdt.pojavlaunch.utils.JSONUtils;
import net.kdt.pojavlaunch.utils.ZipUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MMCInstanceImporter {

    public static boolean isMMCInstance(File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            return findEntry(zip, "instance.cfg") != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static ZipEntry findEntry(ZipFile zip, String name) {
        ZipEntry entry = zip.getEntry(name);
        if (entry != null) return entry;

        java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if (!e.isDirectory() && e.getName().endsWith("/" + name)) {
                return e;
            }
        }
        return null;
    }

    public static void importInstance(String suggestedName, File zipFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry configEntry = findEntry(zip, "instance.cfg");
            if (configEntry == null) throw new IOException("Not a valid MultiMC instance (instance.cfg missing)");

            String rootPath = "";
            if (configEntry.getName().contains("/")) {
                rootPath = configEntry.getName().substring(0, configEntry.getName().lastIndexOf('/') + 1);
            }

            Map<String, String> config = parseConfig(zip.getInputStream(configEntry));
            String instanceName = config.get("name");
            if (instanceName == null || instanceName.isEmpty()) instanceName = suggestedName;

            String mcVersion = null;
            LoaderInstaller loaderInstaller = null;

            ZipEntry packEntry = zip.getEntry(rootPath + "mmc-pack.json");
            if (packEntry != null) {
                MMCPack pack = JSONUtils.readFromStream(zip.getInputStream(packEntry), MMCPack.class);
                if (pack != null && pack.components != null) {
                    for (MMCPack.Component comp : pack.components) {
                        if ("net.minecraft".equals(comp.uid)) {
                            mcVersion = comp.version;
                        } else if ("net.fabricmc.fabric-loader".equals(comp.uid)) {
                            loaderInstaller = new FabriclikeLoaderInstaller(FabriclikeUtils.FABRIC_UTILS, mcVersion, comp.version);
                        } else if ("net.minecraftforge".equals(comp.uid)) {
                            loaderInstaller = new ForgelikeLoaderInstaller(ForgelikeUtils.FORGE_UTILS, mcVersion, comp.version);
                        } else if ("org.quiltmc.quilt-loader".equals(comp.uid)) {
                            loaderInstaller = new FabriclikeLoaderInstaller(FabriclikeUtils.QUILT_UTILS, mcVersion, comp.version);
                        }
                    }
                }
            }

            if (mcVersion == null) {

                mcVersion = config.get("IntendedVersion");
                if (mcVersion == null) mcVersion = "1.20.1";
            }

            final String finalMcVersion = mcVersion;
            final LoaderInstaller finalLoaderInstaller = loaderInstaller;
            final String finalName = instanceName;

            Instance instance = Instances.createInstance(i -> {
                i.name = finalName;
                i.versionId = finalLoaderInstaller != null ? finalLoaderInstaller.getVersionId() : finalMcVersion;
            }, "mmc");


            String dotMinecraft = rootPath + ".minecraft/";
            if (zip.getEntry(dotMinecraft) == null) {
                dotMinecraft = rootPath + "minecraft/";
            }

            if (zip.getEntry(dotMinecraft) != null) {
                ZipUtils.zipExtract(zip, dotMinecraft, instance.getGameDirectory());
            } else {


                Log.w("MMCImporter", "No .minecraft or minecraft folder found in MMC instance");
            }

            instance.write();
            Instances.setSelectedInstance(instance);
        }
    }

    private static Map<String, String> parseConfig(InputStream is) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        }
        return map;
    }

    private static class MMCPack {
        public Component[] components;
        public static class Component {
            public String uid;
            public String version;
        }
    }
}
