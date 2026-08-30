package com.dailyrecord.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyrecord.app.data.TodayState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val app = application as RecordApplication
        lifecycleScope.launch {
            ReminderScheduler.schedule(
                applicationContext,
                app.repository.getReminderHour(),
                app.repository.getReminderMinute(),
            )
        }
        setContent {
            MaterialTheme {
                var screen by remember { mutableStateOf(Screen.Today) }
                BackHandler(enabled = screen != Screen.Today) {
                    screen = Screen.Today
                }
                when (screen) {
                    Screen.Today -> {
                        val vm: MainViewModel = viewModel { MainViewModel(app.repository) }
                        val state by vm.state.collectAsState()
                        TodayScreen(
                            state = state,
                            onAdd = vm::addEntry,
                            onEdit = vm::editEntry,
                            onDelete = vm::deleteEntry,
                            onComplete = vm::completeToday,
                            onUndo = vm::undoToday,
                            onReset = vm::reset,
                            onShowHistory = { screen = Screen.History },
                            onShowSettings = { screen = Screen.Settings },
                        )
                    }
                    Screen.History -> {
                        val historyVm: HistoryViewModel = viewModel { HistoryViewModel(app.repository) }
                        HistoryScreen(vm = historyVm, onBack = { screen = Screen.Today })
                    }
                    Screen.Settings -> {
                        val settingsVm: SettingsViewModel = viewModel { SettingsViewModel(app, app.repository) }
                        SettingsScreen(vm = settingsVm, onBack = { screen = Screen.Today })
                    }
                }
            }
        }
    }
}

private enum class Screen { Today, History, Settings }

@Composable
fun TodayScreen(
    state: TodayState?,
    onAdd: (String) -> Unit,
    onEdit: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingText by remember { mutableStateOf("") }

    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("连续天数", style = MaterialTheme.typography.labelMedium)
                Text("${state.streak}", style = MaterialTheme.typography.headlineLarge)
                if (state.isBroken) Text("已断链", color = MaterialTheme.colorScheme.error)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.today.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onShowHistory) { Text("历史") }
                    TextButton(onClick = onShowSettings) { Text("设置") }
                }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isCompleted,
                        label = { Text("我今天做了什么…") },
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(input); input = "" },
                        enabled = input.isNotBlank() && !state.isCompleted,
                    ) {
                        Text("添加")
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (state.isBroken) {
                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("清除连续天数，重新开始")
                    }
                } else if (state.isCompleted) {
                    OutlinedButton(onClick = onUndo) { Text("撤销完成") }
                } else {
                    Button(onClick = onComplete) { Text("完成今天 ✓") }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(state.entries, key = { it.id }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isCompleted) {
                            editingId = entry.id
                            editingText = entry.text
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(entry.text, Modifier.weight(1f))
                    IconButton(onClick = { onDelete(entry.id) }, enabled = !state.isCompleted) { Text("✕") }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("确认重置？") },
            text = { Text("连续天数将归零，历史保留为只读。") },
            confirmButton = { TextButton(onClick = { showResetConfirm = false; onReset() }) { Text("重置") } },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("取消") } },
        )
    }

    if (editingId != null) {
        AlertDialog(
            onDismissRequest = { editingId = null },
            title = { Text("编辑条目") },
            text = { OutlinedTextField(value = editingText, onValueChange = { editingText = it }) },
            confirmButton = {
                TextButton(onClick = { onEdit(editingId!!, editingText); editingId = null }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingId = null }) { Text("取消") } },
        )
    }
}
