package com.ashmeet.hyperlauncher.utils

import com.ashmeet.hyperbackend.Logger
import java.util.concurrent.CopyOnWriteArrayList

object LoggerProxy : Logger.eventLogListener {
    private val listeners = CopyOnWriteArrayList<Logger.eventLogListener>()

    fun addListener(listener: Logger.eventLogListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Logger.eventLogListener) {
        listeners.remove(listener)
    }

    override fun onEventLogged(text: String?) {
        if (text == null) return
        listeners.forEach { it.onEventLogged(text) }
    }

    fun init() {
        Logger.setLogListener(this)
    }
}
