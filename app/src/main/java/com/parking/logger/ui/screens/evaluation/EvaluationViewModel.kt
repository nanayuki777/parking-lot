package com.parking.logger.ui.screens.evaluation

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
    private val boundaryName: String,
    private val totalCount: Int
) : ViewModel() {

    private val _isIO = MutableStateFlow(true)
    val isIO: StateFlow<Boolean> = _isIO

    private val _selectedIssues = MutableStateFlow(setOf<String>())
    val selectedIssues: StateFlow<Set<String>> = _selectedIssues

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private val _distanceBL = MutableStateFlow("")
    val distanceBL: StateFlow<String> = _distanceBL
    private val _distanceBack = MutableStateFlow("")
    val distanceBack: StateFlow<String> = _distanceBack
    private val _distanceBR = MutableStateFlow("")
    val distanceBR: StateFlow<String> = _distanceBR
    private val _distanceFR = MutableStateFlow("")
    val distanceFR: StateFlow<String> = _distanceFR
    private val _distanceFront = MutableStateFlow("")
    val distanceFront: StateFlow<String> = _distanceFront
    private val _distanceFL = MutableStateFlow("")
    val distanceFL: StateFlow<String> = _distanceFL

    private val _photoPath = MutableStateFlow("")
    val photoPath: StateFlow<String> = _photoPath

    private val _audioPath = MutableStateFlow("")
    val audioPath: StateFlow<String> = _audioPath

    private val _savedRecordId = MutableStateFlow<Long?>(null)
    val savedRecordId: StateFlow<Long?> = _savedRecordId

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val needsPullOut = parkingTypeName == ParkingType.FRONT_IN.name ||
            parkingTypeName == ParkingType.SIDE_PARKING.name ||
            parkingTypeName == ParkingType.FISHBONE.name

    val parkingTypeLabel: String = runCatching {
        ParkingType.valueOf(parkingTypeName).label
    }.getOrDefault(parkingTypeName)

    val issueLabel: String = runCatching {
        ParkingType.valueOf(parkingTypeName).issueLabel
    }.getOrDefault(parkingTypeName)

    private val maneuver: String = if (parkingTypeName == ParkingType.FISHBONE.name) "Park-out" else "Park-in"

    fun setIsIO(value: Boolean) { _isIO.value = value }

    fun toggleIssue(issue: String) {
        _selectedIssues.value = if (issue in _selectedIssues.value)
            _selectedIssues.value - issue
        else
            _selectedIssues.value + issue
    }

    fun updateNote(v: String) { _note.value = v }
    fun updateDistanceBL(v: String) { _distanceBL.value = v }
    fun updateDistanceBack(v: String) { _distanceBack.value = v }
    fun updateDistanceBR(v: String) { _distanceBR.value = v }
    fun updateDistanceFR(v: String) { _distanceFR.value = v }
    fun updateDistanceFront(v: String) { _distanceFront.value = v }
    fun updateDistanceFL(v: String) { _distanceFL.value = v }
    fun setPhotoPath(v: String) { _photoPath.value = v }
    fun setAudioPath(v: String) { _audioPath.value = v }

    fun save() {
        viewModelScope.launch {
            val record = ParkingRecord(
                sessionId = sessionId,
                issue = issueLabel,
                maneuver = maneuver,
                parkingType = parkingTypeName,
                condition = conditionName,
                boundary = boundaryName,
                totalCount = totalCount,
                isIO = _isIO.value,
                ioIssues = if (_isIO.value) "" else _selectedIssues.value.joinToString(","),
                note = _note.value,
                skewAngle = 0f,
                distanceBL = _distanceBL.value.toFloatOrNull() ?: 0f,
                distanceBack = _distanceBack.value.toFloatOrNull() ?: 0f,
                distanceBR = _distanceBR.value.toFloatOrNull() ?: 0f,
                distanceFR = _distanceFR.value.toFloatOrNull() ?: 0f,
                distanceFront = _distanceFront.value.toFloatOrNull() ?: 0f,
                distanceFL = _distanceFL.value.toFloatOrNull() ?: 0f,
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
            "Abort",
            "Skew angle",
            "too many strokes",
            "far away WS",
            "PEP outside",
            "jerk brake",
            "no offer",
            "others"
        )
    }
}
