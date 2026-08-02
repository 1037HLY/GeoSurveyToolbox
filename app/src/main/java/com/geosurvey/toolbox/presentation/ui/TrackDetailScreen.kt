package com.geosurvey.toolbox.presentation.ui

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
import com.geosurvey.toolbox.data.model.TrackPointEntity
import com.geosurvey.toolbox.presentation.viewmodel.TrackViewModel
import com.geosurvey.toolbox.utils.TrackExportHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackDetailScreen(
    date: String,
    trackViewModel: TrackViewModel
) {
    val context = LocalContext.current
    val points by trackViewModel.filteredPoints.collectAsState()
    
    // 加载该日期的轨迹点
    LaunchedEffect(date) {
        trackViewModel.filterByDate(date)
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
                text = "📅 $date",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            
            // 导出按钮
            if (points.isNotEmpty()) {
                Row {
                    IconButton(
                        onClick = {
                            val helper = TrackExportHelper(context)
                            helper.exportToGPX(points, "track_$date")
                        }
                    ) {
                        Text("📄", fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = {
                            val helper = TrackExportHelper(context)
                            helper.exportToKML(points, "track_$date")
                        }
                    ) {
                        Text("🗺️", fontSize = 18.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 统计信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("点数", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${points.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("开始", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        points.firstOrNull()?.let { formatTime(it.timestamp) } ?: "--",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("结束", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        points.lastOrNull()?.let { formatTime(it.timestamp) } ?: "--",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 轨迹点列表
        if (points.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "该日期没有轨迹数据",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(points) { point ->
                    TrackPointItem(point)
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
