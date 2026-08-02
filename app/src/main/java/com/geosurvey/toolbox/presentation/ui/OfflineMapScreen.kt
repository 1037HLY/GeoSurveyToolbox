package com.geosurvey.toolbox.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.IArchiveFile
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import com.geosurvey.toolbox.GeoSurveyApplication
import com.geosurvey.toolbox.presentation.viewmodel.TrackViewModel
import java.io.File

@Composable
fun OfflineMapScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val trackViewModel = remember { TrackViewModel.getInstance(application) }
    
    val trackPoints by trackViewModel.trackPoints.collectAsState()
    
    // 地图View
    val mapView = remember {
        // 初始化Osmdroid配置
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        
        MapView(context).apply {
            // 设置地图源
            setTileSource(TileSourceFactory.MAPNIK)
            
            // 设置缩放控制
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
            
            // 禁用网络，强制使用离线
            setUseDataConnection(false)
            
            // ⭐ 关键：加载离线地图文件
            try {
                val offlinePath = File(
                    Environment.getExternalStorageDirectory(),
                    "osmdroid/offline"
                )
                
                if (offlinePath.exists() && offlinePath.isDirectory) {
                    val archives = mutableListOf<IArchiveFile>()
                    
                    offlinePath.listFiles()?.forEach { file ->
                        if (file.extension.equals("zip", ignoreCase = true)) {
                            try {
                                val archive = IArchiveFile.getArchiveFile(file)
                                archive?.let { archives.add(it) }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    
                    if (archives.isNotEmpty()) {
                        val tileProvider = OfflineTileProvider(archives)
                        val tilesOverlay = TilesOverlay(tileProvider, context)
                        overlays.add(tilesOverlay)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 启用当前位置
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isTilesScaledToDpi = true
            }
            
            // 默认缩放
            controller.setZoom(14.0)
        }
    }
    
    // 绘制轨迹
    LaunchedEffect(trackPoints) {
        val map = mapView
        
        // 清除旧的覆盖层（保留第一个离线地图覆盖层）
        val overlaysToKeep = mutableListOf<Any>()
        if (map.overlays.isNotEmpty()) {
            // 保留第一个（离线地图覆盖层）
            overlaysToKeep.add(map.overlays[0])
        }
        map.overlays.clear()
        overlaysToKeep.forEach { map.overlays.add(it as org.osmdroid.views.overlay.Overlay) }
        
        if (trackPoints.isEmpty()) {
            map.invalidate()
            return@LaunchedEffect
        }
        
        // 绘制轨迹线
        if (trackPoints.size > 1) {
            val points = trackPoints.map { 
                GeoPoint(it.latitude, it.longitude)
            }
            
            val polyline = Polyline().apply {
                setPoints(points)
                width = 8f
                color = 0xFF0EA5E9.toInt()
            }
            map.overlays.add(polyline)
            
            // 添加起点标记（绿色）
            val startPoint = trackPoints.first()
            val startMarker = Marker(map).apply {
                position = GeoPoint(startPoint.latitude, startPoint.longitude)
                title = "起点"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // 使用默认颜色，绿色起点
            }
            map.overlays.add(startMarker)
            
            // 添加终点标记（红色）
            val endPoint = trackPoints.last()
            val endMarker = Marker(map).apply {
                position = GeoPoint(endPoint.latitude, endPoint.longitude)
                title = "终点"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(endMarker)
            
            // 定位到起点或所有点中心
            if (points.size > 1) {
                val centerLat = points.map { it.latitude }.average()
                val centerLon = points.map { it.longitude }.average()
                val centerGeo = GeoPoint(centerLat, centerLon)
                map.controller.setZoom(15.0)
                map.controller.animateTo(centerGeo)
            } else {
                map.controller.setZoom(16.0)
                map.controller.animateTo(points.first())
            }
        } else if (trackPoints.size == 1) {
            val point = trackPoints.first()
            val geoPoint = GeoPoint(point.latitude, point.longitude)
            map.controller.setZoom(16.0)
            map.controller.animateTo(geoPoint)
        }
        
        map.invalidate()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 标题栏 - 显示离线状态
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.85f)
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗺️ 离线地图",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Row {
                    Text(
                        text = "${trackPoints.size} 个点",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 离线状态指示
                    Text(
                        text = "●",
                        fontSize = 12.sp,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
        
        // 地图
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
        
        // 底部信息 - 显示轨迹统计
        if (trackPoints.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.85f)
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🟢 起点", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(
                            trackPoints.first().let {
                                String.format("%.5f, %.5f", it.latitude, it.longitude)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔴 终点", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(
                            trackPoints.last().let {
                                String.format("%.5f, %.5f", it.latitude, it.longitude)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊 点数", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("${trackPoints.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // 没有轨迹点时显示提示
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.85f)
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "💡 暂无轨迹数据，请先记录轨迹",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
