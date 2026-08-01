package com.geosurvey.toolbox.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "track_points")
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val accuracy: Float? = null,
    val satelliteCount: Int? = null,
    val hdop: Float? = null,
    val pdop: Float? = null,
    val timestamp: Date = Date(),
    val isProcessed: Boolean = false
)
