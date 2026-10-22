package com.nothing.essential.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nothing.essential.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InactivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val db = AppDatabase.getInstance(context)

        when (intent.action) {
            "com.nothing.essential.ACTION_CHECK_INACTIVITY" -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        InactivityTracker.checkAndNotify(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "com.nothing.essential.ACTION_SEND_DAILY_SUMMARY" -> {
                InactivityTracker.postDailySummaryNotification(context)
            }
            "com.nothing.essential.ACTION_SNOOZE_15M" -> {
                val until = System.currentTimeMillis() + 15 * 60 * 1000L
                db.setExcusedSnooze(until, "Quick 15M Snooze")
                InactivityTracker.dismissNotification(context)
                InactivityTracker.scheduleNextCheck(context, 15)
            }
            "com.nothing.essential.ACTION_SNOOZE_30M" -> {
                val until = System.currentTimeMillis() + 30 * 60 * 1000L
                db.setExcusedSnooze(until, "30M Snooze")
                InactivityTracker.dismissNotification(context)
                InactivityTracker.scheduleNextCheck(context, 30)
            }
            "com.nothing.essential.ACTION_SNOOZE_1H" -> {
                val until = System.currentTimeMillis() + 60 * 60 * 1000L
                db.setExcusedSnooze(until, "1H Excused Snooze")
                InactivityTracker.dismissNotification(context)
                InactivityTracker.scheduleNextCheck(context, 60)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                InactivityTracker.createNotificationChannels(context)
                InactivityTracker.scheduleNextCheck(context, 15)
            }
        }
    }
}
