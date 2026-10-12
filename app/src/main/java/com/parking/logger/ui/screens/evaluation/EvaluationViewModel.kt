package com.parking.logger.ui.screens.evaluation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parking.logger.data.model.ParkingRecord
import com.parking.logger.data.model.ParkingType
import com.parking.logger.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EvaluationViewModel(
    private val repository: ParkingRepository,
    private val sessionId: Long,
    private val parkingTypeName: String,
    private val conditionName: String,
    private val boundaryName: String
) : ViewModel() {

    private val _forwardCount = MutableStateFlow("")
    val forwardCount: StateFlow<String> = _forwardCount

    private val _backwardCount = MutableStateFlow("")
    val backwardCount: StateFlow<String> = _backwardCount

    private val _count3 = MutableStateFlow("")
    val count3: StateFlow<String> = _count3

    private val _count4 = MutableStateFlow("")
    val count4: StateFlow<String> = _count4

    private val _count5 = MutableStateFlow("")
    val count5: StateFlow<String> = _count5

    private val _count6 = MutableStateFlow("")
    val count6: StateFlow<String> = _count6

    private val _isIO = MutableStateFlow(true)
    val isIO: StateFlow<Boolean> = _isIO

    private val _selectedIssues = MutableStateFlow(setOf<String>())
    val selectedIssues: StateFlow<Set<String>> = _selectedIssues

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private val _photoPath = MutableStateFlow("")
    val photoPath: StateFlow<String> = _photoPath

    private val _audioPath = MutableStateFlow("")
    val audioPath: StateFlow<String> = _audioPath

    private val _savedRecordId = MutableStateFlow<Long?>(null)
    val savedRecordId: StateFlow<Long?> = _savedRecordId

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val needsPullOut = parkingTypeName == ParkingType.FRONT_IN.name ||
            parkingTypeName == ParkingType.SIDE_PARKING.name

    fun updateForwardCount(v: String) { _forwardCount.value = v }
    fun updateBackwardCount(v: String) { _backwardCount.value = v }
    fun updateCount3(v: String) { _count3.value = v }
    fun updateCount4(v: String) { _count4.value = v }
    fun updateCount5(v: String) { _count5.value = v }
    fun updateCount6(v: String) { _count6.value = v }

    fun setIsIO(value: Boolean) { _isIO.value = value }

    fun toggleIssue(issue: String) {
        _selectedIssues.value = if (issue in _selectedIssues.value)
            _selectedIssues.value - issue
        else
            _selectedIssues.value + issue
    }

    fun updateNote(v: String) { _note.value = v }
    fun setPhotoPath(v: String) { _photoPath.value = v }
    fun setAudioPath(v: String) { _audioPath.value = v }

    fun save() {
        val forward = _forwardCount.value.toIntOrNull() ?: 0
        val backward = _backwardCount.value.toIntOrNull() ?: 0
        val c3 = _count3.value.toIntOrNull() ?: 0
        val c4 = _count4.value.toIntOrNull() ?: 0
        val c5 = _count5.value.toIntOrNull() ?: 0
        val c6 = _count6.value.toIntOrNull() ?: 0

        if (forward <= 0) {
            _error.value = "请填写有效的泊入把数（前进）"
            return
        }

        viewModelScope.launch {
            val record = ParkingRecord(
                sessionId = sessionId,
                parkingType = parkingTypeName,
                condition = conditionName,
                boundary = boundaryName,
                forwardCount = forward,
                backwardCount = backward,
                count3 = c3,
                count4 = c4,
                count5 = c5,
                count6 = c6,
                isIO = _isIO.value,
                ioIssues = if (_isIO.value) "" else _selectedIssues.value.joinToString(","),
                note = _note.value,
                photoPath = _photoPath.value,
                audioPath = _audioPath.value
            )
            val id = repository.saveRecord(record)
            _savedRecordId.value = id
        }
    }

    fun clearError() { _error.value = null }

    companion object {
        val ISSUES = listOf(
            "Skew Angle",
            "PEP Outside",
            "Too Many Strokes",
            "离Wheel Stopper太远",
            "功能中断",
            "碰撞（风险）",
            "车辆纵向控制"
        )
    }
}
