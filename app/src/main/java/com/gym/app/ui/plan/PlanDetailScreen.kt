package com.gym.app.ui.plan

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gym.app.data.entity.Exercise
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.displayName
import com.gym.app.ui.components.pressEffect
import com.gym.app.ui.components.TrendPoint
import com.gym.app.ui.exercise.ExerciseListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White
private val OrangeColor = Color(0xFFFF7043)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    viewModel: PlanDetailViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    // 查看/编辑模式
    var isEditing by remember { mutableStateOf(false) }

    // 保存成功弹窗
    var showSavedSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSavedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSavedSheet = false; onSaved() },
            sheetState = sheetState,
            containerColor = WhiteColor,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "计划已保存！",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "动作列表和顺序已更新",
                    fontSize = 14.sp,
                    color = MediumGray,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showSavedSheet = false; onSaved() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                ) {
                    Text("返回计划列表", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "编辑计划" else "查看计划",
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = WhiteColor)
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = {
                            viewModel.deletePlan { onBack() }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除计划", tint = WhiteColor)
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = WhiteColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
            )
        },
        containerColor = LightGray,
        bottomBar = {
            if (isEditing && !showSavedSheet) {
                Button(
                    onClick = {
                        viewModel.savePlan { showSavedSheet = true }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    enabled = !uiState.isSaving && uiState.exercises.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = WhiteColor)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (uiState.isSaving) "保存中..." else "✓ 保存",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 计划名称 + 元信息
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    uiState.planName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                )
                if (!isEditing) {
                    val dateStr = if (uiState.createdDate > 0) {
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                        sdf.format(Date(uiState.createdDate))
                    } else ""
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (dateStr.isNotEmpty()) {
                            Text("创建于 $dateStr", fontSize = 13.sp, color = MediumGray)
                        }
                        Text("${uiState.totalSessions} 次训练", fontSize = 13.sp, color = MediumGray)
                        if (uiState.totalDurationSec > 0) {
                            Text("共 ${formatFullDuration(uiState.totalDurationSec)}", fontSize = 13.sp, color = MediumGray)
                        }
                    }
                }
            }

            // 动作列表 + 趋势图
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(uiState.exercises, key = { _, item -> item.planExercise.id }) { index, item ->
                    ExerciseItemCard(
                        number = index + 1,
                        item = item,
                        totalCount = uiState.exercises.size,
                        isEditing = isEditing,
                        onRemove = { viewModel.removeExercise(index) },
                        onSetsChanged = { viewModel.updateSets(index, it) },
                        onRepsChanged = { viewModel.updateReps(index, it) },
                        onMoveUp = {
                            if (index > 0) viewModel.moveItem(index, index - 1)
                        },
                        onMoveDown = {
                            if (index < uiState.exercises.lastIndex) viewModel.moveItem(index, index + 1)
                        },
                    )
                }

                // 编辑模式下显示搜索区
                if (isEditing) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("搜索并添加动作...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MediumGray) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Filled.Clear, "清除", tint = MediumGray)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = WhiteColor,
                                unfocusedContainerColor = WhiteColor,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 搜索结果
                    if (uiState.isSearching) {
                        uiState.searchResults.take(8).forEach { exercise ->
                            item {
                                SearchAddItem(
                                    exercise = exercise,
                                    isAdded = uiState.exercises.any { it.planExercise.exerciseId == exercise.id },
                                    onAdd = { viewModel.addExercise(exercise) },
                                )
                            }
                        }
                    }

                    if (!uiState.isSearching && uiState.exercises.isEmpty()) {
                        item {
                            Text(
                                "暂无动作，上方搜索添加",
                                color = MediumGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                }

                // 查看模式：训练趋势图
                if (!isEditing) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.trendPoints.size >= 2) {
                            PlanTrendSection(trendPoints = uiState.trendPoints)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "还未完成过当前计划",
                                    color = MediumGray,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 动作卡片：查看模式只读，编辑模式可交互
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ExerciseItemCard(
    number: Int,
    item: PlanExerciseItem,
    totalCount: Int,
    isEditing: Boolean,
    onRemove: () -> Unit,
    onSetsChanged: (Int) -> Unit,
    onRepsChanged: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().pressEffect(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 序号圆圈
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(DarkBg),
                contentAlignment = Alignment.Center,
            ) {
                Text("$number", color = WhiteColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // GIF 缩略图
            if (item.exercise != null) {
                AsyncImage(
                    model = "file:///android_asset/${item.exercise.gifPath}",
                    contentDescription = item.name,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            // 名称 + 组数次
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditing) {
                        Stepper(value = item.planExercise.targetSets, onChanged = onSetsChanged, label = "组")
                        Text(" × ", fontSize = 13.sp, color = MediumGray)
                        Stepper(value = item.planExercise.targetReps, onChanged = onRepsChanged, label = "次")
                    } else {
                        Text(
                            "${item.planExercise.targetSets} 组 × ${item.planExercise.targetReps} 次",
                            fontSize = 13.sp,
                            color = MediumGray,
                        )
                    }
                }
            }

            if (isEditing) {
                // 编辑模式：上移/下移/删除
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(28.dp),
                        enabled = number > 1,
                    ) {
                        Icon(Icons.Filled.ExpandLess, "上移", Modifier.size(18.dp), tint = if (number > 1) AccentBlue else MediumGray.copy(alpha = 0.3f))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(28.dp),
                        enabled = number < totalCount,
                    ) {
                        Icon(Icons.Filled.ExpandMore, "下移", Modifier.size(18.dp), tint = if (number < totalCount) AccentBlue else MediumGray.copy(alpha = 0.3f))
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.RemoveCircleOutline, "删除", tint = OrangeColor, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 步进器（组数/次数 ±）
// ═══════════════════════════════════════════════════════════════

@Composable
private fun Stepper(value: Int, onChanged: (Int) -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "−",
            modifier = Modifier.clickable { if (value > 1) onChanged(value - 1) }.padding(horizontal = 5.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (value > 1) AccentBlue else MediumGray.copy(alpha = 0.3f),
        )
        Text("$value", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBg)
        Text(
            "+",
            modifier = Modifier.clickable { if (value < 99) onChanged(value + 1) }.padding(horizontal = 5.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (value < 99) AccentBlue else MediumGray.copy(alpha = 0.3f),
        )
        Text(" $label", fontSize = 12.sp, color = MediumGray)
    }
}

// ═══════════════════════════════════════════════════════════════
// 搜索结果条目
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SearchAddItem(exercise: Exercise, isAdded: Boolean, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable(enabled = !isAdded, onClick = onAdd),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isAdded) LightGray else WhiteColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = "file:///android_asset/${exercise.gifPath}",
                contentDescription = exercise.displayName,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isAdded) MediumGray else DarkBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${ExerciseListViewModel.bodyPartLabel(exercise.bodyPart)} · ${ExerciseListViewModel.equipmentLabel(exercise.equipment)}",
                    fontSize = 11.sp,
                    color = MediumGray,
                )
            }
            if (isAdded) {
                Text("已添加", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Filled.Add, "添加", tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 计划级别趋势图（汇总所有动作的总训练量）
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PlanTrendSection(trendPoints: List<TrendPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "训练成果趋势",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBg,
            )
            Spacer(modifier = Modifier.height(6.dp))

            // 百分比变化标签行
            if (trendPoints.size >= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (i in 1 until trendPoints.size) {
                        val prev = trendPoints[i - 1]
                        val curr = trendPoints[i]
                        val change = if (prev.totalVolume > 0) {
                            ((curr.totalVolume - prev.totalVolume) / prev.totalVolume * 100f).toInt()
                        } else 0
                        val (symbol, color) = when {
                            change > 0 -> "↑" to AccentGreen
                            change < 0 -> "↓" to OrangeColor
                            else -> "→" to MediumGray
                        }
                        val shortDate = trendPoints[i].date.takeLast(5) // MM-DD
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "$shortDate $symbol${kotlin.math.abs(change)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = color,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 折线图
            PlanTotalVolumeChart(
                points = trendPoints,
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AccentBlue)
                        .size(8.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("总训练量 (kg)", fontSize = 11.sp, color = MediumGray)
            }
        }
    }
}

@Composable
private fun PlanTotalVolumeChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
) {
    val lineColor = AccentBlue
    val gridColor = Color(0xFFE0E0E0)

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val values = points.map { it.totalVolume.toFloat() }
        val minVal = values.min()
        val maxVal = values.max()
        val range = if (maxVal == minVal) 1f else maxVal - minVal

        val paddingLeft = 44f
        val paddingRight = 16f
        val paddingTop = 12f
        val paddingBottom = 28f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        // 数据点
        val pointOffsets = values.mapIndexed { index, value ->
            val x = paddingLeft + (index.toFloat() / (values.size - 1)) * chartWidth
            val y = paddingTop + (1f - (value - minVal) / range) * chartHeight
            Offset(x, y)
        }

        // 网格 + Y轴标签
        val gridLines = 3
        for (i in 0..gridLines) {
            val fraction = i.toFloat() / gridLines
            val y = paddingTop + (1f - fraction) * chartHeight
            val labelValue = minVal + fraction * range
            drawLine(gridColor, Offset(paddingLeft, y), Offset(size.width - paddingRight, y), 1f)
            drawContext.canvas.nativeCanvas.drawText(
                labelValue.toInt().toString(), 4f, y + 4f,
                android.graphics.Paint().apply { color = 0xFF9E9E9E.toInt(); textSize = 22f; isAntiAlias = true },
            )
        }

        // 面积填充（半透明）
        if (pointOffsets.size >= 2) {
            val areaPath = Path().apply {
                moveTo(pointOffsets.first().x, pointOffsets.first().y)
                for (i in 1 until pointOffsets.size) {
                    lineTo(pointOffsets[i].x, pointOffsets[i].y)
                }
                // 闭合到底部
                lineTo(pointOffsets.last().x, paddingTop + chartHeight)
                lineTo(pointOffsets.first().x, paddingTop + chartHeight)
                close()
            }
            drawPath(areaPath, lineColor.copy(alpha = 0.1f))
        }

        // 折线
        if (pointOffsets.size >= 2) {
            val linePath = Path().apply {
                moveTo(pointOffsets.first().x, pointOffsets.first().y)
                for (i in 1 until pointOffsets.size) {
                    lineTo(pointOffsets[i].x, pointOffsets[i].y)
                }
            }
            drawPath(linePath, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // 数据点 + 标签
        for ((i, offset) in pointOffsets.withIndex()) {
            drawCircle(lineColor, 5f, offset)
            drawCircle(Color.White, 3f, offset)

            // X轴日期标签
            if (points.size <= 12 || i % (points.size / 6).coerceAtLeast(1) == 0 || i == points.lastIndex) {
                val dateLabel = points[i].date.takeLast(5)
                drawContext.canvas.nativeCanvas.drawText(
                    dateLabel, offset.x - 16f, size.height - 4f,
                    android.graphics.Paint().apply { color = 0xFF9E9E9E.toInt(); textSize = 20f; isAntiAlias = true },
                )
            }
        }
    }
}

private fun formatFullDuration(totalSeconds: Int): String {
    if (totalSeconds == 0) return "0s"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return buildString {
        if (hours > 0) append("${hours}h")
        if (minutes > 0 || hours == 0) append("${minutes}m")
    }.trim()
}
