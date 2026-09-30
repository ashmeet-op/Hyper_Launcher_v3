package net.kdt.pojavlaunch.utils;

import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static boolean exists(String filePath){
        return new File(filePath).exists();
    }


    public static String getFileName(String pathOrUrl) {
        int lastSlashIndex = pathOrUrl.lastIndexOf('/');
        if(lastSlashIndex == -1) return pathOrUrl;
        return pathOrUrl.substring(lastSlashIndex);
    }


    public static boolean ensureDirectorySilently(File targetFile) {
        if(targetFile.isFile()) return false;
        if(targetFile.exists()) return targetFile.canWrite();
        else return targetFile.mkdirs();

    }


    public static boolean ensureParentDirectorySilently(File targetFile) {
        File parentFile = targetFile.getParentFile();
        if(parentFile == null) return false;
        return ensureDirectorySilently(parentFile);
    }


    public static void ensureDirectory(File targetFile) throws IOException {
        if(targetFile.isFile()) throw new IOException("Target directory is a file");
        if(targetFile.exists()) {
            if(!targetFile.canWrite()) throw new IOException("Target directory is not writable");
        }else if(!targetFile.mkdirs()) {

            if(!targetFile.isDirectory()) throw new IOException("Unable to create target directory");
        }
    }


    public static void ensureParentDirectory(File targetFile) throws IOException{
        File parentFile = targetFile.getParentFile();
        if(parentFile == null) throw new IOException("targetFile does not have a parent");
        ensureDirectory(parentFile);
    }
}
