package com.gym.app.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val uiState by viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleStrings.editPlan, color = WhiteColor, fontWeight = FontWeight.Bold) },
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
                    IconButton(onClick = { /* rename dialog */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑名称", tint = WhiteColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                ),
            )
        },
        containerColor = LightGray,
        bottomBar = {
            // 固定在底部的保存按钮
            Button(
                onClick = { viewModel.savePlan(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                ),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = WhiteColor,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (uiState.isSaving) "保存中..." else "✓ 保存",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 计划名称
            Text(
                uiState.planName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBg,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // 动作列表（支持长按拖拽排序）
            val draggedIndex = remember { mutableStateOf(-1) }
            val dragOffsetY = remember { mutableStateOf(0f) }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(uiState.exercises) { index, item ->
                    val isDragging = draggedIndex.value == index
                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragOffsetY.value
                                    scaleX = 1.05f
                                    scaleY = 1.05f
                                    alpha = 0.9f
                                    shadowElevation = 16f
                                }
                            }
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedIndex.value = index
                                        dragOffsetY.value = 0f
                                    },
                                    onDrag = { change, offset ->
                                        change.consume()
                                        dragOffsetY.value += offset.y
                                        // 计算目标位置：每项约 80dp (~110px)
                                        val itemHeight = 110f
                                        val delta = (dragOffsetY.value / itemHeight).toInt()
                                        val newIndex = (index + delta).coerceIn(0, uiState.exercises.lastIndex)
                                        if (newIndex != index && draggedIndex.value == index) {
                                            viewModel.moveItem(index, newIndex)
                                            draggedIndex.value = newIndex
                                            dragOffsetY.value -= delta * itemHeight
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex.value = -1
                                        dragOffsetY.value = 0f
                                    },
                                    onDragCancel = {
                                        draggedIndex.value = -1
                                        dragOffsetY.value = 0f
                                    },
                                )
                            },
                    ) {
                        ExerciseEditItem(
                            number = index + 1,
                            item = item,
                            onRemove = { viewModel.removeExercise(index) },
                            onSetsChanged = { viewModel.updateSets(index, it) },
                            onRepsChanged = { viewModel.updateReps(index, it) },
                        )
                    }
                }

                // 搜索添加区域
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    // 搜索栏
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search to add exercise...", fontSize = 14.sp) },
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
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = WhiteColor,
                            unfocusedContainerColor = WhiteColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 搜索结果
                    if (uiState.isSearching) {
                        uiState.searchResults.take(8).forEach { exercise ->
                            SearchAddItem(
                                exercise = exercise,
                                isAdded = uiState.exercises.any {
                                    it.planExercise.exerciseId == exercise.id
                                },
                                onAdd = { viewModel.addExercise(exercise) },
                            )
                        }
                    }

                    if (!uiState.isSearching && uiState.exercises.isEmpty()) {
                        Text(
                            "No exercises yet. Search above to add.",
                            color = MediumGray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun ExerciseEditItem(
    number: Int,
    item: PlanExerciseItem,
    onRemove: () -> Unit,
    onSetsChanged: (Int) -> Unit,
    onRepsChanged: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 序号圆圈
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DarkBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$number",
                    color = WhiteColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // GIF缩略图
            if (item.exercise != null) {
                AsyncImage(
                    model = "file:///android_asset/${item.exercise.gifPath}",
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp)),
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
                    Stepper(
                        value = item.planExercise.targetSets,
                        onChanged = onSetsChanged,
                        label = "组",
                    )
                    Text(" × ", fontSize = 13.sp, color = MediumGray)
                    Stepper(
                        value = item.planExercise.targetReps,
                        onChanged = onRepsChanged,
                        label = "次",
                    )
                }
            }

            // 删除按钮
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.RemoveCircleOutline,
                    contentDescription = "删除",
                    tint = OrangeColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun Stepper(
    value: Int,
    onChanged: (Int) -> Unit,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "−",
            modifier = Modifier
                .clickable { if (value > 1) onChanged(value - 1) }
                .padding(horizontal = 5.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (value > 1) AccentBlue else MediumGray.copy(alpha = 0.3f),
        )
        Text(
            "$value",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkBg,
        )
        Text(
            "+",
            modifier = Modifier
                .clickable { if (value < 99) onChanged(value + 1) }
                .padding(horizontal = 5.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (value < 99) AccentBlue else MediumGray.copy(alpha = 0.3f),
        )
        Text(" $label", fontSize = 12.sp, color = MediumGray)
    }
}

@Composable
private fun SearchAddItem(
    exercise: Exercise,
    isAdded: Boolean,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(enabled = !isAdded, onClick = onAdd),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdded) LightGray else WhiteColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = "file:///android_asset/${exercise.gifPath}",
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isAdded) MediumGray else DarkBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${ExerciseListViewModel.bodyPartLabel(exercise.bodyPart)} · ${exercise.equipment}",
                    fontSize = 11.sp,
                    color = MediumGray,
                )
            }
            if (isAdded) {
                Text("Added", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Filled.Add, contentDescription = "添加", tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}
