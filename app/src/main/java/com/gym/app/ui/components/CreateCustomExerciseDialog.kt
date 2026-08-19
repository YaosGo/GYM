package com.gym.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DialogDark = Color(0xFF3E2E23)
private val DialogGray = Color(0xFF9E9E9E)
private val DialogOrange = Color(0xFFF2994A)

/**
 * 创建自定义动作对话框：输入动作名称，确认后由调用方写入动作库。
 * 用于图片导入未匹配条目和动作库搜索无结果两个入口。
 */
@Composable
fun CreateCustomExerciseDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LocaleStrings.createCustomTitle, color = DialogDark) },
        text = {
            Column {
                Text(
                    LocaleStrings.createCustomDesc,
                    fontSize = 13.sp,
                    color = DialogGray,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(LocaleStrings.customNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) { Text(LocaleStrings.confirm, color = DialogOrange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(LocaleStrings.cancel, color = DialogGray) }
        },
    )
}
