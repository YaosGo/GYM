package com.gym.app.ui.importimage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.gym.app.ui.exercise.ExerciseListViewModel
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val DarkBg = Color(0xFF3E2E23)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White
private val AccentOrange = Color(0xFFF2994A)
private val AccentGreen = Color(0xFF4CAF50)
private val OrangeColor = Color(0xFFFF7043)
private val ErrorRed = Color(0xFFC62828)
private val ErrorBg = Color(0xFFFFEBEE)

private val weightFormat = DecimalFormat("#.#")

/**
 * 从图片导入训练记录页：
 * 选图 → OCR 识别 → 解析匹配 → 确认编辑（计划名/日期/组数据）→ 创建计划 + 导入一次训练。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    weightUnit: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onImagePicked(context, it) } }
    val launchPicker = {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        LocaleStrings.importImageTitle,
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = LocaleStrings.back,
                            tint = WhiteColor,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
            )
        },
        containerColor = LightGray,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (uiState.phase) {
                ImportPhase.EMPTY -> EmptyState(onPickImage = launchPicker)
                ImportPhase.RECOGNIZING -> RecognizingState(imageUri = uiState.imageUri)
                ImportPhase.ERROR -> ErrorState(
                    message = uiState.errorMessage,
                    onRetry = {
                        uiState.imageUri?.let {
                            viewModel.onImagePicked(context, android.net.Uri.parse(it))
                        }
                    },
                    onPickImage = launchPicker,
                )
                ImportPhase.REVIEW, ImportPhase.SAVING -> ReviewContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    weightUnit = weightUnit,
                    isSaving = uiState.phase == ImportPhase.SAVING,
                    onPickImage = launchPicker,
                    onDone = onDone,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onPickImage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.PhotoLibrary,
            contentDescription = null,
            tint = MediumGray,
            modifier = Modifier.size(72.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            LocaleStrings.importEmptyDesc,
            fontSize = 14.sp,
            color = MediumGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onPickImage,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
        ) {
            Text(
                LocaleStrings.importSelectImage,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WhiteColor,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun RecognizingState(imageUri: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(modifier = Modifier.height(20.dp))
        CircularProgressIndicator(color = AccentOrange)
        Spacer(modifier = Modifier.height(12.dp))
        Text(LocaleStrings.importRecognizing, fontSize = 14.sp, color = MediumGray)
    }
}

@Composable
private fun ErrorState(
    message: String?,
    onRetry: () -> Unit,
    onPickImage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            LocaleStrings.importErrorTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DarkBg,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message ?: "",
            fontSize = 13.sp,
            color = MediumGray,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row {
            OutlinedButton(onClick = onRetry) {
                Text(LocaleStrings.importRetry, color = AccentOrange)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onPickImage,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            ) {
                Text(LocaleStrings.importChangeImage, color = WhiteColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewContent(
    uiState: ImportUiState,
    viewModel: ImportViewModel,
    weightUnit: String,
    isSaving: Boolean,
    onPickImage: () -> Unit,
    onDone: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // 未匹配动作处理面板状态（选项 / 搜索映射 / 创建自定义）
    var handleIndex by remember { mutableStateOf<Int?>(null) }
    var searchIndex by remember { mutableStateOf<Int?>(null) }
    var createIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 截图预览 + 更换图片
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = uiState.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = onPickImage) {
                        Text(LocaleStrings.importChangeImage, color = AccentOrange)
                    }
                }
            }

            // 计划名称 + 训练日期
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = uiState.planName,
                            onValueChange = viewModel::updatePlanName,
                            label = { Text(LocaleStrings.planName) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                LocaleStrings.importDateLabel,
                                fontSize = 14.sp,
                                color = DarkBg,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                uiState.date,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentOrange,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = LocaleStrings.importDateLabel,
                                tint = AccentOrange,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // 识别摘要
            item {
                Text(
                    LocaleStrings.importSummary
                        .replace("{0}", uiState.matchedCount.toString())
                        .replace("{1}", uiState.matchedSetCount.toString()),
                    fontSize = 13.sp,
                    color = MediumGray,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // 一个动作都没解析出来：提示原因（OCR 无文字 vs 解析失败）
            if (uiState.exercises.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorBg),
                    ) {
                        Text(
                            if (uiState.rawLines.isEmpty()) {
                                LocaleStrings.importNoText
                            } else {
                                LocaleStrings.importParsedNothing
                            },
                            fontSize = 13.sp,
                            color = ErrorRed,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            // 诊断面板：OCR 原始逐行文本（折叠，默认收起）
            if (uiState.rawLines.isNotEmpty()) {
                item {
                    RawLinesCard(lines = uiState.rawLines)
                }
            }

            val matchedIndices = uiState.exercises.indices.filter {
                uiState.exercises[it].exercise != null
            }
            val unmatchedIndices = uiState.exercises.indices.filter {
                uiState.exercises[it].exercise == null
            }

            items(matchedIndices, key = { it }) { index ->
                ExerciseImportCard(
                    item = uiState.exercises[index],
                    index = index,
                    weightUnit = weightUnit,
                    viewModel = viewModel,
                )
            }

            if (unmatchedIndices.isNotEmpty()) {
                item {
                    Text(
                        LocaleStrings.importUnmatchedTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeColor,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                }
                item {
                    Text(
                        LocaleStrings.importUnmatchedHint,
                        fontSize = 12.sp,
                        color = MediumGray,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                items(unmatchedIndices, key = { it }) { index ->
                    UnmatchedCard(
                        item = uiState.exercises[index],
                        onHandle = { handleIndex = index },
                        onSkip = { viewModel.removeExercise(index) },
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorBg),
                    ) {
                        Text(
                            message,
                            fontSize = 13.sp,
                            color = ErrorRed,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }

        // 底部固定导入按钮
        Surface(shadowElevation = 8.dp, color = WhiteColor) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Button(
                    onClick = { viewModel.importNow(onDone) },
                    enabled = !isSaving && uiState.matchedCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        disabledContainerColor = MediumGray,
                    ),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = WhiteColor,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (isSaving) LocaleStrings.importSaving else LocaleStrings.importButton,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhiteColor,
                    )
                }
            }
        }
    } // Column 结束

    // ── 未匹配动作：选项面板 ──
    handleIndex?.let { index ->
        val item = uiState.exercises.getOrNull(index)
        if (item != null) {
            FrostedBottomSheet(
                visible = true,
                onDismiss = { handleIndex = null },
                sheetHeightFraction = 0.42f,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        item.rawName.ifBlank { LocaleStrings.importUnknownName },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBg,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        LocaleStrings.importUnmatchedBadge,
                        fontSize = 12.sp,
                        color = OrangeColor,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OptionRow(
                        icon = { Icon(Icons.Filled.Search, null, tint = AccentOrange, modifier = Modifier.size(20.dp)) },
                        title = LocaleStrings.importSearchLibrary,
                    ) {
                        searchIndex = index
                        handleIndex = null
                    }
                    OptionRow(
                        icon = { Icon(Icons.Filled.Add, null, tint = OrangeColor, modifier = Modifier.size(20.dp)) },
                        title = LocaleStrings.importCreateCustom,
                    ) {
                        createIndex = index
                        handleIndex = null
                    }
                }
            }
        }
    }

    // ── 未匹配动作：搜索动作库映射 ──
    searchIndex?.let { index ->
        FrostedBottomSheet(
            visible = true,
            onDismiss = { searchIndex = null },
            sheetHeightFraction = 0.9f,
        ) {
            ExerciseSearchSheetContent(
                viewModel = viewModel,
                onExercisePicked = { exercise ->
                    viewModel.mapExercise(index, exercise)
                    searchIndex = null
                },
                onCreateCustom = {
                    createIndex = index
                    searchIndex = null
                },
            )
        }
    }

    // ── 未匹配动作：创建自定义动作对话框 ──
    createIndex?.let { index ->
        val item = uiState.exercises.getOrNull(index)
        if (item != null) {
            CreateCustomExerciseDialog(
                initialName = item.rawName,
                onConfirm = { name ->
                    viewModel.createCustomExerciseFor(index, name)
                    createIndex = null
                },
                onDismiss = { createIndex = null },
            )
        }
    }
    } // Box 结束

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.toMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateDate(millis.toDateString())
                    }
                    showDatePicker = false
                }) { Text(LocaleStrings.confirm, color = AccentOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(LocaleStrings.cancel, color = MediumGray)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ExerciseImportCard(
    item: ImportExerciseState,
    index: Int,
    weightUnit: String,
    viewModel: ImportViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val exercise = item.exercise
                if (exercise != null) {
                    ExerciseGifImage(
                        exercise = exercise,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LightGray),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    exercise?.displayName ?: item.rawName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (exercise?.isCustom == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        LocaleStrings.customBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentOrange,
                    )
                }
                IconButton(onClick = { viewModel.removeExercise(index) }) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = LocaleStrings.removeExercise,
                        tint = OrangeColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            item.sets.forEachIndexed { setIndex, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "第${setIndex + 1}组",
                        fontSize = 13.sp,
                        color = MediumGray,
                        modifier = Modifier.width(52.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Stepper(
                        value = "${weightFormat.format(set.weight)}$weightUnit",
                        minEnabled = set.weight > 0f,
                        onMinus = {
                            viewModel.updateSetWeight(index, setIndex, set.weight - 2.5f)
                        },
                        onPlus = {
                            viewModel.updateSetWeight(index, setIndex, set.weight + 2.5f)
                        },
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Stepper(
                        value = "${set.reps}次",
                        minEnabled = set.reps > 1,
                        onMinus = { viewModel.updateSetReps(index, setIndex, set.reps - 1) },
                        onPlus = { viewModel.updateSetReps(index, setIndex, set.reps + 1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnmatchedCard(
    item: ImportExerciseState,
    onHandle: () -> Unit,
    onSkip: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHandle)
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.rawName.ifBlank { LocaleStrings.importUnknownName },
                    fontSize = 14.sp,
                    color = DarkBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    LocaleStrings.importUnmatchedBadge,
                    fontSize = 11.sp,
                    color = OrangeColor,
                )
            }
            IconButton(onClick = onSkip) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = LocaleStrings.importSkip,
                    tint = MediumGray,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** 选项面板中的一行操作项。 */
@Composable
private fun OptionRow(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = DarkBg)
    }
}

/** 搜索动作库面板：输入关键词过滤，点击结果完成映射；无结果时可创建自定义动作。 */
@Composable
private fun ExerciseSearchSheetContent(
    viewModel: ImportViewModel,
    onExercisePicked: (Exercise) -> Unit,
    onCreateCustom: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.onSearchQueryChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(LocaleStrings.searchHint, fontSize = 14.sp, color = MediumGray) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MediumGray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        viewModel.onSearchQueryChanged("")
                    }) {
                        Icon(Icons.Filled.DeleteOutline, LocaleStrings.clear, tint = MediumGray, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LightGray,
                unfocusedContainerColor = LightGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        if (query.isNotBlank() && results.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(LocaleStrings.noResults, fontSize = 13.sp, color = MediumGray)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onCreateCustom) {
                    Text(
                        LocaleStrings.importCreateCustomWith.replace("{0}", query.trim()),
                        color = AccentOrange,
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            items(results, key = { it.id }) { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onExercisePicked(exercise) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseGifImage(
                        exercise = exercise,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        exercise.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkBg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (exercise.isCustom) LocaleStrings.customBadge
                        else ExerciseListViewModel.bodyPartLabel(exercise.bodyPart),
                        fontSize = 11.sp,
                        color = if (exercise.isCustom) AccentOrange else MediumGray,
                    )
                }
            }
        }
    }
}

/** 诊断面板：OCR 原始逐行文本，默认收起，点击展开。 */
@Composable
private fun RawLinesCard(lines: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${LocaleStrings.importRawLines}（${lines.size} 行）",
                    fontSize = 13.sp,
                    color = MediumGray,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MediumGray,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (expanded) {
                Text(
                    lines.joinToString("\n"),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DarkBg,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                )
            }
        }
    }
}

/** 紧凑步进器：[−] 值 [+]，minEnabled 控制减号是否可用。 */
@Composable
private fun Stepper(
    value: String,
    minEnabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(
            label = "−",
            enabled = minEnabled,
            onClick = onMinus,
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DarkBg,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 58.dp),
        )
        StepperButton(label = "+", enabled = true, onClick = onPlus)
    }
}

@Composable
private fun StepperButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (enabled) LightGray else LightGray.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) AccentOrange else MediumGray.copy(alpha = 0.5f),
        )
    }
}

private fun String.toMillis(): Long? = try {
    LocalDate.parse(this).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
} catch (_: Exception) {
    null
}

private fun Long.toDateString(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toString()
