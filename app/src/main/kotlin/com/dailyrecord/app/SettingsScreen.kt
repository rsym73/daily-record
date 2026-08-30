package com.dailyrecord.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    hasWallpaper: Boolean,
    onPickWallpaper: () -> Unit,
    onRemoveWallpaper: () -> Unit,
) {
    val hour by vm.hour.collectAsState()
    val minute by vm.minute.collectAsState()
    val saved by vm.saved.collectAsState()

    var hourText by remember { mutableStateOf(hour.toString()) }
    var minuteText by remember { mutableStateOf(minute.toString()) }

    LaunchedEffect(hour, minute) {
        hourText = hour.toString()
        minuteText = minute.toString()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("最后提醒时间", style = MaterialTheme.typography.titleMedium)
            Text("每天这个时间提醒（凌晨 1 点边界前），默认 0:30；当天已完成则不提醒", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { hourText = it },
                    label = { Text("时") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp),
                )
                Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it },
                    label = { Text("分") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: hour
                val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: minute
                vm.save(h, m)
            }) { Text("保存") }
            if (saved) {
                Spacer(Modifier.height(8.dp))
                Text("已保存", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(32.dp))
            Text("备份", style = MaterialTheme.typography.titleMedium)
            Text("导出为 JSON 文件，或从 JSON 文件导入恢复", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row {
                OutlinedButton(onClick = onExport) { Text("导出备份") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = onImport) { Text("导入备份") }
            }

            Spacer(Modifier.height(32.dp))
            Text("壁纸", style = MaterialTheme.typography.titleMedium)
            Text("从相册选择背景图，或恢复默认白底", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row {
                OutlinedButton(onClick = onPickWallpaper) { Text("更换壁纸") }
                if (hasWallpaper) {
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = onRemoveWallpaper) { Text("恢复默认") }
                }
            }
        }
    }
}
