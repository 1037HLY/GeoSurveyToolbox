package com.geosurvey.toolbox.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.delay

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    
    // 状态变量
    var isActive by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }
    var satelliteCount by remember { mutableStateOf(0) }
    var locationText by remember { mutableStateOf("等待定位...") }
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted && coarseLocationGranted) {
            startLocationUpdates(context, { newLocation ->
                location = newLocation
                locationText = "定位中..."
            }, { count ->
                satelliteCount = count
            })
            isActive = true
        }
    }
    
    // 检查权限
    fun checkAndRequestPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (fineLocation == PackageManager.PERMISSION_GRANTED &&
            coarseLocation == PackageManager.PERMISSION_GRANTED
        ) {
            // 权限已授予，启动定位
            startLocationUpdates(context, { newLocation ->
                location = newLocation
                locationText = "定位中..."
            }, { count ->
                satelliteCount = count
            })
            isActive = true
        } else {
            // 请求权限
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
    
    // 自动检查权限
    LaunchedEffect(Unit) {
        checkAndRequestPermissions()
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
        LocationInfoCard(location, isActive)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // GPS状态卡片
        GpsStatusCard(location, satelliteCount)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    checkAndRequestPermissions()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开始定位")
            }
            
            Button(
                onClick = {
                    isActive = false
                    location = null
                    locationText = "已停止"
                },
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
            text = locationText,
            fontSize = 14.sp,
            color = if (location != null) Color(0xFF10B981) else Color(0xFF94A3B8)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "🔧 开发中... 请等待后续版本",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

// 定位功能函数
fun startLocationUpdates(
    context: Context,
    onLocationUpdate: (Location) -> Unit,
    onSatelliteUpdate: (Int) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    // 检查权限
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    
    // GPS状态监听（获取卫星数量）
    try {
        locationManager.addGpsStatusListener { event ->
            when (event) {
                android.location.GpsStatus.GPS_EVENT_SATELLITE_STATUS -> {
                    val gpsStatus = locationManager.getGpsStatus(null)
                    val satellites = gpsStatus?.satellites
                    val count = satellites?.count() ?: 0
                    onSatelliteUpdate(count)
                }
            }
        }
    } catch (e: Exception) {
        // 某些设备可能不支持
    }
    
    // 位置监听
    val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocationUpdate(location)
        }
        
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    
    // 请求位置更新（使用GPS和网络）
    try {
        // GPS定位
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,  // 1秒
            1f,    // 1米
            locationListener
        )
        
        // 网络定位（辅助）
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            2000,  // 2秒
            10f,   // 10米
            locationListener
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun LocationInfoCard(location: Location?, isActive: Boolean) {
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
                    text = if (isActive && location != null) "● 定位中" else "○ 已停止",
                    fontSize = 14.sp,
                    color = if (isActive && location != null) Color(0xFF10B981) else Color(0xFF94A3B8)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
                    text = "等待定位...\n请确保GPS已开启并到室外",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun GpsStatusCard(location: Location?, satelliteCount: Int) {
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("卫星数", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        "$satelliteCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (satelliteCount > 0) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("状态", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        if (location != null) "已定位" else "未定位",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (location != null) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("精度", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        location?.accuracy?.let { String.format("%.1f", it) } ?: "--",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}
