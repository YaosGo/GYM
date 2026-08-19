package com.gym.app.ui.history

import androidx.compose.runtime.collectAsState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import com.gym.app.data.entity.WorkoutSession
import com.gym.app.ui.components.AnimationConfig
import com.gym.app.ui.components.FrostedBottomSheet
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.pressEffect
import java.text.SimpleDateFormat
import java.util.Locale

private val DarkBg = Color(0xFF3E2E23)
private val AccentOrange = Color(0xFFF2994A)
private val AccentGreen = Color(0xFF4CAF50)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: (() -> Unit)? = null,
    onSessionClick: (Long) -> Unit,
    workoutRepository: com.gym.app.data.repository.WorkoutRepository? = null,
    exerciseRepository: com.gym.app.data.repository.ExerciseRepository? = null,
    sheetShowing: androidx.compose.runtime.MutableState<Boolean> = mutableStateOf(false),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedSessionId = remember { mutableStateOf<Long?>(null) }
    val sheetContentVisible = remember { mutableStateOf(false) }
    val sheetTriggered = selectedSessionId.value != null &&
            workoutRepository != null && exerciseRepository != null

    // 通知导航栏 + 控制面板显隐
    LaunchedEffect(sheetTriggered) {
        sheetShowing.value = sheetTriggered
        if (sheetTriggered) {
            delay(250L)
            sheetContentVisible.value = true
        } else {
            sheetContentVisible.value = false
        }
    }

    // 返回键关闭面板
    BackHandler(enabled = sheetContentVisible.value) {
        selectedSessionId.value = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区 — 固定不动，作为面板的下层
        Scaffold(
                topBar = {
                TopAppBar(
                    title = { Text(LocaleStrings.historyTitle, color = WhiteColor, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = WhiteColor)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* more */ }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = WhiteColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
                )
            },
            containerColor = LightGray,
        ) { padding ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
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
                            workoutCounts = uiState.workoutCounts,
                            selectedDate = uiState.selectedDate,
                            onDateSelected = viewModel::selectDate,
                            onPreviousMonth = viewModel::previousMonth,
                            onNextMonth = viewModel::nextMonth,
                        )
                    }

                    // 选中日期标题
                    if (uiState.selectedDate != null) {
                        item {
                            val displayDate: String = try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val parsed = sdf.parse(uiState.selectedDate!!)
                                java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.getDefault()).format(parsed!!)
                            } catch (_: Exception) { uiState.selectedDate ?: "" }
                            Text(displayDate, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBg, modifier = Modifier.padding(top = 8.dp))
                        }

                        if (uiState.selectedDateSessions.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("当天没有训练记录", color = MediumGray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(uiState.selectedDateSessions, key = { it.sessionId }) { session ->
                                SessionCard(
                                    session = session,
                                    planName = uiState.planNames[session.planId] ?: "训练",
                                    onClick = { selectedSessionId.value = session.sessionId },
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                "选择日期查看训练记录",
                                color = MediumGray,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            } // Scaffold 结束

        // 磨砂玻璃底部滑出
        if (sheetContentVisible.value && sheetTriggered) {
            FrostedBottomSheet(
                visible = true,
                onDismiss = {
                    selectedSessionId.value = null
                },
            ) {
                SessionDetailScreenContent(
                    sessionId = selectedSessionId.value!!,
                    workoutRepository = workoutRepository,
                    exerciseRepository = exerciseRepository,
                    onHome = { selectedSessionId.value = null },
                    showHomeButton = false,
                )
            }
        }
    }
}

@Composable
private fun StatsHeader(totalWorkouts: Int, monthlyWorkouts: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Filled.FitnessCenter, iconColor = AccentOrange, value = "$totalWorkouts", label = "总训练次数")
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Filled.CalendarMonth, iconColor = AccentGreen, value = "$monthlyWorkouts", label = "本月")
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
    val numericValue = value.toIntOrNull() ?: 0
    val animatedValue by animateIntAsState(
        targetValue = numericValue,
        animationSpec = tween(durationMillis = AnimationConfig.Stat.countUpMs),
        label = "statCount",
    )
    Card(
        modifier = modifier.pressEffect(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("${if (numericValue > 0) animatedValue else value}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DarkBg)
            Text(label, fontSize = 12.sp, color = MediumGray)
        }
    }
}

@Composable
private fun SessionCard(session: WorkoutSession, planName: String, onClick: () -> Unit) {
    val dateStr = try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val parsed = sdf.parse(session.date)
        if (parsed != null) java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault()).format(parsed) else session.date
    } catch (_: Exception) { session.date }
    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(session.completedAt))

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(AccentOrange))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateStr, fontSize = 12.sp, color = MediumGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(timeStr, fontSize = 12.sp, color = MediumGray)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    planName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, null, Modifier.size(14.dp), tint = MediumGray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(formatDuration(session.durationSeconds), fontSize = 12.sp, color = MediumGray)
                }
            }
            Icon(Icons.Filled.ChevronRight, "详情", tint = MediumGray, modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds == 0) return "0s"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        if (seconds > 0 || (hours == 0 && minutes == 0)) append("${seconds}s")
    }.trim()
}
