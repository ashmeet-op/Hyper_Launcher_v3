package dev.horizon.android;

import android.content.Context;
import android.content.Intent;
import java.io.FileOutputStream;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Launch configuration and NativeActivity entry point for the Horizon engine AAR. */
public final class HorizonEngine {
    private HorizonEngine() {}

    public static void launch(Context context, String... arguments) throws IOException, JSONException {
        JSONObject config = new JSONObject();
        JSONArray args = new JSONArray();
        for (String argument : arguments) args.put(argument);
        config.put("args", args);
        config.put("renderer", "vulkan");
        config.put("render_scale", 100);
        config.put("input_host", "keyboard-mouse");
        try (FileOutputStream output = new FileOutputStream(
                new java.io.File(context.getFilesDir(), "launch_config.json"))) {
            output.write(config.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        Intent intent = new Intent(context, HorizonActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
