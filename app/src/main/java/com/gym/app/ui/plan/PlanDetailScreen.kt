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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gym.app.data.entity.Exercise
import com.gym.app.ui.components.CreateCustomExerciseDialog
import com.gym.app.ui.components.ExerciseGifImage
import com.gym.app.ui.components.FrostedBottomSheet
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.displayName
import com.gym.app.ui.components.pressEffect
import com.gym.app.ui.components.PlanTrendCard
import com.gym.app.ui.components.TrendPoint
import com.gym.app.ui.components.WeightModeBadge
import com.gym.app.ui.components.WeightModeSelectorRow
import com.gym.app.ui.exercise.ExerciseListViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF3E2E23)
private val AccentOrange = Color(0xFFF2994A)
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

    // 动作详情面板
    var selectedDetailExercise by remember { mutableStateOf<Exercise?>(null) }

    // 创建自定义动作对话框（编辑模式搜索无结果入口）
    var showCreateCustom by remember { mutableStateOf(false) }

    // 保存成功弹窗
    var showSavedSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 分享计划弹层
    var showShareSheet by remember { mutableStateOf(false) }
    var shareString by remember { mutableStateOf<String?>(null) }
    var shareCopied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

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
                    "动作列表、顺序和名称已更新",
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

    // 分享计划弹层：字符串 + 复制 + 系统分享
    if (showShareSheet && shareString != null) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WhiteColor,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Text(
                    LocaleStrings.sharePlan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    LocaleStrings.sharePlanHint,
                    fontSize = 13.sp,
                    color = MediumGray,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 110.dp)
                        .verticalScroll(rememberScrollState())
                        .background(LightGray, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        shareString!!,
                        fontSize = 12.sp,
                        color = DarkBg,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(shareString!!))
                            shareCopied = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    ) {
                        Text(
                            if (shareCopied) LocaleStrings.copied else LocaleStrings.copy,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareString)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, LocaleStrings.sharePlan))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    ) {
                        Text(LocaleStrings.shareToFriends, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            shareString = viewModel.buildShareString()
                            if (shareString != null) {
                                shareCopied = false
                                showShareSheet = true
                            }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = LocaleStrings.sharePlan, tint = WhiteColor)
                        }
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
            // 计划名称 + 元信息（编辑模式下名称可修改，保存时一起持久化）
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (isEditing) {
                    TextField(
                        value = uiState.planName,
                        onValueChange = viewModel::onPlanNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(LocaleStrings.planName, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = WhiteColor,
                            unfocusedContainerColor = WhiteColor,
                            focusedIndicatorColor = AccentOrange,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                } else {
                    Text(
                        uiState.planName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBg,
                    )
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
                        onWeightModeChanged = { mode -> viewModel.updateWeightMode(index, mode) },
                        onMoveUp = {
                            if (index > 0) viewModel.moveItem(index, index - 1)
                        },
                        onMoveDown = {
                            if (index < uiState.exercises.lastIndex) viewModel.moveItem(index, index + 1)
                        },
                        onExerciseClick = { selectedDetailExercise = it },
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

                    // 搜索无结果 → 创建自定义动作入口（与图片导入未匹配动作的支持一致）
                    if (uiState.isSearching && uiState.searchResults.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(LocaleStrings.noResults, color = MediumGray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { showCreateCustom = true }) {
                                    Text(
                                        LocaleStrings.createCustomFromLibrary.replace("{0}", uiState.searchQuery.trim()),
                                        color = AccentOrange,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
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
                        if (uiState.trendPoints.isNotEmpty()) {
                            PlanTrendCard(trendPoints = uiState.trendPoints)
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
    } // Scaffold 结束

        // 动作详情面板
        if (selectedDetailExercise != null) {
            FrostedBottomSheet(
                visible = true,
                onDismiss = { selectedDetailExercise = null },
            ) {
                ExerciseDetailSheetContent(exercise = selectedDetailExercise!!)
            }
        }

        // 创建自定义动作对话框（编辑模式搜索无结果入口）
        if (showCreateCustom) {
            CreateCustomExerciseDialog(
                initialName = uiState.searchQuery.trim(),
                onConfirm = { name ->
                    viewModel.createCustomExercise(name)
                    showCreateCustom = false
                },
                onDismiss = { showCreateCustom = false },
            )
        }
    } // Box 结束
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
    onExerciseClick: (Exercise) -> Unit = {},
    onWeightModeChanged: (Int) -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressEffect()
            .clickable(enabled = !isEditing && item.exercise != null) {
                item.exercise?.let { onExerciseClick(it) }
            },
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

            // GIF 缩略图（自定义动作显示占位图标）
            if (item.exercise != null) {
                ExerciseGifImage(
                    exercise = item.exercise,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                    placeholderIconSize = 18.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            // 名称 + 组数次
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // 查看模式：助力/负重只读标签
                    if (!isEditing && item.exercise != null && (item.exercise.isAssisted || item.exercise.isWeighted)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        WeightModeBadge(item.exercise.weightMode)
                    }
                }
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
                // 编辑模式：重量记录方式三态切换（普通/助力/负重）
                if (isEditing && item.exercise != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    WeightModeSelectorRow(
                        currentMode = item.exercise.weightMode,
                        onModeChanged = onWeightModeChanged,
                    )
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
                        Icon(Icons.Filled.ExpandLess, "上移", Modifier.size(18.dp), tint = if (number > 1) AccentOrange else MediumGray.copy(alpha = 0.3f))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(28.dp),
                        enabled = number < totalCount,
                    ) {
                        Icon(Icons.Filled.ExpandMore, "下移", Modifier.size(18.dp), tint = if (number < totalCount) AccentOrange else MediumGray.copy(alpha = 0.3f))
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
            color = if (value > 1) AccentOrange else MediumGray.copy(alpha = 0.3f),
        )
        Text("$value", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBg)
        Text(
            "+",
            modifier = Modifier.clickable { if (value < 99) onChanged(value + 1) }.padding(horizontal = 5.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (value < 99) AccentOrange else MediumGray.copy(alpha = 0.3f),
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
            ExerciseGifImage(
                exercise = exercise,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                placeholderIconSize = 16.dp,
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
                    if (exercise.isCustom) LocaleStrings.customBadge
                    else "${ExerciseListViewModel.bodyPartLabel(exercise.bodyPart)} · ${ExerciseListViewModel.equipmentLabel(exercise.equipment)}",
                    fontSize = 11.sp,
                    color = if (exercise.isCustom) AccentOrange else MediumGray,
                )
            }
            if (isAdded) {
                Text("已添加", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Filled.Add, "添加", tint = AccentOrange, modifier = Modifier.size(20.dp))
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

// ═══════════════════════════════════════════════════════════════
// 动作详情 Sheet 内容
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailSheetContent(exercise: Exercise) {
    val steps = remember(exercise) {
        try {
            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
            val json = exercise.instructionStepsZh
            if (json.isBlank() || json == "[]") emptyList()
            else moshi.adapter<List<String>>(type).fromJson(json) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (exercise.isCustom) 1.4f else 1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (exercise.isCustom) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = exercise.displayName,
                    tint = WhiteColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp),
                )
            } else {
                AsyncImage(
                    model = "file:///android_asset/${exercise.gifPath}",
                    contentDescription = exercise.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Box(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f)).padding(16.dp),
            ) {
                Column {
                    Text(exercise.displayName, color = WhiteColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (exercise.isCustom) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(LocaleStrings.customBadge, color = AccentOrange, fontSize = 13.sp)
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(exercise.name, color = WhiteColor.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().background(WhiteColor).padding(16.dp)) {
            if (exercise.isCustom) {
                // 自定义动作：无标签、无要点
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LightGray).padding(16.dp),
                ) {
                    Text(LocaleStrings.customNoInstructions, color = DarkBg, lineHeight = 24.sp)
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(ExerciseListViewModel.bodyPartLabel(exercise.bodyPart), AccentOrange)
                    TagChip(ExerciseListViewModel.targetLabel(exercise.target), OrangeColor)
                    TagChip(ExerciseListViewModel.equipmentLabel(exercise.equipment), Color(0xFF26A69A))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("步骤指导", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBg)
                Spacer(modifier = Modifier.height(16.dp))
                if (steps.isNotEmpty()) {
                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(DarkBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${index + 1}", color = WhiteColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(step.trim(), fontSize = 14.sp, color = DarkBg, lineHeight = 22.sp, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    Text(exercise.instructionsZh, color = DarkBg, lineHeight = 24.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TagChip(text: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
    }
}
