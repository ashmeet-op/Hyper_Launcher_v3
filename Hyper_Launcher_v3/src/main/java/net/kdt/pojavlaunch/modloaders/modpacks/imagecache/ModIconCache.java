package net.kdt.pojavlaunch.modloaders.modpacks.imagecache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ModIconCache {
    ThreadPoolExecutor cacheLoaderPool = new ThreadPoolExecutor(10,
            10,
            1000,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());
    File cachePath;
    private final List<WeakReference<ImageReceiver>> mCancelledReceivers = new ArrayList<>();
    public ModIconCache() {
        cachePath = getImageCachePath();
        if(!FileUtils.ensureDirectorySilently(cachePath)) {
                throw new RuntimeException("Failed to create icon cache directory");
        }

    }
    public static File getImageCachePath() {
        return new File(Tools.DIR_CACHE, "mod_icons");
    }


    public void getImage(ImageReceiver imageReceiver, String imageTag, String imageUrl) {
        cacheLoaderPool.execute(new ReadFromDiskTask(this, imageReceiver, imageTag, imageUrl));
    }


    public void cancelImage(ImageReceiver imageReceiver) {
        synchronized (mCancelledReceivers) {
            mCancelledReceivers.add(new WeakReference<>(imageReceiver));
        }
    }

    boolean checkCancelled(ImageReceiver imageReceiver) {
        boolean isCanceled = false;
        synchronized (mCancelledReceivers) {
            Iterator<WeakReference<ImageReceiver>> iterator = mCancelledReceivers.iterator();
            while (iterator.hasNext()) {
                WeakReference<ImageReceiver> reference = iterator.next();
                if (reference.get() == null) {
                    iterator.remove();
                    continue;
                }
                if(reference.get() == imageReceiver) {
                    isCanceled = true;
                }
            }
        }
        if(isCanceled) Log.i("IconCache", "checkCancelled("+imageReceiver.hashCode()+") == true");
        return isCanceled;
    }



    public static void writeInstanceImage(Instance instance, String imageTag) {
        File imagePath = new File(Tools.DIR_CACHE, "mod_icons/"+imageTag+".ca");
        Log.i("IconCache", "Creating base64 version of icon "+imageTag);
        if(!imagePath.canRead() || !imagePath.isFile()) {
            Log.i("IconCache", "Icon does not exist");
            return;
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath.getAbsolutePath(), options);
            options.inSampleSize = calculateInSampleSize(options, 128, 128);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath.getAbsolutePath(), options);
            if(bitmap == null) return;
            instance.encodeNewIcon(bitmap);
        }catch (IOException e) {
            Log.i("ModIconCache", "Failed to reencode icon for instance");
            e.printStackTrace();
        }
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
