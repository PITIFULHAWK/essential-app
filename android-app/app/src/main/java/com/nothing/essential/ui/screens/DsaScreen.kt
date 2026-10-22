package com.nothing.essential.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.data.model.AttachmentItem
import com.nothing.essential.data.model.ChecklistItem
import com.nothing.essential.data.model.ContestExtractionResult
import com.nothing.essential.data.model.DsaProblem
import com.nothing.essential.service.AiService
import com.nothing.essential.util.AttachmentHelper
import com.nothing.essential.ui.components.*
import com.nothing.essential.ui.dialogs.AgentProposalDialog
import com.nothing.essential.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DsaScreen(
    database: AppDatabase
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var problems by remember { mutableStateOf<List<DsaProblem>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Agent Pipeline State
    var showRawInputDialog by remember { mutableStateOf(false) }
    var rawInputText by remember { mutableStateOf("") }
    var extraContextText by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<AttachmentItem>>(emptyList()) }
    var isAgentAnalyzing by remember { mutableStateOf(false) }
    var agentError by remember { mutableStateOf<String?>(null) }
    var stagedContestResult by remember { mutableStateOf<ContestExtractionResult?>(null) }
    var isReanalyzing by remember { mutableStateOf(false) }

    // Multi-Document & Multi-Image Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapNotNull { AttachmentHelper.processUri(context, it) }
            attachments = (attachments + newItems).distinctBy { it.uriString }
        }
    }

    fun loadProblems() {
        problems = database.getDsaProblems()
    }

    LaunchedEffect(Unit) {
        loadProblems()
    }

    val activeCount = problems.count { it.status != "SOLVED" }
    val solvedCount = problems.count { it.status == "SOLVED" }
    val allCount = problems.size

    val filteredProblems = remember(problems, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> problems.filter { it.status != "SOLVED" }
            "DONE" -> problems.filter { it.status == "SOLVED" }
            else -> problems
        }
    }

    Scaffold(
        containerColor = AmoledBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            // --- TOP BAR ---
            TopBarHeader(
                title = "DSA QUEUE",
                subtitle = "$activeCount ACTIVE • $solvedCount SOLVED",
                actionText = "+ ADD",
                isActionPrimary = true,
                onActionClick = {
                    rawInputText = ""
                    extraContextText = ""
                    attachments = emptyList()
                    agentError = null
                    showRawInputDialog = true
                }
            )

            // --- FILTER PILLS ---
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CapsulePill(
                        text = "ALL ($allCount)",
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" }
                    )
                }
                item {
                    CapsulePill(
                        text = "ACTIVE ($activeCount)",
                        selected = selectedFilter == "ACTIVE",
                        onClick = { selectedFilter = "ACTIVE" }
                    )
                }
                item {
                    CapsulePill(
                        text = "DONE ($solvedCount)",
                        selected = selectedFilter == "DONE",
                        onClick = { selectedFilter = "DONE" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- EMPTY STATE OR LIST ---
            if (filteredProblems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.Code,
                        title = "QUEUE IS EMPTY",
                        subtitle = "Snap a contest screenshot or paste a Codeforces / LeetCode / TUF link to create a summarized tile.",
                        buttonText = "+ CAPTURE / ADD TILE",
                        onButtonClick = {
                            rawInputText = ""
                            extraContextText = ""
                            attachments = emptyList()
                            agentError = null
                            showRawInputDialog = true
                        }
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProblems, key = { it.id }) { problem ->
                        DsaProblemCard(
                            problem = problem,
                            onToggleChecklist = { item ->
                                database.updateChecklistItem(item.id, !item.isCompleted)
                                loadProblems()
                            },
                            onCycleStatus = {
                                val nextStatus = when (problem.status) {
                                    "TODO" -> "IN_PROGRESS"
                                    "IN_PROGRESS" -> "SOLVED"
                                    else -> "TODO"
                                }
                                database.updateDsaProblemStatus(problem.id, nextStatus)
                                loadProblems()
                            },
                            onDelete = {
                                database.deleteDsaProblem(problem.id)
                                loadProblems()
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // --- STEP 1: RAW INPUT PROMPT INTO AGENT PIPELINE (URL + MULTI-ATTACHMENT) ---
    if (showRawInputDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAgentAnalyzing) showRawInputDialog = false },
            containerColor = CardBackground,
            shape = RoundedCornerShape(16.dp),
            title = {
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
                        text = "AGENT PIPELINE // ADD TILE",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste a problem/contest URL, upload screenshots or syllabus/sheet documents (PDF/HTML/TXT), and add extra context below:",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextTertiary, lineHeight = 16.sp)
                    )

                    // URL or Contest Link
                    MaskedInputField(
                        value = rawInputText,
                        onValueChange = { rawInputText = it },
                        label = "Problem or Contest URL",
                        placeholder = "e.g. https://codeforces.com/group/DNruwPkRNA/contest/725607",
                        isSecret = false
                    )

                    // MULTI-IMAGE & DOCUMENT ATTACHMENT SECTION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFF333333)), RoundedCornerShape(10.dp))
                            .background(Color(0xFF181818))
                            .clickable {
                                filePickerLauncher.launch(
                                    arrayOf("image/*", "application/pdf", "text/html", "text/plain")
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Files",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "+ ATTACH SCREENSHOTS / FILES",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Supports multi-image, PDF sheets, HTML syllabus",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = TextTertiary
                                )
                            )
                        }
                    }

                    // Attached Items Carousel
                    if (attachments.isNotEmpty()) {
                        Text(
                            text = "ATTACHED (${attachments.size}):",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = TextSecondary)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attachments) { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF222222))
                                        .border(BorderStroke(1.dp, Color(0xFF383838)), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (item.bytes.isNotEmpty()) {
                                            val bmp = remember(item.bytes) {
                                                BitmapFactory.decodeByteArray(item.bytes, 0, item.bytes.size)
                                            }
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = item.name.take(14) + if (item.name.length > 14) "..." else "",
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextPrimary)
                                            )
                                            Text(
                                                text = if (item.bytes.isNotEmpty()) "${item.bytes.size / 1024} KB" else item.mimeType.substringAfter('/'),
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextTertiary)
                                            )
                                        }
                                        IconButton(
                                            onClick = { attachments = attachments.filter { it != item } },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Extra Context Text Field (below image/attachments)
                    MaskedInputField(
                        value = extraContextText,
                        onValueChange = { extraContextText = it },
                        label = "Extra Context / Below-Image Notes",
                        placeholder = "e.g. Focus on dynamic programming, solve A and B first...",
                        isSecret = false
                    )

                    if (agentError != null) {
                        Text(
                            text = agentError!!,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NothingRed)
                        )
                    }
                }
            },
            confirmButton = {
                PillButton(
                    text = if (isAgentAnalyzing) "ANALYZING..." else "RUN AGENT PIPELINE",
                    onClick = {
                        val hasInput = rawInputText.isNotBlank() || extraContextText.isNotBlank() || attachments.isNotEmpty()
                        if (!hasInput) return@PillButton
                        isAgentAnalyzing = true
                        agentError = null
                        coroutineScope.launch {
                            val combinedPrompt = buildString {
                                if (rawInputText.isNotBlank()) appendLine(rawInputText)
                                if (extraContextText.isNotBlank()) {
                                    appendLine("\nExtra Context Notes:\n$extraContextText")
                                }
                            }.trim()

                            val result = AiService.extractContestOrProblem(
                                context = context,
                                attachments = attachments,
                                textInput = combinedPrompt
                            )
                            isAgentAnalyzing = false
                            result.onSuccess { extractionResult ->
                                stagedContestResult = extractionResult
                                showRawInputDialog = false
                            }.onFailure { err ->
                                agentError = err.message ?: "Agent analysis failed"
                            }
                        }
                    },
                    enabled = !isAgentAnalyzing && (rawInputText.isNotBlank() || extraContextText.isNotBlank() || attachments.isNotEmpty()),
                    isSolidWhite = true
                )
            },
            dismissButton = {
                PillButton(
                    text = "CANCEL",
                    onClick = { showRawInputDialog = false }
                )
            }
        )
    }

    // --- STEP 2: AGENT CONTEST / PROBLEM PROPOSAL CARD (1 TILE) ---
    if (stagedContestResult != null) {
        AgentProposalDialog(
            extractionResult = stagedContestResult!!,
            onDismiss = { stagedContestResult = null },
            onConfirmTile = { finalProblemTile ->
                database.insertDsaProblem(finalProblemTile)
                loadProblems()
                stagedContestResult = null
                rawInputText = ""
                extraContextText = ""
                attachments = emptyList()
                Toast.makeText(context, "TILE COMMITTED TO QUEUE", Toast.LENGTH_SHORT).show()
            },
            onReanalyze = { customInstructions ->
                isReanalyzing = true
                coroutineScope.launch {
                    val combinedInput = buildString {
                        if (rawInputText.isNotBlank()) appendLine(rawInputText)
                        if (extraContextText.isNotBlank()) appendLine(extraContextText)
                        appendLine("\nUser Adjustments:\n$customInstructions")
                    }.trim()

                    val reResult = AiService.extractContestOrProblem(
                        context = context,
                        attachments = attachments,
                        textInput = combinedInput
                    )
                    isReanalyzing = false
                    reResult.onSuccess { reExtracted ->
                        stagedContestResult = reExtracted
                    }.onFailure { e ->
                        Toast.makeText(context, "Re-analysis failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            isReanalyzing = isReanalyzing
        )
    }
}

@Composable
fun DsaProblemCard(
    problem: DsaProblem,
    onToggleChecklist: (ChecklistItem) -> Unit,
    onCycleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val platformColor = when (problem.platform.lowercase()) {
        "leetcode" -> LeetCodeOrange
        "codeforces" -> CodeforcesBlue
        "tuf" -> TufViolet
        else -> TextSecondary
    }

    val difficultyColor = when (problem.difficulty.uppercase()) {
        "EASY" -> StatusGreen
        "HARD" -> NothingRed
        else -> LeetCodeOrange
    }

    val isSolved = problem.status == "SOLVED"
    val completedCount = problem.checklist.count { it.isCompleted }
    val totalCount = problem.checklist.size
    val isMultiQuestion = totalCount > 1

    NothingCard(
        borderColor = if (isSolved) CardBorder else Color(0xFF333333)
    ) {
        // Platform, Difficulty & Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Platform Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(platformColor.copy(alpha = 0.15f))
                        .border(1.dp, platformColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = problem.platform.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = platformColor
                        )
                    )
                }

                // Rollover Debt Badge
                if (problem.isRolledOver && !isSolved) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NothingRed.copy(alpha = 0.2f))
                            .border(1.dp, NothingRed, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "CARRIED OVER",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = NothingRed
                            )
                        )
                    }
                }

                // Overall Difficulty or Question Count Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, difficultyColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isMultiQuestion) "$totalCount QS • ${problem.difficulty.uppercase()}" else problem.difficulty.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = difficultyColor
                        )
                    )
                }

                // Progress Badge if multiple questions
                if (isMultiQuestion) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (completedCount == totalCount) StatusGreen.copy(alpha = 0.2f) else Color(0xFF1E1E1E))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$completedCount/$totalCount DONE",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (completedCount == totalCount) StatusGreen else TextSecondary
                            )
                        )
                    }
                }
            }

            // Status Badge / Cycle Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSolved) StatusGreen else PillBackground)
                    .clickable { onCycleStatus() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = problem.status.replace("_", " "),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isSolved) AmoledBlack else TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tile Title
        Text(
            text = problem.title,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isSolved) TextSecondary else TextPrimary
            )
        )

        // Topic Summary / Notes
        if (problem.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = problem.notes,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    lineHeight = 15.sp
                )
            )
        }

        // Clean Questions List (inside this single tile, with individual difficulties, NO verbose hints)
        if (problem.checklist.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isMultiQuestion) "QUESTIONS ($completedCount / $totalCount)" else "CHECKPOINTS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (item in problem.checklist) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (item.isCompleted) Color(0xFF0F0F0F) else Color(0xFF161616))
                            .border(
                                BorderStroke(1.dp, if (item.isCompleted) Color(0xFF222222) else Color(0xFF2A2A2A)),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onToggleChecklist(item) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .border(BorderStroke(1.dp, if (item.isCompleted) StatusGreen else Color(0xFF444444)), RoundedCornerShape(3.dp))
                                .background(if (item.isCompleted) StatusGreen.copy(alpha = 0.2f) else AmoledBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Question Name
                        Text(
                            text = item.text,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (item.isCompleted) TextTertiary else TextPrimary,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Individual Question Difficulty Badge!
                        if (item.difficulty.isNotBlank()) {
                            val qDiffColor = when (item.difficulty.uppercase()) {
                                "EASY" -> StatusGreen
                                "HARD" -> NothingRed
                                else -> LeetCodeOrange
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(qDiffColor.copy(alpha = 0.15f))
                                    .border(1.dp, qDiffColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.difficulty.uppercase(),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        color = qDiffColor
                                    )
                                )
                            }
                        }

                        // Direct Link Icon
                        if (item.url.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                                    } catch (e: Exception) {}
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open Problem",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Actions: Contest Link & Delete Tile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (problem.url.isNotBlank()) {
                Row(
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(problem.url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Text(
                        text = if (isMultiQuestion) "OPEN CONTEST" else "OPEN LINK",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextSecondary)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Tile", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
