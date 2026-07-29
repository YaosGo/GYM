package com.gym.app.ui.exercise

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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gym.app.data.entity.Exercise
import com.gym.app.ui.components.LocaleStrings

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
) {
    val uiState by viewModel.uiState

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
                            onClick = { onExerciseClick(exercise.id) },
                        )
                    }
                }
            }
        }
    }
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
                    contentDescription = exercise.name,
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
                    exercise.name,
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
