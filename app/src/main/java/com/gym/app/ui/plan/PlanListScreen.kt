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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.planshare.PlanShareCodec
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.components.WeightModeBadge
import com.gym.app.ui.components.pressEffect

private val DarkBg = Color(0xFF3E2E23)
private val AccentOrange = Color(0xFFF2994A)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White
private val OrangeColor = Color(0xFFFF7043)
private val AccentGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    viewModel: PlanListViewModel,
    onCreatePlan: () -> Unit,
    onImportImage: () -> Unit,
    onPlanClick: (Long) -> Unit,
    onPlanView: (Long) -> Unit,
    onPlanImported: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleStrings.myPlan, color = WhiteColor, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val text = clipboard.getText()?.text ?: ""
                        // 剪贴板内容前后可能带多余文字，用提取器拿到干净的分享串再预填
                        viewModel.openImportDialog(PlanShareCodec.extractShareString(text) ?: "")
                    }) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = LocaleStrings.importPlan, tint = WhiteColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                ),
            )
        },
        containerColor = LightGray,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // "Created by you" 标签
            item {
                Text(
                    LocaleStrings.createdByYou,
                    fontSize = 14.sp,
                    color = MediumGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (uiState.plans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No plans yet", color = MediumGray, fontSize = 14.sp)
                    }
                }
            } else {
                items(uiState.plans, key = { it.planId }) { plan ->
                    PlanCard(
                        plan = plan,
                        onView = { onPlanView(plan.planId) },
                        onStart = { onPlanClick(plan.planId) },
                        onDelete = { viewModel.deletePlan(plan) },
                    )
                }
            }

            // 添加新计划
            item {
                AddPlanCard(onClick = onCreatePlan)
            }

            // 从图片导入训练记录（设置开关可关闭）
            if (uiState.showImageImport) {
                item {
                    ImportImageCard(onClick = onImportImage)
                }
            }
        }
    }

    if (importState.showDialog) {
        ImportPlanDialog(
            state = importState,
            onTextChanged = viewModel::onImportTextChanged,
            onParse = viewModel::parseImportText,
            onConfirm = { viewModel.confirmImport(onPlanImported) },
            onBackToInput = viewModel::backToImportInput,
            onDismiss = viewModel::closeImportDialog,
        )
    }
}

/** 导入分享串对话框：输入 → 解析预览 → 确认导入。 */
@Composable
private fun ImportPlanDialog(
    state: ImportUiState,
    onTextChanged: (String) -> Unit,
    onParse: () -> Unit,
    onConfirm: () -> Unit,
    onBackToInput: () -> Unit,
    onDismiss: () -> Unit,
) {
    val preview = state.preview
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (preview == null) LocaleStrings.importPlan else LocaleStrings.importPreviewTitle,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            if (preview == null) {
                Column {
                    OutlinedTextField(
                        value = state.raw,
                        onValueChange = onTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(LocaleStrings.importPlanHint, fontSize = 13.sp) },
                        minLines = 3,
                        shape = RoundedCornerShape(10.dp),
                    )
                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.error.orEmpty(), fontSize = 13.sp, color = Color(0xFFE53935))
                    }
                }
            } else {
                Column {
                    Text(preview.planName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBg)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        itemsIndexed(preview.items) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${index + 1}. ${item.name}  ${item.sets}×${item.reps}",
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                if (item.weightMode != 0) {
                                    WeightModeBadge(item.weightMode)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                if (item.willCreateCustom) {
                                    Text(LocaleStrings.willCreateCustom, fontSize = 11.sp, color = OrangeColor)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (preview == null) {
                TextButton(onClick = onParse, enabled = state.raw.isNotBlank() && !state.isWorking) {
                    Text(LocaleStrings.parseImport, fontWeight = FontWeight.Bold, color = AccentOrange)
                }
            } else {
                TextButton(onClick = onConfirm, enabled = !state.isWorking) {
                    Text(LocaleStrings.importConfirm, fontWeight = FontWeight.Bold, color = AccentOrange)
                }
            }
        },
        dismissButton = {
            if (preview == null) {
                TextButton(onClick = onDismiss) { Text(LocaleStrings.cancel) }
            } else {
                TextButton(onClick = onBackToInput) { Text(LocaleStrings.back) }
            }
        },
    )
}

@Composable
private fun PlanCard(
    plan: WorkoutPlan,
    onView: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().pressEffect(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧彩色图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 计划名称（居中、靠左）
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (plan.name.length > 10) plan.name.take(10) + "…" else plan.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 开始训练按钮
            TextButton(onClick = onStart) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("开始", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
            }

            // View 按钮
            TextButton(onClick = onView) {
                Text(LocaleStrings.viewPlan, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccentOrange)
            }
        }
    }
}

@Composable
private fun AddPlanCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                "Add new plan",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AccentOrange,
            )
        }
    }
}

@Composable
private fun ImportImageCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OrangeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = OrangeColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    LocaleStrings.importImageTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkBg,
                )
                Text(
                    "识别旧训练截图，导入计划与记录",
                    fontSize = 12.sp,
                    color = MediumGray,
                )
            }
        }
    }
}

