package com.dailyrecord.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyrecord.app.data.TodayState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf(Screen.Today)

    private var pendingWallpaperUri by mutableStateOf<Uri?>(null)

    private val pickWallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> pendingWallpaperUri = uri }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val app = applicationContext as RecordApplication
            lifecycleScope.launch {
                try {
                    val json = app.backupService.exportData()
                    contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    Toast.makeText(this@MainActivity, "已导出", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val app = applicationContext as RecordApplication
            lifecycleScope.launch {
                try {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        app.backupService.importData(json)
                        Toast.makeText(this@MainActivity, "已导入", Toast.LENGTH_SHORT).show()
                        screen = Screen.Today
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val app = application as RecordApplication
        lifecycleScope.launch {
            ReminderAlarmScheduler.schedule(
                applicationContext,
                app.repository.getReminderHour(),
                app.repository.getReminderMinute(),
            )
        }
        setContent {
            MaterialTheme {
                val wallpaper by app.wallpaper.collectAsState()
                BackHandler(enabled = screen != Screen.Today) {
                    screen = Screen.Today
                }
                WallpaperBackground(file = wallpaper.file, generation = wallpaper.generation) {
                    when (screen) {
                        Screen.Today -> {
                            val vm: MainViewModel = viewModel { MainViewModel(app.repository) }
                            val state by vm.state.collectAsState()
                            LaunchedEffect(Unit) { vm.refresh() }
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
                            LaunchedEffect(Unit) { historyVm.refresh() }
                            HistoryScreen(vm = historyVm, onBack = { screen = Screen.Today })
                        }
                        Screen.Settings -> {
                            val settingsVm: SettingsViewModel = viewModel { SettingsViewModel(app, app.repository) }
                            LaunchedEffect(pendingWallpaperUri) {
                                val u = pendingWallpaperUri ?: return@LaunchedEffect
                                lifecycleScope.launch {
                                    contentResolver.openInputStream(u)?.use { app.wallpaperStore.save(it) }
                                    app.refreshWallpaper()
                                }
                                pendingWallpaperUri = null
                            }
                            SettingsScreen(
                                vm = settingsVm,
                                onBack = { screen = Screen.Today },
                                onExport = { exportLauncher.launch("daily-record-backup.json") },
                                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                hasWallpaper = wallpaper.file != null,
                                onPickWallpaper = { pickWallpaperLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                onRemoveWallpaper = { app.wallpaperStore.remove(); app.refreshWallpaper() },
                                onOpenAppSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) },
                            )
                        }
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
        containerColor = Color.Transparent,
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
                        enabled = !state.isCompleted && !state.isBroken,
                        label = { Text("我今天做了什么…") },
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(input); input = "" },
                        enabled = input.isNotBlank() && !state.isCompleted && !state.isBroken,
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
