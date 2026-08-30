package com.dailyrecord.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: HistoryViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()

    if (state.selectedDate != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.selectedDate.toString()) },
                    navigationIcon = { IconButton(onClick = { vm.clearSelection() }) { Text("←") } },
                )
            },
        ) { padding ->
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                if (state.selectedEntries.isEmpty()) {
                    item { Text("（当天没有条目）", Modifier.padding(16.dp)) }
                } else {
                    items(state.selectedEntries, key = { it.id }) { entry ->
                        Text(entry.text, Modifier.fillMaxWidth().padding(16.dp, 8.dp))
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("历史") },
                    navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
                )
            },
        ) { padding ->
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(state.days.reversed()) { day ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.select(day.date) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        val color = when {
                            day.completed -> MaterialTheme.colorScheme.primary
                            day.missed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(day.date.toString(), Modifier.weight(1f), color = color)
                        if (day.completed) Text("✓") else if (day.missed) Text("✗")
                    }
                }
            }
        }
    }
}
