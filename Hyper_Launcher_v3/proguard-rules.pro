# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# General optimization settings
-allowaccessmodification

# The "Reverse" Approach: Keep everything outside com.ashmeet.hyperlauncher intact
# This allows R8 to shrink, optimize, and obfuscate only com.ashmeet.hyperlauncher
-keep class net.kdt.pojavlaunch.** { *; }
-keep class git.artdeell.** { *; }
-keep class git.mojo.** { *; }
-keep class com.kdt.mcgui.** { *; }


# We use Reflection on the builder to avoid creating too many objects
 -keep class net.objecthunter.exp4j.ExpressionBuilder**
 -keepclassmembers class net.objecthunter.exp4j.ExpressionBuilder** {
    *;
 }
# Option screens
-keep class com.ashmeet.hyperlauncher.fragments.settings.LauncherPreferenceFragment { *; }
-keep class com.ashmeet.hyperlauncher.fragments.settings.LauncherPreferenceJavaFragment { *; }
-keep class com.ashmeet.hyperlauncher.fragments.settings.LauncherPreferenceVideoFragment { *; }
-keep class com.ashmeet.hyperlauncher.fragments.settings.LauncherPreferenceControlFragment { *; }
-keep class com.ashmeet.hyperlauncher.fragments.settings.LauncherPreferenceAppearanceFragment { *; }
-keep class com.ashmeet.hyperlauncher.fragments.settings.LauncherPreferenceExperimentalFragment { *; }
-keep class com.ashmeet.hyperlauncher.fragments.settings.LauncherPreferenceMiscellaneousFragment { *; }

# Minecraft JSON classes (GSON mapping and Reflection)
-keep class net.kdt.pojavlaunch.JVersionList** { *; }
-keep class net.kdt.pojavlaunch.modloaders.modpacks.api.** { *; }

# JNI keep rules
-keep class git.artdeell.dnbootstrap.glfw.GLFW { *; }
-keep class git.mojo.sdl.SDLActivity {
    public static boolean sendMessage(int, int);
    public static void setOrientation(int, int, boolean, java.lang.String);
    public static void manualBackButton();
    public static boolean showTextInput(int, int, int, int, int);
}

# ASM and Bytecode Injectors
-keep class org.objectweb.asm.** { *; }
-keep class org.angelauramc.methodsInjectorAgent.** { *; }
-keep class java.lang.instrument.** { *; }

# Keep AWT/Swing classes if they are bundled (common issue in PojavLauncher)
-keep class java.awt.** { *; }
-keep class javax.swing.** { *; }
-dontwarn java.awt.**
-dontwarn javax.swing.**

# Gson rules
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter
-keep class com.google.gson.stream.**
-keep class net.kdt.pojavlaunch.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Ktor
#noinspection ExpensiveKeepRuleInspection
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Zstd-jni (Distant Horizons)
-keep class com.github.luben.zstd.** { *; }
-dontwarn com.github.luben.zstd.**

# Prevent enum obfuscation for Gson compatibility
-keepclassmembers enum * { *; }


