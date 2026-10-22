package com.nothing.essential.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    // Model constants
    const val GEMINI_VISION_MODEL = "gemini-3-flash-preview" // Gemini 3 Flash (10 RPM)
    const val GEMINI_FAST_MODEL = "gemini-3.1-flash-lite"    // Gemini 3.1 Flash-Lite (15 RPM)
    const val DEEPSEEK_MODEL = "deepseek-flash"               // DeepSeek 4.1 Flash
    const val OPENAI_MODEL = "gpt-4o-mini"

    private const val MULTI_PROBLEM_EXTRACT_PROMPT = """
You are an expert competitive programming & DSA assistant.
Analyze the input (contest link, contest table screenshot, problem list, sheet PDF, or problem description).

CRITICAL FORMATTING INSTRUCTIONS:
1. MULTIPLE QUESTIONS / CONTESTS (e.g. Codeforces contest 725607, sheet, or multiple problems):
   - You MUST group all questions under ONE SINGLE CONTEST TILE.
   - Do NOT generate separate cards for every question.
   - "contest_title": Generate a concise, informative title summarizing the contest/topic (e.g. "Codeforces Contest 725607 - Factors & Intervals" or "TUF Sheet - Dynamic Programming").
   - "summary": A brief 1-line summary of the core algorithmic concepts covered across the problems.
   - "platform": "codeforces" | "leetcode" | "tuf" | "custom"
   - "overall_difficulty": "EASY" | "MEDIUM" | "HARD"
   - "url": full contest or sheet link
   - "questions": An array of each question found:
     * "name": Problem letter and concise name (e.g. "A. Natural Sum", "B. Interval Sum", "C. Counting Intervals").
     * "difficulty": Strictly "EASY", "MEDIUM", or "HARD".
     * "url": Direct problem URL (e.g. [contestUrl]/problem/A).
   - DO NOT generate hints, step-by-step milestones, or solutions! The user wants a clean, minimal checklist of questions only!

2. SINGLE PROBLEM (e.g. one LeetCode question):
   - "contest_title": Exact Problem Name (e.g. "3Sum")
   - "summary": 1-line key insight or time complexity
   - "difficulty": Strictly "EASY", "MEDIUM", or "HARD"
   - "platform": "leetcode" | "codeforces" | "tuf" | "custom"
   - "url": problem URL
   - "questions": []

OUTPUT STRICTLY VALID JSON (no markdown formatting, no backticks):
{
  "contest_title": "Concise Summarized Title for the Tile",
  "summary": "1-line summary of the questions topic",
  "platform": "codeforces" | "leetcode" | "tuf" | "custom",
  "overall_difficulty": "EASY" | "MEDIUM" | "HARD",
  "url": "contest or problem link",
  "questions": [
    {
      "name": "A. Natural Sum",
      "difficulty": "EASY",
      "url": "https://codeforces.com/group/.../problem/A"
    }
  ]
}
"""

    private const val COURSE_EXTRACT_PROMPT = """
You are an expert technical curriculum and developer bootcamp assistant.
Analyze the course screenshot, syllabus document, outline, or URL input.

Extract structured course progress details into JSON:
- "title": Concise course or bootcamp title (e.g. "100xDevs Winter Bootcamp 1.0")
- "provider": Organization, platform, or teacher (e.g. "100xDevs", "Coursera", "Frontend Masters")
- "total_modules": Estimated total number of modules, weeks, or chapters (integer, default 10)
- "current_topic": Starting topic or current chapter (e.g. "Week 1: Foundations & DSA")
- "notes": Concise 1-2 sentence overview of core technologies and skills covered.

OUTPUT STRICTLY VALID JSON (no markdown formatting, no backticks):
{
  "title": "Course Name",
  "provider": "Platform / Provider",
  "total_modules": 12,
  "current_topic": "Week 1: ...",
  "notes": "Core summary"
}
"""

    private const val JOB_EXTRACT_PROMPT = """
You are an expert tech career and software engineering recruiter assistant.
Analyze the job description screenshot, careers listing, recruiter message, or PDF.

Extract structured job application details into JSON:
- "company": Company or organization name (e.g. "Google", "Nothing", "Razorpay")
- "role": Role title (e.g. "Software Engineer II - Backend")
- "status": Strictly one of "SAVED", "APPLIED", "INTERVIEWING", "OFFER", "REJECTED" (default "APPLIED" if applied, or "SAVED")
- "salary": Salary or compensation range if mentioned, else ""
- "url": Job posting, company careers link, or application portal URL
- "notes": Key requirements, tech stack (e.g. "Go, Kubernetes, distributed systems"), or referral notes.

OUTPUT STRICTLY VALID JSON (no markdown formatting, no backticks):
{
  "company": "Company Name",
  "role": "Role Title",
  "status": "SAVED",
  "salary": "$120k / 25 LPA",
  "url": "https://...",
  "notes": "Tech stack and key details"
}
"""

    private const val SCHEDULE_PARSE_PROMPT = """
You are an assistant configuring a developer's late-night work schedule and sleep window in IST (India Standard Time).
The developer typically works late into the night (around 2:00 AM to 4:00 AM IST) and sleeps for 6 to 8 hours.

Parse the user's natural language schedule preference into structured hours:
- "day_cutoff_hour": Integer hour (0-23, in 24h format) when the logical day ends (e.g. 3 for 3:00 AM).
- "sleep_start_hour": Integer hour (0-23, in 24h format) when sleep begins (e.g. 3 for 3:00 AM).
- "sleep_duration_hours": Integer number of hours of sleep (e.g. 7 for 7 hours).
- "explanation": Brief 1-line friendly confirmation (e.g. "Configured day cutoff at 3:00 AM IST. Sleep DND active from 3:00 AM to 10:00 AM (7h).").

OUTPUT STRICTLY VALID JSON (no markdown formatting, no backticks):
{
  "day_cutoff_hour": 3,
  "sleep_start_hour": 3,
  "sleep_duration_hours": 7,
  "explanation": "..."
}
"""

    private fun compressImageIfNeeded(bytes: ByteArray, maxDim: Int = 1600): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            val width = bitmap.width
            val height = bitmap.height
            val maxSide = maxOf(width, height)
            val scaledBitmap = if (maxSide > maxDim) {
                val scale = maxDim.toFloat() / maxSide
                Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                bitmap
            }
            val out = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        } catch (e: Exception) {
            bytes
        }
    }

    // --- Key Pool Execution with Auto-Failover ---
    suspend fun executeWithPool(
        context: Context,
        action: (provider: String, apiKey: String) -> String
    ): Result<String> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val settings = db.getSettings()

        // 1. Try Gemini Pool first (Primary / 100% Free: Gemini 3 Flash / 3.1 Flash-Lite)
        val geminiKey = db.getNextHealthyKey("gemini")
        if (geminiKey != null) {
            try {
                val response = action("gemini", geminiKey.apiKey)
                return@withContext Result.success(response)
            } catch (e: Exception) {
                if (isRateLimitError(e)) {
                    db.updateApiKeyStatus(geminiKey.id, "COOLING_DOWN", System.currentTimeMillis() + 60_000L)
                    val nextGemini = db.getNextHealthyKey("gemini")
                    if (nextGemini != null) {
                        try {
                            val resp = action("gemini", nextGemini.apiKey)
                            return@withContext Result.success(resp)
                        } catch (e2: Exception) {
                            if (isRateLimitError(e2)) {
                                db.updateApiKeyStatus(nextGemini.id, "COOLING_DOWN", System.currentTimeMillis() + 60_000L)
                            }
                        }
                    }
                }
            }
        } else if (settings.geminiApiKey.isNotBlank()) {
            try {
                val resp = action("gemini", settings.geminiApiKey)
                return@withContext Result.success(resp)
            } catch (e: Exception) {
                // fall through
            }
        }

        // 2. Try DeepSeek Pool (Secondary Fallback: DeepSeek 4.1 Flash)
        val deepseekKey = db.getNextHealthyKey("deepseek")
        if (deepseekKey != null) {
            try {
                val response = action("deepseek", deepseekKey.apiKey)
                return@withContext Result.success(response)
            } catch (e: Exception) {
                if (isRateLimitError(e)) {
                    db.updateApiKeyStatus(deepseekKey.id, "COOLING_DOWN", System.currentTimeMillis() + 60_000L)
                }
            }
        } else if (settings.deepseekApiKey.isNotBlank()) {
            try {
                val resp = action("deepseek", settings.deepseekApiKey)
                return@withContext Result.success(resp)
            } catch (e: Exception) {
                // fall through
            }
        }

        // 3. Try OpenAI Pool (Tertiary)
        val openaiKey = db.getNextHealthyKey("openai")
        if (openaiKey != null) {
            try {
                val response = action("openai", openaiKey.apiKey)
                return@withContext Result.success(response)
            } catch (e: Exception) {
                if (isRateLimitError(e)) {
                    db.updateApiKeyStatus(openaiKey.id, "COOLING_DOWN", System.currentTimeMillis() + 60_000L)
                }
            }
        } else if (settings.openaiApiKey.isNotBlank()) {
            try {
                val resp = action("openai", settings.openaiApiKey)
                return@withContext Result.success(resp)
            } catch (e: Exception) {
                // fall through
            }
        }

        Result.failure(Exception("All API keys in Key Pool are either exhausted or in cooldown. Add healthy keys in Config."))
    }

    private fun isRateLimitError(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("429") || msg.contains("resource_exhausted") || msg.contains("quota") || msg.contains("rate limit")
    }

    private fun buildCombinedPrompt(attachments: List<AttachmentItem>, textInput: String): String {
        return buildString {
            if (textInput.isNotBlank()) {
                appendLine("USER INPUT & URLS:")
                appendLine(textInput)
                appendLine()
            }
            val docs = attachments.filter { it.extractedText.isNotBlank() }
            if (docs.isNotEmpty()) {
                appendLine("ATTACHED DOCUMENTS & SYLLABUS:")
                docs.forEach { doc ->
                    appendLine("--- FILE: ${doc.name} (${doc.mimeType}) ---")
                    appendLine(doc.extractedText)
                    appendLine()
                }
            }
        }.trim()
    }

    // --- DSA Multi-Problem / Contest Extraction ---
    suspend fun extractContestOrProblem(
        context: Context,
        attachments: List<AttachmentItem> = emptyList(),
        textInput: String = ""
    ): Result<ContestExtractionResult> = withContext(Dispatchers.IO) {
        val images = attachments.mapNotNull { if (it.bytes.isNotEmpty()) it.bytes else null }
        val promptText = buildCombinedPrompt(attachments, textInput)

        val rawResult = executeWithPool(context) { provider, key ->
            when (provider) {
                "deepseek" -> callDeepSeek(key, images, promptText, MULTI_PROBLEM_EXTRACT_PROMPT)
                "openai" -> callOpenAi(key, images, promptText, MULTI_PROBLEM_EXTRACT_PROMPT)
                else -> callGemini(key, images, promptText, MULTI_PROBLEM_EXTRACT_PROMPT)
            }
        }

        rawResult.map { parseContestJson(it, textInput) }
    }

    // Legacy single-image adapter
    suspend fun extractContestOrProblem(
        context: Context,
        imageBytes: ByteArray?,
        textInput: String
    ): Result<ContestExtractionResult> {
        val attachments = if (imageBytes != null && imageBytes.isNotEmpty()) {
            listOf(AttachmentItem(uriString = "", name = "screenshot.jpg", mimeType = "image/jpeg", sizeBytes = imageBytes.size.toLong(), bytes = imageBytes))
        } else emptyList()
        return extractContestOrProblem(context, attachments, textInput)
    }

    // --- Courses Extraction ---
    suspend fun extractCourse(
        context: Context,
        attachments: List<AttachmentItem> = emptyList(),
        textInput: String = ""
    ): Result<Course> = withContext(Dispatchers.IO) {
        val images = attachments.mapNotNull { if (it.bytes.isNotEmpty()) it.bytes else null }
        val promptText = buildCombinedPrompt(attachments, textInput)

        val rawResult = executeWithPool(context) { provider, key ->
            when (provider) {
                "deepseek" -> callDeepSeek(key, images, promptText, COURSE_EXTRACT_PROMPT)
                "openai" -> callOpenAi(key, images, promptText, COURSE_EXTRACT_PROMPT)
                else -> callGemini(key, images, promptText, COURSE_EXTRACT_PROMPT)
            }
        }

        rawResult.map { parseCourseJson(it) }
    }

    // --- Jobs Extraction ---
    suspend fun extractJob(
        context: Context,
        attachments: List<AttachmentItem> = emptyList(),
        textInput: String = ""
    ): Result<JobApplication> = withContext(Dispatchers.IO) {
        val images = attachments.mapNotNull { if (it.bytes.isNotEmpty()) it.bytes else null }
        val promptText = buildCombinedPrompt(attachments, textInput)

        val rawResult = executeWithPool(context) { provider, key ->
            when (provider) {
                "deepseek" -> callDeepSeek(key, images, promptText, JOB_EXTRACT_PROMPT)
                "openai" -> callOpenAi(key, images, promptText, JOB_EXTRACT_PROMPT)
                else -> callGemini(key, images, promptText, JOB_EXTRACT_PROMPT)
            }
        }

        rawResult.map { parseJobJson(it, textInput) }
    }

    // --- Natural Language Schedule Config Parser ---
    suspend fun parseScheduleNaturalLanguage(
        context: Context,
        userInput: String
    ): Result<ScheduleParseResult> = withContext(Dispatchers.IO) {
        val rawResult = executeWithPool(context) { provider, key ->
            when (provider) {
                "deepseek" -> callDeepSeek(key, emptyList(), userInput, SCHEDULE_PARSE_PROMPT)
                "openai" -> callOpenAi(key, emptyList(), userInput, SCHEDULE_PARSE_PROMPT)
                else -> callGemini(key, emptyList(), userInput, SCHEDULE_PARSE_PROMPT, model = GEMINI_FAST_MODEL)
            }
        }

        rawResult.map { parseScheduleJson(it) }
    }

    // --- Escalating Brutality Poke Synthesis with Rollover Bully ---
    suspend fun synthesizeInactivityPoke(
        context: Context,
        inactivityMinutes: Int,
        episodicDigest: String
    ): String = withContext(Dispatchers.IO) {
        val hasRolloverDebt = episodicDigest.contains("CARRIED OVER") || episodicDigest.contains("UNFINISHED DEBT")

        val debtInstruction = if (hasRolloverDebt) {
            """
CRITICAL BULLY DIRECTIVE:
The user has carried over UNFINISHED tasks from yesterday! They accumulated debt and dragged unfinished work into today.
RUTHLESSLY bully them for this specific failure. Call out their procrastination, demand they finish yesterday's lingering problems right now, and make them feel the weight of unfinished business.
"""
        } else ""

        val stagePrompt = when {
            inactivityMinutes < 90 -> """
You are an austere, intellectual, and poetic focus coach for a software engineer.
The user has been inactive for $inactivityMinutes minutes.
Stage 1: Deliver ONE short, profound, cinematic or poetic quote (Marcus Aurelius, Oppenheimer, anime, Batman, Hunter S. Thompson, or ancient wisdom) that makes them reflect on wasted time and re-engage with their DSA or 100xDevs bootcamp code.
$debtInstruction
Keep it strictly 1 to 2 sentences. No conversational filler.
Digest:
$episodicDigest
"""
            inactivityMinutes < 180 -> """
You are a sharp, sarcastic, and witty tech lead watching a junior engineer procrastinate.
The user has been inactive for $inactivityMinutes minutes.
Stage 2: Deliver ONE razor-sharp, sarcastic reality check about their dying streak, their 100xDevs bootcamp progress, and LeetCode/Codeforces procrastination. Be witty, direct, and push them to open their editor right now.
$debtInstruction
Keep it strictly 1 to 2 sentences. No conversational filler.
Digest:
$episodicDigest
"""
            else -> """
You are an uncompromising, unfiltered drill sergeant who does not tolerate excuses.
The user has been inactive for $inactivityMinutes minutes (over 3 hours of procrastination).
Stage 3: Deliver ONE brutal, unfiltered reality check. Call out their excuses, the empty queue, or the half-finished problem. Remind them that tech interviews and bootcamps don't care about excuses. Break their inertia right now.
$debtInstruction
Keep it strictly 1 to 2 sentences. No conversational filler.
Digest:
$episodicDigest
"""
        }

        val result = executeWithPool(context) { provider, key ->
            when (provider) {
                "deepseek" -> callDeepSeek(key, emptyList(), "Generate poke", stagePrompt)
                "openai" -> callOpenAi(key, emptyList(), "Generate poke", stagePrompt)
                else -> callGemini(key, emptyList(), "Generate poke", stagePrompt, model = GEMINI_FAST_MODEL)
            }
        }

        result.getOrElse { getLocalPokeFallback(inactivityMinutes, hasRolloverDebt) }.trim().replace("\"", "")
    }

    fun getLocalPokeFallback(inactivityMinutes: Int, hasRolloverDebt: Boolean = false): String {
        if (hasRolloverDebt) {
            return listOf(
                "You carried over unfinished problems from yesterday. Procrastination is compounding like high-interest debt. Clear it now.",
                "Yesterday called: it wants its unfinished algorithms back. Stop dodging and open your editor.",
                "Unfinished tasks from yesterday are staring right at you. Finish yesterday's debt before today disappears too."
            ).random()
        }
        return when {
            inactivityMinutes < 90 -> listOf(
                "\"Waste no more time arguing what a good man should be. Be one.\" — Marcus Aurelius. 1 hour idle.",
                "\"It's not who I am underneath, but what I do that defines me.\" Open your next problem.",
                "\"We suffer more often in imagination than in reality.\" Get back to the algorithm.",
                "\"The only way out is through.\" Resume your 100xDevs bootcamp code."
            ).random()
            inactivityMinutes < 180 -> listOf(
                "Your streak is hanging by a thread while you stare into space. Harkirat already pushed 3 commits.",
                "Over 90 minutes inactive. That LeetCode problem isn't going to solve itself.",
                "You said you'd finish the module today. Procrastination is the thief of your tech career.",
                "Queue is waiting. Staring at social media won't pass your tech interviews."
            ).random()
            else -> listOf(
                "3+ hours of silence. Stop making excuses. Open Codeforces or TUF+ right now.",
                "You promised yourself discipline. This is how streaks die and goals vanish. Open the editor.",
                "Zero problems solved in 3 hours. Break the inertia before today is a complete write-off.",
                "If you can't commit to 1 problem today, don't complain about interview outcomes tomorrow."
            ).random()
        }
    }

    // --- Provider Call Implementations ---
    private fun callGemini(
        apiKey: String,
        imageList: List<ByteArray>,
        textInput: String,
        systemInstruction: String,
        model: String = if (imageList.isNotEmpty()) GEMINI_VISION_MODEL else GEMINI_FAST_MODEL
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val contents = JSONArray()
        val parts = JSONArray()

        val promptText = if (textInput.isNotBlank()) "$systemInstruction\n\nInput Context:\n$textInput" else systemInstruction
        parts.put(JSONObject().put("text", promptText))

        for (imgBytes in imageList) {
            if (imgBytes.isNotEmpty()) {
                val compressed = compressImageIfNeeded(imgBytes)
                val base64Data = Base64.encodeToString(compressed, Base64.NO_WRAP)
                parts.put(JSONObject().put("inline_data", JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", base64Data)
                }))
            }
        }

        contents.put(JSONObject().put("parts", parts))
        val bodyJson = JSONObject().put("contents", contents)

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                if (response.code == 429) {
                    throw Exception("Gemini 429: Resource Exhausted")
                }
                throw Exception("Gemini error (${response.code}): $body")
            }
            val root = JSONObject(body)
            val candidates = root.getJSONArray("candidates")
            val first = candidates.getJSONObject(0)
            val content = first.getJSONObject("content")
            val resParts = content.getJSONArray("parts")
            return resParts.getJSONObject(0).getString("text")
        }
    }

    private fun callDeepSeek(
        apiKey: String,
        imageList: List<ByteArray>,
        textInput: String,
        systemInstruction: String
    ): String {
        val url = "https://api.deepseek.com/chat/completions"

        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemInstruction)
        })

        val userContent = JSONArray()
        userContent.put(JSONObject().apply {
            put("type", "text")
            put("text", textInput.ifBlank { "Execute prompt" })
        })

        for (imgBytes in imageList) {
            if (imgBytes.isNotEmpty()) {
                val compressed = compressImageIfNeeded(imgBytes)
                val base64Data = Base64.encodeToString(compressed, Base64.NO_WRAP)
                userContent.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64Data"))
                })
            }
        }

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userContent)
        })

        val reqBody = JSONObject().apply {
            put("model", DEEPSEEK_MODEL)
            put("messages", messages)
            put("temperature", 0.3)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                if (response.code == 429) throw Exception("DeepSeek 429: Rate Limit")
                throw Exception("DeepSeek error (${response.code}): $body")
            }
            val root = JSONObject(body)
            val choices = root.getJSONArray("choices")
            return choices.getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun callOpenAi(
        apiKey: String,
        imageList: List<ByteArray>,
        textInput: String,
        systemInstruction: String
    ): String {
        val url = "https://api.openai.com/v1/chat/completions"

        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemInstruction)
        })

        val userContent = JSONArray()
        userContent.put(JSONObject().apply {
            put("type", "text")
            put("text", textInput.ifBlank { "Execute prompt" })
        })

        for (imgBytes in imageList) {
            if (imgBytes.isNotEmpty()) {
                val compressed = compressImageIfNeeded(imgBytes)
                val base64Data = Base64.encodeToString(compressed, Base64.NO_WRAP)
                userContent.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64Data"))
                })
            }
        }

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userContent)
        })

        val reqBody = JSONObject().apply {
            put("model", OPENAI_MODEL)
            put("messages", messages)
            put("temperature", 0.3)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                if (response.code == 429) throw Exception("OpenAI 429: Rate Limit")
                throw Exception("OpenAI error (${response.code}): $body")
            }
            val root = JSONObject(body)
            val choices = root.getJSONArray("choices")
            return choices.getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun cleanJson(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) clean = clean.removePrefix("```json")
        else if (clean.startsWith("```")) clean = clean.removePrefix("```")
        if (clean.endsWith("```")) clean = clean.removeSuffix("```")
        return clean.trim()
    }

    private fun parseContestJson(rawText: String, originalInput: String): ContestExtractionResult {
        val clean = cleanJson(rawText)
        val json = JSONObject(clean)
        val tileTitle = json.optString("contest_title", json.optString("title", "DSA Problem Set"))
        val summary = json.optString("summary", json.optString("notes", ""))
        val platform = json.optString("platform", if (originalInput.contains("codeforces")) "codeforces" else "leetcode").lowercase()
        val rawOverallDiff = json.optString("overall_difficulty", json.optString("difficulty", "MEDIUM")).uppercase()
        val overallDifficulty = when {
            rawOverallDiff.contains("HARD") -> "HARD"
            rawOverallDiff.contains("EASY") -> "EASY"
            else -> "MEDIUM"
        }
        val url = json.optString("url", originalInput.trim())

        val questionsList = mutableListOf<ContestQuestion>()

        if (json.has("questions")) {
            val qArr = json.getJSONArray("questions")
            for (i in 0 until qArr.length()) {
                val qObj = qArr.getJSONObject(i)
                val qName = qObj.optString("name", qObj.optString("title", "Question ${('A' + i)}"))
                val rawDiff = qObj.optString("difficulty", "MEDIUM").uppercase()
                val qDiff = when {
                    rawDiff.contains("HARD") -> "HARD"
                    rawDiff.contains("EASY") -> "EASY"
                    else -> "MEDIUM"
                }
                var qUrl = qObj.optString("url", "")
                if (qUrl.isBlank() && originalInput.contains("codeforces.com") && originalInput.contains("contest")) {
                    val cleanBase = originalInput.trim().substringBefore('?').removeSuffix("/")
                    val letterMatch = Regex("(?:Problem\\s+)?([A-Z])(?:[\\.\\s\\-:]|$)", RegexOption.IGNORE_CASE).find(qName)
                    val letter = letterMatch?.groupValues?.get(1)?.uppercase() ?: ('A' + i).toString()
                    qUrl = "$cleanBase/problem/$letter"
                }
                questionsList.add(ContestQuestion(name = qName, difficulty = qDiff, url = qUrl, isIncluded = true))
            }
        } else if (json.has("problems")) {
            val pArr = json.getJSONArray("problems")
            for (i in 0 until pArr.length()) {
                val pObj = pArr.getJSONObject(i)
                val qName = pObj.optString("title", "Question ${('A' + i)}")
                val rawDiff = pObj.optString("difficulty", "MEDIUM").uppercase()
                val qDiff = when {
                    rawDiff.contains("HARD") -> "HARD"
                    rawDiff.contains("EASY") -> "EASY"
                    else -> "MEDIUM"
                }
                var qUrl = pObj.optString("url", "")
                if (qUrl.isBlank() && originalInput.contains("codeforces.com") && originalInput.contains("contest")) {
                    val cleanBase = originalInput.trim().substringBefore('?').removeSuffix("/")
                    val letterMatch = Regex("(?:Problem\\s+)?([A-Z])(?:[\\.\\s\\-:]|$)", RegexOption.IGNORE_CASE).find(qName)
                    val letter = letterMatch?.groupValues?.get(1)?.uppercase() ?: ('A' + i).toString()
                    qUrl = "$cleanBase/problem/$letter"
                }
                questionsList.add(ContestQuestion(name = qName, difficulty = qDiff, url = qUrl, isIncluded = true))
            }
        }

        return ContestExtractionResult(
            tileTitle = tileTitle,
            summary = summary,
            platform = platform,
            overallDifficulty = overallDifficulty,
            url = url,
            questions = questionsList
        )
    }

    private fun parseCourseJson(rawText: String): Course {
        val clean = cleanJson(rawText)
        val json = JSONObject(clean)
        return Course(
            title = json.optString("title", "Online Course"),
            provider = json.optString("provider", "100xDevs"),
            totalModules = json.optInt("total_modules", 10),
            completedModules = 0,
            currentTopic = json.optString("current_topic", "Week 1: Foundations"),
            notes = json.optString("notes", "")
        )
    }

    private fun parseJobJson(rawText: String, originalInput: String): JobApplication {
        val clean = cleanJson(rawText)
        val json = JSONObject(clean)
        val rawStatus = json.optString("status", "SAVED").uppercase()
        val validStatus = when {
            rawStatus.contains("OFFER") -> "OFFER"
            rawStatus.contains("INTERVIEW") -> "INTERVIEWING"
            rawStatus.contains("APPLIED") -> "APPLIED"
            rawStatus.contains("REJECT") -> "REJECTED"
            else -> "SAVED"
        }
        return JobApplication(
            company = json.optString("company", "Tech Company"),
            role = json.optString("role", "Software Engineer"),
            status = validStatus,
            salary = json.optString("salary", ""),
            url = json.optString("url", originalInput.lines().firstOrNull { it.startsWith("http") } ?: ""),
            notes = json.optString("notes", "")
        )
    }

    private fun parseScheduleJson(rawText: String): ScheduleParseResult {
        val clean = cleanJson(rawText)
        val json = JSONObject(clean)
        val cutoff = json.optInt("day_cutoff_hour", 3).coerceIn(0, 23)
        val sleepStart = json.optInt("sleep_start_hour", cutoff).coerceIn(0, 23)
        val sleepDuration = json.optInt("sleep_duration_hours", 7).coerceIn(4, 12)
        val explanation = json.optString(
            "explanation",
            "Configured day cutoff at ${cutoff}:00 IST. Sleep DND active from ${sleepStart}:00 to ${(sleepStart + sleepDuration) % 24}:00 IST."
        )
        return ScheduleParseResult(
            dayCutoffHour = cutoff,
            sleepStartHour = sleepStart,
            sleepDurationHours = sleepDuration,
            explanation = explanation
        )
    }

    suspend fun testConnection(provider: String, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext Result.failure(Exception("API Key cannot be blank"))

            when (provider.lowercase()) {
                "deepseek" -> {
                    val request = Request.Builder()
                        .url("https://api.deepseek.com/models")
                        .header("Authorization", "Bearer $apiKey")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) Result.success("DeepSeek 4.1 Flash Connected Successfully!")
                        else Result.failure(Exception("DeepSeek Error: HTTP ${response.code}"))
                    }
                }
                "openai" -> {
                    val request = Request.Builder()
                        .url("https://api.openai.com/v1/models")
                        .header("Authorization", "Bearer $apiKey")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) Result.success("OpenAI Connected Successfully!")
                        else Result.failure(Exception("OpenAI Error: HTTP ${response.code}"))
                    }
                }
                else -> {
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_FAST_MODEL?key=$apiKey")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) Result.success("Google Gemini 3 / 3.1 Connected Successfully!")
                        else Result.failure(Exception("Gemini Error: HTTP ${response.code}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
