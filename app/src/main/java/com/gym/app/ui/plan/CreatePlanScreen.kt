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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.gym.app.ui.exercise.ExerciseListViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.displayName

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanScreen(
    viewModel: CreatePlanViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    // 保存对话框中的计划名称（本地状态）
    var dialogPlanName by remember { mutableStateOf("") }

    // 保存对话框
    if (uiState.showSaveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissSaveDialog() },
            title = { Text("保存计划", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "请输入计划名称",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dialogPlanName,
                        onValueChange = { dialogPlanName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如：三分化-推日") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.savePlan(dialogPlanName, onSaved) },
                    enabled = dialogPlanName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                    ),
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissSaveDialog() }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建健身计划", color = WhiteColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = WhiteColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                ),
            )
        },
        containerColor = LightGray,
    ) { padding ->
        if (uiState.isSaved) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("计划已保存！", style = MaterialTheme.typography.headlineSmall)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ===== 搜索栏（固定在上方）=====
            TextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "搜索动作名称（支持模糊匹配）",
                        fontSize = 14.sp,
                        color = MediumGray,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MediumGray)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除", tint = MediumGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = WhiteColor,
                    unfocusedContainerColor = WhiteColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            // ===== 滚动内容区域（已选动作 + 搜索结果）=====
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                // 已选动作区域
                if (uiState.selectedExercises.isNotEmpty()) {
                    item {
                        Text(
                            text = "已选动作 (${uiState.selectedExercises.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(
                        uiState.selectedExercises,
                        key = { _, se -> "sel_${se.exercise.id}" },
                    ) { index, selected ->
                        SelectedExerciseCard(
                            selected = selected,
                            index = index,
                            onSetsChanged = { viewModel.updateExerciseSets(selected.exercise.id, it) },
                            onRepsChanged = { viewModel.updateExerciseReps(selected.exercise.id, it) },
                            onRemove = { viewModel.removeExercise(selected.exercise.id) },
                            onMoveUp = {
                                if (index > 0) viewModel.moveExercise(index, index - 1)
                            },
                            onMoveDown = {
                                if (index < uiState.selectedExercises.size - 1) {
                                    viewModel.moveExercise(index, index + 1)
                                }
                            },
                        )
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }

                // 搜索结果或提示
                if (uiState.isSearching) {
                    if (uiState.searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "未找到匹配的动作",
                                    color = MediumGray,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "搜索结果",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        items(uiState.searchResults, key = { "search_${it.id}" }) { exercise ->
                            SearchResultItem(
                                exercise = exercise,
                                isAlreadyAdded = uiState.selectedExercises.any {
                                    it.exercise.id == exercise.id
                                },
                                onAdd = { viewModel.addExercise(exercise) },
                            )
                        }
                    }
                } else if (uiState.selectedExercises.isEmpty()) {
                    // 未搜索且没有已选动作时显示提示
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MediumGray,
                                    modifier = Modifier.size(40.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "输入关键词搜索动作",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MediumGray,
                                )
                                Text(
                                    "支持中英文模糊匹配",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MediumGray,
                                )
                            }
                        }
                    }
                }
            }

            // ===== 底部保存按钮（始终可见）=====
            Button(
                onClick = { viewModel.onShowSaveDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                enabled = uiState.selectedExercises.isNotEmpty() && !uiState.isSaving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                ),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = WhiteColor,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (uiState.isSaving) "保存中..." else "保存计划",
                    color = WhiteColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun SelectedExerciseCard(
    selected: SelectedExercise,
    index: Int,
    onSetsChanged: (Int) -> Unit,
    onRepsChanged: (Int) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = WhiteColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 序号
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp),
            )

            // GIF 缩略图
            AsyncImage(
                model = "file:///android_asset/${selected.exercise.gifPath}",
                contentDescription = selected.exercise.displayName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 名称 + 目标
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selected.exercise.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberInput(
                        value = selected.targetSets,
                        onValueChanged = onSetsChanged,
                        label = "组",
                        modifier = Modifier.width(64.dp),
                    )
                    Text(" × ", style = MaterialTheme.typography.bodySmall)
                    NumberInput(
                        value = selected.targetReps,
                        onValueChanged = onRepsChanged,
                        label = "次",
                        modifier = Modifier.width(64.dp),
                    )
                }
            }

            // 操作按钮
            Column {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                    Text("↑", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                    Text("↓", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun NumberInput(
    value: Int,
    onValueChanged: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "-",
            modifier = Modifier
                .clickable { if (value > 1) onValueChanged(value - 1) }
                .padding(horizontal = 4.dp),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$value",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "+",
            modifier = Modifier
                .clickable { if (value < 99) onValueChanged(value + 1) }
                .padding(horizontal = 4.dp),
            fontWeight = FontWeight.Bold,
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SearchResultItem(
    exercise: Exercise,
    isAlreadyAdded: Boolean,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clickable(enabled = !isAlreadyAdded, onClick = onAdd),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlreadyAdded)
                WhiteColor.copy(alpha = 0.5f)
            else WhiteColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // GIF 缩略图
            AsyncImage(
                model = "file:///android_asset/${exercise.gifPath}",
                contentDescription = exercise.displayName,
                modifier = Modifier
                    .size(48.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${ExerciseListViewModel.bodyPartLabel(exercise.bodyPart)} · ${exercise.target} · ${exercise.equipment}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isAlreadyAdded) {
                Text(
                    text = "已添加",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                )
            } else {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加",
                    tint = AccentBlue,
                )
            }
        }
    }
}
