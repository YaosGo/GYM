package com.gym.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.app.ui.components.AnimationConfig
import com.gym.app.ui.components.LocaleStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    scope: CoroutineScope,
    onBack: () -> Unit,
) {
    // 从 DataStore 读取当前值作为初始状态
    val savedLanguage by settings.language.collectAsState(initial = "zh")
    val savedWeightUnit by settings.weightUnit.collectAsState(initial = "kg")

    // 本地编辑状态（未保存前不生效）
    var editLanguage by remember { mutableStateOf(savedLanguage) }
    var editWeightUnit by remember { mutableStateOf(savedWeightUnit) }

    // 首次加载时同步
    if (editLanguage != savedLanguage && editLanguage == "zh") {
        editLanguage = savedLanguage
    }
    if (editWeightUnit != savedWeightUnit && editWeightUnit == "kg") {
        editWeightUnit = savedWeightUnit
    }

    // 是否有未保存的修改
    val hasChanges = editLanguage != savedLanguage || editWeightUnit != savedWeightUnit

    // 未保存退出提醒对话框
    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("未保存的修改", fontWeight = FontWeight.Bold) },
            text = { Text("你有未保存的设置修改，确定要放弃更改并退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) {
                    Text("放弃更改", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("继续编辑")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleStrings.settingsTitle, color = WhiteColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showExitDialog = true
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WhiteColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
            )
        },
        containerColor = LightGray,
        bottomBar = {
            // 保存按钮（仅在有修改时启用）
            Button(
                onClick = {
                    scope.launch {
                        settings.setLanguage(editLanguage)
                        settings.setWeightUnit(editWeightUnit)
                    }
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                enabled = hasChanges,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    disabledContainerColor = MediumGray.copy(alpha = 0.3f),
                ),
            ) {
                Text(
                    "应用保存",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (hasChanges) WhiteColor else MediumGray,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 语言设置
            SettingsCard(LocaleStrings.language) {
                SelectableRow(LocaleStrings.langZh, editLanguage == "zh") {
                    editLanguage = "zh"
                }
                SelectableRow(LocaleStrings.langEn, editLanguage == "en") {
                    editLanguage = "en"
                }
            }

            // 重量单位
            SettingsCard(LocaleStrings.weightUnit) {
                SelectableRow(LocaleStrings.unitKg, editWeightUnit == "kg") {
                    editWeightUnit = "kg"
                }
                SelectableRow(LocaleStrings.unitLb, editWeightUnit == "lb") {
                    editWeightUnit = "lb"
                }
            }

            // 身体数据（预留）
            SettingsCard(LocaleStrings.bodyInfo) {
                ComingSoonRow(LocaleStrings.gender)
                ComingSoonRow(LocaleStrings.height)
                ComingSoonRow(LocaleStrings.weightInfo)
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBg)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) AccentBlue.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(AnimationConfig.Selector.bgColorMs),
        label = "selBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) AccentBlue else DarkBg,
        animationSpec = tween(AnimationConfig.Selector.textColorMs),
        label = "selText",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = textColor, modifier = Modifier.weight(1f))
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(AnimationConfig.Selector.checkFadeInMs)) + scaleIn(spring(dampingRatio = AnimationConfig.Press.springDamping, stiffness = AnimationConfig.Press.springStiff)),
            exit = fadeOut(tween(AnimationConfig.Selector.checkFadeOutMs)),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ComingSoonRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = MediumGray, modifier = Modifier.weight(1f))
        Text(LocaleStrings.comingSoon, fontSize = 11.sp, color = MediumGray)
    }
}
