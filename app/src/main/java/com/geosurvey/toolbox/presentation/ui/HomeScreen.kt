package com.geosurvey.toolbox.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // 状态变量
    var isActive by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }
    var satelliteCount by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("点击「开始定位」获取位置") }
    
    // 位置回调
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { 
                    location = it
                    statusText = "✅ 已定位"
                }
            }
        }
    }
    
    // GPS状态监听（获取卫星数量）
    val gpsListener = remember {
        android.location.GpsStatus.Listener { event ->
            if (event == android.location.GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                try {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val gpsStatus = locationManager.getGpsStatus(null)
                        val satellites = gpsStatus?.satellites
                        satelliteCount = satellites?.count() ?: 0
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }
    
    // 权限请求 - 必须在startLocation之前定义
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted && coarseLocationGranted) {
            // 权限获取成功，启动定位
            startLocationAfterPermission()
            statusText = "🔍 正在搜索GPS..."
        } else {
            statusText = "⚠️ 需要位置权限才能定位"
        }
    }
    
    // 开始位置更新函数
    fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
            ).apply {
                setMinUpdateIntervalMillis(500)
                setMaxUpdateDelayMillis(2000)
            }.build()
            
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            statusText = "❌ 定位启动失败: ${e.message}"
        }
    }
    
    // 权限获取后启动定位
    fun startLocationAfterPermission() {
        try {
            // 注册GPS状态监听
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.addGpsStatusListener(gpsListener)
            }
        } catch (e: Exception) {
            // ignore
        }
        
        startLocationUpdates()
        isActive = true
        statusText = "🔍 正在搜索GPS..."
    }
    
    // 开始定位函数（检查权限）
    fun startLocation() {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (fineLocation == PackageManager.PERMISSION_GRANTED &&
            coarseLocation == PackageManager.PERMISSION_GRANTED
        ) {
            // 权限已授予
            startLocationAfterPermission()
        } else {
            // 请求权限
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }
    }
    
    // 停止定位函数
    fun stopLocation() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            locationManager.removeGpsStatusListener(gpsListener)
        } catch (e: Exception) {
            // ignore
        }
        isActive = false
        statusText = "⏹️ 已停止定位"
    }
    
    // 自动检查权限并启动
    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (fineLocation == PackageManager.PERMISSION_GRANTED &&
            coarseLocation == PackageManager.PERMISSION_GRANTED
        ) {
            // 权限已授予，自动启动
            startLocationAfterPermission()
        }
    }
    
    // 组件销毁时清理
    DisposableEffect(Unit) {
        onDispose {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                locationManager.removeGpsStatusListener(gpsListener)
            } catch (e: Exception) {
                // ignore
            }
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
        LocationInfoCard(location, isActive, statusText)
        
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
                    if (!isActive) {
                        startLocation()
                    } else {
                        statusText = "📡 定位已激活"
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0xFF10B981) else Color(0xFF0EA5E9)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = true
            ) {
                Text(if (isActive) "● 定位中" else "开始定位")
            }
            
            Button(
                onClick = { 
                    if (isActive) {
                        stopLocation()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0xFFEF4444) else Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isActive
            ) {
                Text("停止定位")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 状态信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            )
        ) {
            Text(
                text = statusText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                fontSize = 14.sp,
                color = when {
                    statusText.contains("✅") -> Color(0xFF10B981)
                    statusText.contains("⚠️") -> Color(0xFFF59E0B)
                    statusText.contains("❌") -> Color(0xFFEF4444)
                    else -> Color(0xFF475569)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "🔧 开发中... 请等待后续版本",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun LocationInfoCard(location: Location?, isActive: Boolean, statusText: String) {
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
                    text = if (isActive) "🔍 正在搜索GPS信号...\n请确保在室外开阔地" else "等待定位",
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
                        if (location != null) "✅ 已定位" else "⏳ 搜星中",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (location != null) Color(0xFF10B981) else Color(0xFFF59E0B)
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
