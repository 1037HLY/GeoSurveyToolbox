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
    
    // ⭐ 轨迹优化参数
    private var lastLocation: Location? = null
    private var isStatic = false
    private var staticCounter = 0
    
    // 可调参数
    companion object {
        private const val MIN_ACCURACY = 15f              // 最小精度要求（米），大于此值丢弃
        private const val MIN_DISTANCE = 2.0              // 最小移动距离（米），小于此值丢弃
        private const val STATIC_THRESHOLD = 3            // 连续3次定位在阈值内判定为静止
        private const val STATIC_DISTANCE_THRESHOLD = 1.0 // 静止判定距离阈值（米）
        private const val MAX_SPEED_THRESHOLD = 0.5       // 速度小于此值视为静止
    }
    
    init {
        loadTrackPoints()
        loadAvailableDates()
    }
    
    fun startRecording() {
        _isRecording.value = true
        // 重置状态
        lastLocation = null
        isStatic = false
        staticCounter = 0
    }
    
    fun stopRecording() {
        _isRecording.value = false
    }
    
    /**
     * ⭐ 优化的轨迹点添加方法
     * 包含：精度过滤、静止检测、距离过滤
     */
    fun addTrackPoint(location: Location) {
        // 如果不在记录状态，不处理
        if (!_isRecording.value) return
        
        // 1. 精度过滤：精度太差则丢弃
        if (location.accuracy != null && location.accuracy > MIN_ACCURACY) {
            return
        }
        
        // 2. 速度过滤：速度小于阈值视为静止（不记录）
        if (location.speed != null && location.speed < MAX_SPEED_THRESHOLD) {
            // 静止状态，检查是否已经保存过静止点
            if (lastLocation != null) {
                val distance = lastLocation!!.distanceTo(location)
                // 如果距离小于静止阈值，不记录
                if (distance < STATIC_DISTANCE_THRESHOLD) {
                    return
                }
            }
        }
        
        // 3. 距离过滤：与上一个点距离太近则丢弃（防止漂移）
        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location)
            if (distance < MIN_DISTANCE) {
                return
            }
        }
        
        // 4. 通过所有过滤，保存轨迹点
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
            lastLocation = location  // 更新上一个点
            loadTrackPoints()
            loadAvailableDates()
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
