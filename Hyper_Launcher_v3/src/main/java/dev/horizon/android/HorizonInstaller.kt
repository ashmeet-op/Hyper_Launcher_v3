package dev.horizon.android

@Suppress("KotlinJniMissingFunction")
object HorizonInstaller {
    init {
        System.loadLibrary("horizon_android")
    }

    const val STAGE_COUNT: Int = 6

    fun isReady(home: String?): Boolean {
        return nativeReady(home)
    }

    fun install(home: String?, cache: String?, listener: Listener?): String? {
        return nativeInstall(home, cache, listener)
    }

    private external fun nativeReady(home: String?): Boolean

    private external fun nativeInstall(home: String?, cache: String?, listener: Listener?): String?


    interface Listener {
        fun onProgress(stageIndex: Int, label: String?, done: Long, total: Long)
    }
}