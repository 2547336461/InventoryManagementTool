package com.inventory.manager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.inventory.manager.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", "首页", Icons.Outlined.Home, Icons.Filled.Home)
    object Devices : Screen("devices", "设备", Icons.Outlined.Computer, Icons.Filled.Computer)
    object Records : Screen("records", "记录", Icons.Outlined.List, Icons.Filled.List)
    object More : Screen("more", "更多", Icons.Outlined.Settings, Icons.Filled.Settings)
}

val bottomNavItems = listOf(Screen.Dashboard, Screen.Devices, Screen.Records, Screen.More)

@Composable
fun MainNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToDevices = { navController.navigate(Screen.Devices.route) },
                onNavigateToAddDevice = { navController.navigate("add_device") }
            )
        }

        composable(Screen.Devices.route) {
            DeviceListScreen(
                onNavigateToDetail = { id -> navController.navigate("device_detail/$id") },
                onNavigateToAdd = { navController.navigate("add_device") }
            )
        }

        composable(
            route = "device_detail/{deviceId}",
            arguments = listOf(navArgument("deviceId") { type = NavType.IntType })
        ) { backStack ->
            val deviceId = backStack.arguments?.getInt("deviceId") ?: return@composable
            DeviceDetailScreen(
                deviceId = deviceId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("edit_device/$id") }
            )
        }

        composable("add_device") {
            AddEditDeviceScreen(
                deviceId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_device/{deviceId}",
            arguments = listOf(navArgument("deviceId") { type = NavType.IntType })
        ) { backStack ->
            val deviceId = backStack.arguments?.getInt("deviceId") ?: return@composable
            AddEditDeviceScreen(
                deviceId = deviceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Records.route) {
            RecordsScreen()
        }

        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToStaff = { navController.navigate("staff") },
                onNavigateToCategories = { navController.navigate("categories") }
            )
        }

        composable("staff") {
            StaffListScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("categories") {
            CategoryScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = { Icon(if (selected) screen.selectedIcon else screen.icon, screen.label) },
                label = { Text(screen.label) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
