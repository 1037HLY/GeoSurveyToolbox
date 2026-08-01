package com.geosurvey.toolbox.presentation.viewmodel

import android.content.Context
import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.toolbox.domain.service.GnssLocationService
import com.geosurvey.toolbox.domain.service.GnssSatelliteService
import com.geosurvey.toolbox.domain.service.SatelliteInfo
import com.geosurvey.toolbox.presentation.ui.LocationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LocationViewModel(
    private val context: Context
) : ViewModel() {
    
    private val locationService = GnssLocationService(context)
    private val satelliteService = GnssSatelliteService(context)
    
    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()
    
    init {
        // 监听位置更新
        locationService.locationFlow
            .onEach { location ->
                updateLocation(location)
            }
            .launchIn(viewModelScope)
        
        // 监听卫星状态更新
        satelliteService.satelliteFlow
            .onEach { satelliteInfo ->
                updateSatelliteInfo(satelliteInfo)
            }
            .launchIn(viewModelScope)
    }
    
    fun startLocationUpdates() {
        viewModelScope.launch {
            _locationState.value = _locationState.value.copy(
                isActive = true
            )
            locationService.startUpdates()
            satelliteService.startUpdates()
        }
    }
    
    fun stopLocationUpdates() {
        viewModelScope.launch {
            _locationState.value = _locationState.value.copy(
                isActive = false
            )
            locationService.stopUpdates()
            satelliteService.stopUpdates()
        }
    }
    
    private fun updateLocation(location: Location) {
        val currentState = _locationState.value
        _locationState.value = currentState.copy(
            currentLocation = location
        )
    }
    
    private fun updateSatelliteInfo(satelliteInfo: SatelliteInfo) {
        val currentState = _locationState.value
        val satCount = satelliteInfo.totalCount
        
        _locationState.value = currentState.copy(
            satelliteCount = satCount,
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
            gpsCount = satelliteInfo.gpsCount,
            glonassCount = satelliteInfo.glonassCount,
            beidouCount = satelliteInfo.beidouCount,
            galileoCount = satelliteInfo.galileoCount
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        locationService.stopUpdates()
        satelliteService.stopUpdates()
    }
}
