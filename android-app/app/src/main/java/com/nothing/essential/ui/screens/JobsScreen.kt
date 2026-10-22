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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.data.model.AttachmentItem
import com.nothing.essential.data.model.JobApplication
import com.nothing.essential.service.AiService
import com.nothing.essential.util.AttachmentHelper
import com.nothing.essential.ui.components.*
import com.nothing.essential.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun JobsScreen(
    database: AppDatabase
) {
    var jobs by remember { mutableStateOf<List<JobApplication>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadJobs() {
        jobs = database.getJobs()
    }

    LaunchedEffect(Unit) {
        loadJobs()
    }

    val totalCount = jobs.size
    val filteredJobs = remember(jobs, selectedFilter) {
        if (selectedFilter == "ALL") jobs else jobs.filter { it.status.equals(selectedFilter, ignoreCase = true) }
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
                title = "JOB PIPELINE",
                subtitle = "$totalCount APPLICATIONS TRACKED",
                actionText = "+ ADD",
                isActionPrimary = true,
                onActionClick = { showAddDialog = true }
            )

            // --- STAGE FILTERS ---
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val stages = listOf("ALL", "SAVED", "APPLIED", "INTERVIEWING", "OFFER")
                items(stages) { stage ->
                    CapsulePill(
                        text = stage,
                        selected = selectedFilter == stage,
                        onClick = { selectedFilter = stage }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- EMPTY STATE OR LIST ---
            if (filteredJobs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.Work,
                        title = "NO APPLICATIONS",
                        subtitle = "Track links and company names found online to stay organized during hiring rounds.",
                        buttonText = "+ ADD APPLICATION",
                        onButtonClick = { showAddDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredJobs, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            onCycleStatus = {
                                val nextStatus = when (job.status.uppercase()) {
                                    "SAVED" -> "APPLIED"
                                    "APPLIED" -> "INTERVIEWING"
                                    "INTERVIEWING" -> "OFFER"
                                    "OFFER" -> "REJECTED"
                                    else -> "SAVED"
                                }
                                database.updateJobStatus(job.id, nextStatus)
                                loadJobs()
                            },
                            onDelete = {
                                database.deleteJob(job.id)
                                loadJobs()
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

    if (showAddDialog) {
        AddJobDialog(
            database = database,
            onDismiss = { showAddDialog = false },
            onJobAdded = {
                loadJobs()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun JobCard(
    job: JobApplication,
    onCycleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = when (job.status.uppercase()) {
        "OFFER" -> StatusGreen
        "INTERVIEWING" -> LeetCodeOrange
        "APPLIED" -> CodeforcesBlue
        "REJECTED" -> NothingRed
        else -> TextSecondary
    }

    NothingCard {
        // Status Badge & Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, statusColor, RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .clickable { onCycleStatus() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${job.status.uppercase()} (TAP TO CHANGE)",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = statusColor
                    )
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Company & Role
        Text(
            text = job.company.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        )
        Text(
            text = job.role,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = TextSecondary
            )
        )

        if (job.salary.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "COMP: ${job.salary}",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            )
        }

        if (job.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = job.notes,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            )
        }

        if (job.url.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(job.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Text(
                    text = "JOB POSTING LINK",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextSecondary)
                )
            }
        }
    }
}

@Composable
fun AddJobDialog(
    database: AppDatabase,
    onDismiss: () -> Unit,
    onJobAdded: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var company by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("SAVED") }
    var salary by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var jobContext by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<AttachmentItem>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapNotNull { AttachmentHelper.processUri(context, it) }
            attachments = (attachments + newItems).distinctBy { it.uriString }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isAnalyzing) onDismiss() },
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(NothingRed))
                Text(
                    text = "ADD JOB // AGENT INGESTION",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Attach job posting screenshot, JD document (PDF/HTML), or enter posting link:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextTertiary, lineHeight = 16.sp)
                )

                // Context or URL
                MaskedInputField(
                    value = jobContext,
                    onValueChange = { jobContext = it },
                    label = "Job Posting URL / Description",
                    placeholder = "https://careers.google.com/... or job requirements",
                    isSecret = false
                )

                // Attachment Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, Color(0xFF333333)), RoundedCornerShape(10.dp))
                        .background(Color(0xFF181818))
                        .clickable { filePickerLauncher.launch(arrayOf("image/*", "application/pdf", "text/html", "text/plain")) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "+ ATTACH SCREENSHOT / JD PDF",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                        )
                        Text(
                            text = "Supports screenshots, JD PDFs, HTML postings",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextTertiary)
                        )
                    }
                }

                // Attachments row
                if (attachments.isNotEmpty()) {
                    Text(
                        text = "ATTACHED FILES (${attachments.size}):",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = TextSecondary)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(attachments) { item ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222222))
                                    .border(BorderStroke(1.dp, Color(0xFF383838)), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (item.bytes.isNotEmpty()) {
                                        val bmp = remember(item.bytes) { BitmapFactory.decodeByteArray(item.bytes, 0, item.bytes.size) }
                                        if (bmp != null) {
                                            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(26.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                                        }
                                    }
                                    Column {
                                        Text(text = item.name.take(12) + if (item.name.length > 12) "..." else "", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextPrimary))
                                        Text(text = if (item.bytes.isNotEmpty()) "${item.bytes.size / 1024} KB" else item.mimeType.substringAfter('/'), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextTertiary))
                                    }
                                    IconButton(onClick = { attachments = attachments.filter { it != item } }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Extract Button
                PillButton(
                    text = if (isAnalyzing) "ANALYZING ROLE..." else "⚡ EXTRACT VIA AGENT",
                    onClick = {
                        isAnalyzing = true
                        coroutineScope.launch {
                            val res = AiService.extractJob(context, attachments, jobContext)
                            isAnalyzing = false
                            res.onSuccess { extracted ->
                                company = extracted.company
                                role = extracted.role
                                status = extracted.status
                                salary = extracted.salary
                                if (extracted.url.isNotBlank()) url = extracted.url
                                notes = extracted.notes
                                Toast.makeText(context, "Role Parsed Successfully!", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Extraction failed: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isAnalyzing && (jobContext.isNotBlank() || attachments.isNotEmpty()),
                    isSolidWhite = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color(0xFF2E2E2E), modifier = Modifier.padding(vertical = 4.dp))

                // Editable Fields
                MaskedInputField(
                    value = company,
                    onValueChange = { company = it },
                    label = "Company Name",
                    placeholder = "e.g. Google / Nothing / Startup",
                    isSecret = false
                )
                MaskedInputField(
                    value = role,
                    onValueChange = { role = it },
                    label = "Role",
                    placeholder = "e.g. Software Engineer (Backend)",
                    isSecret = false
                )

                Text(
                    text = "STATUS",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextSecondary)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (st in listOf("SAVED", "APPLIED", "INTERVIEWING")) {
                        CapsulePill(
                            text = st,
                            selected = status == st,
                            onClick = { status = st },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                MaskedInputField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = "Compensation / Range",
                    placeholder = "e.g. $120k / 25 LPA",
                    isSecret = false
                )
                MaskedInputField(
                    value = url,
                    onValueChange = { url = it },
                    label = "Listing / Careers URL",
                    placeholder = "https://...",
                    isSecret = false
                )
                MaskedInputField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes / Referral Info",
                    placeholder = "Referral contact, tech stack, etc.",
                    isSecret = false
                )
            }
        },
        confirmButton = {
            PillButton(
                text = "SAVE APPLICATION",
                onClick = {
                    if (company.isBlank() || role.isBlank()) return@PillButton
                    database.insertJob(
                        JobApplication(
                            company = company.trim(),
                            role = role.trim(),
                            status = status,
                            salary = salary.trim(),
                            url = url.trim(),
                            notes = notes.trim()
                        )
                    )
                    onJobAdded()
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
