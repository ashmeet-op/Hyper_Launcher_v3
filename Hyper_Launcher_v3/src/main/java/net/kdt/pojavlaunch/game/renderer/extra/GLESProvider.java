package net.kdt.pojavlaunch.game.renderer.extra;

import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.ANGLE_EGL;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.ANGLE_GLES;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.ENV_EGL;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.ENV_GLES;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.NATIVE_EGL;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.NATIVE_GLES;

import android.content.Context;


import com.ashmeet.hyperlauncher.utils.Architecture;
import com.ashmeet.hyperlauncher.utils.Tools;

import net.kdt.pojavlaunch.game.renderer.impl.GLESRenderSpec;

import java.io.File;
import java.util.Map;

/**
 * OpenGL ES driver provider for {@link GLESRenderSpec} based renderers (a.k.a. wrappers on-top of OpenGL ES)
 */
public interface GLESProvider {
    /**
     * Get fitting OpenGL ES provider for the current device
     *
     * @param context     Application context
     * @param preferAngle Whether the ANGLE provider should be selected
     * @return OpenGL ES provider
     */
    static GLESProvider getGlesProvider(Context context, boolean preferAngle) {
        if (!preferAngle) return new NativeGLESProvider();
        GLESProvider provider = new SystemAngleProvider(context);
        if (provider.supported()) {
            return provider;
        }
        return new NativeGLESProvider();
    }

    /**
     * Name of the provider
     *
     * @return name
     */
    String type();

    /**
     * OpenGL EGL library name or the absolute path to it
     *
     * @return path
     */

    String eglPath();

    /**
     * OpenGL ES driver library name or the absolute path to it
     *
     * @return path
     */
    String glesPath();

    /**
     * {@link File} of the EGL library. You can use this to check if the library exists
     *
     * @return instance of {@link File}
     */
    File egl();

    /**
     * {@link File} of the OpenGL ES library. ou can use this to check if the library exists
     *
     * @return instance of {@link File}
     */
    File gles();

    /**
     * Set environment needed for this OpenGL ES provider
     *
     * @param envMap environment map
     */
    default void setEnvironment(Map<String, String> envMap) {
        envMap.put(ENV_EGL, eglPath());
        envMap.put(ENV_GLES, glesPath());
    }

    /**
     * Check if the current device supports this OpenGL ES provider
     *
     * @return state
     */
    boolean supported();

    /**
     * Check if the current OpenGL ES provider requires to load its libraries in a global/unrestricted namespace to avoid linker issues
     *
     * @return state
     */
    boolean requiresNamespace();

    /**
     * Native OpenGL ES provider. Doesn't do much as the wrappers already use it automatically if no EGL/GLES override was given, but we still implement this
     * for the correctness
     */
    class NativeGLESProvider implements GLESProvider {
        public String type() {
            return "Native OpenGL ES Driver";
        }
        public String eglPath() {
            return NATIVE_EGL;
        }
        public String glesPath() {
            return NATIVE_GLES;
        }
        public File egl() {
            return null;
        }
        public File gles() {
            return null;
        }
        public void setEnvironment(Map<String, String> envMap) {
        }
        public boolean supported() {
            return true; // Native GLES is always present even in a form of ANGLE (hello Samsung)
        }
        public boolean requiresNamespace() {
            return false;
        }
    }

    /**
     * ANGLE provider. Checks for bundled ANGLE in native library directory first, then falls back to system ANGLE.
     */
    class SystemAngleProvider implements GLESProvider {
        private final Context context;

        public SystemAngleProvider(Context context) {
            this.context = context;
        }

        public String type() {
            return "ANGLE Driver";
        }
        public String eglPath() {
            File file = egl();
            return file != null ? file.getAbsolutePath() : ANGLE_EGL;
        }
        public String glesPath() {
            File file = gles();
            return file != null ? file.getAbsolutePath() : ANGLE_GLES;
        }
        public File egl() {
            if (context != null && context.getApplicationInfo() != null && context.getApplicationInfo().nativeLibraryDir != null) {
                File local = new File(context.getApplicationInfo().nativeLibraryDir, ANGLE_EGL);
                if (local.exists()) return local;
            }
            if (Tools.NATIVE_LIB_DIR != null) {
                File localTools = new File(Tools.NATIVE_LIB_DIR, ANGLE_EGL);
                if (localTools.exists()) return localTools;
            }
            String basePath = Architecture.is64BitsDevice() ? "/system/lib64/" : "/system/lib/";
            File systemFile = new File(basePath, ANGLE_EGL);
            if (systemFile.exists()) return systemFile;
            return null;
        }
        public File gles() {
            return egl();
        }
        public boolean supported() {
            File file = egl();
            return file != null && file.exists();
        }
        public boolean requiresNamespace() {
            File file = egl();
            if (file != null && Tools.NATIVE_LIB_DIR != null && file.getAbsolutePath().startsWith(Tools.NATIVE_LIB_DIR)) {
                return false;
            }
            return true;
        }
    }
    // One might add other OpenGLES providers (such as Mesa and/or bundled ANGLE), but this is not something we want right now
}
