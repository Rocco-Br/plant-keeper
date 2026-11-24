package com.plantkeeper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.plantkeeper.data.PlantStatus

// Kleuren uit het prototype (docs/poc/index.html): bosgroen en saliegroen.
private val Light = lightColorScheme(
    primary = Color(0xFF2C6A48), onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE6D2), onPrimaryContainer = Color(0xFF0F3920),
    secondary = Color(0xFF9BBE95), onSecondary = Color(0xFF16231B),
    secondaryContainer = Color(0xFFCBE6D2), onSecondaryContainer = Color(0xFF0F3920),
    tertiary = Color(0xFF9A520E), tertiaryContainer = Color(0xFFF7E4CB), onTertiaryContainer = Color(0xFF5E3307),
    background = Color(0xFFF6F8F3), onBackground = Color(0xFF16231B),
    surface = Color(0xFFF6F8F3), onSurface = Color(0xFF16231B), onSurfaceVariant = Color(0xFF4A5A4F),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEDF2E9), surfaceContainerHigh = Color(0xFFE6EDE1), surfaceContainerHighest = Color(0xFFE6EDE1),
    outline = Color(0xFFCFDACB), outlineVariant = Color(0xFFE1E8DD),
    error = Color(0xFFB13A2E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8ED3A5), onPrimary = Color(0xFF0A2615),
    primaryContainer = Color(0xFF23482F), onPrimaryContainer = Color(0xFFCBEBD4),
    secondary = Color(0xFF5E875C), onSecondary = Color(0xFFE3ECE4),
    secondaryContainer = Color(0xFF23482F), onSecondaryContainer = Color(0xFFCBEBD4),
    tertiary = Color(0xFFF2B26C), tertiaryContainer = Color(0xFF3A2913), onTertiaryContainer = Color(0xFFF6D6B0),
    background = Color(0xFF0F1813), onBackground = Color(0xFFE3ECE4),
    surface = Color(0xFF0F1813), onSurface = Color(0xFFE3ECE4), onSurfaceVariant = Color(0xFFA8B8AC),
    surfaceContainerLowest = Color(0xFF0A110D), surfaceContainerLow = Color(0xFF16221B),
    surfaceContainer = Color(0xFF131F18), surfaceContainerHigh = Color(0xFF1E2D24), surfaceContainerHighest = Color(0xFF1E2D24),
    outline = Color(0xFF2B3B31), outlineVariant = Color(0xFF213027),
    error = Color(0xFFF29C92),
)

@Composable
fun PlantKeeperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

/** Achtergrond- en tekstkleur van het statuslabel. */
@Composable
fun statusColors(status: PlantStatus): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    val (light, night) = when (status) {
        PlantStatus.GEZAAID -> (0xFFEFE2CF to 0xFF6E4E26) to (0xFF3A2D1C to 0xFFE9CFA8)
        PlantStatus.ONTKIEMD -> (0xFFDDEDC8 to 0xFF3F6B17) to (0xFF273A16 to 0xFFC5E59E)
        PlantStatus.UITGEPLANT -> (0xFFCBE6D2 to 0xFF1C5233) to (0xFF1D4029 to 0xFFA9DDB9)
        PlantStatus.GEOOGST -> (0xFFF5E5B6 to 0xFF775300) to (0xFF3D3010 to 0xFFF1D27E)
        PlantStatus.MISLUKT -> (0xFFF6D9D3 to 0xFF8E2A1F) to (0xFF45201B to 0xFFF4AFA5)
        PlantStatus.OVERLEDEN -> (0xFFE2E6E2 to 0xFF4F5852) to (0xFF262C28 to 0xFFAEB7B1)
    }
    val (bg, fg) = if (dark) night else light
    return Color(bg) to Color(fg)
}
