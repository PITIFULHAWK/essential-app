package com.nothing.essential.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.service.SnoozeParser
import com.nothing.essential.ui.components.*
import com.nothing.essential.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SnoozeDialog(
    database: AppDatabase,
    onDismiss: () -> Unit,
    onSnoozeApplied: () -> Unit
) {
    var customText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var selectedTargetMillis by remember { mutableStateOf<Long?>(null) }

    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    fun setQuickMinutes(mins: Int) {
        selectedTargetMillis = System.currentTimeMillis() + mins * 60 * 1000L
        customText = "${mins}m"
    }

    LaunchedEffect(customText) {
        val parsed = SnoozeParser.parse(customText)
        if (parsed != null) {
            selectedTargetMillis = parsed
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "PAUSE // EXCUSED SNOOZE",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Setting a duration and reason excuses you from background agent pokes:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextTertiary, lineHeight = 16.sp)
                )

                // Quick Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CapsulePill(text = "+15M", selected = customText == "15m", onClick = { setQuickMinutes(15) }, modifier = Modifier.weight(1f))
                        CapsulePill(text = "+30M", selected = customText == "30m", onClick = { setQuickMinutes(30) }, modifier = Modifier.weight(1f))
                        CapsulePill(text = "+1H", selected = customText == "60m", onClick = { setQuickMinutes(60) }, modifier = Modifier.weight(1f))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CapsulePill(text = "+2H", selected = customText == "120m", onClick = { setQuickMinutes(120) }, modifier = Modifier.weight(1f))
                        CapsulePill(
                            text = "REST OF DAY",
                            selected = customText == "eod",
                            onClick = {
                                customText = "eod"
                                selectedTargetMillis = SnoozeParser.parse("eod")
                            },
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }

                MaskedInputField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = "Duration (e.g. 45m, 2h, until 5pm, tomorrow)",
                    placeholder = "45m...",
                    isSecret = false
                )

                MaskedInputField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = "Excused Reason (e.g. college exams, traveling)",
                    placeholder = "Why are you pausing? (Suppresses pokes)",
                    isSecret = false
                )

                // Preview
                if (selectedTargetMillis != null && selectedTargetMillis!! > System.currentTimeMillis()) {
                    val expireTime = sdf.format(Date(selectedTargetMillis!!))
                    Text(
                        text = "RESUMES AT: $expireTime (${SnoozeParser.formatDuration(selectedTargetMillis!!)})",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = StatusGreen
                        )
                    )
                }
            }
        },
        confirmButton = {
            PillButton(
                text = "APPLY EXCUSED SNOOZE",
                onClick = {
                    val target = selectedTargetMillis ?: (System.currentTimeMillis() + 15 * 60 * 1000L)
                    database.setExcusedSnooze(target, reasonText.trim())
                    onSnoozeApplied()
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
