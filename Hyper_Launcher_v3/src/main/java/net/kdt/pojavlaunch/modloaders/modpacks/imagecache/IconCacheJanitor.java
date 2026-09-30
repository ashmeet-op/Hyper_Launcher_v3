package net.kdt.pojavlaunch.modloaders.modpacks.imagecache;

import android.util.Log;

import com.ashmeet.hyperlauncher.activity.PojavApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class IconCacheJanitor implements Runnable{
    public static final long CACHE_SIZE_LIMIT = 104857600;
    public static final long CACHE_BRINGDOWN = 52428800;

    private static Future<?> sJanitorFuture;
    private static boolean sJanitorRan = false;
    private IconCacheJanitor() {

    }
    @Override
    public void run() {
        File modIconCachePath = ModIconCache.getImageCachePath();
        if(!modIconCachePath.isDirectory() || !modIconCachePath.canRead()) return;
        File[] modIconFiles = modIconCachePath.listFiles();
        if(modIconFiles == null) return;
        ArrayList<File> writableModIconFiles = new ArrayList<>(modIconFiles.length);
        long directoryFileSize = 0;
        for(File modIconFile : modIconFiles) {
            if(!modIconFile.isFile() || !modIconFile.canRead()) continue;
            directoryFileSize += modIconFile.length();
            if(!modIconFile.canWrite()) continue;
            writableModIconFiles.add(modIconFile);
        }
        if(directoryFileSize < CACHE_SIZE_LIMIT)  {
            Log.i("IconCacheJanitor", "Skipping cleanup because there's not enough to clean up");
            return;
        }
        Arrays.sort(modIconFiles,
                (x,y)-> Long.compare(y.lastModified(), x.lastModified())
        );
        int filesCleanedUp = 0;
        for(File modFile : writableModIconFiles) {
            if(directoryFileSize < CACHE_BRINGDOWN) break;
            long modFileSize = modFile.length();
            if(modFile.delete()) {
                directoryFileSize -= modFileSize;
                filesCleanedUp++;
            }
        }
        Log.i("IconCacheJanitor", "Cleaned up "+filesCleanedUp+ " files");
        synchronized (IconCacheJanitor.class) {
            sJanitorFuture = null;
            sJanitorRan = true;
        }
    }


    public static void runJanitor() {
        synchronized (IconCacheJanitor.class) {
            if (sJanitorFuture != null || sJanitorRan) return;
            sJanitorFuture = PojavApplication.sExecutorService.submit(new IconCacheJanitor());
        }
    }


    public static void waitForJanitorToFinish() {
        synchronized (IconCacheJanitor.class) {
            if (sJanitorFuture == null) return;
            try {
                sJanitorFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Should not happen!", e);
            }
        }
    }
}
