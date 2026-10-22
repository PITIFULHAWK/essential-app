package com.nothing.essential.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.data.model.ApiKeyItem
import com.nothing.essential.service.AiService
import com.nothing.essential.service.InactivityTracker
import com.nothing.essential.service.SnoozeParser
import com.nothing.essential.ui.components.*
import com.nothing.essential.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    database: AppDatabase
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(database.getSettings()) }
    var keyPool by remember { mutableStateOf<List<ApiKeyItem>>(emptyList()) }
    var showAddKeyDialog by remember { mutableStateOf(false) }

    var intervalMinutes by remember { mutableStateOf(settings.nudgeIntervalMinutes) }
    var targetGoal by remember { mutableStateOf(settings.dailyTargetGoal) }
    var isPokeEnabled by remember { mutableStateOf(settings.isPokeEnabled) }

    var dayCutoffHour by remember { mutableStateOf(settings.dayCutoffHour) }
    var sleepStartHour by remember { mutableStateOf(settings.sleepStartHour) }
    var sleepDurationHours by remember { mutableStateOf(settings.sleepDurationHours) }
    var isSleepDndEnabled by remember { mutableStateOf(settings.isSleepDndEnabled) }
    var scheduleNaturalInput by remember { mutableStateOf("") }
    var isConfiguringSchedule by remember { mutableStateOf(false) }
    var scheduleExplanation by remember { mutableStateOf<String?>(null) }

    fun refreshKeys() {
        keyPool = database.getApiKeyPool()
        settings = database.getSettings()
        intervalMinutes = settings.nudgeIntervalMinutes
        targetGoal = settings.dailyTargetGoal
        isPokeEnabled = settings.isPokeEnabled
        dayCutoffHour = settings.dayCutoffHour
        sleepStartHour = settings.sleepStartHour
        sleepDurationHours = settings.sleepDurationHours
        isSleepDndEnabled = settings.isSleepDndEnabled
    }

    LaunchedEffect(Unit) {
        refreshKeys()
    }

    fun saveSettings() {
        val updated = settings.copy(
            nudgeIntervalMinutes = intervalMinutes,
            dailyTargetGoal = targetGoal,
            isPokeEnabled = isPokeEnabled,
            dayCutoffHour = dayCutoffHour,
            sleepStartHour = sleepStartHour,
            sleepDurationHours = sleepDurationHours,
            isSleepDndEnabled = isSleepDndEnabled,
            deepseekModel = "deepseek-flash"
        )
        database.updateSettings(updated)
        settings = updated
        InactivityTracker.scheduleNextCheck(context, updated.nudgeIntervalMinutes)
        Toast.makeText(context, "SETTINGS SAVED", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- TOP BAR ---
        TopBarHeader(
            title = "CONFIGURATION",
            subtitle = "KEY POOL • COACHING • LATE-NIGHT IST",
            actionText = "SAVE",
            isActionPrimary = true,
            onActionClick = { saveSettings() },
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 6.dp)
        )

        // --- CARD 1: MULTI-ACCOUNT API KEY POOL ---
        NothingCard {
            CardHeader(
                title = "API KEY POOL (${keyPool.size} REGISTERED)",
                trailing = {
                    PillButton(
                        text = "+ ADD KEY",
                        onClick = { showAddKeyDialog = true },
                        isSolidWhite = true
                    )
                }
            )

            // Tier Hierarchy Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181818))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = "FAILOVER HIERARCHY:",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "1. Google Gemini 3 Flash (10 RPM) & 3.1 Flash-Lite (15 RPM) ➔ 2. DeepSeek 4.1 Flash (deepseek-flash) ➔ 3. OpenAI gpt-4o-mini",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (keyPool.isEmpty()) {
                Text(
                    text = "No keys registered in pool. Tap + ADD KEY to add your Google AI Studio free tier keys or DeepSeek keys.",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextTertiary)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    keyPool.forEach { item ->
                        KeyPoolItemCard(
                            item = item,
                            onDelete = {
                                database.deleteApiKey(item.id)
                                refreshKeys()
                            },
                            onTest = {
                                coroutineScope.launch {
                                    val res = AiService.testConnection(item.provider, item.apiKey)
                                    val msg = res.getOrElse { it.message ?: "Failed" }
                                    Toast.makeText(context, "${item.label}: $msg", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- CARD 2: ESCALATING BRUTALITY COACHING ENGINE ---
        NothingCard {
            CardHeader(
                title = "ESCALATING POKE ENGINE",
                trailing = {
                    Switch(
                        checked = isPokeEnabled,
                        onCheckedChange = {
                            isPokeEnabled = it
                            saveSettings()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = NothingRed,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = Color(0xFF242424)
                        )
                    )
                }
            )

            // Stage Timeline
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "BRUTALITY TIMELINE (TRIGGERS AFTER 60M IDLE):",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = TextSecondary)
                )
                Text(
                    text = "• 60–90m (Stage 1): Literary, poetic & cinematic wisdom (Marcus Aurelius, Oppenheimer, anime).",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFAAAAAA))
                )
                Text(
                    text = "• 90–180m (Stage 2): Sarcastic tough love on streak preservation & 100xDevs bootcamp.",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = LeetCodeOrange)
                )
                Text(
                    text = "• 180m+ (Stage 3): Unfiltered brutal reality check calling out procrastination.",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NothingRed)
                )
                Text(
                    text = "• Rollover Bully: Ruthlessly targets incomplete tasks carried over from yesterday.",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = NothingRed)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "INACTIVITY CHECK INTERVAL",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Cadence for background checks (10m, 15m, 20m, 30m). Triggers gentle nudge first, then escalating pokes after 60m of inactivity.",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextTertiary, lineHeight = 13.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (mins in listOf(10, 15, 20, 30)) {
                    CapsulePill(
                        text = "${mins}M",
                        selected = intervalMinutes == mins,
                        onClick = {
                            intervalMinutes = mins
                            saveSettings()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "DAILY DSA TARGET GOAL",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (goal in listOf(3, 5, 8, 10)) {
                    CapsulePill(
                        text = "$goal Qs",
                        selected = targetGoal == goal,
                        onClick = {
                            targetGoal = goal
                            saveSettings()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "DAILY PROGRESS SUMMARY NOTIFICATION",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Sends a notification detailing tasks solved today, target completion %, streak, and carried over debt.",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextTertiary, lineHeight = 13.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            PillButton(
                text = "SEND DAY SUMMARY NOTIFICATION",
                onClick = {
                    InactivityTracker.postDailySummaryNotification(context)
                    Toast.makeText(context, "DAY SUMMARY NOTIFICATION POSTED", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- CARD 3: AGENT SCHEDULE CONFIG // LATE-NIGHT IST ---
        NothingCard {
            CardHeader(
                title = "LATE-NIGHT SCHEDULE & SLEEP DND",
                trailing = {
                    Switch(
                        checked = isSleepDndEnabled,
                        onCheckedChange = {
                            isSleepDndEnabled = it
                            saveSettings()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = StatusGreen,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = Color(0xFF242424)
                        )
                    )
                }
            )

            val istCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            val istTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(istCal.time)
            val isSleeping = database.isInsideSleepWindow(settings)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSleeping) Color(0xFF1E281E) else Color(0xFF181818))
                    .border(BorderStroke(1.dp, if (isSleeping) StatusGreen.copy(alpha = 0.5f) else Color(0xFF2E2E2E)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IST CLOCK: $istTimeStr",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSleeping) StatusGreen.copy(alpha = 0.2f) else NothingRed.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isSleeping) "SLEEP DND ACTIVE" else "ACTIVE DEV SESSION",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (isSleeping) StatusGreen else NothingRed)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Logical Day runs until ${dayCutoffHour}:00 AM IST. Sleep window: ${sleepStartHour}:00 AM – ${(sleepStartHour + sleepDurationHours) % 24}:00 AM IST (${sleepDurationHours}h). Inactivity pokes silenced while sleeping.",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextSecondary, lineHeight = 13.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Natural Language Agent Config Input
            Text(
                text = "CONFIGURE SCHEDULE VIA AGENT",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            MaskedInputField(
                value = scheduleNaturalInput,
                onValueChange = { scheduleNaturalInput = it },
                label = "Natural Language Schedule Prompt",
                placeholder = "e.g. I sleep at 2:30 AM for 8 hours, day ends at 3 AM",
                isSecret = false
            )
            Spacer(modifier = Modifier.height(8.dp))
            PillButton(
                text = if (isConfiguringSchedule) "AGENT PARSING SCHEDULE..." else "⚡ CONFIGURE VIA AGENT",
                onClick = {
                    if (scheduleNaturalInput.isBlank()) return@PillButton
                    isConfiguringSchedule = true
                    coroutineScope.launch {
                        val res = AiService.parseScheduleNaturalLanguage(context, scheduleNaturalInput)
                        isConfiguringSchedule = false
                        res.onSuccess { parsed ->
                            dayCutoffHour = parsed.dayCutoffHour
                            sleepStartHour = parsed.sleepStartHour
                            sleepDurationHours = parsed.sleepDurationHours
                            scheduleExplanation = parsed.explanation
                            saveSettings()
                            Toast.makeText(context, "Schedule updated: ${parsed.explanation}", Toast.LENGTH_LONG).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "Agent config failed: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isConfiguringSchedule && scheduleNaturalInput.isNotBlank(),
                isSolidWhite = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (scheduleExplanation != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Agent: $scheduleExplanation",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = StatusGreen)
                )
            }

            HorizontalDivider(color = Color(0xFF262626), modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "LOGICAL DAY CUTOFF (IST)",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (hour in listOf(1, 2, 3, 4)) {
                    CapsulePill(
                        text = "${hour}:00 AM",
                        selected = dayCutoffHour == hour,
                        onClick = {
                            dayCutoffHour = hour
                            saveSettings()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SLEEP START TIME (IST)",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (sh in listOf(1, 2, 3, 4)) {
                    CapsulePill(
                        text = "${sh}:00 AM",
                        selected = sleepStartHour == sh,
                        onClick = {
                            sleepStartHour = sh
                            saveSettings()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SLEEP DURATION",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (dur in listOf(6, 7, 8, 9)) {
                    CapsulePill(
                        text = "${dur} Hours",
                        selected = sleepDurationHours == dur,
                        onClick = {
                            sleepDurationHours = dur
                            saveSettings()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- CARD 4: EXCUSED SNOOZE STATUS ---
        val isSnoozed = settings.snoozedUntilMillis > System.currentTimeMillis()
        if (isSnoozed) {
            NothingCard {
                CardHeader(title = "ACTIVE EXCUSED SNOOZE")
                Text(
                    text = "Expires in: ${SnoozeParser.formatDuration(settings.snoozedUntilMillis)}",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StatusGreen)
                )
                if (settings.snoozeReason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reason: ${settings.snoozeReason} (Pokes suppressed)",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                PillButton(
                    text = "CLEAR SNOOZE & RESUME MONITORING",
                    onClick = {
                        database.clearSnooze()
                        refreshKeys()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- CARD 5: DATA & CLOUD SYNC ---
        NothingCard {
            CardHeader(title = "DATA & CLOUD SYNC")
            Text(
                text = "All data is stored offline on this device. JSON backup is ready for Google Drive sync.",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextTertiary)
            )
            Spacer(modifier = Modifier.height(14.dp))
            PillButton(
                text = "EXPORT JSON",
                icon = Icons.Default.FileDownload,
                onClick = {
                    val json = database.exportToJson()
                    clipboardManager.setText(AnnotatedString(json))
                    Toast.makeText(context, "BACKUP COPIED TO CLIPBOARD", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- FOOTER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ESSENTIAL SPACE // AGENT CORE",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF555555), letterSpacing = 1.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: Nothing Phone (A059) • Android 17",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF444444))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAddKeyDialog) {
        AddApiKeyDialog(
            onDismiss = { showAddKeyDialog = false },
            onAddKey = { provider, label, key ->
                database.insertApiKey(ApiKeyItem(provider = provider, label = label, apiKey = key))
                refreshKeys()
                showAddKeyDialog = false
            }
        )
    }
}

@Composable
fun KeyPoolItemCard(
    item: ApiKeyItem,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    val providerColor = when (item.provider.lowercase()) {
        "gemini" -> TextPrimary
        "deepseek" -> CodeforcesBlue
        "openai" -> StatusGreen
        else -> TextSecondary
    }

    val statusColor = when (item.status) {
        "ACTIVE" -> StatusGreen
        "COOLING_DOWN" -> LeetCodeOrange
        else -> NothingRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141414))
            .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(providerColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.provider.uppercase(),
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = providerColor)
                        )
                    }
                    Text(
                        text = item.label,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.status,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = statusColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${item.apiKey.take(8)}••••••••••••${item.apiKey.takeLast(4)}",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextTertiary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillButton(
                    text = "TEST",
                    icon = Icons.Default.Bolt,
                    onClick = onTest,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddApiKeyDialog(
    onDismiss: () -> Unit,
    onAddKey: (provider: String, label: String, key: String) -> Unit
) {
    var provider by remember { mutableStateOf("gemini") }
    var label by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "ADD KEY TO POOL",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PROVIDER",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (p in listOf("gemini", "deepseek", "openai")) {
                        CapsulePill(
                            text = p,
                            selected = provider == p,
                            onClick = {
                                provider = p
                                if (label.isBlank()) label = "$p Project"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                MaskedInputField(
                    value = label,
                    onValueChange = { label = it },
                    label = "Account / Project Label",
                    placeholder = "e.g. Gemini Project 1",
                    isSecret = false
                )

                MaskedInputField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key Token",
                    placeholder = if (provider == "gemini") "AIzaSy..." else "sk-...",
                    isSecret = true
                )
            }
        },
        confirmButton = {
            PillButton(
                text = "ADD TO POOL",
                onClick = {
                    if (apiKey.isNotBlank()) {
                        val finalLabel = label.ifBlank { "$provider Key" }
                        onAddKey(provider, finalLabel.trim(), apiKey.trim())
                    }
                },
                isSolidWhite = true
            )
        },
        dismissButton = {
            PillButton(
                text = "CANCEL",
                onClick = onDismiss
            )
        }
    )
}
