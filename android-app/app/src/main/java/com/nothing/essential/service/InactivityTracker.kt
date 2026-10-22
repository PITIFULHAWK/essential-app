package com.nothing.essential.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nothing.essential.MainActivity
import com.nothing.essential.R
import com.nothing.essential.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object InactivityTracker {

    const val CHANNEL_ID = "essential_inactivity_channel"
    const val SUMMARY_CHANNEL_ID = "essential_daily_summary_channel"
    private const val NOTIFICATION_ID = 1001
    private const val SUMMARY_NOTIFICATION_ID = 1002

    fun createNotificationChannel(context: Context) {
        createNotificationChannels(context)
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val inactChannel = NotificationChannel(
                CHANNEL_ID,
                "Agent Inactivity & Motivation Pokes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nudges and escalating motivation pokes during inactivity"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 120, 80, 120)
            }
            notificationManager.createNotificationChannel(inactChannel)

            val summaryChannel = NotificationChannel(
                SUMMARY_CHANNEL_ID,
                "Daily Task & Streak Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily progress, completed tasks, and streak breakdown"
            }
            notificationManager.createNotificationChannel(summaryChannel)
        }
    }

    fun scheduleNextCheck(context: Context, delayMinutes: Int = 15) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, InactivityReceiver::class.java).apply {
            action = "com.nothing.essential.ACTION_CHECK_INACTIVITY"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    suspend fun checkAndNotify(context: Context) {
        val db = AppDatabase.getInstance(context)
        val settings = db.getSettings()

        if (!settings.isNudgeEnabled && !settings.isPokeEnabled) {
            scheduleNextCheck(context, settings.nudgeIntervalMinutes)
            return
        }

        // 1. Check Late-Night Sleep Window (IST DND)
        if (db.isInsideSleepWindow(settings)) {
            // User is sleeping (e.g. 2:00/3:00 AM - 9:00/10:00 AM IST) - suppress all nudges and pokes
            scheduleNextCheck(context, 30) // check again in 30 mins
            return
        }

        // 2. Perform daily rollover check for past logical days
        try {
            db.performDailyRolloverCheck()
        } catch (e: Exception) {}

        val now = System.currentTimeMillis()
        // Check Excused Snooze
        if (settings.snoozedUntilMillis > now) {
            val remainingMs = settings.snoozedUntilMillis - now
            val remainingMinutes = ((remainingMs / 60000) + 1).toInt().coerceAtLeast(1)
            scheduleNextCheck(context, remainingMinutes)
            return
        }

        val elapsedMillis = now - settings.lastActiveTimestamp
        val elapsedMinutes = (elapsedMillis / 60000).toInt()

        if (elapsedMinutes >= 60 && settings.isPokeEnabled) {
            // Trigger Escalating Brutality Poke sequentially within coroutine
            try {
                val digest = db.getEpisodicDigest()
                val pokeMessage = AiService.synthesizeInactivityPoke(context, elapsedMinutes, digest)
                db.setLatestPokeMessage(pokeMessage)
                showEscalatingPokeNotification(context, elapsedMinutes, pokeMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (elapsedMinutes >= settings.nudgeIntervalMinutes && settings.isNudgeEnabled) {
            // Standard gentle reminder
            showGentleNudgeNotification(context, elapsedMinutes)
        }

        scheduleNextCheck(context, settings.nudgeIntervalMinutes)
    }

    fun postDailySummaryNotification(context: Context) {
        createNotificationChannels(context)

        val db = AppDatabase.getInstance(context)
        val settings = db.getSettings()
        val streak = db.calculateStreak(settings)
        val solvedToday = db.getTodaySolvedDsaCount(settings)
        val target = settings.dailyTargetGoal
        val percent = if (target > 0) ((solvedToday.toFloat() / target) * 100).toInt() else 0
        val carriedOverCount = db.getDsaProblems().count { it.isRolledOver && it.status != "SOLVED" }

        val title = "DAILY TASK SUMMARY // ${solvedToday}/${target} SOLVED ($percent%)"
        val body = buildString {
            append("Streak: ${streak}d | Solved Today: ${solvedToday}/${target} (${percent}%)")
            if (carriedOverCount > 0) {
                append(" | Debt: ${carriedOverCount} carried over")
            }
            append(" | Cutoff: ${settings.dayCutoffHour}:00 AM IST")
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPending = PendingIntent.getActivity(
            context,
            100,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, SUMMARY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(appPending)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SUMMARY_NOTIFICATION_ID, builder.build())
        db.logActivityEvent("SUMMARY_NOTIFIED", "Posted daily summary notification ($solvedToday/$target solved)")
    }

    private fun showEscalatingPokeNotification(context: Context, minutesInactive: Int, message: String) {
        createNotificationChannel(context)

        val stageTitle = when {
            minutesInactive < 90 -> "STAGE 1 // DISCIPLINE AWAKENING"
            minutesInactive < 180 -> "STAGE 2 // STREAK REALITY CHECK"
            else -> "STAGE 3 // BREAK THE INERTIA"
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPending = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snooze30Intent = Intent(context, InactivityReceiver::class.java).apply { action = "com.nothing.essential.ACTION_SNOOZE_30M" }
        val snooze30Pending = PendingIntent.getBroadcast(context, 1, snooze30Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snooze1hIntent = Intent(context, InactivityReceiver::class.java).apply { action = "com.nothing.essential.ACTION_SNOOZE_1H" }
        val snooze1hPending = PendingIntent.getBroadcast(context, 2, snooze1hIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(stageTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(appPending)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 150, 100, 150))
            .addAction(0, "+30M SNOOZE", snooze30Pending)
            .addAction(0, "+1H EXCUSED", snooze1hPending)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showGentleNudgeNotification(context: Context, minutesInactive: Int) {
        createNotificationChannel(context)

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPending = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snooze15Intent = Intent(context, InactivityReceiver::class.java).apply { action = "com.nothing.essential.ACTION_SNOOZE_15M" }
        val snooze15Pending = PendingIntent.getBroadcast(context, 1, snooze15Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("INACTIVITY DETECTED // ${minutesInactive}M")
            .setContentText("No DSA or course progress detected. Ready to continue?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(appPending)
            .setAutoCancel(true)
            .addAction(0, "+15 MINS", snooze15Pending)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
