package com.geosurvey.toolbox.presentation.viewmodel

import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.toolbox.presentation.ui.LocationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationViewModel : ViewModel() {
    
    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()
    
    fun startLocationUpdates() {
        viewModelScope.launch {
            _locationState.value = _locationState.value.copy(
                isActive = true
            )
            // TODO: 实现真实的GNSS定位
            // 目前模拟数据
            simulateLocation()
        }
    }
    
    fun stopLocationUpdates() {
        viewModelScope.launch {
            _locationState.value = _locationState.value.copy(
                isActive = false
            )
        }
    }
    
    private suspend fun simulateLocation() {
        // TODO: 替换为真实的GNSS定位实现
        // 目前模拟数据用于UI测试
        var lat = 39.9042
        var lng = 116.4074
        
        while (_locationState.value.isActive) {
            lat += 0.0001
            lng += 0.0001
            
            val location = Location("gps").apply {
                this.latitude = lat
                this.longitude = lng
                altitude = 50.0 + (Math.random() * 10)
                speed = (Math.random() * 5).toFloat()
                accuracy = 3.0f + (Math.random() * 5).toFloat()
            }
            
            val satCount = 8 + (Math.random() * 10).toInt()
            
            _locationState.value = _locationState.value.copy(
                currentLocation = location,
                satelliteCount = satCount,
                hdop = 0.8f + (Math.random() * 0.5).toFloat(),
                pdop = 1.2f + (Math.random() * 0.8).toFloat(),
                qualityText = when {
                    satCount > 15 -> "优秀"
                    satCount > 10 -> "良好"
                    satCount > 6 -> "一般"
                    else -> "较差"
                },
                qualityColor = when {
                    satCount > 15 -> Color(0xFF10B981)
                    satCount > 10 -> Color(0xFF0EA5E9)
                    satCount > 6 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                },
                gpsCount = 4 + (Math.random() * 6).toInt(),
                glonassCount = 2 + (Math.random() * 4).toInt(),
                beidouCount = 2 + (Math.random() * 4).toInt(),
                galileoCount = 1 + (Math.random() * 3).toInt()
            )
            
            delay(2000)
        }
    }
}
