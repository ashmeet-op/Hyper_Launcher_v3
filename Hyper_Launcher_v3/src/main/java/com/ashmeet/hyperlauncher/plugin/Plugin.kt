package com.ashmeet.hyperlauncher.plugin

interface Plugin {
    fun getIdentifier(): String
    fun getNativeLibPath(): String
}