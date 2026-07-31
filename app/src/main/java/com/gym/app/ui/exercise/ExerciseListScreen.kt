package com.gym.app.ui.exercise

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import com.gym.app.data.entity.Exercise
import com.gym.app.ui.components.AnimationConfig
import com.gym.app.ui.components.FrostedBottomSheet
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.displayName

// 颜色常量
private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val CardBg = Color(0xFFFAFAFA)
private val WhiteColor = Color.White

// 部位标签颜色映射
private val bodyPartColors = mapOf(
    "chest" to Color(0xFFFF7043),
    "back" to Color(0xFF42A5F5),
    "shoulders" to Color(0xFFFFCA28),
    "upper arms" to Color(0xFFAB47BC),
    "upper legs" to Color(0xFF26A69A),
    "lower legs" to Color(0xFF8D6E63),
    "waist" to Color(0xFFEF5350),
    "cardio" to Color(0xFFEC407A),
    "lower arms" to Color(0xFF78909C),
    "neck" to Color(0xFF7E57C2),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseListViewModel,
    onExerciseClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    exerciseRepository: com.gym.app.data.repository.ExerciseRepository? = null,
    sheetShowing: androidx.compose.runtime.MutableState<Boolean> = mutableStateOf(false),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedExerciseId = remember { mutableStateOf<String?>(null) }
    val selectedExercise = remember { mutableStateOf<Exercise?>(null) }

    var sheetContentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(selectedExerciseId.value) {
        sheetShowing.value = selectedExerciseId.value != null
        if (selectedExerciseId.value != null) {
            delay(250L)
            sheetContentVisible = true
        } else {
            sheetContentVisible = false
        }
    }

    BackHandler(enabled = sheetContentVisible) {
        selectedExerciseId.value = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        LocaleStrings.exerciseTitle,
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = WhiteColor,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* search focus */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索", tint = WhiteColor)
                    }
                    IconButton(onClick = { /* settings */ }) {
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
        // 加载选中动作的详情
        if (selectedExerciseId.value != null && exerciseRepository != null) {
            LaunchedEffect(selectedExerciseId.value) {
                selectedExercise.value = exerciseRepository.getExerciseById(selectedExerciseId.value!!)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 分类标签
            BodyPartTabs(
                bodyParts = uiState.bodyParts,
                selectedBodyPart = uiState.selectedBodyPart,
                onBodyPartSelected = viewModel::onBodyPartSelected,
            )

            // 内容区域
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (uiState.exercises.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No exercises found",
                        color = MediumGray,
                        fontSize = 16.sp,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.exercises, key = { it.id }) { exercise ->
                        ExerciseGridCard(
                            exercise = exercise,
                            onClick = {
                                if (exerciseRepository != null) {
                                    selectedExerciseId.value = exercise.id
                                } else {
                                    onExerciseClick(exercise.id)
                                }
                            },
                        )
                    }

                }
            }
        }
    } // Scaffold 结束

        // 磨砂玻璃底部滑出 — 动作详情
        if (sheetContentVisible && selectedExercise.value != null) {
            FrostedBottomSheet(
                visible = true,
                onDismiss = {
                    selectedExerciseId.value = null
                },
                sheetHeightFraction = 0.96f,
            ) {
                ExerciseDetailSheetContent(exercise = selectedExercise.value!!)
            }
        }
    } // Box end
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        placeholder = {
            Text(
                "Search or add new exercise",
                color = MediumGray,
                fontSize = 14.sp,
            )
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MediumGray)
        },
        trailingIcon = {
            Row {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Clear, contentDescription = "清除", tint = MediumGray)
                    }
                }
                IconButton(onClick = { /* filter bottom sheet */ }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.FilterList, contentDescription = "筛选", tint = MediumGray)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyPartTabs(
    bodyParts: List<String>,
    selectedBodyPart: String?,
    onBodyPartSelected: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedBodyPart == null,
                onClick = { onBodyPartSelected(null) },
                label = { Text(LocaleStrings.exerciseAll, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DarkBg,
                    selectedLabelColor = WhiteColor,
                ),
                shape = RoundedCornerShape(20.dp),
            )
        }
        items(bodyParts) { part ->
            val label = ExerciseListViewModel.bodyPartLabel(part)
            FilterChip(
                selected = selectedBodyPart == part,
                onClick = {
                    onBodyPartSelected(if (selectedBodyPart == part) null else part)
                },
                label = { Text(label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DarkBg,
                    selectedLabelColor = WhiteColor,
                ),
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun ExerciseGridCard(
    exercise: Exercise,
    onClick: () -> Unit,
) {
    val accentColor = bodyPartColors[exercise.bodyPart] ?: AccentBlue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // GIF 动图区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(CardBg),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = "file:///android_asset/${exercise.gifPath}",
                    contentDescription = exercise.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            // 底部信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    exercise.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 彩色部位标签条
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ExerciseListViewModel.bodyPartLabel(exercise.bodyPart),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 动作详情 Sheet 内容（磨砂玻璃底部滑出）
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailSheetContent(exercise: Exercise) {
    val steps = remember(exercise) { parseInstructionSteps(exercise.instructionStepsZh) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // GIF 展示区
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
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
                    Text(exercise.name, color = WhiteColor.copy(alpha = 0.6f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(ExerciseListViewModel.bodyPartLabel(exercise.bodyPart), color = WhiteColor.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(WhiteColor).padding(16.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(ExerciseListViewModel.bodyPartLabel(exercise.bodyPart), AccentBlue)
                TagChip(ExerciseListViewModel.targetLabel(exercise.target), Color(0xFFFF7043))
                TagChip(ExerciseListViewModel.equipmentLabel(exercise.equipment), Color(0xFF26A69A))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(LocaleStrings.instructions, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBg)
            Spacer(modifier = Modifier.height(16.dp))

            if (steps.isNotEmpty()) {
                steps.forEachIndexed { index, step ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(durationMillis = AnimationConfig.Stagger.itemDurationMs, delayMillis = index * AnimationConfig.Stagger.sheetStepDelayMs)) +
                                slideInVertically(animationSpec = tween(durationMillis = AnimationConfig.Stagger.itemDurationMs, delayMillis = index * AnimationConfig.Stagger.sheetStepDelayMs)) { it / 2 },
                    ) {
                        StepChip(stepNumber = index + 1, text = step)
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LightGray).padding(16.dp)) {
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
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun StepChip(stepNumber: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 2.dp).size(26.dp).clip(CircleShape).background(DarkBg), contentAlignment = Alignment.Center) {
            Text("$stepNumber", color = WhiteColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text.trim(), fontSize = 14.sp, color = DarkBg, lineHeight = 22.sp, modifier = Modifier.weight(1f))
    }
}

private fun parseInstructionSteps(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
        moshi.adapter<List<String>>(type).fromJson(json) ?: emptyList()
    } catch (_: Exception) { emptyList() }
}
