package net.kdt.pojavlaunch.contracts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ashmeet.hyperlauncher.activity.PojavApplication;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;



public class OpenDocumentWithExtension extends ActivityResultContract<Object, Uri> {
    private final Future<String> extensionMimeTypeFuture;


    public OpenDocumentWithExtension(String extension) {

        extensionMimeTypeFuture = PojavApplication.sExecutorService.submit(()->{
            String extensionMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if(extensionMimeType == null) extensionMimeType = "*/*";
            return extensionMimeType;
        });
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Object input) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            intent.setType(extensionMimeTypeFuture.get());
        }catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return intent;
    }

    @Nullable
    @Override
    public final SynchronousResult<Uri> getSynchronousResult(@NonNull Context context,
                                                             @NonNull Object input) {
        return null;
    }

    @Nullable
    @Override
    public final Uri parseResult(int resultCode, @Nullable Intent intent) {
        if (intent == null || resultCode != Activity.RESULT_OK) return null;
        return intent.getData();
    }
}
