package com.nothing.essential.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.data.model.AttachmentItem
import com.nothing.essential.data.model.Course
import com.nothing.essential.service.AiService
import com.nothing.essential.util.AttachmentHelper
import com.nothing.essential.ui.components.*
import com.nothing.essential.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CoursesScreen(
    database: AppDatabase
) {
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadCourses() {
        courses = database.getCourses()
    }

    LaunchedEffect(Unit) {
        loadCourses()
    }

    val activeCount = courses.size

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
                title = "COURSE TRACKER",
                subtitle = "$activeCount ACTIVE COURSES",
                actionText = "+ ADD",
                isActionPrimary = true,
                onActionClick = { showAddDialog = true }
            )

            // --- EMPTY STATE OR LIST ---
            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "NO COURSES TRACKED",
                        subtitle = "Add your online courses, certifications, or books to monitor chapter progress.",
                        buttonText = "+ ADD COURSE",
                        onButtonClick = { showAddDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(courses, key = { it.id }) { course ->
                        CourseCard(
                            course = course,
                            onUpdateModules = { delta ->
                                val newCount = (course.completedModules + delta).coerceIn(0, course.totalModules)
                                database.updateCourseProgress(course.id, newCount, course.currentTopic)
                                loadCourses()
                            },
                            onUpdateTopic = { newTopic ->
                                database.updateCourseProgress(course.id, course.completedModules, newTopic)
                                loadCourses()
                            },
                            onDelete = {
                                database.deleteCourse(course.id)
                                loadCourses()
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
        AddCourseDialog(
            database = database,
            onDismiss = { showAddDialog = false },
            onCourseAdded = {
                loadCourses()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CourseCard(
    course: Course,
    onUpdateModules: (Int) -> Unit,
    onUpdateTopic: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditingTopic by remember { mutableStateOf(false) }
    var topicText by remember { mutableStateOf(course.currentTopic) }

    val progress = if (course.totalModules > 0) {
        course.completedModules.toFloat() / course.totalModules.toFloat()
    } else 0f

    NothingCard(
        borderColor = if (course.completedModules >= course.totalModules && course.totalModules > 0) StatusGreen else CardBorder
    ) {
        // Provider tag & Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = course.provider.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = NothingRed,
                    letterSpacing = 1.sp
                )
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = course.title,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Current Topic
        if (isEditingTopic) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaskedInputField(
                    value = topicText,
                    onValueChange = { topicText = it },
                    label = "CURRENT TOPIC",
                    placeholder = "Topic...",
                    isSecret = false,
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "SAVE",
                    onClick = {
                        onUpdateTopic(topicText)
                        isEditingTopic = false
                    },
                    isSolidWhite = true,
                    modifier = Modifier.padding(top = 18.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CURRENT TOPIC:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (course.currentTopic.isNotBlank()) course.currentTopic else "Tap edit to set current topic...",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (course.currentTopic.isNotBlank()) TextPrimary else TextTertiary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                PillButton(
                    text = "EDIT TOPIC",
                    onClick = { isEditingTopic = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (progress >= 1f) StatusGreen else NothingRed,
            trackColor = Color(0xFF1E1E1E)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Module Counts & Delta Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${course.completedModules} / ${course.totalModules} COMPLETED (${(progress * 100).toInt()}%)",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    text = "-1",
                    onClick = { onUpdateModules(-1) },
                    enabled = course.completedModules > 0
                )
                PillButton(
                    text = "+1 MODULE",
                    onClick = { onUpdateModules(1) },
                    isSolidWhite = true,
                    enabled = course.completedModules < course.totalModules
                )
            }
        }
    }
}

@Composable
fun AddCourseDialog(
    database: AppDatabase,
    onDismiss: () -> Unit,
    onCourseAdded: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("100xDevs") }
    var totalModulesStr by remember { mutableStateOf("10") }
    var currentTopic by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var courseContext by remember { mutableStateOf("") }
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
                    text = "ADD COURSE // AGENT INGESTION",
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
                    text = "Attach syllabus documents (PDF/HTML), curriculum screenshots, or enter course link:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextTertiary, lineHeight = 16.sp)
                )

                // Context or URL
                MaskedInputField(
                    value = courseContext,
                    onValueChange = { courseContext = it },
                    label = "Course URL or Syllabus Outline",
                    placeholder = "https://100xdevs.com or syllabus notes...",
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
                            text = "+ ATTACH SYLLABUS / SCREENSHOTS",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                        )
                        Text(
                            text = "Supports PDF curriculum, HTML syllabus, screenshots",
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
                    text = if (isAnalyzing) "EXTRACTING CURRICULUM..." else "⚡ EXTRACT VIA AGENT",
                    onClick = {
                        isAnalyzing = true
                        coroutineScope.launch {
                            val res = AiService.extractCourse(context, attachments, courseContext)
                            isAnalyzing = false
                            res.onSuccess { extracted ->
                                title = extracted.title
                                provider = extracted.provider
                                totalModulesStr = extracted.totalModules.toString()
                                currentTopic = extracted.currentTopic
                                notes = extracted.notes
                                Toast.makeText(context, "Syllabus Extracted Successfully!", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Extraction failed: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isAnalyzing && (courseContext.isNotBlank() || attachments.isNotEmpty()),
                    isSolidWhite = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color(0xFF2E2E2E), modifier = Modifier.padding(vertical = 4.dp))

                // Editable Fields
                MaskedInputField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Course Title",
                    placeholder = "e.g. Winter Bootcamp 1.0",
                    isSecret = false
                )
                MaskedInputField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = "Provider / Platform",
                    placeholder = "e.g. 100xDevs / Coursera / Udacity",
                    isSecret = false
                )
                MaskedInputField(
                    value = totalModulesStr,
                    onValueChange = { totalModulesStr = it },
                    label = "Total Modules / Weeks",
                    placeholder = "10",
                    isSecret = false
                )
                MaskedInputField(
                    value = currentTopic,
                    onValueChange = { currentTopic = it },
                    label = "Starting Topic",
                    placeholder = "e.g. Week 1: Rust Basics",
                    isSecret = false
                )
                MaskedInputField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes / Core Stack",
                    placeholder = "Technologies, modules summary...",
                    isSecret = false
                )
            }
        },
        confirmButton = {
            PillButton(
                text = "SAVE COURSE",
                onClick = {
                    if (title.isBlank()) return@PillButton
                    val total = totalModulesStr.toIntOrNull() ?: 10
                    database.insertCourse(
                        Course(
                            title = title.trim(),
                            provider = provider.trim(),
                            totalModules = total,
                            completedModules = 0,
                            currentTopic = currentTopic.trim(),
                            notes = notes.trim()
                        )
                    )
                    onCourseAdded()
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
