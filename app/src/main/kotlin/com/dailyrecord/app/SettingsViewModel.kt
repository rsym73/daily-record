package com.dailyrecord.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyrecord.app.data.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val repository: RecordRepository,
) : AndroidViewModel(application) {

    private val _hour = MutableStateFlow(0)
    val hour: StateFlow<Int> = _hour

    private val _minute = MutableStateFlow(30)
    val minute: StateFlow<Int> = _minute

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init {
        viewModelScope.launch {
            _hour.value = repository.getReminderHour()
            _minute.value = repository.getReminderMinute()
        }
    }

    fun save(hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.setReminderTime(hour, minute)
            _hour.value = hour
            _minute.value = minute
            _saved.value = true
            // 关键：保存后立即按新时间重新排定提醒
            ReminderScheduler.schedule(getApplication(), hour, minute)
        }
    }
}
