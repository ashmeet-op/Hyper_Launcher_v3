package net.kdt.pojavlaunch;

import static com.ashmeet.hyperlauncher.utils.Tools.shareLog;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;

import com.ashmeet.hyperlauncher.utils.Tools;
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper;
import net.ashmeet.hyperlauncher.R;

import java.io.File;
import java.io.IOException;

@Keep
public class ExitActivity extends AppCompatActivity {

    @SuppressLint("StringFormatInvalid") //invalid on some translations but valid on most, cant fix that atm
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int code = -1;
        boolean isSignal = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            code = extras.getInt("code",-1);
            isSignal = extras.getBoolean("isSignal", false);
        }

        String title = isSignal ? getString(R.string.mcn_abort_title) : getString(R.string.mcn_exit_title, code);
        
        String logs = "";
        try {
            logs = Tools.read(new File(Tools.DIR_GAME_HOME, "latestlog.txt"));
        } catch (IOException e) {
            logs = "Failed to read logs: " + e.getMessage();
        }

        ComposeView composeView = new ComposeView(this);
        LauncherComposeHelper.ensureViewTreeOwners(composeView);
        String finalLogs = logs;
        LauncherComposeHelper.setExitContent(
                composeView,
                title,
                finalLogs,
                () -> { shareLog(this); return kotlin.Unit.INSTANCE; },
                () -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("logs", finalLogs);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
                    return kotlin.Unit.INSTANCE;
                },
                () -> { Tools.restartLauncherActivity(this); return kotlin.Unit.INSTANCE; },
                (path) -> { Tools.openPath(this, new File(path), false); return kotlin.Unit.INSTANCE; }
        );
        setContentView(composeView);
    }

    @SuppressWarnings("unused") //used by native jre_launcher_new
    public static void showExitMessage(Context ctx, int code, boolean isSignal) {
        if((!isSignal && code == 0)) {
            if(ctx != null) Tools.restartLauncherActivity(ctx);
            System.exit(0);
            return;
        }

        Object lock = new Object();
        Tools.runOnUiThread(()->{
            Intent i = new Intent(ctx,ExitActivity.class);
            i.putExtra("code",code);
            i.putExtra("isSignal", isSignal);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            synchronized (lock) {
                lock.notify();
            }
        });
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.e("ExitActivity", "Waiting on lock failed: "+e);
            }
        }
        System.exit(0);
    }

}
