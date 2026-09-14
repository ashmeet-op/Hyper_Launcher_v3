package net.kdt.pojavlaunch.modloaders.modpacks.imagecache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.ashmeet.hyperlauncher.utils.Tools;

import java.io.File;

public class ReadFromDiskTask implements Runnable {
    final ModIconCache iconCache;
    final ImageReceiver imageReceiver;
    final File cacheFile;
    final String imageUrl;

    ReadFromDiskTask(ModIconCache iconCache, ImageReceiver imageReceiver, String cacheTag, String imageUrl) {
        this.iconCache = iconCache;
        this.imageReceiver = imageReceiver;
        this.cacheFile = new File(iconCache.cachePath, cacheTag+".ca");
        this.imageUrl = imageUrl;
    }

    public void runDownloadTask() {
        iconCache.cacheLoaderPool.execute(new DownloadImageTask(this));
    }

    @Override
    public void run() {
        if(cacheFile.isDirectory()) {
            return;
        }
        if(cacheFile.canRead()) {
            IconCacheJanitor.waitForJanitorToFinish();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(cacheFile.getAbsolutePath(), options);
            options.inSampleSize = calculateInSampleSize(options, 256, 256);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath(), options);
            if(bitmap != null) {
                Tools.runOnUiThread(()->{
                    if(taskCancelled()) {
                        bitmap.recycle(); // do not leak the bitmap if the task got cancelled right at the end
                        return;
                    }
                    imageReceiver.onImageAvailable(bitmap);
                });
                return;
            }
        }
        if(iconCache.cachePath.canWrite() &&
                !taskCancelled()) { // don't run the download task if the task got canceled
            runDownloadTask();
        }
    }
    @SuppressWarnings("BooleanMethodAlwaysInverted")
    public boolean taskCancelled() {
        return iconCache.checkCancelled(imageReceiver);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
