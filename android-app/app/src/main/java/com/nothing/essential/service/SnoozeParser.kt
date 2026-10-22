package com.nothing.essential.service

import java.util.*
import java.util.regex.Pattern

object SnoozeParser {

    /**
     * Parses natural language snooze input into epoch milliseconds.
     * Examples: "15m", "30 mins", "1h", "2 hours", "until 5pm", "until 17:30", "eod", "rest of day", "tomorrow"
     * Returns the target epoch millis, or null if unparseable.
     */
    fun parse(input: String): Long? {
        val clean = input.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) return null

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // 1. Direct minutes pattern: e.g. "15m", "30 mins", "45 minutes"
        val minMatcher = Pattern.compile("^(\\d+)\\s*(m|min|mins|minute|minutes)$").matcher(clean)
        if (minMatcher.matches()) {
            val minutes = minMatcher.group(1)?.toLongOrNull() ?: return null
            return now + minutes * 60 * 1000
        }

        // 2. Direct hours pattern: e.g. "1h", "2 hrs", "3 hours"
        val hourMatcher = Pattern.compile("^(\\d+)\\s*(h|hr|hrs|hour|hours)$").matcher(clean)
        if (hourMatcher.matches()) {
            val hours = hourMatcher.group(1)?.toLongOrNull() ?: return null
            return now + hours * 60 * 60 * 1000
        }

        // 3. Combined hours + minutes: e.g. "1h 30m"
        val combinedMatcher = Pattern.compile("^(\\d+)\\s*h(?:ours?)?\\s*(\\d+)\\s*m(?:ins?)?$").matcher(clean)
        if (combinedMatcher.matches()) {
            val h = combinedMatcher.group(1)?.toLongOrNull() ?: 0L
            val m = combinedMatcher.group(2)?.toLongOrNull() ?: 0L
            return now + (h * 60 + m) * 60 * 1000
        }

        // 4. "rest of day" / "eod" / "end of day" / "tonight"
        if (clean.contains("eod") || clean.contains("rest of day") || clean.contains("end of day") || clean.contains("tonight")) {
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            return calendar.timeInMillis
        }

        // 5. "tomorrow" / "tomorrow morning"
        if (clean.contains("tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }

        // 6. "until X pm / am" or "at X:XX pm"
        val timeMatcher = Pattern.compile("(?:until|at)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").matcher(clean)
        if (timeMatcher.find()) {
            var hour = timeMatcher.group(1)?.toIntOrNull() ?: return null
            val minute = timeMatcher.group(2)?.toIntOrNull() ?: 0
            val ampm = timeMatcher.group(3)

            if (ampm != null) {
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0
            } else {
                // If no AM/PM specified and hour is between 1 and 7, assume PM if current hour is past that morning
                if (hour in 1..7 && calendar.get(Calendar.HOUR_OF_DAY) >= hour) {
                    hour += 12
                }
            }

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)

            // If time is already past today, set for tomorrow
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        return null
    }

    fun formatDuration(targetMillis: Long): String {
        val diff = targetMillis - System.currentTimeMillis()
        if (diff <= 0) return "Active"
        val totalMinutes = (diff / (60 * 1000)).toInt()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
}
