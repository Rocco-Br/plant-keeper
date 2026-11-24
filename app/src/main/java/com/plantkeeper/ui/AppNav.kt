package com.plantkeeper.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

object Routes {
    const val HOME = "home"
    const val SEEDS = "seeds"
    const val PLANTS = "plants"
    const val MORE = "more"
    const val REMINDERS = "reminders"
    const val BACKUP = "backup"
    fun seed(id: String) = "seed/$id"
    fun seedForm(id: String? = null, mother: String? = null) = "seedForm?id=${id.orEmpty()}&mother=${mother.orEmpty()}"
    fun plant(id: String) = "plant/$id"
    fun plantForm(id: String? = null, seed: String? = null) = "plantForm?id=${id.orEmpty()}&seed=${seed.orEmpty()}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Nu zaaibaar", Icons.Outlined.WbSunny),
    Tab(Routes.SEEDS, "Zaden", Icons.Outlined.Grain),
    Tab(Routes.PLANTS, "Planten", Icons.Outlined.LocalFlorist),
    Tab(Routes.MORE, "Meer", Icons.Outlined.MoreHoriz),
)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.SEEDS) { SeedListScreen(nav) }
            composable("seed/{id}") { SeedDetailScreen(nav, it.arg("id")!!) }
            composable("seedForm?id={id}&mother={mother}") { SeedFormScreen(nav, it.arg("id"), it.arg("mother")) }
            composable(Routes.MORE) { MoreScreen(nav) }
            composable(Routes.REMINDERS) { RemindersScreen(nav) }
            composable(Routes.BACKUP) { BackupScreen(nav) }
            composable(Routes.PLANTS) { PlantListScreen(nav) }
            composable("plant/{id}") { PlantDetailScreen(nav, it.arg("id")!!) }
            composable("plantForm?id={id}&seed={seed}") { PlantFormScreen(nav, it.arg("id"), it.arg("seed")) }
        }
    }
}

private fun androidx.navigation.NavBackStackEntry.arg(name: String): String? = arguments?.getString(name)?.ifEmpty { null }

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
