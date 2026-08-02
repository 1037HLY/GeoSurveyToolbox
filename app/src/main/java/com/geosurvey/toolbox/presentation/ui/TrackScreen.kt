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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.geosurvey.toolbox.data.model.TrackPointEntity
import com.geosurvey.toolbox.domain.service.LocationForegroundService
import com.geosurvey.toolbox.presentation.viewmodel.TrackViewModel
import com.geosurvey.toolbox.presentation.viewmodel.TrackViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackScreen(
    trackViewModel: TrackViewModel = viewModel(
        factory = TrackViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val isRecording by trackViewModel.isRecording.collectAsState()
    val trackPoints by trackViewModel.trackPoints.collectAsState()
    val pointCount by trackViewModel.pointCount.collectAsState()
    val availableDates by trackViewModel.availableDates.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "🛣️ 轨迹记录",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("轨迹点", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("$pointCount", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("状态", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        if (isRecording) "● 记录中" else "○ 已停止",
                        color = if (isRecording) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("日期数", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${availableDates.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (!isRecording) {
                        // 启动前台服务
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
                        // 停止前台服务
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
                Text("停止记录")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 删除按钮
        if (pointCount > 0) {
            Button(
                onClick = { trackViewModel.deleteAllTrackPoints() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🗑️ 删除所有轨迹")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 轨迹列表
        Text(
            text = "📋 轨迹点列表 (最新50个)",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (trackPoints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无轨迹数据\n点击「开始记录」开始采集",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(trackPoints.take(50)) { point ->
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

// ViewModel Factory
class TrackViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val app = context.applicationContext as com.geosurvey.toolbox.GeoSurveyApplication
            return TrackViewModel(
                trackRepository = app.trackRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
