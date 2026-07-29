package com.gym.app.ui.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.app.data.entity.ExerciseRecord
import com.gym.app.data.entity.WorkoutSession
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.ProgressResult
import com.gym.app.ui.components.TrendChartCard
import com.gym.app.ui.components.TrendPoint
import com.gym.app.ui.components.calculateProgress
import com.gym.app.ui.components.extractTrendPoints

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

private data class SessionDetailData(
    val session: WorkoutSession,
    val planName: String,
    val records: List<ExerciseRecord>,
    val previousRecords: List<ExerciseRecord>,
    val exerciseNames: Map<String, String>,
    val exerciseHistories: Map<String, List<TrendPoint>>,
    val progressMap: Map<String, ProgressResult>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    onBack: () -> Unit,
    isCompletionView: Boolean = false,
    onHome: (() -> Unit)? = null,
) {
    var detail by remember { mutableStateOf<SessionDetailData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sessionId) {
        val session = workoutRepository.getSessionById(sessionId)
        if (session != null) {
            val plan = workoutRepository.getPlanById(session.planId)
            val planId = session.planId
            val records = workoutRepository.getRecordsBySessionId(sessionId)
            val names = mutableMapOf<String, String>()
            records.forEach { r ->
                val ex = exerciseRepository.getExerciseById(r.exerciseId)
                if (ex != null) names[r.exerciseId] = ex.name
            }

            // 查询历史趋势数据
            val histories = mutableMapOf<String, List<TrendPoint>>()
            records.forEach { record ->
                val allRecords = workoutRepository.getRecordsForExerciseHistory(planId, record.exerciseId)
                // 按 session 关联获取日期
                val dates = allRecords.map { r ->
                    val s = workoutRepository.getSessionById(r.sessionId)
                    s?.date ?: "—"
                }
                val points = extractTrendPoints(allRecords, dates)
                histories[record.exerciseId] = points
            }

            // 查询上次训练记录用于进步对比
            val previousSession = workoutRepository.getLatestSession(planId)
            val previousRecords = if (previousSession != null && previousSession.sessionId != sessionId) {
                workoutRepository.getRecordsBySessionId(previousSession.sessionId)
            } else {
                emptyList()
            }

            val progressResults = calculateProgress(records, previousRecords, names)
            val progressMap = progressResults.associateBy { it.exerciseId }

            detail = SessionDetailData(
                session = session,
                planName = plan?.name ?: "Workout",
                records = records,
                previousRecords = previousRecords,
                exerciseNames = names,
                exerciseHistories = histories,
                progressMap = progressMap,
            )
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isCompletionView) "训练完成" else "详情",
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = WhiteColor,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                ),
            )
        },
        containerColor = LightGray,
        bottomBar = {
            if (isCompletionView && onHome != null) {
                androidx.compose.material3.Button(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                    ),
                ) {
                    Text(LocaleStrings.returnHome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        },
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (detail != null) {
            val d = detail!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 训练头部摘要
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteColor),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                d.planName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBg,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = MediumGray,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(d.session.date, fontSize = 14.sp, color = MediumGray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Timer, contentDescription = null, tint = MediumGray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(formatFullDuration(d.session.durationSeconds), fontSize = 14.sp, color = MediumGray)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${d.records.size} exercises · ${d.records.sumOf { it.actualSets }} sets",
                                fontSize = 14.sp,
                                color = MediumGray,
                            )
                        }
                    }
                }

                // 每个动作的趋势图 + 对比
                itemsIndexed(d.records) { index, record ->
                    val name = d.exerciseNames[record.exerciseId] ?: record.exerciseId
                    val trendPoints = d.exerciseHistories[record.exerciseId] ?: emptyList()
                    val progress = d.progressMap[record.exerciseId]

                    // 当前数据点
                    val currentPoint = if (trendPoints.isNotEmpty()) trendPoints.last() else null

                    TrendChartCard(
                        exerciseName = "${index + 1}. $name",
                        trendPoints = trendPoints,
                        currentPoint = currentPoint,
                        progressResult = progress,
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * 可复用的训练详情内容（无 Scaffold），供 ModalBottomSheet 等场景使用。
 */
@Composable
fun SessionDetailScreenContent(
    sessionId: Long,
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    onHome: () -> Unit,
) {
    var detail by remember { mutableStateOf<SessionDetailData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sessionId) {
        val session = workoutRepository.getSessionById(sessionId)
        if (session != null) {
            val plan = workoutRepository.getPlanById(session.planId)
            val planId = session.planId
            val records = workoutRepository.getRecordsBySessionId(sessionId)
            val names = mutableMapOf<String, String>()
            records.forEach { r ->
                val ex = exerciseRepository.getExerciseById(r.exerciseId)
                if (ex != null) names[r.exerciseId] = ex.name
            }
            val histories = mutableMapOf<String, List<TrendPoint>>()
            records.forEach { record ->
                val allRecords = workoutRepository.getRecordsForExerciseHistory(planId, record.exerciseId)
                val dates = allRecords.map { r -> workoutRepository.getSessionById(r.sessionId)?.date ?: "—" }
                histories[record.exerciseId] = extractTrendPoints(allRecords, dates)
            }
            val previousSession = workoutRepository.getLatestSession(planId)
            val previousRecords = if (previousSession != null && previousSession.sessionId != sessionId) {
                workoutRepository.getRecordsBySessionId(previousSession.sessionId)
            } else emptyList()
            val progressResults = calculateProgress(records, previousRecords, names)
            val progressMap = progressResults.associateBy { it.exerciseId }

            detail = SessionDetailData(session, plan?.name ?: "Workout", records, previousRecords, names, histories, progressMap)
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteColor)
            .padding(bottom = 16.dp),
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (detail != null) {
            val d = detail!!
            // 完成图标
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("训练完成！", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBg)
                    Text(d.planName, fontSize = 14.sp, color = AccentBlue)
                }
            }
            // 统计
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatLabel("时长", formatFullDuration(d.session.durationSeconds))
                StatLabel("动作数", "${d.records.size}")
                StatLabel("总组数", "${d.records.sumOf { it.actualSets }}")
            }
            // 动作详情
            Text("动作详情", fontWeight = FontWeight.Bold, color = DarkBg, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            d.records.forEachIndexed { idx, record ->
                val name = d.exerciseNames[record.exerciseId] ?: record.exerciseId
                val trendPoints = d.exerciseHistories[record.exerciseId] ?: emptyList()
                val progress = d.progressMap[record.exerciseId]
                val currentPoint = if (trendPoints.isNotEmpty()) trendPoints.last() else null
                TrendChartCard(
                    exerciseName = "${idx + 1}. $name",
                    trendPoints = trendPoints,
                    currentPoint = currentPoint,
                    progressResult = progress,
                )
            }
            // 返回按钮
            Button(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            ) {
                Text(LocaleStrings.returnHome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
        Text(label, fontSize = 11.sp, color = MediumGray)
    }
}

private fun formatFullDuration(totalSeconds: Int): String {
    val min = totalSeconds / 60
    return if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"
}
