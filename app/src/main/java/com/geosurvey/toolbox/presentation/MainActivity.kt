package com.geosurvey.toolbox.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.geosurvey.toolbox.presentation.theme.GeoSurveyTheme
import com.geosurvey.toolbox.presentation.ui.HomeScreen
import com.geosurvey.toolbox.presentation.ui.OfflineMapScreen
import com.geosurvey.toolbox.presentation.ui.TrackDetailScreen
import com.geosurvey.toolbox.presentation.ui.TrackScreen
import com.geosurvey.toolbox.presentation.viewmodel.TrackViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoSurveyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as com.geosurvey.toolbox.GeoSurveyApplication
    val trackViewModel = remember { TrackViewModel.getInstance(application) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White.copy(alpha = 0.8f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Text("🏠", fontSize = 20.sp) },
                    label = { Text("首页") },
                    selected = selectedTab == 0,
                    onClick = { 
                        selectedTab = 0
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Text("🛣️", fontSize = 20.sp) },
                    label = { Text("轨迹") },
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        navController.navigate("track") {
                            popUpTo("track") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Text("🗺️", fontSize = 20.sp) },
                    label = { Text("地图") },
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        navController.navigate("map") {
                            popUpTo("map") { inclusive = true }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen()
            }
            composable("track") {
                TrackScreen(navController = navController)
            }
            composable("map") {
                OfflineMapScreen()
            }
            composable(
                route = "track_detail/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                TrackDetailScreen(
                    date = date,
                    trackViewModel = trackViewModel
                )
            }
        }
    }
}
