package com.ashmeet.hyperlauncher.utils.helper

import androidx.compose.runtime.mutableStateOf

object LoadingComposeHelper {
    private var _isLoading = mutableStateOf(true)
    val isLoading get() = _isLoading.value

    private var _loadingText = mutableStateOf("")
    val loadingText get() = _loadingText.value

    private var _loadingWarning = mutableStateOf("")
    val loadingWarning get() = _loadingWarning.value

    @JvmStatic
    fun setLoadingVisible(visible: Boolean) {
        _isLoading.value = visible
    }

    @JvmStatic
    fun setLoadingText(text: String) {
        _loadingText.value = text
    }

    @JvmStatic
    fun setLoadingWarning(warning: String) {
        _loadingWarning.value = warning
    }
}
