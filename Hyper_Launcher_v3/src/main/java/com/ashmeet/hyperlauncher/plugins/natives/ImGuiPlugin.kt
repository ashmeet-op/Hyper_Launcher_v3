package com.ashmeet.hyperlauncher.plugins.natives

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ashmeet.hyperlauncher.plugins.interfaces.HyperPlugin
import com.ashmeet.hyperlauncher.plugins.manager.HyperPluginManager
import com.ashmeet.hyperlauncher.utils.Tools
import org.apache.commons.io.FileUtils
import java.io.File

class ImGuiPlugin : HyperPlugin {
    override fun prepare(activity: AppCompatActivity, javaArgList: MutableList<String>, mcVersion: String) {
        val nativeLibDir = activity.applicationInfo.nativeLibraryDir
        val imguiLib = HyperPluginManager.findNative(activity, mcVersion, "libimgui-java.so", "libimgui.so", "libimgui-moulberry-java.so", "libimgui-moulberry92-java.so")

        if (imguiLib != null) {
            Log.i("ImGuiPlugin", "Found ImGui native at: ${imguiLib.absolutePath}")
            val imguiDir = File(Tools.DIR_CACHE, "imgui_natives")
            if (!imguiDir.exists() && !imguiDir.mkdirs()) {
                Log.e("ImGuiPlugin", "Failed to create ImGui natives directory")
            }

            val forkNames = arrayOf("libimgui-moulberry92-java64.so", "libimgui-moulberry-java64.so", "libimgui-java64.so")
            for (forkName in forkNames) {
                val forkLib = File(imguiDir, forkName)
                if (!forkLib.exists() || forkLib.length() != imguiLib.length()) {
                    try {
                        FileUtils.copyFile(imguiLib, forkLib)
                    } catch (e: Exception) {
                        Log.e("ImGuiPlugin", "Failed to copy ImGui native fork: $forkName", e)
                    }
                }
            }

            val libName = imguiLib.name
            val libPath = imguiLib.parent
            javaArgList.add("-Dimgui.library.path=$libPath")
            javaArgList.add("-Dimgui.library.name=$libName")
            javaArgList.add("-Dimgui.moulberry.library.path=$libPath")
            javaArgList.add("-Dimgui.moulberry.library.name=$libName")
            javaArgList.add("-Dimgui.moulberry92.library.path=$libPath")
            javaArgList.add("-Dimgui.moulberry92.library.name=$libName")
            javaArgList.add("-Dimgui.moulberry.native.path=${imguiDir.absolutePath}")
            javaArgList.add("-Dimgui.moulberry92.native.path=${imguiDir.absolutePath}")
        } else {
            Log.w("ImGuiPlugin", "ImGui native library not found, using defaults")
            javaArgList.add("-Dimgui.library.path=$nativeLibDir")
            javaArgList.add("-Dimgui.library.name=libimgui-java.so")
            javaArgList.add("-Dimgui.moulberry92.library.path=$nativeLibDir")
            javaArgList.add("-Dimgui.moulberry92.library.name=libimgui-java.so")
        }
    }
}
