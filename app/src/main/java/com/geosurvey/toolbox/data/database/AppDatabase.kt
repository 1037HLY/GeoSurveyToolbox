package com.geosurvey.toolbox.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geosurvey.toolbox.data.model.TrackPointEntity

@Database(
    entities = [
        TrackPointEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
