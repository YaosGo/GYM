package com.gym.app.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.app.ui.components.LocaleStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
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
    val language by settings.language.collectAsState(initial = "zh")
    val weightUnit by settings.weightUnit.collectAsState(initial = "kg")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleStrings.settingsTitle, color = WhiteColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WhiteColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
            )
        },
        containerColor = LightGray,
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
                SelectableRow(LocaleStrings.langZh, language == "zh") {
                    scope.launch { settings.setLanguage("zh") }
                }
                SelectableRow(LocaleStrings.langEn, language == "en") {
                    scope.launch { settings.setLanguage("en") }
                }
            }

            // 重量单位
            SettingsCard(LocaleStrings.weightUnit) {
                SelectableRow(LocaleStrings.unitKg, weightUnit == "kg") {
                    scope.launch { settings.setWeightUnit("kg") }
                }
                SelectableRow(LocaleStrings.unitLb, weightUnit == "lb") {
                    scope.launch { settings.setWeightUnit("lb") }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = if (selected) AccentBlue else DarkBg, modifier = Modifier.weight(1f))
        if (selected) {
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
