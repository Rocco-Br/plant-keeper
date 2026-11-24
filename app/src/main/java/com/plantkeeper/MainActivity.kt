package com.plantkeeper

import android.os.Bundle
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.plantkeeper.data.SettingKeys
import com.plantkeeper.data.settings
import com.plantkeeper.ui.AppNav
import com.plantkeeper.ui.PlantKeeperTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Het opstartscherm blijft staan tot we weten of het welkomstscherm nog moet komen, zodat er niets knippert.
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        splash.setOnExitAnimationListener { provider ->
            provider.iconView.animate().scaleX(1.25f).scaleY(1.25f).alpha(0f)
                .setDuration(320).setInterpolator(AnticipateInterpolator()).start()
            provider.view.animate().alpha(0f).setStartDelay(120).setDuration(320).withEndAction { provider.remove() }.start()
        }

        setContent {
            PlantKeeperTheme {
                val welcomed by remember { settings.data.map { it[SettingKeys.WELCOMED] ?: false } }.collectAsState(initial = null)
                LaunchedEffect(welcomed) { if (welcomed != null) ready = true }
                welcomed?.let { AppNav(showWelcome = !it) }
            }
        }
    }
}
