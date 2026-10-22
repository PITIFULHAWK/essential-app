package com.nothing.essential.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.nothing.essential.data.model.ChecklistItem
import com.nothing.essential.data.model.ContestExtractionResult
import com.nothing.essential.data.model.ContestQuestion
import com.nothing.essential.data.model.DsaProblem
import com.nothing.essential.ui.components.*
import com.nothing.essential.ui.theme.*

@Composable
fun AgentProposalDialog(
    extractionResult: ContestExtractionResult,
    onDismiss: () -> Unit,
    onConfirmTile: (DsaProblem) -> Unit,
    onReanalyze: (instructions: String) -> Unit,
    isReanalyzing: Boolean = false
) {
    var title by remember(extractionResult) { mutableStateOf(extractionResult.tileTitle) }
    var summary by remember(extractionResult) { mutableStateOf(extractionResult.summary) }
    var platform by remember(extractionResult) { mutableStateOf(extractionResult.platform) }
    var overallDifficulty by remember(extractionResult) { mutableStateOf(extractionResult.overallDifficulty) }
    var questions by remember(extractionResult) { mutableStateOf(extractionResult.questions) }
    var selectedIndices by remember(extractionResult) { mutableStateOf(extractionResult.questions.indices.toSet()) }
    var customInstructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NothingRed)
                    )
                    Text(
                        text = if (questions.isNotEmpty()) "AGENT CONTEST TILE" else "AGENT PROBLEM TILE",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    )
                }
                Text(
                    text = if (questions.isNotEmpty())
                        "Summarized under 1 tile with ${questions.size} questions"
                    else
                        "Review tile before committing to SQLite",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Platform pills
                item {
                    Text(
                        text = "PLATFORM",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (p in listOf("codeforces", "leetcode", "tuf", "custom")) {
                            CapsulePill(
                                text = p,
                                selected = platform == p,
                                onClick = { platform = p },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Tile Title Input
                item {
                    MaskedInputField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Tile Title",
                        placeholder = "e.g. Codeforces Contest 725607 - Factors & Intervals",
                        isSecret = false
                    )
                }

                // Summary / Notes Input
                item {
                    MaskedInputField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = "Topic Summary",
                        placeholder = "e.g. 8 Problems on interval sums and factor checking",
                        isSecret = false
                    )
                }

                // Overall Difficulty
                item {
                    Text(
                        text = "OVERALL DIFFICULTY",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (diff in listOf("EASY", "MEDIUM", "HARD")) {
                            CapsulePill(
                                text = diff,
                                selected = overallDifficulty == diff,
                                onClick = { overallDifficulty = diff },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Questions inside this tile
                if (questions.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUESTIONS IN THIS TILE (${selectedIndices.size} / ${questions.size})",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
                            )
                            Text(
                                text = if (selectedIndices.size == questions.size) "DESELECT ALL" else "SELECT ALL",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NothingRed),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        selectedIndices = if (selectedIndices.size == questions.size) emptySet() else questions.indices.toSet()
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    itemsIndexed(questions) { idx, q ->
                        val isSelected = selectedIndices.contains(idx)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF141414) else Color(0xFF0D0D0D))
                                .border(
                                    BorderStroke(1.dp, if (isSelected) Color(0xFF333333) else Color(0xFF1A1A1A)),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Checkbox
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(
                                                BorderStroke(1.dp, if (isSelected) StatusGreen else Color(0xFF444444)),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .background(if (isSelected) StatusGreen.copy(alpha = 0.2f) else AmoledBlack)
                                            .clickable {
                                                selectedIndices = if (isSelected) selectedIndices - idx else selectedIndices + idx
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(12.dp))
                                        }
                                    }

                                    // Question name
                                    Text(
                                        text = q.name,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) TextPrimary else TextTertiary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Inline difficulty pills for this question
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "DIFF:",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextTertiary)
                                    )
                                    for (d in listOf("EASY", "MEDIUM", "HARD")) {
                                        val isCurrent = q.difficulty.equals(d, ignoreCase = true)
                                        val diffColor = when (d) {
                                            "EASY" -> StatusGreen
                                            "HARD" -> NothingRed
                                            else -> LeetCodeOrange
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isCurrent) diffColor.copy(alpha = 0.2f) else Color.Transparent)
                                                .border(
                                                    BorderStroke(1.dp, if (isCurrent) diffColor else Color(0xFF2A2A2A)),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable {
                                                    val updated = questions.toMutableList()
                                                    updated[idx] = q.copy(difficulty = d)
                                                    questions = updated
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = d,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 8.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) diffColor else TextTertiary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom refinement box
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    MaskedInputField(
                        value = customInstructions,
                        onValueChange = { customInstructions = it },
                        label = "Adjust Contest Context / Instructions",
                        placeholder = "e.g. Exclude problem H, make tile title shorter...",
                        isSecret = false
                    )
                    if (customInstructions.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        PillButton(
                            text = if (isReanalyzing) "RE-ANALYZING..." else "RE-ANALYZE TILE",
                            onClick = { onReanalyze(customInstructions) },
                            enabled = !isReanalyzing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            PillButton(
                text = if (questions.isNotEmpty())
                    "CONFIRM & CREATE 1 TILE (${selectedIndices.size} Qs)"
                else
                    "CONFIRM & COMMIT TILE",
                onClick = {
                    val finalChecklist = questions
                        .filterIndexed { index, _ -> selectedIndices.contains(index) }
                        .mapIndexed { i, q ->
                            ChecklistItem(
                                problemId = 0,
                                text = q.name.trim(),
                                difficulty = q.difficulty,
                                url = q.url,
                                isCompleted = false,
                                sortOrder = i
                            )
                        }

                    val finalProblem = DsaProblem(
                        title = title.trim().ifBlank { "DSA Problem Set" },
                        platform = platform,
                        difficulty = overallDifficulty,
                        url = extractionResult.url.trim(),
                        notes = summary.trim(),
                        checklist = finalChecklist
                    )
                    onConfirmTile(finalProblem)
                },
                enabled = questions.isEmpty() || selectedIndices.isNotEmpty(),
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
