package com.gym.app.ui.workout

import androidx.compose.runtime.collectAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.scale
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Info
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Popup
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.app.ui.components.AnimationConfig
import com.gym.app.ui.components.ExerciseGifImage
import com.gym.app.ui.components.FrostedBottomSheet
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.displayName
import com.gym.app.ui.components.pressEffect
import com.gym.app.ui.exercise.ExerciseListViewModel
import com.gym.app.ui.history.SessionDetailScreenContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF3E2E23)
private val AccentOrange = Color(0xFFF2994A)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFE53935)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    workoutRepository: com.gym.app.data.repository.WorkoutRepository,
    exerciseRepository: com.gym.app.data.repository.ExerciseRepository,
    weightUnit: String = "kg",
    /** 设置里的当前体重（kg），开始训练弹窗的预填值；0 表示未设置 */
    bodyWeightKg: Float = 0f,
    /** 开始训练确认体重后回写设置 */
    onBodyWeightConfirmed: (Float) -> Unit = {},
    onBack: () -> Unit,
    onComplete: (Long) -> Unit,
    sheetShowing: androidx.compose.runtime.MutableState<Boolean> = mutableStateOf(false),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showTips by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showStartWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }

    var sheetContentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.showCompletionSheet) {
        sheetShowing.value = uiState.showCompletionSheet
        if (uiState.showCompletionSheet) {
            delay(250L)
            sheetContentVisible = true
        } else {
            sheetContentVisible = false
        }
    }

    // 离开训练页时恢复底部导航栏（覆盖所有退出路径：返回键/完成/放弃训练）
    DisposableEffect(Unit) {
        onDispose { sheetShowing.value = false }
    }

    // 退出确认弹窗
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("确认退出", fontWeight = FontWeight.Bold) },
            text = { Text("训练正在进行中，确定要退出吗？退出后本次训练记录将不会被保存。") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) {
                    Text("放弃训练", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("继续训练", color = AccentOrange)
                }
            },
        )
    }

    // 关闭完成记录页 → 返回"我的计划"（训练已结束，不再停留在训练页）
    val exitWorkout = {
        viewModel.dismissCompletionSheet()
        uiState.savedSessionId?.let { onComplete(it) }
    }

    // 开始训练：先弹体重确认（预填设置值，界面单位斤），确认后换算 kg 随本场训练保存
    val requestStartWorkout = {
        weightInput = if (bodyWeightKg > 0f) formatWeight(bodyWeightKg * 2f) else ""
        showStartWeightDialog = true
    }

    if (showStartWeightDialog) {
        AlertDialog(
            onDismissRequest = { showStartWeightDialog = false },
            title = { Text(LocaleStrings.confirmBodyWeightTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(LocaleStrings.confirmBodyWeightDesc, fontSize = 14.sp, lineHeight = 20.sp, color = MediumGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { input ->
                            // 仅允许数字和一个小数点
                            weightInput = input.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("${LocaleStrings.weightInfo} (${LocaleStrings.weightUnitJin})", fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = weightInput.toFloatOrNull() ?: 0f
                    if (w > 0f) {
                        showStartWeightDialog = false
                        // 界面输入为斤，存储与换算用 kg
                        val weightKg = w / 2f
                        onBodyWeightConfirmed(weightKg)
                        viewModel.startWorkout(weightKg)
                    }
                }) {
                    Text(LocaleStrings.confirm, color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartWeightDialog = false }) {
                    Text(LocaleStrings.cancel, color = MediumGray)
                }
            },
        )
    }

    // 系统返回键拦截：训练进行中（非通过"完成训练"按钮）→ 弹退出确认；
    // 完成面板显示时 → 关闭面板并返回"我的计划"（训练已保存，无需确认）
    BackHandler(enabled = uiState.isRunning || uiState.isPaused || sheetContentVisible) {
        if (sheetContentVisible) {
            exitWorkout()
        } else {
            showExitDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleStrings.workoutTitle, color = WhiteColor) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isRunning || uiState.isPaused) showExitDialog = true
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WhiteColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showTips = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "提示", tint = WhiteColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
            )
        },
        bottomBar = {
            if (!uiState.showCompletionSheet) WorkoutBottomBar(
                uiState = uiState,
                viewModel = viewModel,
                scope = scope,
                onRequestStart = requestStartWorkout,
            )
        },
        containerColor = LightGray,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 统计栏（吸顶：下滑时完成重量+训练时间保持可见）
            stickyHeader {
                StatsHeader(
                    completedTons = uiState.completedTons,
                    // 训练未开始时显示占位符，不显示 00:00（看起来像时间戳）
                    elapsedSeconds = if (uiState.isRunning || uiState.isPaused) uiState.elapsedSeconds else null,
                    weightUnit = weightUnit,
                )
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
                    weightUnit = weightUnit,
                    bodyWeightKg = uiState.bodyWeightKg,
                    // 训练未开始时不允许勾选完成（可提前编辑次数/重量，但不算完成）
                    setsEnabled = uiState.isRunning || uiState.isPaused,
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

    // 提示气泡（右上角对齐）
    if (showTips) {
        Popup(
            onDismissRequest = { showTips = false },
            alignment = Alignment.TopEnd,
        ) {
            Surface(
                modifier = Modifier.padding(top = 56.dp, end = 4.dp).width(260.dp),
                shape = RoundedCornerShape(12.dp),
                color = DarkBg,
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = AccentOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("训练提示", color = WhiteColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "首次训练需手动记录每组的重量和次数。下次训练同一动作时，会自动填入上次每组数据。",
                        color = WhiteColor.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTips = false }) {
                            Text("知道了", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } // Scaffold 结束

        // 完成弹窗：磨砂玻璃底部滑出
        if (sheetContentVisible && uiState.savedSessionId != null) {
            FrostedBottomSheet(
                visible = true,
                onDismiss = { exitWorkout() },
            ) {
                SessionDetailScreenContent(
                    sessionId = uiState.savedSessionId!!,
                    workoutRepository = workoutRepository,
                    exerciseRepository = exerciseRepository,
                    onHome = { exitWorkout() },
                    showHomeButton = true,
            )
            }
        } // if sheetContentVisible
    } // Box end
}

@Composable
private fun WorkoutBottomBar(
    uiState: WorkoutUiState,
    viewModel: WorkoutViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onRequestStart: () -> Unit,
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
                    onClick = onRequestStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                ) {
                    Text("开始", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // 状态二&三：运行中或暂停后 → 完成训练(70%) + 暂停/开始(30%)
            else -> {
                // 完成训练按钮（70%，长按3秒触发，松手取消）
                var isPressing by remember { mutableStateOf(false) }

                // 长按计时器：仅当 isPressing = true 时运行
                if (isPressing) {
                    LaunchedEffect(Unit) {
                        val startTime = System.currentTimeMillis()
                        while (isPressing) {
                            val elapsed = System.currentTimeMillis() - startTime
                            val progress = (elapsed / 3000f).coerceAtMost(1f)
                            viewModel.updateLongPressProgress(progress)
                            if (progress >= 1f) {
                                viewModel.triggerCompletion()
                                isPressing = false
                                viewModel.updateLongPressProgress(0f)
                                break
                            }
                            delay(50)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentRed)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressing = event.changes.any { it.pressed }
                                    if (!pressing && isPressing) {
                                        // 未长按满 3 秒松手：进度归零，文字恢复"完成训练"
                                        viewModel.updateLongPressProgress(0f)
                                    }
                                    isPressing = pressing
                                }
                            }
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
                        if (uiState.longPressProgress > 0f) "继续按住..." else "完成训练",
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
private fun StatsHeader(completedTons: Float, elapsedSeconds: Int?, weightUnit: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(DarkBg).padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(LocaleStrings.completedWeight, formatTons(completedTons), weightUnit)
        StatItem(LocaleStrings.totalWorkout, elapsedSeconds?.let { formatTimer(it) } ?: "--:--", "")
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
    weightUnit: String,
    bodyWeightKg: Float,
    /** 训练是否已开始（未开始时禁用勾选完成） */
    setsEnabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleSetCompleted: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRepsChanged: (Int, Int) -> Unit,
    onWeightChanged: (Int, Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).pressEffect(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // GIF 缩略图（自定义动作显示占位图标）
                ExerciseGifImage(
                    exercise = state.exercise,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                    placeholderIconSize = 20.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.exercise.displayName, fontWeight = FontWeight.Bold, color = DarkBg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (state.exercise.isCustom) LocaleStrings.customBadge
                        else ExerciseListViewModel.bodyPartLabel(state.exercise.bodyPart),
                        fontSize = 12.sp,
                        color = if (state.exercise.isCustom) AccentOrange else MediumGray,
                    )
                    // 助力/负重动作：说明重量输入语义
                    if (state.exercise.isAssisted) {
                        Text(LocaleStrings.weightModeAssistedDesc, fontSize = 11.sp, color = Color(0xFFFF7043))
                    } else if (state.exercise.isWeighted) {
                        Text(LocaleStrings.weightModeWeightedDesc, fontSize = 11.sp, color = Color(0xFFFF7043))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                // 展开/折叠按钮（右上角圆形）
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.size(30.dp).background(LightGray, CircleShape),
                ) {
                    Icon(
                        if (state.isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (state.isExpanded) "折叠" else "展开",
                        tint = DarkBg,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PrevBest("上次", state.previous?.weight, state.previous?.reps, state.previous != null, weightUnit)
                PrevBest("最佳", state.bestSet?.weight, state.bestSet?.reps, state.bestSet != null, weightUnit, star = state.isNewRecord)
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
                        // 助力/负重动作实时换算有效对抗重量
                        val effective = if (bodyWeightKg > 0f && (state.exercise.isAssisted || state.exercise.isWeighted)) {
                            state.exercise.effectiveWeight(bodyWeightKg, s.weight)
                        } else null
                        SetRow(i + 1, s, state.actualSets.size > 1, weightUnit, effective, setsEnabled, { onToggleSetCompleted(i) }, { onRepsChanged(i, it) }, { onWeightChanged(i, it) }, { onRemoveSet(i) })
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
private fun PrevBest(label: String, weight: Float?, reps: Int?, hasData: Boolean, unit: String, star: Boolean = false) {
    Column {
        Text(label, fontSize = 11.sp, color = MediumGray)
        if (hasData && weight != null && reps != null) {
            Text(
                "${if (star) "★ " else ""}${formatWeight(weight)} $unit × $reps",
                fontSize = 13.sp,
                fontWeight = if (star) FontWeight.Bold else FontWeight.Medium,
                color = if (star) Color(0xFFFFA000) else DarkBg,
            )
        } else {
            Text("— $unit × —", fontSize = 13.sp, color = MediumGray)
        }
    }
}

/** 弹窗编辑的目标字段 */
private enum class EditTarget { Reps, Weight }

@Composable
private fun SetRow(n: Int, s: ActualSet, canRemove: Boolean, unit: String, effectiveWeight: Float?, toggleEnabled: Boolean, onToggle: () -> Unit, onReps: (Int) -> Unit, onWeight: (Float) -> Unit, onRemove: () -> Unit) {
    val checkScale by animateFloatAsState(
        targetValue = if (s.completed) 1f else 0.3f,
        animationSpec = spring(dampingRatio = AnimationConfig.CheckMark.scaleDamping, stiffness = AnimationConfig.CheckMark.scaleStiffness),
        label = "checkScale",
    )
    val checkBgColor by animateColorAsState(
        targetValue = if (s.completed) AccentGreen else MediumGray.copy(alpha = 0.3f),
        animationSpec = tween(AnimationConfig.CheckMark.bgColorMs),
        label = "checkBg",
    )
    // 未开始训练时禁用勾选：置灰且不可点击
    val checkAlpha = if (toggleEnabled) 1f else 0.35f
    // 点击数字弹窗编辑（替代 +/− 步进器）
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    var editText by remember { mutableStateOf("") }

    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(checkBgColor.copy(alpha = checkBgColor.alpha * checkAlpha))
                .clickable(enabled = toggleEnabled, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (s.completed) {
                Icon(
                    Icons.Filled.Check, null, tint = WhiteColor.copy(alpha = checkAlpha),
                    modifier = Modifier.size(16.dp).scale(checkScale),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        // 次数：点击弹窗输入
        Text(
            "${s.reps} 次",
            fontWeight = FontWeight.Bold,
            color = DarkBg,
            modifier = Modifier
                .width(56.dp)
                .clickable {
                    editTarget = EditTarget.Reps
                    editText = if (s.reps > 0) "${s.reps}" else ""
                }
                .padding(vertical = 6.dp),
        )
        Spacer(Modifier.width(8.dp))
        // 重量输入列：点击弹窗输入；助力/负重动作下方实时显示换算后的实际对抗重量
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (s.weight > 0f) "${formatWeight(s.weight)} $unit" else "— $unit",
                fontWeight = FontWeight.Bold,
                color = if (s.weight > 0f) AccentGreen else MediumGray,
                modifier = Modifier
                    .width(64.dp)
                    .clickable {
                        editTarget = EditTarget.Weight
                        editText = if (s.weight > 0f) formatWeight(s.weight) else ""
                    }
                    .padding(vertical = 6.dp),
                textAlign = TextAlign.Center,
            )
            if (effectiveWeight != null && s.weight > 0f) {
                Text(
                    "${LocaleStrings.effectiveWeightShort} ${formatWeight(effectiveWeight)} $unit",
                    fontSize = 10.sp,
                    color = AccentOrange,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (canRemove) IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Remove, null, Modifier.size(14.dp), tint = MediumGray) }
    }

    // 编辑弹窗
    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text(if (editTarget == EditTarget.Reps) "编辑次数" else "编辑重量", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "第 $n 组 · 单位：${if (editTarget == EditTarget.Reps) "次" else unit}",
                        fontSize = 12.sp,
                        color = MediumGray,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { input ->
                            // 只保留 ASCII 数字/小数点：isDigit() 会放过全角数字（１２），
                            // toIntOrNull/toFloatOrNull 解析不了，导致输入静默丢失
                            editText = if (editTarget == EditTarget.Reps) input.filter { it in '0'..'9' }
                            else input.filter { it in '0'..'9' || it == '.' }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (editTarget == EditTarget.Reps) KeyboardType.Number else KeyboardType.Decimal,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when (editTarget) {
                        EditTarget.Reps -> editText.toIntOrNull()?.let { if (it in 0..999) onReps(it) }
                        EditTarget.Weight -> editText.toFloatOrNull()?.let { if (it in 0f..500f) onWeight(it) }
                        null -> {}
                    }
                    editTarget = null
                }) {
                    Text("确定", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) {
                    Text("取消", color = MediumGray)
                }
            },
        )
    }
}

private fun formatTons(t: Float): String {
    val v = t.toDouble()
    return if (v == v.toInt().toDouble()) "${v.toInt()}" else java.text.DecimalFormat("#.#").format(v)
}
private fun formatTimer(sec: Int): String = "${(sec / 60).toString().padStart(2, '0')}:${(sec % 60).toString().padStart(2, '0')}"
private fun formatWeight(w: Float): String {
    val v = w.toDouble()
    return if (v == v.toInt().toDouble()) "${v.toInt()}" else java.text.DecimalFormat("#.#").format(v)
}
