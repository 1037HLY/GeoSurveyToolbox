package com.geosurvey.toolbox.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.geosurvey.toolbox.GeoSurveyApplication
import com.geosurvey.toolbox.data.model.TrackPointEntity
import com.geosurvey.toolbox.domain.service.LocationForegroundService
import com.geosurvey.toolbox.presentation.viewmodel.TrackViewModel
import com.geosurvey.toolbox.utils.TrackExportHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackScreen(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    
    val trackViewModel = remember { TrackViewModel.getInstance(application) }
    val isRecording by trackViewModel.isRecording.collectAsState()
    val trackPoints by trackViewModel.trackPoints.collectAsState()
    val pointCount by trackViewModel.pointCount.collectAsState()
    val availableDates by trackViewModel.availableDates.collectAsState()
    val selectedDate by trackViewModel.selectedDate.collectAsState()
    
    // 协程作用域
    val coroutineScope = rememberCoroutineScope()
    
    // 导出所有轨迹
    fun exportAllTracks() {
        if (trackPoints.isEmpty()) return
        val helper = TrackExportHelper(context)
        helper.exportToGPX(trackPoints, "all_tracks")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛣️ 轨迹记录",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            
            // 导出全部按钮
            if (trackPoints.isNotEmpty()) {
                IconButton(onClick = { exportAllTracks() }) {
                    Text("📤", fontSize = 20.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 统计卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("轨迹点", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("$pointCount", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("状态", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        if (isRecording) "● 记录中" else "○ 已停止",
                        color = if (isRecording) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("日期", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${availableDates.size}天", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 日期筛选行
        if (availableDates.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedDate ?: "📅 选择日期")
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部") },
                            onClick = {
                                trackViewModel.clearDateFilter()
                                expanded = false
                            }
                        )
                        availableDates.forEach { date ->
                            DropdownMenuItem(
                                text = { Text(date) },
                                onClick = {
                                    trackViewModel.filterByDate(date)
                                    expanded = false
                                    navController?.navigate("track_detail/$date")
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (!isRecording) {
                        val intent = Intent(context, LocationForegroundService::class.java)
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.FOREGROUND_SERVICE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            context.startForegroundService(intent)
                        }
                        trackViewModel.startRecording()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFF10B981) else Color(0xFF0EA5E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isRecording) "● 记录中" else "开始记录")
            }
            
            Button(
                onClick = {
                    if (isRecording) {
                        val intent = Intent(context, LocationForegroundService::class.java)
                        context.stopService(intent)
                        trackViewModel.stopRecording()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFEF4444) else Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isRecording
            ) {
                Text("停止")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 删除按钮
        if (pointCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedDate != null) {
                    Button(
                        onClick = {
                            val date = selectedDate!!
                            coroutineScope.launch {
                                trackViewModel.deleteTrackPointsByDate(date)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🗑️ 删除当天")
                    }
                }
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            trackViewModel.deleteAllTrackPoints()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🗑️ 删除全部")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 轨迹列表标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (selectedDate != null) "📋 $selectedDate 轨迹" else "📋 全部轨迹 (最新50个)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            if (selectedDate != null) {
                TextButton(onClick = { trackViewModel.clearDateFilter() }) {
                    Text("显示全部", fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 轨迹列表
        val displayPoints = if (selectedDate != null) trackViewModel.filteredPoints.value else trackPoints
        
        if (displayPoints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedDate != null) "该日期没有轨迹数据" else "暂无轨迹数据\n点击「开始记录」开始采集",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displayPoints.take(50)) { point ->
                    TrackPointItem(point)
                }
            }
        }
    }
}

@Composable
fun TrackPointItem(point: TrackPointEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "📍 ${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "海拔: ${point.altitude?.let { String.format("%.1f", it) } ?: "--"}m | 精度: ±${point.accuracy?.let { String.format("%.1f", it) } ?: "--"}m",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
            Text(
                text = formatTimestamp(point.timestamp),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
