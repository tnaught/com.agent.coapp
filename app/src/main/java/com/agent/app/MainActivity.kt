package com.agent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agent.app.ui.chat.ChatScreen
import com.agent.app.ui.config.ConfigScreen
import com.agent.app.ui.logs.LogsScreen
import com.agent.app.ui.navigation.BottomNavItem
import com.agent.app.ui.navigation.NavRoutes
import com.agent.app.ui.provisioning.BleProvisioningScreen
import com.agent.app.ui.skills.SkillsScreen
import com.agent.app.ui.theme.AndroidAgentAppTheme

/**
 * 主Activity
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidAgentAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

/**
 * 主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    // 避免重复创建相同的导航栈
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Provisioning,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Provisioning) {
                BleProvisioningScreen()
            }
            composable(NavRoutes.Config) {
                ConfigScreen()
            }
            composable(NavRoutes.Skills) {
                SkillsScreen()
            }
            composable(NavRoutes.Logs) {
                LogsScreen()
            }
            composable(NavRoutes.Chat) {
                ChatScreen()
            }
        }
    }
}
