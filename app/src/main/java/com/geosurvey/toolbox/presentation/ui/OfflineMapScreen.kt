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
    
    var offlineLoaded by remember { mutableStateOf(false) }
    var offlineMessage by remember { mutableStateOf("检查离线地图...") }
    
    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
            
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
                                IArchiveFile.getArchiveFile(file)?.let { archives.add(it) }
                            } catch (e: Exception) { }
                        }
                    }
                    
                    if (archives.isNotEmpty()) {
                        val tileProvider = OfflineTileProvider(archives)
                        val tilesOverlay = TilesOverlay(tileProvider, context)
                        overlays.add(tilesOverlay)
                        offlineLoaded = true
                        offlineMessage = "✅ 离线地图已加载 (${archives.size}个文件)"
                    } else {
                        offlineMessage = "⚠️ 未找到ZIP文件"
                    }
                } else {
                    offlineMessage = "⚠️ 目录不存在: /osmdroid/offline/"
                }
            } catch (e: Exception) {
                offlineMessage = "❌ 加载失败: ${e.message}"
            }
            
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isTilesScaledToDpi = true
            }
            
            controller.setZoom(14.0)
        }
    }
    
    LaunchedEffect(trackPoints) {
        val map = mapView
        val offlineOverlay = if (map.overlays.isNotEmpty()) map.overlays[0] else null
        
        map.overlays.clear()
        offlineOverlay?.let { map.overlays.add(it) }
        
        if (trackPoints.isEmpty()) {
            map.invalidate()
            return@LaunchedEffect
        }
        
        if (trackPoints.size > 1) {
            val points = trackPoints.map { GeoPoint(it.latitude, it.longitude) }
            
            Polyline().apply {
                setPoints(points)
                width = 8f
                color = 0xFF0EA5E9.toInt()
            }.let { map.overlays.add(it) }
            
            val startPoint = trackPoints.first()
            Marker(map).apply {
                position = GeoPoint(startPoint.latitude, startPoint.longitude)
                title = "起点"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }.let { map.overlays.add(it) }
            
            val endPoint = trackPoints.last()
            Marker(map).apply {
                position = GeoPoint(endPoint.latitude, endPoint.longitude)
                title = "终点"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }.let { map.overlays.add(it) }
            
            val centerLat = points.map { it.latitude }.average()
            val centerLon = points.map { it.longitude }.average()
            controller.setZoom(15.0)
            controller.animateTo(GeoPoint(centerLat, centerLon))
        } else if (trackPoints.size == 1) {
            val point = trackPoints.first()
            controller.setZoom(16.0)
            controller.animateTo(GeoPoint(point.latitude, point.longitude))
        }
        
        map.invalidate()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗺️ 离线地图", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${trackPoints.size} 个点", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(offlineMessage, fontSize = 10.sp, color = if (offlineLoaded) Color(0xFF10B981) else Color(0xFFF59E0B))
                }
            }
        }
        
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize().weight(1f))
        
        if (trackPoints.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🟢 起点", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(trackPoints.first().let { String.format("%.5f", it.latitude) }, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔴 终点", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(trackPoints.last().let { String.format("%.5f", it.latitude) }, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊 点数", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("${trackPoints.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    "💡 暂无轨迹数据，请先记录轨迹",
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
