package com.gym.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gym.app.data.entity.ExerciseRecord
import com.gym.app.data.entity.WorkoutSession
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository

data class CompletedExerciseInfo(
    val exerciseName: String,
    val bodyPart: String,
    val targetSets: Int,
    val targetReps: Int,
    val actualSets: Int,
    val actualReps: List<Int>,
    val weight: List<Float>,
)

data class WorkoutCompleteUiState(
    val session: WorkoutSession? = null,
    val records: List<ExerciseRecord> = emptyList(),
    val exerciseInfos: List<CompletedExerciseInfo> = emptyList(),
    val planName: String = "",
)

@Composable
fun WorkoutCompleteScreen(
    sessionId: Long,
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    onHome: () -> Unit,
) {
    var uiState by remember { mutableStateOf(WorkoutCompleteUiState()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sessionId) {
        val session = workoutRepository.getSessionById(sessionId)
        val records = workoutRepository.getRecordsBySessionId(sessionId)
        val plan = session?.let { workoutRepository.getPlanById(it.planId) }

        val infos = records.map { record ->
            val exercise = exerciseRepository.getExerciseById(record.exerciseId)
            val repsList = parseRepsArray(record.actualReps)
            val weightList = parseWeightArray(record.weight)
            CompletedExerciseInfo(
                exerciseName = exercise?.name ?: record.exerciseId,
                bodyPart = exercise?.bodyPart ?: "",
                targetSets = record.targetSets,
                targetReps = record.targetReps,
                actualSets = record.actualSets,
                actualReps = repsList,
                weight = weightList,
            )
        }

        uiState = WorkoutCompleteUiState(
            session = session,
            records = records,
            exerciseInfos = infos,
            planName = plan?.name ?: "",
        )
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中...")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 完成动画区
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color(0xFF4CAF50),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "训练完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            uiState.planName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 训练数据摘要
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatItem("时长", formatDuration(uiState.session?.durationSeconds ?: 0))
            StatItem("动作数", "${uiState.records.size}")
            StatItem(
                "总组数",
                "${uiState.exerciseInfos.sumOf { it.actualSets }}"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 动作完成详情
        Text(
            "动作详情",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(uiState.exerciseInfos) { index, info ->
                CompletedExerciseCard(exerciseInfo = info, index = index)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Filled.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("返回首页", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CompletedExerciseCard(
    exerciseInfo: CompletedExerciseInfo,
    index: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    exerciseInfo.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "目标 ${exerciseInfo.targetSets}×${exerciseInfo.targetReps}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 每组完成情况
            exerciseInfo.actualReps.forEachIndexed { setIdx, reps ->
                val weight = exerciseInfo.weight.getOrElse(setIdx) { 0f }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "第${setIdx + 1}组",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(48.dp),
                    )
                    Text(
                        "$reps 次",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (weight > 0f) {
                        Text(
                            " · ${formatWeight(weight)}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}分${seconds}秒"
}

private fun formatWeight(weight: Float): String {
    return if (weight == weight.toInt().toFloat()) "${weight.toInt()}" else "$weight"
}

private fun parseRepsArray(json: String): List<Int> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Int::class.java)
        moshi.adapter<List<Int>>(type).fromJson(json) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseWeightArray(json: String): List<Float> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Float::class.java)
        moshi.adapter<List<Float>>(type).fromJson(json) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}
