package net.kdt.pojavlaunch;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;

import com.ashmeet.hyperlauncher.utils.Tools;
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps;
import net.kdt.pojavlaunch.utils.FileUtils;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An activity dedicated to importing control files.
 */
@SuppressWarnings("IOStreamConstructor")
public class ImportControlActivity extends BaseActivity {

    private Uri mUriData;
    private boolean mHasIntentChanged = true;
    private volatile boolean mIsFileVerified = false;

    private String mFileName = "";
    private ComposeView mComposeView;

    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getDisplayMetrics(this);
        if(Tools.checkStorageInteractive(this)) {
            Tools.initStorageConstants(getApplicationContext());
        }else {
            // Return early, no initialization needed.
            return;
        }

        mComposeView = new ComposeView(this);
        setContentView(mComposeView);
        updateComposeContent();
    }

    private void updateComposeContent() {
        LauncherComposeHelper.setImportControlContent(mComposeView, mFileName, fileName -> {
            mFileName = fileName;
            startImport();
            return kotlin.Unit.INSTANCE;
        });
    }

    /**
     * Override the previous loaded intent
     * @param intent the intent used to replace the old one.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent != null) setIntent(intent);
        mHasIntentChanged = true;
    }

    /**
     * Update all over again if the intent changed.
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(!Tools.checkStorageInteractive(this)) {
            // Don't try to read the file as when this check fails, external storage paths
            // are no longer valid (likely unmounted).
            // checkStorageInteractive() will finish this activity for us.
            return;
        }
        if(!mHasIntentChanged) return;
        mIsFileVerified = false;
        getUriData();
        if(mUriData == null) {
            finishAndRemoveTask();
            return;
        }
        mFileName = trimFileName(Tools.getFileName(this, mUriData));
        updateComposeContent();
        mHasIntentChanged = false;

        //Import and verify thread
        //Kill the app if the file isn't valid.
        new Thread(() -> {
            importControlFile();

            if(verify())mIsFileVerified = true;
            else runOnUiThread(() -> {
                Toast.makeText(
                        ImportControlActivity.this,
                        getText(R.string.import_control_invalid_file),
                        Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
            });
        }).start();
    }

    /**
     * Start the import.
     */
    public void startImport() {
        String fileName = trimFileName(mFileName);
        //Step 1 check for suffixes.
        if(!isFileNameValid(fileName)){
            Toast.makeText(this, getText(R.string.import_control_invalid_name), Toast.LENGTH_SHORT).show();
            return;
        }
        if(!mIsFileVerified){
            Toast.makeText(this, getText(R.string.import_control_verifying_file), Toast.LENGTH_LONG).show();
            return;
        }

        new File(Tools.CTRLMAP_PATH + "/TMP_IMPORT_FILE.json").renameTo(new File(Tools.CTRLMAP_PATH + "/" + fileName + ".json"));
        Toast.makeText(getApplicationContext(), getText(R.string.import_control_done), Toast.LENGTH_SHORT).show();
        finishAndRemoveTask();
    }

    /**
     * Copy a the file from the Intent data with a provided name into the controlmap folder.
     */
    private void importControlFile(){
        InputStream is;
        try {
            is = getContentResolver().openInputStream(mUriData);
            OutputStream os = new FileOutputStream(Tools.CTRLMAP_PATH + "/" + "TMP_IMPORT_FILE" + ".json");
            IOUtils.copy(is, os);

            os.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tell if the clean version of the filename is valid.
     * @param fileName the string to test
     * @return whether the filename is valid
     */
    private static boolean isFileNameValid(String fileName){
        fileName = trimFileName(fileName);

        if(fileName.isEmpty()) return false;
        return !FileUtils.exists(Tools.CTRLMAP_PATH + "/" + fileName + ".json");
    }

    /**
     * Remove or undesirable chars from the string
     * @param fileName The string to trim
     * @return The trimmed string
     */
    private static String trimFileName(String fileName){
        return fileName
                .replace(".json", "")
                .replaceAll("%..", "/")
                .replace("/", "")
                .replace("\\", "")
                .trim();
    }

    /**
     * Tries to get an Uri from the various sources
     */
    private void getUriData(){
        mUriData = getIntent().getData();
        if(mUriData != null) return;
        try {
            mUriData = getIntent().getClipData().getItemAt(0).getUri();
        }catch (Exception ignored){}
    }

    /**
     * Verify if the control file is valid
     * @return Whether the control file is valid
     */
    private static boolean verify() {
        try {
            LayoutBitmaps.ControlsContainer layout = LayoutBitmaps.load(new File(Tools.CTRLMAP_PATH,"TMP_IMPORT_FILE.json"));
            JSONObject layoutJobj = new JSONObject(layout.mControlsJson);
            return layoutJobj.has("version") && layoutJobj.has("mControlDataList");
        }catch (IOException | JSONException e) {
            Log.w("ImportControlActivity", "Failed to validate layout", e);
            return false;
        }
    }

}
