package com.dailyrecord.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyrecord.app.data.RecordRepository
import com.dailyrecord.app.data.TodayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class MainViewModel(private val repository: RecordRepository) : ViewModel() {

    private val _state = MutableStateFlow<TodayState?>(null)
    val state: StateFlow<TodayState?> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = repository.loadToday(Instant.now())
        }
    }

    fun addEntry(text: String) = act { repository.addEntry(Instant.now(), text) }
    fun editEntry(id: Long, text: String) = act { repository.editEntry(Instant.now(), id, text) }
    fun deleteEntry(id: Long) = act { repository.deleteEntry(Instant.now(), id) }
    fun completeToday() = act { repository.completeToday(Instant.now()) }
    fun undoToday() = act { repository.undoToday(Instant.now()) }
    fun reset() = act { repository.reset(Instant.now()) }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            refresh()
        }
    }
}
