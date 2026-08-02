package com.geosurvey.toolbox.presentation.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.toolbox.GeoSurveyApplication
import com.geosurvey.toolbox.data.model.TrackPointEntity
import com.geosurvey.toolbox.data.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    companion object {
        @Volatile
        private var INSTANCE: TrackViewModel? = null
        
        fun getInstance(application: Application): TrackViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrackViewModel(application).also { INSTANCE = it }
            }
        }
    }
    
    private val trackRepository: TrackRepository = (application as GeoSurveyApplication).trackRepository
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _trackPoints = MutableStateFlow<List<TrackPointEntity>>(emptyList())
    val trackPoints: StateFlow<List<TrackPointEntity>> = _trackPoints.asStateFlow()
    
    private val _pointCount = MutableStateFlow(0)
    val pointCount: StateFlow<Int> = _pointCount.asStateFlow()
    
    private val _availableDates = MutableStateFlow<List<String>>(emptyList())
    val availableDates: StateFlow<List<String>> = _availableDates.asStateFlow()
    
    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()
    
    private val _filteredPoints = MutableStateFlow<List<TrackPointEntity>>(emptyList())
    val filteredPoints: StateFlow<List<TrackPointEntity>> = _filteredPoints.asStateFlow()
    
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
    
    fun addTrackPoint(location: Location) {
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
            loadAvailableDates()
            // 如果有选中的日期，刷新筛选
            _selectedDate.value?.let { filterByDate(it) }
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
    
    fun filterByDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            trackRepository.getTrackPointsByDate(date).collect { points ->
                _filteredPoints.value = points
            }
        }
    }
    
    fun clearDateFilter() {
        _selectedDate.value = null
        _filteredPoints.value = emptyList()
    }
    
    suspend fun deleteTrackPointsByDate(date: String): Boolean {
        return try {
            trackRepository.deleteTrackPointsByDate(date)
            loadTrackPoints()
            loadAvailableDates()
            if (_selectedDate.value == date) {
                clearDateFilter()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteAllTrackPoints(): Boolean {
        return try {
            trackRepository.deleteAllTrackPoints()
            loadTrackPoints()
            loadAvailableDates()
            clearDateFilter()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getDisplayPoints(): List<TrackPointEntity> {
        return if (_selectedDate.value != null) _filteredPoints.value else _trackPoints.value
    }
}
