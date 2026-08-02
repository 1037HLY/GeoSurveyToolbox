package com.geosurvey.toolbox.presentation.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.toolbox.data.model.TrackPointEntity
import com.geosurvey.toolbox.data.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackViewModel(
    private val trackRepository: TrackRepository
) : ViewModel() {
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _trackPoints = MutableStateFlow<List<TrackPointEntity>>(emptyList())
    val trackPoints: StateFlow<List<TrackPointEntity>> = _trackPoints.asStateFlow()
    
    private val _pointCount = MutableStateFlow(0)
    val pointCount: StateFlow<Int> = _pointCount.asStateFlow()
    
    private val _availableDates = MutableStateFlow<List<String>>(emptyList())
    val availableDates: StateFlow<List<String>> = _availableDates.asStateFlow()
    
    init {
        loadTrackPoints()
        loadAvailableDates()
    }
    
    fun startRecording() {
        _isRecording.value = true
    }
    
    fun stopRecording() {
        _isRecording.value = false
    }
    
    // 🔥 关键方法：添加轨迹点
    fun addTrackPoint(location: Location) {
        // 检查是否正在记录
        if (!_isRecording.value) return
        
        viewModelScope.launch {
            val point = TrackPointEntity(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = location.speed,
                bearing = location.bearing,
                accuracy = location.accuracy,
                timestamp = System.currentTimeMillis()
            )
            trackRepository.insertTrackPoint(point)
            loadTrackPoints()
        }
    }
    
    fun loadTrackPoints() {
        viewModelScope.launch {
            trackRepository.getAllTrackPoints().collect { points ->
                _trackPoints.value = points
                _pointCount.value = points.size
            }
        }
    }
    
    fun loadAvailableDates() {
        viewModelScope.launch {
            trackRepository.getAvailableDates().collect { dates ->
                _availableDates.value = dates
            }
        }
    }
    
    fun deleteAllTrackPoints() {
        viewModelScope.launch {
            trackRepository.deleteAllTrackPoints()
            loadTrackPoints()
            loadAvailableDates()
        }
    }
}
