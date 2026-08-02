package com.geosurvey.toolbox.data.repository

import com.geosurvey.toolbox.data.database.TrackPointDao
import com.geosurvey.toolbox.data.model.TrackPointEntity
import kotlinx.coroutines.flow.Flow

class TrackRepository(
    private val trackPointDao: TrackPointDao
) {
    suspend fun insertTrackPoint(point: TrackPointEntity) {
        trackPointDao.insertTrackPoint(point)
    }
    
    suspend fun insertTrackPoints(points: List<TrackPointEntity>) {
        trackPointDao.insertTrackPoints(points)
    }
    
    fun getAllTrackPoints(): Flow<List<TrackPointEntity>> {
        return trackPointDao.getAllTrackPoints()
    }
    
    fun getTrackPointsBetween(startTime: Long, endTime: Long): Flow<List<TrackPointEntity>> {
        return trackPointDao.getTrackPointsBetween(startTime, endTime)
    }
    
    fun getTrackPointsByDate(date: String): Flow<List<TrackPointEntity>> {
        return trackPointDao.getTrackPointsByDate(date)
    }
    
    fun getAvailableDates(): Flow<List<String>> {
        return trackPointDao.getAvailableDates()
    }
    
    suspend fun deleteAllTrackPoints() {
        trackPointDao.deleteAllTrackPoints()
    }
    
    fun getTrackPointCount(): Flow<Int> {
        return trackPointDao.getTrackPointCount()
    }
}
