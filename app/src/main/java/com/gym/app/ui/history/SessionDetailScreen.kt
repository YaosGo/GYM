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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.gym.app.ui.components.pressEffect
import com.gym.app.ui.components.ProgressResult
import com.gym.app.ui.components.ExerciseTrendCard
import com.gym.app.ui.components.displayName
import com.gym.app.ui.components.TrendPoint
import com.gym.app.ui.components.calculateProgress
import com.gym.app.ui.components.extractTrendPoints
import com.gym.app.ui.components.parseEffectiveWeights

private val DarkBg = Color(0xFF3E2E23)
private val AccentOrange = Color(0xFFF2994A)
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
    val exerciseWeightModes: Map<String, Int>,
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
            val weightModes = mutableMapOf<String, Int>()
            records.forEach { r ->
                val ex = exerciseRepository.getExerciseById(r.exerciseId)
                if (ex != null) {
                    names[r.exerciseId] = ex.displayName
                    weightModes[r.exerciseId] = ex.weightMode
                }
            }

            // 查询历史趋势数据（仅包含当前 session 及之前的训练）
            val histories = mutableMapOf<String, List<TrendPoint>>()
            records.forEach { record ->
                val allRecords = workoutRepository.getRecordsForExerciseHistory(planId, record.exerciseId)
                // 过滤：仅保留当前 session 及之前创建的记录（按 sessionId 自增序）
                val filtered = allRecords.filter { it.sessionId <= sessionId }
                // 确保当前记录在列表中
                val mergedRecords = if (filtered.none { it.sessionId == sessionId }) filtered + record else filtered
                val dates = mergedRecords.map { r ->
                    if (r.sessionId == sessionId) session.date
                    else workoutRepository.getSessionById(r.sessionId)?.date ?: "—"
                }
                histories[record.exerciseId] = extractTrendPoints(mergedRecords, dates)
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
                exerciseWeightModes = weightModes,
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
                CircularProgressIndicator(color = AccentOrange)
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
                            if (d.session.bodyWeightKg > 0f) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    // 体重快照界面单位为斤（存储为 kg）
                                    "${LocaleStrings.weightInfo} ${formatWeightKg(d.session.bodyWeightKg * 2f)} ${LocaleStrings.weightUnitJin}",
                                    fontSize = 13.sp,
                                    color = AccentOrange,
                                )
                            }
                        }
                    }
                }

                // 每个动作的趋势图 + 对比
                itemsIndexed(d.records) { index, record ->
                    val name = d.exerciseNames[record.exerciseId] ?: record.exerciseId
                    val trendPoints = d.exerciseHistories[record.exerciseId] ?: emptyList()
                    val progress = d.progressMap[record.exerciseId]
                    val lastPt = trendPoints.lastOrNull()

                    ExerciseTrendCard(
                        exerciseName = "${index + 1}. $name",
                        trendPoints = trendPoints,
                        currentVolume = lastPt?.totalVolume ?: 0f,
                        currentSets = lastPt?.sets ?: record.actualSets,
                        currentReps = lastPt?.maxReps ?: 0,
                        weightMode = d.exerciseWeightModes[record.exerciseId] ?: 0,
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
    showHomeButton: Boolean = true,
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
            val weightModes = mutableMapOf<String, Int>()
            records.forEach { r ->
                val ex = exerciseRepository.getExerciseById(r.exerciseId)
                if (ex != null) {
                    names[r.exerciseId] = ex.displayName
                    weightModes[r.exerciseId] = ex.weightMode
                }
            }
            // 查询历史趋势数据（仅包含当前 session 及之前的训练）
            val histories = mutableMapOf<String, List<TrendPoint>>()
            records.forEach { record ->
                val allRecords = workoutRepository.getRecordsForExerciseHistory(planId, record.exerciseId)
                // 过滤：仅保留当前 session 及之前创建的记录（按 sessionId 自增序）
                val filtered = allRecords.filter { it.sessionId <= sessionId }
                // 确保当前记录在列表中
                val mergedRecords = if (filtered.none { it.sessionId == sessionId }) filtered + record else filtered
                val dates = mergedRecords.map { r ->
                    if (r.sessionId == sessionId) session.date
                    else workoutRepository.getSessionById(r.sessionId)?.date ?: "—"
                }
                histories[record.exerciseId] = extractTrendPoints(mergedRecords, dates)
            }
            val previousSession = workoutRepository.getLatestSession(planId)
            val previousRecords = if (previousSession != null && previousSession.sessionId != sessionId) {
                workoutRepository.getRecordsBySessionId(previousSession.sessionId)
            } else emptyList()
            val progressResults = calculateProgress(records, previousRecords, names)
            val progressMap = progressResults.associateBy { it.exerciseId }

            detail = SessionDetailData(session, plan?.name ?: "Workout", records, previousRecords, names, weightModes, histories, progressMap)
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteColor)
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange)
            }
        } else if (detail != null) {
            val d = detail!!
            // 完成图标
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("训练完成！", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBg)
                    Text(d.planName, fontSize = 14.sp, color = AccentOrange)
                }
            }
            // 统计
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatLabel("时长", formatFullDuration(d.session.durationSeconds))
                StatLabel("动作数", "${d.records.size}")
                StatLabel("总组数", "${d.records.sumOf { it.actualSets }}")
                StatLabel("总重量", formatTotalWeight(d.records))
                if (d.session.bodyWeightKg > 0f) {
                    // 体重快照界面单位为斤（存储为 kg）
                    StatLabel(LocaleStrings.weightInfo, "${formatWeightKg(d.session.bodyWeightKg * 2f)} ${LocaleStrings.weightUnitJin}")
                }
            }
            // 动作详情
            Text("动作详情", fontWeight = FontWeight.Bold, color = DarkBg, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            d.records.forEachIndexed { idx, record ->
                val name = d.exerciseNames[record.exerciseId] ?: record.exerciseId
                val trendPoints = d.exerciseHistories[record.exerciseId] ?: emptyList()
                val lastPt = trendPoints.lastOrNull()
                ExerciseTrendCard(
                    exerciseName = "${idx + 1}. $name",
                    trendPoints = trendPoints,
                    currentVolume = lastPt?.totalVolume ?: 0f,
                    currentSets = lastPt?.sets ?: record.actualSets,
                    currentReps = lastPt?.maxReps ?: 0,
                )
            }
            // 返回按钮（仅非 Sheet 模式显示）
            if (showHomeButton) {
                Button(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                ) {
                    Text(LocaleStrings.returnHome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("加载失败", color = MediumGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
        Text(label, fontSize = 11.sp, color = MediumGray)
    }
}

private fun formatFullDuration(totalSeconds: Int): String {
    val min = totalSeconds / 60
    return if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"
}

private fun formatTotalWeight(records: List<ExerciseRecord>): String {
    val total = records.sumOf { record ->
        // 有效对抗重量：助力=体重−助力值，负重=体重+附加重（旧数据回退 weight）
        val weights = parseEffectiveWeights(record)
        val reps = parseIntArray(record.actualReps)
        if (weights.size == reps.size) weights.zip(reps).sumOf { (w, r) -> (w.toDouble() * r).toDouble() }
        else 0.0
    }.toFloat()
    return formatTons(total)
}

private fun formatWeightKg(w: Float): String =
    if (w == w.toInt().toFloat()) w.toInt().toString()
    else java.text.DecimalFormat("#.#").format(w.toDouble())

private fun formatTons(t: Float): String {
    val v = t.toDouble()
    return if (v < 1000) (if (v == v.toInt().toDouble()) "${v.toInt()}" else java.text.DecimalFormat("#.#").format(v))
    else "${java.text.DecimalFormat("#.#").format(v / 1000)}k"
}

private fun parseIntArray(json: String): List<Int> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
        moshi.adapter<List<Int>>(type).fromJson(json) ?: emptyList()
    } catch (_: Exception) { emptyList() }
}

private fun parseFloatArray(json: String): List<Float> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Float::class.javaObjectType)
        moshi.adapter<List<Float>>(type).fromJson(json) ?: emptyList()
    } catch (_: Exception) { emptyList() }
}
