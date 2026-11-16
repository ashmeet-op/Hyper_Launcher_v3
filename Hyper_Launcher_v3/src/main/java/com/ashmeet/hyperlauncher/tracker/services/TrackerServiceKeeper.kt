package com.ashmeet.hyperlauncher.tracker.services

import android.content.Context
import com.ashmeet.hyperbackend.progresskeeper.TaskCountListener

class TrackerServiceKeeper(private val context: Context) : TaskCountListener {

    override fun onUpdateTaskCount(taskCount: Int): Boolean {
        if (taskCount > 0) {
            TrackerService.startService(context)
        }
        return false
    }
}
