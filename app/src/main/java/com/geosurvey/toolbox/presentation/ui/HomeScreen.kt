package com.geosurvey.toolbox.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geosurvey.toolbox.presentation.viewmodel.LocationViewModel

@Composable
fun HomeScreen(
    viewModel: LocationViewModel = viewModel(
        factory = LocationViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val locationState by viewModel.locationState.collectAsState()
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val backgroundLocationGranted = permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
        
        if (fineLocationGranted && coarseLocationGranted) {
            viewModel.startLocationUpdates()
        }
    }
    
    // 检查权限并请求
    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (fineLocation == PackageManager.PERMISSION_GRANTED &&
            coarseLocation == PackageManager.PERMISSION_GRANTED) {
            viewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
                )
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 标题
        Text(
            text = "🏔️ 地质勘查工具箱",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "GeoSurvey Toolbox v1.0.0",
            fontSize = 14.sp,
            color = Color(0xFF475569)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 定位信息卡片
        LocationInfoCard(locationState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // GPS状态卡片
        GpsStatusCard(locationState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 卫星信息卡片
        SatelliteInfoCard(locationState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.startLocationUpdates() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开始定位")
            }
            
            Button(
                onClick = { viewModel.stopLocationUpdates() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("停止定位")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "🔧 开发中... 请等待后续版本",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun LocationInfoCard(locationState: LocationState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📍 定位信息",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0EA5E9)
                )
                Text(
                    text = if (locationState.isActive) "● 定位中" else "○ 已停止",
                    fontSize = 14.sp,
                    color = if (locationState.isActive) Color(0xFF10B981) else Color(0xFF94A3B8)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val location = locationState.currentLocation
            if (location != null) {
                Column {
                    Text(
                        text = "纬度: ${String.format("%.6f", location.latitude)}",
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "经度: ${String.format("%.6f", location.longitude)}",
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "海拔: ${location.altitude?.let { String.format("%.1f", it) } ?: "--"} m",
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "速度: ${location.speed?.let { String.format("%.1f", it * 3.6) } ?: "--"} km/h",
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "精度: ±${location.accuracy?.let { String.format("%.1f", it) } ?: "--"} m",
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            } else {
                Text(
                    text = "等待定位...",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun GpsStatusCard(locationState: LocationState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📡 GPS状态",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF10B981)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("卫星总数", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        "${locationState.satelliteCount}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Column {
                    Text("HDOP", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        locationState.hdop?.let { String.format("%.1f", it) } ?: "--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Column {
                    Text("PDOP", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        locationState.pdop?.let { String.format("%.1f", it) } ?: "--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Column {
                    Text("质量", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        locationState.qualityText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = locationState.qualityColor
                    )
                }
            }
        }
    }
}

@Composable
fun SatelliteInfoCard(locationState: LocationState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🛰️ 卫星信息",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8B5CF6)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🟢 GPS", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${locationState.gpsCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔵 GLONASS", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${locationState.glonassCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔴 北斗", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${locationState.beidouCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🟡 Galileo", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${locationState.galileoCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 数据类
data class LocationState(
    val isActive: Boolean = false,
    val currentLocation: Location? = null,
    val satelliteCount: Int = 0,
    val hdop: Float? = null,
    val pdop: Float? = null,
    val qualityText: String = "未知",
    val qualityColor: Color = Color(0xFF94A3B8),
    val gpsCount: Int = 0,
    val glonassCount: Int = 0,
    val beidouCount: Int = 0,
    val galileoCount: Int = 0
)

// ViewModel Factory
class LocationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
