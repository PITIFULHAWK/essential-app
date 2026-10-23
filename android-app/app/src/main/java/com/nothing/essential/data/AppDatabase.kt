package com.nothing.essential.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.nothing.essential.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "essential_space.db"
        private const val DATABASE_VERSION = 2

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: AppDatabase(context.applicationContext).also { instance = it }
            }
        }

        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE dsa_problems (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                platform TEXT NOT NULL,
                url TEXT,
                difficulty TEXT DEFAULT 'MEDIUM',
                status TEXT DEFAULT 'TODO',
                notes TEXT,
                created_at INTEGER NOT NULL,
                completed_at INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE dsa_checklists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                problem_id INTEGER NOT NULL,
                text TEXT NOT NULL,
                is_completed INTEGER DEFAULT 0,
                sort_order INTEGER DEFAULT 0,
                difficulty TEXT DEFAULT '',
                url TEXT DEFAULT '',
                completed_at INTEGER,
                FOREIGN KEY (problem_id) REFERENCES dsa_problems (id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE courses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                provider TEXT NOT NULL,
                total_modules INTEGER DEFAULT 10,
                completed_modules INTEGER DEFAULT 0,
                notes TEXT,
                current_topic TEXT,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE jobs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                company TEXT NOT NULL,
                role TEXT NOT NULL,
                status TEXT DEFAULT 'SAVED',
                salary TEXT,
                url TEXT,
                notes TEXT,
                applied_date INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE streak_logs (
                date TEXT PRIMARY KEY,
                dsa_solved_count INTEGER DEFAULT 0,
                course_progress_count INTEGER DEFAULT 0,
                is_streak_day INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE app_settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )

        createKeyPoolAndEventsTables(db)
        seedDefaults(db)
    }

    private fun createKeyPoolAndEventsTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS api_key_pool (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                provider TEXT NOT NULL,
                label TEXT NOT NULL,
                api_key TEXT NOT NULL,
                status TEXT DEFAULT 'ACTIVE',
                cooldown_until INTEGER DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS agent_activity_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                event_type TEXT NOT NULL,
                summary TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        ensureSchemaColumns(db)
    }

    fun ensureSchemaColumns(db: SQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE dsa_checklists ADD COLUMN difficulty TEXT DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE dsa_checklists ADD COLUMN url TEXT DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE dsa_checklists ADD COLUMN completed_at INTEGER")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE dsa_problems ADD COLUMN is_rolled_over INTEGER DEFAULT 0")
        } catch (e: Exception) {}
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ensureSchemaColumns(db)
        if (oldVersion < 2) {
            createKeyPoolAndEventsTables(db)
            // Migrate single keys if they exist in app_settings
            try {
                val cursor = db.rawQuery("SELECT key, value FROM app_settings WHERE key IN ('gemini_api_key', 'deepseek_api_key')", null)
                cursor.use {
                    while (it.moveToNext()) {
                        val key = it.getString(0)
                        val value = it.getString(1)
                        if (value.isNotBlank()) {
                            val provider = if (key.startsWith("gemini")) "gemini" else "deepseek"
                            val cv = ContentValues().apply {
                                put("provider", provider)
                                put("label", "$provider Primary")
                                put("api_key", value)
                                put("status", "ACTIVE")
                                put("cooldown_until", 0)
                                put("created_at", System.currentTimeMillis())
                            }
                            db.insert("api_key_pool", null, cv)
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore migration error
            }
        }
    }

    private fun seedDefaults(db: SQLiteDatabase) {
        // Seed 100xDevs course
        val courseCv = ContentValues().apply {
            put("title", "100xDevs Winter Bootcamp 1.0")
            put("provider", "100xDevs")
            put("total_modules", 12)
            put("completed_modules", 0)
            put("notes", "Winter Bootcamp 1.0 with Harkirat Singh. DSA & Web Dev")
            put("current_topic", "Week 1: Basics, Time Complexity & C++ / Rust")
            put("updated_at", System.currentTimeMillis())
        }
        db.insert("courses", null, courseCv)

        // Seed Default Settings
        insertOrUpdateSettingRaw(db, "ai_provider", "gemini")
        insertOrUpdateSettingRaw(db, "gemini_api_key", "")
        insertOrUpdateSettingRaw(db, "deepseek_api_key", "")
        insertOrUpdateSettingRaw(db, "openai_api_key", "")
        insertOrUpdateSettingRaw(db, "nudge_interval_minutes", "15")
        insertOrUpdateSettingRaw(db, "daily_target_goal", "5")
        insertOrUpdateSettingRaw(db, "is_nudge_enabled", "true")
        insertOrUpdateSettingRaw(db, "is_poke_enabled", "true")
        insertOrUpdateSettingRaw(db, "snoozed_until_millis", "0")
        insertOrUpdateSettingRaw(db, "snooze_reason", "")
        insertOrUpdateSettingRaw(db, "last_active_timestamp", System.currentTimeMillis().toString())
        insertOrUpdateSettingRaw(db, "latest_poke_message", "")
        insertOrUpdateSettingRaw(db, "day_cutoff_hour", "3")
        insertOrUpdateSettingRaw(db, "sleep_start_hour", "3")
        insertOrUpdateSettingRaw(db, "sleep_duration_hours", "7")
        insertOrUpdateSettingRaw(db, "is_sleep_dnd_enabled", "true")
        insertOrUpdateSettingRaw(db, "deepseek_model", "deepseek-flash")
    }

    private fun insertOrUpdateSettingRaw(db: SQLiteDatabase, key: String, value: String) {
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Key Pool Management ---
    fun getApiKeyPool(): List<ApiKeyItem> {
        val db = readableDatabase
        val list = mutableListOf<ApiKeyItem>()
        val cursor = db.rawQuery("SELECT id, provider, label, api_key, status, cooldown_until, created_at FROM api_key_pool ORDER BY id ASC", null)
        cursor.use {
            val now = System.currentTimeMillis()
            while (it.moveToNext()) {
                val cooldown = it.getLong(5)
                val rawStatus = it.getString(4)
                val status = if (rawStatus == "COOLING_DOWN" && cooldown < now) "ACTIVE" else rawStatus
                list.add(
                    ApiKeyItem(
                        id = it.getLong(0),
                        provider = it.getString(1),
                        label = it.getString(2),
                        apiKey = it.getString(3),
                        status = status,
                        cooldownUntil = cooldown,
                        createdAt = it.getLong(6)
                    )
                )
            }
        }
        return list
    }

    fun insertApiKey(item: ApiKeyItem): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("provider", item.provider.lowercase())
            put("label", item.label)
            put("api_key", item.apiKey)
            put("status", "ACTIVE")
            put("cooldown_until", 0)
            put("created_at", System.currentTimeMillis())
        }
        return db.insert("api_key_pool", null, cv)
    }

    fun deleteApiKey(id: Long) {
        val db = writableDatabase
        db.delete("api_key_pool", "id = ?", arrayOf(id.toString()))
    }

    fun updateApiKeyStatus(id: Long, status: String, cooldownUntil: Long = 0L) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("status", status)
            put("cooldown_until", cooldownUntil)
        }
        db.update("api_key_pool", cv, "id = ?", arrayOf(id.toString()))
    }

    fun getNextHealthyKey(provider: String): ApiKeyItem? {
        val keys = getApiKeyPool().filter { it.provider.equals(provider, ignoreCase = true) }
        val now = System.currentTimeMillis()
        // Priority 1: ACTIVE keys
        val activeKey = keys.firstOrNull { it.status == "ACTIVE" || (it.status == "COOLING_DOWN" && it.cooldownUntil <= now) }
        if (activeKey != null) {
            if (activeKey.status == "COOLING_DOWN") {
                updateApiKeyStatus(activeKey.id, "ACTIVE", 0L)
            }
            return activeKey
        }
        return null
    }

    // --- Episodic Memory & Activity Events ---
    fun logActivityEvent(eventType: String, summary: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("timestamp", System.currentTimeMillis())
            put("event_type", eventType)
            put("summary", summary)
        }
        db.insert("agent_activity_events", null, cv)
    }

    fun getEpisodicDigest(days: Int = 3): String {
        val db = readableDatabase
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val sb = StringBuilder()

        // 1. Current State
        val streak = calculateStreak()
        val settings = getSettings()
        val elapsedMins = ((System.currentTimeMillis() - settings.lastActiveTimestamp) / 60000).toInt()
        sb.appendLine("[CURRENT STATE: Streak ${streak}d | Inactive: ${elapsedMins}m | Daily Goal: ${settings.dailyTargetGoal}]")

        // 2. Carried Over / Debt Problems
        val rolledOverProblems = getDsaProblems().filter { it.isRolledOver && it.status != "SOLVED" }
        if (rolledOverProblems.isNotEmpty()) {
            sb.appendLine("[CARRIED OVER / UNFINISHED FROM YESTERDAY (${rolledOverProblems.size}):")
            rolledOverProblems.forEach { p ->
                val unchk = p.checklist.count { !it.isCompleted }
                sb.appendLine(" ! UNFINISHED DEBT: ${p.title} (${p.difficulty}) - $unchk questions pending!")
            }
            sb.appendLine("]")
        }

        // 3. Active Problems
        val activeProblems = getDsaProblems().filter { it.status != "SOLVED" }.take(3)
        if (activeProblems.isNotEmpty()) {
            sb.appendLine("[ACTIVE PROBLEMS QUEUE:")
            activeProblems.forEach { p ->
                val unchecked = p.checklist.filter { !it.isCompleted }.joinToString("; ") { it.text }
                sb.appendLine(" - ${p.platform.uppercase()}: ${p.title} (${p.difficulty}) [Remaining: $unchecked]")
            }
            sb.appendLine("]")
        } else {
            sb.appendLine("[ACTIVE PROBLEMS QUEUE: EMPTY (Needs problem suggestion)]")
        }

        // 4. Last Completed Events
        val cursor = db.rawQuery(
            "SELECT event_type, summary, timestamp FROM agent_activity_events WHERE timestamp >= ? ORDER BY timestamp DESC LIMIT 5",
            arrayOf(since.toString())
        )
        val events = mutableListOf<String>()
        val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
        cursor.use {
            while (it.moveToNext()) {
                val t = sdf.format(Date(it.getLong(2)))
                events.add("$t - ${it.getString(0)}: ${it.getString(1)}")
            }
        }

        if (events.isNotEmpty()) {
            sb.appendLine("[RECENT MILESTONES (Last 72h):")
            events.forEach { sb.appendLine(" * $it") }
            sb.appendLine("]")
        }

        return sb.toString()
    }

    // --- Settings Operations ---
    fun getSettings(): AppSettings {
        val db = readableDatabase
        val map = mutableMapOf<String, String>()
        val cursor = db.rawQuery("SELECT key, value FROM app_settings", null)
        cursor.use {
            while (it.moveToNext()) {
                map[it.getString(0)] = it.getString(1)
            }
        }
        return AppSettings(
            aiProvider = map["ai_provider"] ?: "gemini",
            geminiApiKey = map["gemini_api_key"] ?: "",
            deepseekApiKey = map["deepseek_api_key"] ?: "",
            openaiApiKey = map["openai_api_key"] ?: "",
            nudgeIntervalMinutes = map["nudge_interval_minutes"]?.toIntOrNull() ?: 15,
            dailyTargetGoal = map["daily_target_goal"]?.toIntOrNull() ?: 5,
            isNudgeEnabled = map["is_nudge_enabled"] != "false",
            isPokeEnabled = map["is_poke_enabled"] != "false",
            snoozedUntilMillis = map["snoozed_until_millis"]?.toLongOrNull() ?: 0L,
            snoozeReason = map["snooze_reason"] ?: "",
            lastActiveTimestamp = map["last_active_timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
            latestPokeMessage = map["latest_poke_message"] ?: "",
            dayCutoffHour = map["day_cutoff_hour"]?.toIntOrNull() ?: 3,
            sleepStartHour = map["sleep_start_hour"]?.toIntOrNull() ?: 3,
            sleepDurationHours = map["sleep_duration_hours"]?.toIntOrNull() ?: 7,
            isSleepDndEnabled = map["is_sleep_dnd_enabled"] != "false",
            deepseekModel = map["deepseek_model"] ?: "deepseek-flash",
            isFocusModeActive = map["is_focus_mode_active"] == "true",
            focusSessionEndMillis = map["focus_session_end_millis"]?.toLongOrNull() ?: 0L
        )
    }

    fun updateSettings(settings: AppSettings) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            insertOrUpdateSettingRaw(db, "ai_provider", settings.aiProvider)
            insertOrUpdateSettingRaw(db, "gemini_api_key", settings.geminiApiKey)
            insertOrUpdateSettingRaw(db, "deepseek_api_key", settings.deepseekApiKey)
            insertOrUpdateSettingRaw(db, "openai_api_key", settings.openaiApiKey)
            insertOrUpdateSettingRaw(db, "nudge_interval_minutes", settings.nudgeIntervalMinutes.toString())
            insertOrUpdateSettingRaw(db, "daily_target_goal", settings.dailyTargetGoal.toString())
            insertOrUpdateSettingRaw(db, "is_nudge_enabled", settings.isNudgeEnabled.toString())
            insertOrUpdateSettingRaw(db, "is_poke_enabled", settings.isPokeEnabled.toString())
            insertOrUpdateSettingRaw(db, "snoozed_until_millis", settings.snoozedUntilMillis.toString())
            insertOrUpdateSettingRaw(db, "snooze_reason", settings.snoozeReason)
            insertOrUpdateSettingRaw(db, "last_active_timestamp", settings.lastActiveTimestamp.toString())
            insertOrUpdateSettingRaw(db, "latest_poke_message", settings.latestPokeMessage)
            insertOrUpdateSettingRaw(db, "day_cutoff_hour", settings.dayCutoffHour.toString())
            insertOrUpdateSettingRaw(db, "sleep_start_hour", settings.sleepStartHour.toString())
            insertOrUpdateSettingRaw(db, "sleep_duration_hours", settings.sleepDurationHours.toString())
            insertOrUpdateSettingRaw(db, "is_sleep_dnd_enabled", settings.isSleepDndEnabled.toString())
            insertOrUpdateSettingRaw(db, "deepseek_model", settings.deepseekModel)
            insertOrUpdateSettingRaw(db, "is_focus_mode_active", settings.isFocusModeActive.toString())
            insertOrUpdateSettingRaw(db, "focus_session_end_millis", settings.focusSessionEndMillis.toString())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun touchActive() {
        val db = writableDatabase
        insertOrUpdateSettingRaw(db, "last_active_timestamp", System.currentTimeMillis().toString())
    }

    fun setExcusedSnooze(untilMillis: Long, reason: String = "") {
        val db = writableDatabase
        insertOrUpdateSettingRaw(db, "snoozed_until_millis", untilMillis.toString())
        insertOrUpdateSettingRaw(db, "snooze_reason", reason)
        logActivityEvent("SNOOZE_SET", if (reason.isNotBlank()) "Snoozed until $untilMillis ($reason)" else "Snoozed until $untilMillis")
    }

    fun clearSnooze() {
        val db = writableDatabase
        insertOrUpdateSettingRaw(db, "snoozed_until_millis", "0")
        insertOrUpdateSettingRaw(db, "snooze_reason", "")
    }

    fun setLatestPokeMessage(msg: String) {
        val db = writableDatabase
        insertOrUpdateSettingRaw(db, "latest_poke_message", msg)
    }

    // --- Late-Night IST & Sleep Window Helpers ---
    fun isInsideSleepWindow(settings: AppSettings = getSettings()): Boolean {
        if (!settings.isSleepDndEnabled) return false
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val start = settings.sleepStartHour
        val duration = settings.sleepDurationHours
        val end = (start + duration) % 24

        return if (start < (start + duration) && (start + duration) <= 24) {
            currentHour in start until (start + duration)
        } else {
            currentHour >= start || currentHour < end
        }
    }

    fun getLogicalDateString(settings: AppSettings = getSettings()): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        if (currentHour < settings.dayCutoffHour) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        return sdf.format(cal.time)
    }

    fun performDailyRolloverCheck(): List<DsaProblem> {
        val db = writableDatabase
        ensureSchemaColumns(db)
        val settings = getSettings()
        val todayLogical = getLogicalDateString(settings)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }

        val allProblems = getDsaProblems()
        val rolledOverNow = mutableListOf<DsaProblem>()

        for (problem in allProblems) {
            if (problem.status != "SOLVED" && !problem.isRolledOver) {
                val problemCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
                    timeInMillis = problem.createdAt
                }
                if (problemCal.get(Calendar.HOUR_OF_DAY) < settings.dayCutoffHour) {
                    problemCal.add(Calendar.DAY_OF_YEAR, -1)
                }
                val problemLogical = sdf.format(problemCal.time)
                if (problemLogical < todayLogical) {
                    val cv = ContentValues().apply {
                        put("is_rolled_over", 1)
                    }
                    db.update("dsa_problems", cv, "id = ?", arrayOf(problem.id.toString()))
                    rolledOverNow.add(problem.copy(isRolledOver = true))
                }
            }
        }
        if (rolledOverNow.isNotEmpty()) {
            logActivityEvent(
                "ROLLOVER_DETECTED",
                "Carried over ${rolledOverNow.size} incomplete problem(s) into $todayLogical"
            )
        }
        return rolledOverNow
    }

    // --- DSA Problems Operations ---
    fun getDsaProblems(): List<DsaProblem> {
        val db = readableDatabase
        val problems = mutableListOf<DsaProblem>()
        val cursor = db.rawQuery("SELECT id, title, platform, url, difficulty, status, notes, created_at, completed_at, is_rolled_over FROM dsa_problems ORDER BY id DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val checklist = getChecklistForProblem(id)
                val isRolledOver = if (c.columnCount > 9 && !c.isNull(9)) c.getInt(9) == 1 else false
                problems.add(
                    DsaProblem(
                        id = id,
                        title = c.getString(1),
                        platform = c.getString(2),
                        url = c.getString(3) ?: "",
                        difficulty = c.getString(4) ?: "MEDIUM",
                        status = c.getString(5) ?: "TODO",
                        notes = c.getString(6) ?: "",
                        createdAt = c.getLong(7),
                        completedAt = if (c.isNull(8)) null else c.getLong(8),
                        checklist = checklist,
                        isRolledOver = isRolledOver
                    )
                )
            }
        }
        return problems
    }

    fun getChecklistForProblem(problemId: Long): List<ChecklistItem> {
        val db = readableDatabase
        val list = mutableListOf<ChecklistItem>()
        val cursor = db.rawQuery("SELECT id, problem_id, text, is_completed, sort_order, difficulty, url, completed_at FROM dsa_checklists WHERE problem_id = ? ORDER BY sort_order ASC, id ASC", arrayOf(problemId.toString()))
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    ChecklistItem(
                        id = c.getLong(0),
                        problemId = c.getLong(1),
                        text = c.getString(2),
                        isCompleted = c.getInt(3) == 1,
                        sortOrder = c.getInt(4),
                        difficulty = if (c.columnCount > 5 && !c.isNull(5)) c.getString(5) else "",
                        url = if (c.columnCount > 6 && !c.isNull(6)) c.getString(6) else "",
                        completedAt = if (c.columnCount > 7 && !c.isNull(7)) c.getLong(7) else null
                    )
                )
            }
        }
        return list
    }

    fun insertDsaProblem(problem: DsaProblem): Long {
        return insertDsaProblems(listOf(problem)).firstOrNull() ?: 0L
    }

    fun insertDsaProblems(problems: List<DsaProblem>): List<Long> {
        val db = writableDatabase
        ensureSchemaColumns(db)
        db.beginTransaction()
        val ids = mutableListOf<Long>()
        try {
            for (problem in problems) {
                val cv = ContentValues().apply {
                    put("title", problem.title)
                    put("platform", problem.platform.lowercase())
                    put("url", problem.url)
                    put("difficulty", problem.difficulty.uppercase())
                    put("status", problem.status)
                    put("notes", problem.notes)
                    put("created_at", problem.createdAt)
                    put("is_rolled_over", if (problem.isRolledOver) 1 else 0)
                    if (problem.completedAt != null) put("completed_at", problem.completedAt)
                }
                val id = db.insert("dsa_problems", null, cv)
                ids.add(id)
                problem.checklist.forEachIndexed { index, item ->
                    val chCv = ContentValues().apply {
                        put("problem_id", id)
                        put("text", item.text)
                        put("is_completed", if (item.isCompleted) 1 else 0)
                        put("sort_order", index)
                        put("difficulty", item.difficulty)
                        put("url", item.url)
                        if (item.completedAt != null) {
                            put("completed_at", item.completedAt)
                        } else if (item.isCompleted) {
                            put("completed_at", System.currentTimeMillis())
                        }
                    }
                    db.insert("dsa_checklists", null, chCv)
                }
            }
            db.setTransactionSuccessful()
            logActivityEvent("DSA_ADDED", "Added ${problems.size} problems to queue")
            return ids
        } finally {
            db.endTransaction()
        }
    }

    fun updateChecklistItem(itemId: Long, isCompleted: Boolean) {
        val db = writableDatabase
        ensureSchemaColumns(db)

        var pId: Long? = null
        val prevCursor = db.rawQuery("SELECT problem_id FROM dsa_checklists WHERE id = ?", arrayOf(itemId.toString()))
        prevCursor.use {
            if (it.moveToNext()) {
                pId = it.getLong(0)
            }
        }

        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        val cv = ContentValues().apply {
            put("is_completed", if (isCompleted) 1 else 0)
            if (completedAt != null) {
                put("completed_at", completedAt)
            } else {
                putNull("completed_at")
            }
        }
        db.update("dsa_checklists", cv, "id = ?", arrayOf(itemId.toString()))
        logActivityEvent("QUESTION_CHECKED", "Question toggled ($isCompleted)")
        touchActive()

        // Auto-sync status of parent tile
        try {
            if (pId != null) {
                val items = getChecklistForProblem(pId!!)
                if (items.isNotEmpty()) {
                    val allDone = items.all { it.isCompleted }
                    val anyDone = items.any { it.isCompleted }
                    val newStatus = when {
                        allDone -> "SOLVED"
                        anyDone -> "IN_PROGRESS"
                        else -> "TODO"
                    }
                    val statusCv = ContentValues().apply {
                        put("status", newStatus)
                        if (allDone) put("completed_at", System.currentTimeMillis()) else putNull("completed_at")
                    }
                    db.update("dsa_problems", statusCv, "id = ?", arrayOf(pId.toString()))
                }
            }
        } catch (e: Exception) {}

        syncStreakForToday()
    }

    fun updateDsaProblemStatus(problemId: Long, status: String) {
        val db = writableDatabase
        val completedAt = if (status == "SOLVED") System.currentTimeMillis() else null
        val cv = ContentValues().apply {
            put("status", status)
            if (completedAt != null) put("completed_at", completedAt) else putNull("completed_at")
        }
        db.update("dsa_problems", cv, "id = ?", arrayOf(problemId.toString()))
        touchActive()
        logActivityEvent("DSA_STATUS", "Problem #$problemId -> $status")

        syncStreakForToday()
    }

    fun deleteDsaProblem(problemId: Long) {
        val db = writableDatabase
        db.delete("dsa_checklists", "problem_id = ?", arrayOf(problemId.toString()))
        db.delete("dsa_problems", "id = ?", arrayOf(problemId.toString()))
        touchActive()
    }

    // --- Courses Operations ---
    fun getCourses(): List<Course> {
        val db = readableDatabase
        val list = mutableListOf<Course>()
        val cursor = db.rawQuery("SELECT id, title, provider, total_modules, completed_modules, notes, current_topic, updated_at FROM courses ORDER BY updated_at DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    Course(
                        id = c.getLong(0),
                        title = c.getString(1),
                        provider = c.getString(2),
                        totalModules = c.getInt(3),
                        completedModules = c.getInt(4),
                        notes = c.getString(5) ?: "",
                        currentTopic = c.getString(6) ?: "",
                        updatedAt = c.getLong(7)
                    )
                )
            }
        }
        return list
    }

    fun insertCourse(course: Course): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", course.title)
            put("provider", course.provider)
            put("total_modules", course.totalModules)
            put("completed_modules", course.completedModules)
            put("notes", course.notes)
            put("current_topic", course.currentTopic)
            put("updated_at", System.currentTimeMillis())
        }
        val id = db.insert("courses", null, cv)
        logActivityEvent("COURSE_ADDED", "${course.provider}: ${course.title}")
        return id
    }

    fun updateCourseProgress(courseId: Long, completedModules: Int, currentTopic: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("completed_modules", completedModules)
            put("current_topic", currentTopic)
            put("updated_at", System.currentTimeMillis())
        }
        db.update("courses", cv, "id = ?", arrayOf(courseId.toString()))
        touchActive()
        syncStreakForToday(courseDelta = 1)
        logActivityEvent("COURSE_PROGRESS", "Course #$courseId modules: $completedModules ($currentTopic)")
    }

    fun deleteCourse(courseId: Long) {
        val db = writableDatabase
        db.delete("courses", "id = ?", arrayOf(courseId.toString()))
        touchActive()
    }

    // --- Jobs Operations ---
    fun getJobs(): List<JobApplication> {
        val db = readableDatabase
        val list = mutableListOf<JobApplication>()
        val cursor = db.rawQuery("SELECT id, company, role, status, salary, url, notes, applied_date, updated_at FROM jobs ORDER BY updated_at DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    JobApplication(
                        id = c.getLong(0),
                        company = c.getString(1),
                        role = c.getString(2),
                        status = c.getString(3),
                        salary = c.getString(4) ?: "",
                        url = c.getString(5) ?: "",
                        notes = c.getString(6) ?: "",
                        appliedDate = c.getLong(7),
                        updatedAt = c.getLong(8)
                    )
                )
            }
        }
        return list
    }

    fun insertJob(job: JobApplication): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("company", job.company)
            put("role", job.role)
            put("status", job.status)
            put("salary", job.salary)
            put("url", job.url)
            put("notes", job.notes)
            put("applied_date", job.appliedDate)
            put("updated_at", System.currentTimeMillis())
        }
        val id = db.insert("jobs", null, cv)
        logActivityEvent("JOB_APPLIED", "${job.company} - ${job.role} (${job.status})")
        return id
    }

    fun updateJobStatus(jobId: Long, status: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("status", status)
            put("updated_at", System.currentTimeMillis())
        }
        db.update("jobs", cv, "id = ?", arrayOf(jobId.toString()))
        logActivityEvent("JOB_STATUS", "Job #$jobId -> $status")
    }

    fun deleteJob(jobId: Long) {
        val db = writableDatabase
        db.delete("jobs", "id = ?", arrayOf(jobId.toString()))
        touchActive()
    }

    // --- Streak & Stats Operations ---
    fun getLogicalDayBounds(settings: AppSettings = getSettings()): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        if (currentHour < settings.dayCutoffHour) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        cal.set(Calendar.HOUR_OF_DAY, settings.dayCutoffHour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endMillis = cal.timeInMillis
        return Pair(startMillis, endMillis)
    }

    fun getTodaySolvedDsaCount(settings: AppSettings = getSettings()): Int {
        val db = readableDatabase
        ensureSchemaColumns(db)
        val (startMillis, endMillis) = getLogicalDayBounds(settings)

        var checklistCount = 0
        val cCursor = db.rawQuery(
            "SELECT count(*) FROM dsa_checklists WHERE is_completed = 1 AND completed_at >= ? AND completed_at < ?",
            arrayOf(startMillis.toString(), endMillis.toString())
        )
        cCursor.use { if (it.moveToNext()) checklistCount = it.getInt(0) }

        var standaloneCount = 0
        val pCursor = db.rawQuery(
            "SELECT id FROM dsa_problems WHERE status = 'SOLVED' AND completed_at >= ? AND completed_at < ?",
            arrayOf(startMillis.toString(), endMillis.toString())
        )
        pCursor.use {
            while (it.moveToNext()) {
                val pid = it.getLong(0)
                if (getChecklistForProblem(pid).isEmpty()) {
                    standaloneCount++
                }
            }
        }

        return checklistCount + standaloneCount
    }

    fun syncStreakForToday(settings: AppSettings = getSettings(), courseDelta: Int = 0) {
        val db = writableDatabase
        val today = getLogicalDateString(settings)
        val solvedToday = getTodaySolvedDsaCount(settings)
        var currentCourse = 0
        val cursor = db.rawQuery("SELECT course_progress_count FROM streak_logs WHERE date = ?", arrayOf(today))
        cursor.use {
            if (it.moveToNext()) {
                currentCourse = it.getInt(0)
            }
        }
        val newCourse = (currentCourse + courseDelta).coerceAtLeast(0)
        val cv = ContentValues().apply {
            put("date", today)
            put("dsa_solved_count", solvedToday)
            put("course_progress_count", newCourse)
            put("is_streak_day", if (solvedToday > 0 || newCourse > 0) 1 else 0)
        }
        db.insertWithOnConflict("streak_logs", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun hasUnfinishedRolloverDebt(settings: AppSettings = getSettings()): Boolean {
        val db = readableDatabase
        ensureSchemaColumns(db)
        val (startMillis, _) = getLogicalDayBounds(settings)

        val rolloverCursor = db.rawQuery(
            "SELECT count(*) FROM dsa_problems WHERE is_rolled_over = 1 AND status != 'SOLVED'",
            null
        )
        var count = 0
        rolloverCursor.use { if (it.moveToNext()) count = it.getInt(0) }
        if (count > 0) return true

        val pastCursor = db.rawQuery(
            "SELECT count(*) FROM dsa_problems WHERE created_at < ? AND status != 'SOLVED'",
            arrayOf(startMillis.toString())
        )
        var pastCount = 0
        pastCursor.use { if (it.moveToNext()) pastCount = it.getInt(0) }
        return pastCount > 0
    }

    fun getEffectiveInactivityMinutes(settings: AppSettings = getSettings()): Int {
        val rawElapsedMinutes = ((System.currentTimeMillis() - settings.lastActiveTimestamp) / 60000).toInt().coerceAtLeast(0)
        return if (hasUnfinishedRolloverDebt(settings)) {
            maxOf(180, rawElapsedMinutes)
        } else {
            rawElapsedMinutes
        }
    }

    fun reconcileStreakLogs() {
        val db = writableDatabase
        ensureSchemaColumns(db)
        val settings = getSettings()

        // Backfill completed_at for legacy completed checklist items where completed_at is NULL
        db.execSQL("""
            UPDATE dsa_checklists 
            SET completed_at = (SELECT created_at FROM dsa_problems WHERE dsa_problems.id = dsa_checklists.problem_id)
            WHERE is_completed = 1 AND (completed_at IS NULL OR completed_at = 0)
        """)

        syncStreakForToday(settings)
    }

    fun calculateStreak(settings: AppSettings = getSettings()): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT date FROM streak_logs WHERE is_streak_day = 1 ORDER BY date DESC", null)
        val streakDates = mutableSetOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                streakDates.add(it.getString(0))
            }
        }

        if (streakDates.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        if (cal.get(Calendar.HOUR_OF_DAY) < settings.dayCutoffHour) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val todayStr = sdf.format(cal.time)

        var streak = 0
        if (streakDates.contains(todayStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)
            if (!streakDates.contains(yesterdayStr)) {
                return 0
            }
        }

        while (true) {
            val dateStr = sdf.format(cal.time)
            if (streakDates.contains(dateStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun getWeeklyStreakStatus(settings: AppSettings = getSettings()): List<Pair<String, Boolean>> {
        val db = readableDatabase
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        if (cal.get(Calendar.HOUR_OF_DAY) < settings.dayCutoffHour) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val cursor = db.rawQuery("SELECT date FROM streak_logs WHERE is_streak_day = 1", null)
        val activeDates = mutableSetOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                activeDates.add(it.getString(0))
            }
        }

        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        val result = mutableListOf<Pair<String, Boolean>>()
        for (i in 0 until 7) {
            val dStr = sdf.format(cal.time)
            result.add(Pair(dayLabels[i], activeDates.contains(dStr)))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }



    fun exportToJson(): String {
        val root = JSONObject()
        val dsaArr = JSONArray()
        getDsaProblems().forEach { p ->
            val pObj = JSONObject().apply {
                put("title", p.title)
                put("platform", p.platform)
                put("url", p.url)
                put("difficulty", p.difficulty)
                put("status", p.status)
                put("notes", p.notes)
                put("createdAt", p.createdAt)
                put("completedAt", p.completedAt ?: JSONObject.NULL)
                val chArr = JSONArray()
                p.checklist.forEach { ch ->
                    chArr.put(JSONObject().apply {
                        put("text", ch.text)
                        put("isCompleted", ch.isCompleted)
                    })
                }
                put("checklist", chArr)
            }
            dsaArr.put(pObj)
        }
        root.put("dsa_problems", dsaArr)

        val coursesArr = JSONArray()
        getCourses().forEach { c ->
            coursesArr.put(JSONObject().apply {
                put("title", c.title)
                put("provider", c.provider)
                put("totalModules", c.totalModules)
                put("completedModules", c.completedModules)
                put("notes", c.notes)
                put("currentTopic", c.currentTopic)
            })
        }
        root.put("courses", coursesArr)

        val jobsArr = JSONArray()
        getJobs().forEach { j ->
            jobsArr.put(JSONObject().apply {
                put("company", j.company)
                put("role", j.role)
                put("status", j.status)
                put("salary", j.salary)
                put("url", j.url)
                put("notes", j.notes)
            })
        }
        root.put("jobs", jobsArr)

        return root.toString(2)
    }

    fun importFromJson(jsonString: String) {
        val root = JSONObject(jsonString)
        if (root.has("dsa_problems")) {
            val dsaArr = root.getJSONArray("dsa_problems")
            for (i in 0 until dsaArr.length()) {
                val o = dsaArr.getJSONObject(i)
                val checklist = mutableListOf<ChecklistItem>()
                if (o.has("checklist")) {
                    val chArr = o.getJSONArray("checklist")
                    for (j in 0 until chArr.length()) {
                        val ch = chArr.getJSONObject(j)
                        checklist.add(ChecklistItem(problemId = 0, text = ch.getString("text"), isCompleted = ch.optBoolean("isCompleted", false)))
                    }
                }
                insertDsaProblem(
                    DsaProblem(
                        title = o.getString("title"),
                        platform = o.optString("platform", "leetcode"),
                        url = o.optString("url", ""),
                        difficulty = o.optString("difficulty", "MEDIUM"),
                        status = o.optString("status", "TODO"),
                        notes = o.optString("notes", ""),
                        checklist = checklist
                    )
                )
            }
        }

        if (root.has("courses")) {
            val cArr = root.getJSONArray("courses")
            for (i in 0 until cArr.length()) {
                val o = cArr.getJSONObject(i)
                insertCourse(
                    Course(
                        title = o.getString("title"),
                        provider = o.optString("provider", "General"),
                        totalModules = o.optInt("totalModules", 10),
                        completedModules = o.optInt("completedModules", 0),
                        notes = o.optString("notes", ""),
                        currentTopic = o.optString("currentTopic", "")
                    )
                )
            }
        }

        if (root.has("jobs")) {
            val jArr = root.getJSONArray("jobs")
            for (i in 0 until jArr.length()) {
                val o = jArr.getJSONObject(i)
                insertJob(
                    JobApplication(
                        company = o.getString("company"),
                        role = o.getString("role"),
                        status = o.optString("status", "SAVED"),
                        salary = o.optString("salary", ""),
                        url = o.optString("url", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }
    }
}
