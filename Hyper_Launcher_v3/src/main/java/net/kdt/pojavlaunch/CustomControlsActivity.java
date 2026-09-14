package net.kdt.pojavlaunch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;

import androidx.activity.result.ActivityResultLauncher;
import androidx.compose.ui.platform.ComposeView;

import com.ashmeet.hyperlauncher.utils.Tools;
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper;
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;
import com.google.gson.JsonSyntaxException;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.utils.CropperUtils;

import java.io.IOException;


public class CustomControlsActivity extends BaseActivity implements EditorExitable, CropperUtils.CropperReceiver {
	private ControlLayout mControlLayout;
	private CropperUtils.CropperReceiver mCropperReceiver;
	private ActivityResultLauncher<?> mCropperLauncher;

	@Override
	protected boolean shouldEnableEdgeToEdge() {
		return LauncherPreferences.PREF_FULLSCREEN_LAUNCHER;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCropperLauncher = CropperUtils.registerCropper(this, this);

		ComposeView composeView = new ComposeView(this);
		LauncherComposeHelper.ensureViewTreeOwners(composeView);
		setContentView(composeView);

		mControlLayout = new ControlLayout(this);
		mControlLayout.setModifiable(true);

		LauncherComposeHelper.setControlsEditorContent(composeView, mControlLayout, position -> {
			switch(position) {
				case 0: mControlLayout.addControlButton(new ControlData("New")); break;
				case 1: mControlLayout.addDrawer(new ControlDrawerData()); break;
				case 2: mControlLayout.addJoystickButton(new ControlJoystickData()); break;
				case 3: mControlLayout.openLoadDialog(); break;
				case 4: mControlLayout.openSaveDialog(this); break;
				case 5: mControlLayout.openSetDefaultDialog(); break;
				case 6: // Saving the currently shown control
					try {
						Uri contentUri = DocumentsContract.buildDocumentUri(getString(R.string.storageProviderAuthorities), mControlLayout.saveToDirectory(mControlLayout.mLayoutFileName));

						Intent shareIntent = new Intent();
						shareIntent.setAction(Intent.ACTION_SEND);
						shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
						shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						shareIntent.setType("application/json");
						startActivity(shareIntent);

						Intent sendIntent = Intent.createChooser(shareIntent, mControlLayout.mLayoutFileName);
						startActivity(sendIntent);
					}catch (Exception e) {
						Tools.showError(this, e);
					}
					break;
			}
			return kotlin.Unit.INSTANCE;
		});
	}

	@Override
	public void onAttachedToWindow() {
		mControlLayout.post(()->{
			try {
				mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
			}catch (IOException | JsonSyntaxException e) {
				Tools.showError(this, e);
			}
		});
	}

	public void startCropping(CropperUtils.CropperReceiver cropperReceiver) {
		mCropperReceiver = cropperReceiver;
		CropperUtils.startCropper(mCropperLauncher);
	}

	@Override
	public void onBackPressed() {
        super.onBackPressed();
        mControlLayout.askToExit(this);
	}

	@Override
	public void exitEditor() {
		super.onBackPressed();
	}

	@Override
	public float getAspectRatio() {
		if(mCropperReceiver != null) return mCropperReceiver.getAspectRatio();
		return 1f;
	}

	@Override
	public int getTargetMaxSide() {
		if(mCropperReceiver != null) return mCropperReceiver.getTargetMaxSide();
		return 128;
	}

	@Override
	public void onCropped(Bitmap contentBitmap) {
		if(mCropperReceiver != null) mCropperReceiver.onCropped(contentBitmap);
	}

	@Override
	public void onFailed(Exception exception) {
		if(mCropperReceiver != null) mCropperReceiver.onFailed(exception);
	}
}
