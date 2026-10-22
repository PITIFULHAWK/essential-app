package com.nothing.essential

import android.app.Application
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.service.InactivityTracker

class EssentialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Notification Channel
        InactivityTracker.createNotificationChannel(this)

        // Initialize Database
        val db = AppDatabase.getInstance(this)
        val settings = db.getSettings()

        // Schedule first inactivity check if enabled
        if (settings.isNudgeEnabled) {
            InactivityTracker.scheduleNextCheck(this, settings.nudgeIntervalMinutes)
        }
    }
}
