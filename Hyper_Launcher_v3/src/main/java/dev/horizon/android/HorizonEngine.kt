@file:Suppress("unused")

package dev.horizon.android

import android.content.Context
import android.content.Intent
import com.ashmeet.hyperlauncher.extra.ExtraConstants
import com.ashmeet.hyperlauncher.extra.ExtraCore
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/** Launch configuration and NativeActivity entry point for the Horizon engine AAR. */
object HorizonEngine {

    @JvmStatic
    @Throws(IOException::class, JSONException::class)
    fun launch(context: Context, vararg arguments: String?) {
        val config = JSONObject()
        val argList = ArrayList<String>()
        for (argument in arguments) {
            if (argument != null) argList.add(argument)
        }

        val quickPlayWorld = ExtraCore.consumeValue(ExtraConstants.QUICK_PLAY_WORLD) as? String
        if (!quickPlayWorld.isNullOrBlank()) {
            val worldName = quickPlayWorld.trim()
            if (!argList.contains("--quickPlaySingleplayer")) {
                argList.add("--quickPlaySingleplayer")
                argList.add(worldName)
            }
        }

        val quickPlayServer = ExtraCore.consumeValue(ExtraConstants.QUICK_PLAY_SERVER) as? String
        if (!quickPlayServer.isNullOrBlank()) {
            val serverAddress = quickPlayServer.trim()
            if (!argList.contains("--quickPlayMultiplayer") && !argList.contains("--quickPlayServer")) {
                argList.add("--quickPlayMultiplayer")
                argList.add(serverAddress)
            }
        }

        val args = JSONArray()
        for (arg in argList) args.put(arg)
        config.put("args", args)
        config.put("renderer", "vulkan")
        config.put("render_scale", 100)
        config.put("input_host", "keyboard-mouse")

        FileOutputStream(File(context.filesDir, "launch_config.json")).use { output ->
            output.write(config.toString().toByteArray(StandardCharsets.UTF_8))
            output.fd.sync()
        }

        val intent = Intent(context, HorizonActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
