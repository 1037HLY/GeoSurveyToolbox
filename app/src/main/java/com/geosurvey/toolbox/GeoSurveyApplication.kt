package com.geosurvey.toolbox

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.geosurvey.toolbox.data.database.AppDatabase
import com.geosurvey.toolbox.data.repository.LocationRepository
import com.geosurvey.toolbox.domain.usecase.location.LocationUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GeoSurveyApplication : Application() {
    
    companion object {
        lateinit var instance: GeoSurveyApplication
            private set
        
        const val NOTIFICATION_CHANNEL_ID = "geo_survey_channel"
        const val NOTIFICATION_CHANNEL_NAME = "地质勘查工具箱"
        const val NOTIFICATION_ID = 1001
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Database
    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "geo_survey_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    // Repositories
    private val locationRepository by lazy {
        LocationRepository(
            context = this,
            database = database
        )
    }
    
    // Use Cases
    val locationUseCases by lazy {
        LocationUseCases(
            locationRepository = locationRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台定位服务通知"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
