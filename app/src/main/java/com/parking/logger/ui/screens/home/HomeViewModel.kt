package com.parking.logger.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parking.logger.data.model.Tester
import com.parking.logger.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ParkingRepository
) : ViewModel() {

    val testers: StateFlow<List<Tester>> = repository.allTesters
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedTesterId = MutableStateFlow<Long?>(null)
    val selectedTesterId: StateFlow<Long?> = _selectedTesterId

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location

    private val _startedSessionId = MutableStateFlow<Long?>(null)
    val startedSessionId: StateFlow<Long?> = _startedSessionId

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun selectTester(id: Long) {
        _selectedTesterId.value = id
    }

    fun updateLocation(value: String) {
        _location.value = value
    }

    fun startSession() {
        val testerId = _selectedTesterId.value
        val loc = _location.value.trim()
        if (testerId == null) {
            _error.value = "请选择测试人员"
            return
        }
        if (loc.isEmpty()) {
            _error.value = "请输入测试地点"
            return
        }
        viewModelScope.launch {
            val sessionId = repository.createSession(testerId, loc)
            _startedSessionId.value = sessionId
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun onNavigatedToSession() {
        _startedSessionId.value = null
    }
}
