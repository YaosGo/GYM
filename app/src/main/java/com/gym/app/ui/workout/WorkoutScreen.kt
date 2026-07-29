package com.gym.app.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.exercise.ExerciseListViewModel
import com.gym.app.ui.history.SessionDetailScreenContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFE53935)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    workoutRepository: com.gym.app.data.repository.WorkoutRepository,
    exerciseRepository: com.gym.app.data.repository.ExerciseRepository,
    onBack: () -> Unit,
    onComplete: (Long) -> Unit,
) {
    val uiState by viewModel.uiState
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 完成弹窗：从底部滑出
    if (uiState.showCompletionSheet && uiState.savedSessionId != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissCompletionSheet() },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
        ) {
            SessionDetailScreenContent(
                sessionId = uiState.savedSessionId!!,
                workoutRepository = workoutRepository,
                exerciseRepository = exerciseRepository,
                onHome = {
                    viewModel.dismissCompletionSheet()
                    onComplete(uiState.savedSessionId!!)
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleStrings.workoutTitle, color = WhiteColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WhiteColor)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = WhiteColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
            )
        },
        bottomBar = {
            WorkoutBottomBar(
                uiState = uiState,
                viewModel = viewModel,
                scope = scope,
            )
        },
        containerColor = LightGray,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 统计栏
            item {
                StatsHeader(tonsLifted = uiState.tonsLifted, elapsedSeconds = uiState.elapsedSeconds)
            }

            // 训练计划标签
            item {
                Text(
                    LocaleStrings.trainingPlan,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            itemsIndexed(uiState.exercises) { index, state ->
                ExerciseWorkoutCard(
                    state = state,
                    onToggleExpanded = { viewModel.toggleExpanded(index) },
                    onToggleSetCompleted = { setIndex -> viewModel.toggleSetCompleted(index, setIndex) },
                    onAddSet = { viewModel.addSet(index) },
                    onRemoveSet = { setIndex -> viewModel.removeSet(index, setIndex) },
                    onRepsChanged = { setIndex, reps -> viewModel.updateSetReps(index, setIndex, reps) },
                    onWeightChanged = { setIndex, weight -> viewModel.updateSetWeight(index, setIndex, weight) },
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WorkoutBottomBar(
    uiState: WorkoutUiState,
    viewModel: WorkoutViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            // 状态一：未开始 → 显示"开始"
            !uiState.isRunning && !uiState.isPaused -> {
                Button(
                    onClick = { viewModel.startWorkout() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                ) {
                    Text("开始", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // 状态二&三：运行中或暂停后 → 完成训练(70%) + 暂停/开始(30%)
            else -> {
                // 完成训练按钮（70%，长按3秒触发）
                var longPressActive by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentRed)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    longPressActive = true
                                    val start = System.currentTimeMillis()
                                    // 3 秒长按进度
                                    while (longPressActive) {
                                        val elapsed = System.currentTimeMillis() - start
                                        val progress = (elapsed / 3000f).coerceAtMost(1f)
                                        viewModel.updateLongPressProgress(progress)
                                        if (progress >= 1f) {
                                            viewModel.triggerCompletion()
                                            longPressActive = false
                                            break
                                        }
                                        delay(50)
                                    }
                                    tryAwaitRelease()
                                    longPressActive = false
                                    viewModel.updateLongPressProgress(0f)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // 长按进度环
                    if (uiState.longPressProgress > 0f) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 4.dp.toPx()
                            val arcSize = size.minDimension - strokeWidth
                            drawArc(
                                color = Color.White.copy(alpha = 0.6f),
                                startAngle = -90f,
                                sweepAngle = 360f * uiState.longPressProgress,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    (size.width - arcSize) / 2f,
                                    (size.height - arcSize) / 2f
                                ),
                                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                            )
                        }
                    }
                    Text(
                        "完成训练",
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }

                // 暂停/继续按钮（30%）
                OutlinedButton(
                    onClick = {
                        if (uiState.isRunning) viewModel.pauseWorkout() else viewModel.resumeWorkout()
                    },
                    modifier = Modifier
                        .weight(0.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.isRunning) WhiteColor else AccentGreen.copy(alpha = 0.1f),
                        contentColor = if (uiState.isRunning) DarkBg else AccentGreen,
                    ),
                    border = if (uiState.isRunning) ButtonDefaults.outlinedButtonBorder
                    else androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                ) {
                    Text(
                        if (uiState.isRunning) "暂停" else "开始",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (uiState.isRunning) DarkBg else AccentGreen,
                    )
                }
            }
        }
    }
}

// === 复用组件 ===

@Composable
private fun StatsHeader(tonsLifted: Float, elapsedSeconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().background(DarkBg).padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(LocaleStrings.tonsLifted, formatTons(tonsLifted), "Lbs")
        StatItem(LocaleStrings.totalWorkout, formatTimer(elapsedSeconds), "")
    }
}

@Composable
private fun StatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = WhiteColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, color = MediumGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun ExerciseWorkoutCard(
    state: ExerciseWorkoutState,
    onToggleExpanded: () -> Unit,
    onToggleSetCompleted: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRepsChanged: (Int, Int) -> Unit,
    onWeightChanged: (Int, Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.exercise.name, fontWeight = FontWeight.Bold, color = DarkBg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(ExerciseListViewModel.bodyPartLabel(state.exercise.bodyPart), fontSize = 12.sp, color = MediumGray)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PrevBest("上次", state.previous?.weight, state.previous?.reps, state.previous != null)
                PrevBest("最佳", state.bestSet?.weight, state.bestSet?.reps, state.bestSet != null)
            }
            if (state.musclesWorked.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FitnessCenter, null, Modifier.size(12.dp), tint = MediumGray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("训练肌群: ${state.musclesWorked.take(3).joinToString(", ")}", fontSize = 11.sp, color = MediumGray)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpanded).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${state.actualSets.size} 组", fontWeight = FontWeight.Bold, color = DarkBg, fontSize = 14.sp)
                val done = state.actualSets.count { it.completed }
                Text("$done/${state.actualSets.size}", fontSize = 12.sp, color = if (done == state.actualSets.size) AccentGreen else MediumGray, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.weight(1f))
                Icon(if (state.isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = MediumGray, modifier = Modifier.size(20.dp))
            }
            AnimatedVisibility(visible = state.isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    state.actualSets.forEachIndexed { i, s ->
                        SetRow(i + 1, s, state.actualSets.size > 1, { onToggleSetCompleted(i) }, { onRepsChanged(i, it) }, { onWeightChanged(i, it) }, { onRemoveSet(i) })
                    }
                    Row(Modifier.fillMaxWidth().clickable(onClick = onAddSet).padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Add, null, Modifier.size(16.dp), tint = MediumGray)
                        Text("添加一组", fontSize = 12.sp, color = MediumGray, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrevBest(label: String, weight: Float?, reps: Int?, hasData: Boolean) {
    Column {
        Text(label, fontSize = 11.sp, color = MediumGray)
        if (hasData && weight != null && reps != null) {
            Text("${formatWeight(weight)} lb × $reps", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkBg)
        } else {
            Text("— lb × —", fontSize = 13.sp, color = MediumGray)
        }
    }
}

@Composable
private fun SetRow(n: Int, s: ActualSet, canRemove: Boolean, onToggle: () -> Unit, onReps: (Int) -> Unit, onWeight: (Float) -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (s.completed) AccentGreen else MediumGray.copy(alpha = 0.3f)).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
            if (s.completed) Icon(Icons.Filled.Check, null, tint = WhiteColor, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("−", Modifier.clickable { if (s.reps > 0) onReps(s.reps - 1) }.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold, color = if (s.reps > 0) DarkBg else MediumGray.copy(alpha = 0.3f))
        Text("${s.reps} 次", fontWeight = FontWeight.Bold, color = DarkBg, modifier = Modifier.width(50.dp))
        Text("+", Modifier.clickable { if (s.reps < 99) onReps(s.reps + 1) }.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold, color = DarkBg)
        Spacer(Modifier.width(8.dp))
        Text("−", Modifier.clickable { if (s.weight > 0f) onWeight((s.weight - 2.5f).coerceAtLeast(0f)) }.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold, color = if (s.weight > 0f) DarkBg else MediumGray.copy(alpha = 0.3f))
        Text(if (s.weight > 0f) "${formatWeight(s.weight)} lb" else "— lb", fontWeight = FontWeight.Bold, color = if (s.weight > 0f) AccentGreen else MediumGray, modifier = Modifier.width(48.dp))
        Text("+", Modifier.clickable { if (s.weight < 500f) onWeight((s.weight + 2.5f).coerceAtMost(500f)) }.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold, color = DarkBg)
        if (canRemove) IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Remove, null, Modifier.size(14.dp), tint = MediumGray) }
    }
}

private fun formatTons(t: Float): String = if (t < 1000) (if (t == t.toInt().toFloat()) "${t.toInt()}" else "%.1f".format(t)) else "%.1fk".format(t / 1000f)
private fun formatTimer(sec: Int): String = "${(sec / 60).toString().padStart(2, '0')}:${(sec % 60).toString().padStart(2, '0')}"
private fun formatWeight(w: Float): String = if (w == w.toInt().toFloat()) "${w.toInt()}" else "%.1f".format(w)
