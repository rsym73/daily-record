package com.dailyrecord.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyrecord.app.data.EntryEntity
import com.dailyrecord.app.data.HistoryDay
import com.dailyrecord.app.data.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class HistoryViewModel(private val repository: RecordRepository) : ViewModel() {

    data class HistoryState(
        val days: List<HistoryDay> = emptyList(),
        val selectedDate: LocalDate? = null,
        val selectedEntries: List<EntryEntity> = emptyList(),
    )

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val days = repository.loadHistory(Instant.now())
            _state.value = _state.value.copy(days = days)
        }
    }

    fun select(date: LocalDate) {
        viewModelScope.launch {
            val entries = repository.getEntriesForDay(date)
            _state.value = _state.value.copy(selectedDate = date, selectedEntries = entries)
        }
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedDate = null, selectedEntries = emptyList())
    }
}
