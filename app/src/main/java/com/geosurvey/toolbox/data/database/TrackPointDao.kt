package com.geosurvey.toolbox.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.geosurvey.toolbox.data.model.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insertTrackPoint(point: TrackPointEntity)
    
    @Insert
    suspend fun insertTrackPoints(points: List<TrackPointEntity>)
    
    @Query("SELECT * FROM track_points ORDER BY timestamp DESC")
    fun getAllTrackPoints(): Flow<List<TrackPointEntity>>
    
    @Query("SELECT * FROM track_points WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getTrackPointsBetween(startTime: Long, endTime: Long): Flow<List<TrackPointEntity>>
    
    @Query("SELECT * FROM track_points WHERE date(timestamp / 1000, 'unixepoch') = :date ORDER BY timestamp ASC")
    fun getTrackPointsByDate(date: String): Flow<List<TrackPointEntity>>
    
    @Query("SELECT DISTINCT date(timestamp / 1000, 'unixepoch') as date FROM track_points ORDER BY date DESC")
    fun getAvailableDates(): Flow<List<String>>
    
    @Query("DELETE FROM track_points WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun deleteTrackPointsBetween(startTime: Long, endTime: Long)
    
    @Query("DELETE FROM track_points")
    suspend fun deleteAllTrackPoints()
    
    @Query("SELECT COUNT(*) FROM track_points")
    fun getTrackPointCount(): Flow<Int>
}
