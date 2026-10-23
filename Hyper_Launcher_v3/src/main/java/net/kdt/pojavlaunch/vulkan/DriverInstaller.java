package net.kdt.pojavlaunch.vulkan;

import static com.ashmeet.hyperlauncher.activity.PojavApplication.sExecutorService;
import static com.ashmeet.hyperlauncher.utils.Tools.runOnUiThread;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.ashmeet.hyperlauncher.tracker.TrackerLayout;

import java.io.IOException;
import java.io.InputStream;

import com.ashmeet.hyperlauncher.utils.Tools;
import net.ashmeet.hyperlauncher.R;

public class DriverInstaller {

    // Install AdrenoTools driver. Callback will be executed on the main ui thread after driver installation
    public static void installDriverFromUri(Context context, Uri uri, Runnable callback) {
        sExecutorService.execute(() -> {
            String target;
            TrackerLayout.setTracker(TrackerLayout.DRIVER_INSTALL, 0, R.string.driver_progress_validate);
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null || (target = DriverManager.validateDriver(is)) == null) {
                    runOnUiThread(() -> Toast.makeText(context, R.string.driver_config_import_invalid, Toast.LENGTH_LONG).show());
                    TrackerLayout.clearTracker(TrackerLayout.DRIVER_INSTALL);
                    return;
                }
            } catch (IOException e) {
                TrackerLayout.clearTracker(TrackerLayout.DRIVER_INSTALL);
                Tools.showErrorRemote(e);
                return;
            }
            TrackerLayout.setTracker(TrackerLayout.DRIVER_INSTALL, 50, R.string.driver_progress_install);
            // We need to open a new clean InputStream here
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    DriverManager.installDriverContent(target, is, true);
                }
            } catch (IOException e) {
                TrackerLayout.clearTracker(TrackerLayout.DRIVER_INSTALL);
                Tools.showErrorRemote(e);
                return;
            }
            if (callback != null) {
                runOnUiThread(callback);
            }
            TrackerLayout.clearTracker(TrackerLayout.DRIVER_INSTALL);
        });
    }

}
