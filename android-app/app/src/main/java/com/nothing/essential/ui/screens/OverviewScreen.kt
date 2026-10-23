package com.nothing.essential.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.data.model.AppSettings
import com.nothing.essential.data.model.DsaProblem
import com.nothing.essential.service.SnoozeParser
import com.nothing.essential.ui.components.*
import com.nothing.essential.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OverviewScreen(
    database: AppDatabase,
    onNavigateDsa: () -> Unit,
    onOpenSnoozeDialog: () -> Unit
) {
    var streak by remember { mutableStateOf(0) }
    var problems by remember { mutableStateOf<List<DsaProblem>>(emptyList()) }
    var settings by remember { mutableStateOf<AppSettings?>(null) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var isFocusMode by remember { mutableStateOf(false) }
    var weeklyStatus by remember { mutableStateOf<List<Pair<String, Boolean>>>(emptyList()) }

    var focusSecondsRemaining by remember { mutableStateOf(25 * 60) }
    var isFocusTimerRunning by remember { mutableStateOf(false) }

    fun refreshData() {
        val loadedSettings = database.getSettings()
        settings = loadedSettings
        streak = database.calculateStreak(loadedSettings)
        problems = database.getDsaProblems()
        weeklyStatus = database.getWeeklyStreakStatus(loadedSettings)
        currentTime = System.currentTimeMillis()
        if (loadedSettings.isFocusModeActive != isFocusMode) {
            isFocusMode = loadedSettings.isFocusModeActive
        }
    }

    fun toggleFocusMode() {
        val next = !isFocusMode
        isFocusMode = next
        settings?.let {
            database.updateSettings(it.copy(isFocusModeActive = next))
        }
        if (next && focusSecondsRemaining == 0) {
            focusSecondsRemaining = 25 * 60
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
        while (true) {
            kotlinx.coroutines.delay(10000L)
            currentTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(isFocusTimerRunning) {
        if (isFocusTimerRunning) {
            while (focusSecondsRemaining > 0 && isFocusTimerRunning) {
                kotlinx.coroutines.delay(1000L)
                focusSecondsRemaining--
            }
            if (focusSecondsRemaining == 0) {
                isFocusTimerRunning = false
            }
        }
    }

    val currentSettings = settings ?: return
    val rawElapsedMinutes = ((currentTime - currentSettings.lastActiveTimestamp) / 60000).coerceAtLeast(0)
    val hasUnfinishedDebt = database.hasUnfinishedRolloverDebt(currentSettings)
    val effectiveElapsedMinutes = if (hasUnfinishedDebt) maxOf(180, rawElapsedMinutes.toInt()) else rawElapsedMinutes.toInt()
    val isSnoozed = currentSettings.snoozedUntilMillis > currentTime

    val targetGoal = currentSettings.dailyTargetGoal
    val solvedToday = database.getTodaySolvedDsaCount(currentSettings)
    val progressPercent = if (targetGoal > 0) ((solvedToday.toFloat() / targetGoal.toFloat()) * 100).toInt().coerceAtMost(100) else 0

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val todayFormatted = remember { dateFormat.format(Date()).uppercase() }

    // Weekdays M, T, W, T, F, S, S
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val cal = Calendar.getInstance()
    val rawDay = cal.get(Calendar.DAY_OF_WEEK)
    val currentDayIndex = if (rawDay == Calendar.SUNDAY) 6 else rawDay - 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // --- TOP BAR: DATE + TITLE + FOCUS MODE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = todayFormatted,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ESSENTIAL SPACE",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextPrimary,
                        letterSpacing = 1.2.sp
                    )
                )
            }

            PillButton(
                text = if (isFocusMode) "EXIT FOCUS" else "FOCUS MODE",
                onClick = { toggleFocusMode() },
                hasRedDot = true,
                isSolidWhite = isFocusMode
            )
        }

        if (isFocusMode) {
            // --- FOCUS MODE CARD ---
            NothingCard {
                CardHeader(
                    title = "FOCUS MODE // DEEP WORK",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                    }
                )

                // Dot Matrix Timer Display
                val timerMinutes = focusSecondsRemaining / 60
                val timerSeconds = focusSecondsRemaining % 60
                val timerText = String.format("%02d:%02d", timerMinutes, timerSeconds)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DotMatrixText(
                            text = timerText,
                            dotSize = 5.dp,
                            activeColor = if (isFocusTimerRunning) NothingRed else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFocusTimerRunning) "TIMER RUNNING • AI POKES MUTED" else "TIMER PAUSED",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (isFocusTimerRunning) NothingRed else TextSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Timer Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillButton(
                        text = if (isFocusTimerRunning) "PAUSE" else "START",
                        onClick = { isFocusTimerRunning = !isFocusTimerRunning },
                        isSolidWhite = isFocusTimerRunning,
                        modifier = Modifier.weight(1f)
                    )
                    PillButton(
                        text = "RESET 25M",
                        onClick = {
                            isFocusTimerRunning = false
                            focusSecondsRemaining = 25 * 60
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Task / Problem
                val targetProblem = problems.firstOrNull { it.status != "SOLVED" }
                if (targetProblem != null) {
                    val nextItem = targetProblem.checklist.firstOrNull { !it.isCompleted }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141414))
                            .border(BorderStroke(1.dp, Color(0xFF282828)), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TARGET // ${targetProblem.platform.uppercase()}",
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextSecondary)
                                )
                                Text(
                                    text = targetProblem.difficulty.uppercase(),
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NothingRed, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = targetProblem.title,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                            if (nextItem != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1A1A1A))
                                        .clickable {
                                            database.updateChecklistItem(nextItem.id, true)
                                            refreshData()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .border(BorderStroke(1.dp, TextPrimary), RoundedCornerShape(3.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {}
                                    Text(
                                        text = "Next: ${nextItem.text}",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextPrimary),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "DONE ✓",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                PillButton(
                    text = "EXIT FOCUS MODE",
                    onClick = { toggleFocusMode() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- CARD 1: DAILY STREAK ---
        NothingCard {
            CardHeader(
                title = "DAILY STREAK",
                trailing = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1E1E))
                            .clickable {
                                database.touchActive()
                                refreshData()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔥 START TODAY",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (streak > 0) NothingRed else TextSecondary
                            )
                        )
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                DotMatrixText(
                    text = streak.toString().padStart(2, '0'),
                    activeColor = TextPrimary,
                    inactiveColor = DotInactive,
                    dotSize = 4.dp
                )

                Column {
                    Text(
                        text = "DAYS",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SOLVE 1 DSA QUESTION",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextTertiary,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7 Day Bubbles: M, T, W, T, F, S, S
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayList = if (weeklyStatus.size == 7) weeklyStatus else dayLabels.map { Pair(it, false) }
                displayList.forEachIndexed { index, pair ->
                    val dayLabel = pair.first
                    val isDone = pair.second
                    val isToday = index == currentDayIndex

                    val bgColor = when {
                        isDone -> TextPrimary
                        else -> Color(0xFF161616)
                    }
                    val textColor = when {
                        isDone -> Color.Black
                        isToday -> TextPrimary
                        else -> TextTertiary
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .then(
                                if (isToday) Modifier.border(BorderStroke(1.5.dp, NothingRed), CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isDone) "✓" else dayLabel,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isToday || isDone) FontWeight.Bold else FontWeight.Medium,
                                fontSize = if (isDone) 13.sp else 11.sp,
                                color = textColor
                            )
                        )
                    }
                }
            }
        }

        // --- CARD 2: DAILY DSA TARGET ---
        NothingCard {
            CardHeader(
                title = "DAILY DSA TARGET",
                trailing = {
                    Text(
                        text = "${solvedToday.toString().padStart(2, '0')} / ${targetGoal.toString().padStart(2, '0')}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    )
                }
            )

            Text(
                text = "$progressPercent% COMPLETED",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0 until targetGoal) {
                    val isSegmentFilled = i < solvedToday
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSegmentFilled) NothingRed else Color(0xFF1E1E1E))
                    )
                }
            }
        }

        // --- CARD 3: AI INACTIVITY MONITOR & ESCALATING POKE ---
        NothingCard {
            CardHeader(
                title = "AI INACTIVITY MONITOR",
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (hasUnfinishedDebt) NothingRed else if (isSnoozed) LeetCodeOrange else StatusGreen)
                    )
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (hasUnfinishedDebt) NothingRed else TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isSnoozed) "EXCUSED • ${SnoozeParser.formatDuration(currentSettings.snoozedUntilMillis)} REMAINING"
                           else if (hasUnfinishedDebt) "STAGE 3 MAX BRUTALITY • ${effectiveElapsedMinutes}M (DEBT PENDING)"
                           else "ACTIVE • ${effectiveElapsedMinutes}M AGO",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (hasUnfinishedDebt) NothingRed else TextPrimary
                    )
                )
            }

            // Live Escalating Poke Message or Reason
            if (isSnoozed && currentSettings.snoozeReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161616))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Reason: ${currentSettings.snoozeReason}",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                    )
                }
            } else if ((hasUnfinishedDebt || effectiveElapsedMinutes >= 60) && currentSettings.latestPokeMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val pokeColor = if (hasUnfinishedDebt || effectiveElapsedMinutes >= 180) NothingRed
                                else if (effectiveElapsedMinutes >= 90) LeetCodeOrange
                                else TextPrimary
                val pokeTitle = if (hasUnfinishedDebt) "AGENT POKE (STAGE 3 MAX BRUTALITY // UNFINISHED DEBT):"
                                else "AGENT POKE (${effectiveElapsedMinutes}M IDLE):"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF181818))
                        .border(BorderStroke(1.dp, pokeColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = pokeTitle,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = pokeColor)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = currentSettings.latestPokeMessage,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextPrimary, lineHeight = 16.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two Action Buttons: PAUSE / SNOOZE and MARK PROGRESS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton(
                    text = "PAUSE / SNOOZE",
                    onClick = onOpenSnoozeDialog,
                    modifier = Modifier.weight(1f)
                )

                PillButton(
                    text = "MARK PROGRESS",
                    onClick = {
                        database.touchActive()
                        database.logActivityEvent("MANUAL_PROGRESS", "Touched active on Home")
                        refreshData()
                    },
                    isSolidWhite = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
