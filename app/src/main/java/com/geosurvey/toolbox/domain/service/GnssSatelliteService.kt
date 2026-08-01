package com.geosurvey.toolbox.domain.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

data class SatelliteInfo(
    val totalCount: Int = 0,
    val gpsCount: Int = 0,
    val glonassCount: Int = 0,
    val beidouCount: Int = 0,
    val galileoCount: Int = 0,
    val usedCount: Int = 0,
    val averageSnr: Float = 0f
)

class GnssSatelliteService(
    private val context: Context
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val satelliteChannel = Channel<SatelliteInfo>(Channel.BUFFERED)
    val satelliteFlow: Flow<SatelliteInfo> = satelliteChannel.receiveAsFlow()
    
    private var isRunning = false
    
    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val info = parseSatelliteStatus(status)
            satelliteChannel.trySend(info)
        }
    }
    
    fun startUpdates() {
        if (isRunning) return
        if (!hasLocationPermission()) return
        
        isRunning = true
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.registerGnssStatusCallback(
                gnssStatusCallback,
                null
            )
        }
    }
    
    fun stopUpdates() {
        isRunning = false
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
    }
    
    private fun parseSatelliteStatus(status: GnssStatus): SatelliteInfo {
        var gpsCount = 0
        var glonassCount = 0
        var beidouCount = 0
        var galileoCount = 0
        var usedCount = 0
        var totalSnr = 0f
        var totalCount = 0
        
        for (i in 0 until status.satelliteCount) {
            val constellation = status.getConstellationType(i)
            val snr = status.getCn0DbHz(i)
            val used = status.usedInFix(i)
            
            when (constellation) {
                GnssStatus.CONSTELLATION_GPS -> gpsCount++
                GnssStatus.CONSTELLATION_GLONASS -> glonassCount++
                GnssStatus.CONSTELLATION_BEIDOU -> beidouCount++
                GnssStatus.CONSTELLATION_GALILEO -> galileoCount++
            }
            
            if (used) usedCount++
            if (snr > 0) {
                totalSnr += snr
                totalCount++
            }
        }
        
        val avgSnr = if (totalCount > 0) totalSnr / totalCount else 0f
        
        return SatelliteInfo(
            totalCount = status.satelliteCount,
            gpsCount = gpsCount,
            glonassCount = glonassCount,
            beidouCount = beidouCount,
            galileoCount = galileoCount,
            usedCount = usedCount,
            averageSnr = avgSnr
        )
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
