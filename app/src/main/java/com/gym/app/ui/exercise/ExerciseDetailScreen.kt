package com.gym.app.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gym.app.data.entity.Exercise
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.displayName
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val AccentGreen = Color(0xFF4CAF50)
private val WhiteColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    repository: ExerciseRepository,
    onBack: () -> Unit,
) {
    var exercise by remember { mutableStateOf<Exercise?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(exerciseId) {
        exercise = repository.getExerciseById(exerciseId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleStrings.detailTitle, color = WhiteColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = WhiteColor,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* more options */ }) {
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (exercise != null) {
            ExerciseDetailContent(
                exercise = exercise!!,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailContent(
    exercise: Exercise,
    modifier: Modifier = Modifier,
) {
    val steps = remember(exercise) { parseInstructionSteps(exercise.instructionStepsZh) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 大 GIF 展示区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = "file:///android_asset/${exercise.gifPath}",
                contentDescription = exercise.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )

            // 底部渐变 + 信息覆盖层
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.5f)
                    )
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        exercise.displayName,
                        color = WhiteColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        exercise.name,
                        color = WhiteColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        ExerciseListViewModel.bodyPartLabel(exercise.bodyPart),
                        color = WhiteColor.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                }
            }
        }

        // 白色内容区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WhiteColor)
                .padding(16.dp),
        ) {
            // 标签行
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagChip(ExerciseListViewModel.bodyPartLabel(exercise.bodyPart), AccentBlue)
                TagChip(ExerciseListViewModel.targetLabel(exercise.target), Color(0xFFFF7043))
                TagChip(ExerciseListViewModel.equipmentLabel(exercise.equipment), Color(0xFF26A69A))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // INSTRUCTIONS 标题
            Text(
                LocaleStrings.instructions,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBg,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 分步指导
            if (steps.isNotEmpty()) {
                steps.forEachIndexed { index, step ->
                    StepItem(
                        stepNumber = index + 1,
                        text = step,
                        isLast = index == steps.lastIndex,
                    )
                }
            } else {
                // 兜底：展示完整中文指导
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightGray)
                        .padding(16.dp),
                ) {
                    Text(
                        exercise.instructionsZh,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DarkBg,
                        lineHeight = 24.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TagChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    text: String,
    isLast: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 步骤编号圆圈
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(DarkBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$stepNumber",
                color = WhiteColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 步骤文本
        Text(
            text = text.trim(),
            fontSize = 14.sp,
            color = DarkBg,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 解析 instruction_steps JSON 数组为步骤列表。
 */
private fun parseInstructionSteps(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        moshi.adapter<List<String>>(type).fromJson(json) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}
