package com.ashmeet.hyperbackend.modloaders.modpacks.imagecache;

import android.graphics.Bitmap;


public interface ImageReceiver {
    void onImageAvailable(Bitmap image);
}
