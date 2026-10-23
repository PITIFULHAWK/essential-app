package com.nothing.essential.data.model

data class DsaProblem(
    val id: Long = 0,
    val title: String,
    val platform: String, // "leetcode", "codeforces", "tuf", "custom"
    val url: String = "",
    val difficulty: String = "MEDIUM", // "EASY", "MEDIUM", "HARD"
    val status: String = "TODO", // "TODO", "IN_PROGRESS", "SOLVED"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val isRolledOver: Boolean = false
)

data class AttachmentItem(
    val uriString: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val bytes: ByteArray = byteArrayOf(),
    val extractedText: String = ""
)

data class ChecklistItem(
    val id: Long = 0,
    val problemId: Long,
    val text: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val difficulty: String = "", // "EASY", "MEDIUM", "HARD" or empty
    val url: String = "",        // Direct problem URL if part of a contest tile
    val completedAt: Long? = null
)

data class ContestQuestion(
    val name: String,
    val difficulty: String = "MEDIUM",
    val url: String = "",
    val isIncluded: Boolean = true
)

data class ContestExtractionResult(
    val tileTitle: String = "",
    val summary: String = "",
    val platform: String = "codeforces",
    val overallDifficulty: String = "MEDIUM",
    val url: String = "",
    val questions: List<ContestQuestion> = emptyList()
)

data class Course(
    val id: Long = 0,
    val title: String,
    val provider: String = "100xDevs Winter Bootcamp 1.0",
    val totalModules: Int = 10,
    val completedModules: Int = 0,
    val notes: String = "",
    val currentTopic: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class JobApplication(
    val id: Long = 0,
    val company: String,
    val role: String,
    val status: String = "SAVED", // "SAVED", "APPLIED", "INTERVIEWING", "OFFER", "REJECTED"
    val salary: String = "",
    val url: String = "",
    val notes: String = "",
    val appliedDate: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class StreakLog(
    val date: String, // yyyy-MM-dd
    val dsaSolvedCount: Int = 0,
    val courseProgressCount: Int = 0,
    val isStreakDay: Boolean = false
)

data class ApiKeyItem(
    val id: Long = 0,
    val provider: String, // "gemini", "deepseek", "openai"
    val label: String,    // e.g. "Gemini Project A", "Gemini Backup B", "DeepSeek Main"
    val apiKey: String,
    val status: String = "ACTIVE", // "ACTIVE", "COOLING_DOWN", "EXHAUSTED"
    val cooldownUntil: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

data class AgentActivityEvent(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "DSA_SOLVED", "MILESTONE_CHECKED", "COURSE_PROGRESS", "JOB_APPLIED", "SNOOZE_SET"
    val summary: String
)

data class AppSettings(
    val aiProvider: String = "gemini", // "gemini", "deepseek", "openai"
    val geminiApiKey: String = "",
    val deepseekApiKey: String = "",
    val openaiApiKey: String = "",
    val nudgeIntervalMinutes: Int = 15,
    val dailyTargetGoal: Int = 5,
    val isNudgeEnabled: Boolean = true,
    val isPokeEnabled: Boolean = true,
    val snoozedUntilMillis: Long = 0L,
    val snoozeReason: String = "",
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val latestPokeMessage: String = "",
    val dayCutoffHour: Int = 3,         // 3 AM IST (late night cut-off)
    val sleepStartHour: Int = 3,        // 3 AM IST
    val sleepDurationHours: Int = 7,    // 7 hours (wake at 10 AM IST)
    val isSleepDndEnabled: Boolean = true,
    val deepseekModel: String = "deepseek-flash", // DeepSeek 4.1 Flash
    val isFocusModeActive: Boolean = false,
    val focusSessionEndMillis: Long = 0L
)

data class ScheduleParseResult(
    val dayCutoffHour: Int = 3,
    val sleepStartHour: Int = 3,
    val sleepDurationHours: Int = 7,
    val explanation: String = ""
)
