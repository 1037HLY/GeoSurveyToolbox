package com.geosurvey.toolbox.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.geosurvey.toolbox.GeoSurveyApplication
import com.geosurvey.toolbox.presentation.viewmodel.TrackViewModel

@Composable
fun OfflineMapScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val trackViewModel = remember { TrackViewModel.getInstance(application) }
    
    val trackPoints by trackViewModel.trackPoints.collectAsState()
    
    // 地图View
    val mapView = remember {
        // 初始化Osmdroid
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
            
            // 启用当前位置
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isTilesScaledToDpi = true
            }
            
            // 默认缩放
            getController().setZoom(14.0)
        }
    }
    
    // 绘制轨迹
    LaunchedEffect(trackPoints) {
        val map = mapView
        
        // 清除所有覆盖层
        map.overlays.clear()
        
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
            
            // 起点标记（绿色）
            val startPoint = trackPoints.first()
            val startMarker = Marker(map).apply {
                position = GeoPoint(startPoint.latitude, startPoint.longitude)
                title = "起点"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(startMarker)
            
            // 终点标记（红色）
            val endPoint = trackPoints.last()
            val endMarker = Marker(map).apply {
                position = GeoPoint(endPoint.latitude, endPoint.longitude)
                title = "终点"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(endMarker)
            
            // 居中显示所有点
            val centerLat = points.map { it.latitude }.average()
            val centerLon = points.map { it.longitude }.average()
            map.getController().setZoom(15.0)
            map.getController().animateTo(GeoPoint(centerLat, centerLon))
            
        } else if (trackPoints.size == 1) {
            val point = trackPoints.first()
            map.getController().setZoom(16.0)
            map.getController().animateTo(GeoPoint(point.latitude, point.longitude))
        }
        
        map.invalidate()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 标题栏
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
                    text = "🗺️ 轨迹地图",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${trackPoints.size} 个点",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            }
        }
        
        // 地图
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
        
        // 底部信息
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
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔴 终点", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(
                            trackPoints.last().let {
                                String.format("%.5f, %.5f", it.latitude, it.longitude)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊 点数", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("${trackPoints.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
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
