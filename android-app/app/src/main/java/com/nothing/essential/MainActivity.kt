package com.nothing.essential

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.essential.data.AppDatabase
import com.nothing.essential.service.InactivityTracker
import com.nothing.essential.ui.dialogs.SnoozeDialog
import com.nothing.essential.ui.screens.*
import com.nothing.essential.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        InactivityTracker.createNotificationChannels(this)
        checkAndRequestPermissions()

        val database = AppDatabase.getInstance(this)
        database.syncStreakLogsFromHistory()

        setContent {
            NothingTheme {
                MainAppScaffold(database = database)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}

enum class ScreenTab(val title: String, val icon: ImageVector) {
    STREAK("STREAK", Icons.Default.Bolt),
    DSA("DSA", Icons.Default.Code),
    COURSES("COURSES", Icons.AutoMirrored.Filled.MenuBook),
    JOBS("JOBS", Icons.Default.Work),
    CONFIG("CONFIG", Icons.Default.Tune)
}

@Composable
fun MainAppScaffold(database: AppDatabase) {
    var currentTab by remember { mutableStateOf(ScreenTab.STREAK) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = AmoledBlack,
        bottomBar = {
            BottomNavNothing(
                currentTab = currentTab,
                onSelectTab = { tab ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentTab = tab
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when (currentTab) {
                ScreenTab.STREAK -> OverviewScreen(
                    database = database,
                    onNavigateDsa = { currentTab = ScreenTab.DSA },
                    onOpenSnoozeDialog = { showSnoozeDialog = true }
                )
                ScreenTab.DSA -> DsaScreen(database = database)
                ScreenTab.COURSES -> CoursesScreen(database = database)
                ScreenTab.JOBS -> JobsScreen(database = database)
                ScreenTab.CONFIG -> SettingsScreen(database = database)
            }
        }
    }

    if (showSnoozeDialog) {
        SnoozeDialog(
            database = database,
            onDismiss = { showSnoozeDialog = false },
            onSnoozeApplied = {
                showSnoozeDialog = false
            }
        )
    }
}

@Composable
fun BottomNavNothing(
    currentTab: ScreenTab,
    onSelectTab: (ScreenTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmoledBlack)
            .border(BorderStroke(1.dp, Color(0xFF1A1A1A)))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (tab in ScreenTab.entries) {
                val isSelected = currentTab == tab
                val iconColor = if (isSelected) TextPrimary else TextTertiary
                val textColor = if (isSelected) TextPrimary else TextTertiary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tab.title,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp,
                            color = textColor,
                            letterSpacing = 0.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
