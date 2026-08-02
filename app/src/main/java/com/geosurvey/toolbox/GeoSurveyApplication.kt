package com.geosurvey.toolbox

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Environment
import androidx.room.Room
import com.geosurvey.toolbox.data.database.AppDatabase
import com.geosurvey.toolbox.data.repository.TrackRepository
import org.osmdroid.config.Configuration
import java.io.File

class GeoSurveyApplication : Application() {
    
    companion object {
        lateinit var instance: GeoSurveyApplication
            private set
        
        const val NOTIFICATION_CHANNEL_ID = "geo_survey_channel"
        const val NOTIFICATION_CHANNEL_NAME = "地质勘查工具箱"
        const val NOTIFICATION_ID = 1001
    }
    
    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "geo_survey_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    val trackRepository by lazy {
        TrackRepository(
            trackPointDao = database.trackPointDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        initOsmdroid()
    }
    
    private fun initOsmdroid() {
        try {
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = packageName
            
            val externalStorage = Environment.getExternalStorageDirectory()
            val osmdroidDir = File(externalStorage, "osmdroid")
            if (!osmdroidDir.exists()) {
                osmdroidDir.mkdirs()
            }
            
            val offlineDir = File(osmdroidDir, "offline")
            if (!offlineDir.exists()) {
                offlineDir.mkdirs()
            }
            
            Configuration.getInstance().osmdroidBasePath = osmdroidDir
            Configuration.getInstance().osmdroidTileCache = File(osmdroidDir, "tiles")
            Configuration.getInstance().isMapViewHardwareAccelerated = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
