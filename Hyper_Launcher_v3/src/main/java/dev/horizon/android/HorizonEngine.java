package dev.horizon.android;

import android.content.Context;
import android.content.Intent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.ashmeet.hyperlauncher.extra.ExtraConstants;
import com.ashmeet.hyperlauncher.extra.ExtraCore;

/** Launch configuration and NativeActivity entry point for the Horizon engine AAR. */
@SuppressWarnings("unused")
public final class HorizonEngine {
    private HorizonEngine() {}

    public static void launch(Context context, String... arguments) throws IOException, JSONException {
        JSONObject config = new JSONObject();
        List<String> argList = new ArrayList<>();
        if (arguments != null) {
            for (String argument : arguments) {
                if (argument != null) argList.add(argument);
            }
        }

        String quickPlayWorld = (String) ExtraCore.consumeValue(ExtraConstants.QUICK_PLAY_WORLD);
        if (quickPlayWorld != null && !quickPlayWorld.trim().isEmpty()) {
            String worldName = quickPlayWorld.trim();
            if (!argList.contains("--quickPlaySingleplayer")) {
                argList.add("--quickPlaySingleplayer");
                argList.add(worldName);
            }
        }

        String quickPlayServer = (String) ExtraCore.consumeValue(ExtraConstants.QUICK_PLAY_SERVER);
        if (quickPlayServer != null && !quickPlayServer.trim().isEmpty()) {
            String serverAddress = quickPlayServer.trim();
            if (!argList.contains("--quickPlayMultiplayer") && !argList.contains("--quickPlayServer")) {
                argList.add("--quickPlayMultiplayer");
                argList.add(serverAddress);
            }
        }

        JSONArray args = new JSONArray();
        for (String arg : argList) args.put(arg);
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
