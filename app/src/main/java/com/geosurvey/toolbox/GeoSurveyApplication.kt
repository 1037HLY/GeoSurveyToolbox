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
    val trackRepository by lazy {
        TrackRepository(
            trackPointDao = database.trackPointDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        
        // ⭐ 初始化Osmdroid离线地图配置
        initOsmdroid()
    }
    
    /**
     * 初始化Osmdroid离线地图
     * 设置离线地图目录
     */
    private fun initOsmdroid() {
        try {
            // 加载Osmdroid配置
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = packageName
            
            // 获取外部存储目录
            val externalStorage = Environment.getExternalStorageDirectory()
            
            // 创建osmdroid目录
            val osmdroidDir = File(externalStorage, "osmdroid")
            if (!osmdroidDir.exists()) {
                osmdroidDir.mkdirs()
            }
            
            // 创建offline目录（存放离线地图ZIP文件）
            val offlineDir = File(osmdroidDir, "offline")
            if (!offlineDir.exists()) {
                offlineDir.mkdirs()
            }
            
            // 设置Osmdroid基础路径
            Configuration.getInstance().osmdroidBasePath = osmdroidDir
            Configuration.getInstance().osmdroidTileCache = File(osmdroidDir, "tiles")
            
            // 启用硬件加速
            Configuration.getInstance().isMapViewHardwareAccelerated = true
            
            // 日志输出（方便调试）
            android.util.Log.d("GeoSurveyApp", "Osmdroid初始化成功")
            android.util.Log.d("GeoSurveyApp", "离线地图目录: ${offlineDir.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("GeoSurveyApp", "Osmdroid初始化失败: ${e.message}")
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
