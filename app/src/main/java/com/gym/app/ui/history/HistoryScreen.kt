package com.gym.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.app.data.entity.WorkoutSession
import com.gym.app.ui.components.LocaleStrings

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White
private val OrangeColor = Color(0xFFFF7043)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: (() -> Unit)? = null,
    onSessionClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleStrings.historyTitle, color = WhiteColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = LocaleStrings.back,
                                tint = WhiteColor,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* more */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = WhiteColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                ),
            )
        },
        containerColor = LightGray,
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 统计头
                item {
                    StatsHeader(
                        totalWorkouts = uiState.totalWorkouts,
                        monthlyWorkouts = uiState.monthlyWorkouts,
                    )
                }

                // 日历
                item {
                    WorkoutCalendar(
                        year = uiState.currentYear,
                        month = uiState.currentMonth,
                        workoutDates = uiState.workoutDates,
                        selectedDate = uiState.selectedDate,
                        onDateSelected = viewModel::selectDate,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                    )
                }

                // 选中日期的训练列表
                if (uiState.selectedDate != null) {
                    item {
                        Text(
                            java.text.SimpleDateFormat(
                                "MMM dd, yyyy",
                                java.util.Locale.getDefault()
                            ).format(
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    java.util.Locale.getDefault()
                                ).parse(uiState.selectedDate)!!
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBg,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (uiState.selectedDateSessions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No workouts on this day",
                                    color = MediumGray,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    } else {
                        items(uiState.selectedDateSessions, key = { it.sessionId }) { session ->
                            SessionCard(
                                session = session,
                                onClick = { onSessionClick(session.sessionId) },
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Select a date to view workouts",
                            color = MediumGray,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(
    totalWorkouts: Int,
    monthlyWorkouts: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.FitnessCenter,
            iconColor = AccentBlue,
            value = "$totalWorkouts",
            label = "Total workouts",
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.CalendarMonth,
            iconColor = AccentGreen,
            value = "$monthlyWorkouts",
            label = "This month",
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    value: String,
    label: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBg,
            )
            Text(
                label,
                fontSize = 12.sp,
                color = MediumGray,
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: WorkoutSession,
    onClick: () -> Unit,
) {
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val displayFormat = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())

    val dateStr = try {
        val parsed = dateFormat.parse(session.date)
        if (parsed != null) displayFormat.format(parsed) else session.date
    } catch (_: Exception) {
        session.date
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧装饰条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentBlue),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateStr,
                    fontSize = 12.sp,
                    color = MediumGray,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    session.note.ifBlank { "Workout" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MediumGray,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        formatDuration(session.durationSeconds),
                        fontSize = 12.sp,
                        color = MediumGray,
                    )
                }
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "详情",
                tint = MediumGray,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val min = totalSeconds / 60
    return if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"
}
