package com.gym.app.ui.plan

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gym.app.data.entity.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanScreen(
    viewModel: CreatePlanViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val uiState by viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建健身计划") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
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
            // 计划名称输入
            OutlinedTextField(
                value = uiState.planName,
                onValueChange = viewModel::onPlanNameChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("计划名称") },
                placeholder = { Text("例如：三分化-推日") },
                singleLine = true,
            )

            // 已选动作区域
            Text(
                text = "已选动作 (${uiState.selectedExercises.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (uiState.selectedExercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "还未选择动作，在下方搜索并添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        uiState.selectedExercises,
                        key = { _, se -> se.exercise.id },
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
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // 搜索添加动作
            Text(
                text = "搜索添加动作",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // 搜索栏
            TextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("搜索动作名称（支持模糊匹配）") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 搜索结果
            if (uiState.isSearching) {
                if (uiState.searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "未找到匹配的动作",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(uiState.searchResults, key = { it.id }) { exercise ->
                            SearchResultItem(
                                exercise = exercise,
                                isAlreadyAdded = uiState.selectedExercises.any {
                                    it.exercise.id == exercise.id
                                },
                                onAdd = { viewModel.addExercise(exercise) },
                            )
                        }
                    }
                }
            } else {
                // 未搜索时显示提示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "输入关键词搜索动作\n支持中英文模糊匹配",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 保存按钮
            Button(
                onClick = { viewModel.savePlan(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = uiState.planName.isNotBlank()
                        && uiState.selectedExercises.isNotEmpty()
                        && !uiState.isSaving,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isSaving) "保存中..." else "保存计划")
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                contentDescription = selected.exercise.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 名称 + 目标
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selected.exercise.name,
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
            .clickable(enabled = !isAlreadyAdded, onClick = onAdd),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlreadyAdded)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface,
        ),
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
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(56.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${ExerciseListViewModel.bodyPartLabel(exercise.bodyPart)} · ${exercise.target} · ${exercise.equipment}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isAlreadyAdded) {
                Text(
                    text = "已添加",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
