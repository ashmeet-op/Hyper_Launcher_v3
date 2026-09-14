package net.kdt.pojavlaunch;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;

import com.ashmeet.hyperlauncher.utils.Tools;
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper;
import net.ashmeet.hyperlauncher.R;

import java.io.File;

public class FatalErrorActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		if(extras == null) {
			finish();
			return;
		}
		boolean storageAllow = extras.getBoolean("storageAllow", false);
		Throwable throwable = (Throwable) extras.getSerializable("throwable");
		final String stackTrace = throwable != null ? Tools.printToString(throwable) : "<null>";
		String strSavePath = extras.getString("savePath");
		String errHeader = storageAllow ?
			"Crash stack trace saved to " + strSavePath + "." :
			"Storage permission is required to save crash stack trace!";

		String finalLogs = errHeader + "\n\n" + stackTrace;

		ComposeView composeView = new ComposeView(this);
		LauncherComposeHelper.setExitContent(
				composeView,
				getString(R.string.error_fatal),
				finalLogs,
				() -> {
					// Sharing stack trace instead of log file here
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_TEXT, finalLogs);
					startActivity(Intent.createChooser(intent, getString(R.string.main_share_logs)));
					return kotlin.Unit.INSTANCE;
				},
				() -> {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("error", finalLogs);
					clipboard.setPrimaryClip(clip);
					Toast.makeText(this, "Error copied to clipboard", Toast.LENGTH_SHORT).show();
					return kotlin.Unit.INSTANCE;
				},
				() -> {
					startActivity(new Intent(FatalErrorActivity.this, LauncherActivity.class));
					finish();
					return kotlin.Unit.INSTANCE;
				},
				(path) -> {
					if (strSavePath != null) {
						Tools.openPath(this, new File(strSavePath), false);
					}
					return kotlin.Unit.INSTANCE;
				}
		);
		setContentView(composeView);
	}

	public static void showError(Context ctx, String savePath, boolean storageAllow, Throwable th) {
		Intent fatalErrorIntent = new Intent(ctx, FatalErrorActivity.class);
		fatalErrorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		fatalErrorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		fatalErrorIntent.putExtra("throwable", th);
		fatalErrorIntent.putExtra("savePath", savePath);
		fatalErrorIntent.putExtra("storageAllow", storageAllow);
		ctx.startActivity(fatalErrorIntent);
	}
}
