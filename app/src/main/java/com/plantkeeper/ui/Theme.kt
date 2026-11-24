package com.plantkeeper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantkeeper.R
import com.plantkeeper.data.PlantStatus

// ---------- Kleur ----------
// Bosgroen als hoofdkleur, terracotta (de pot op de vensterbank) voor binnen, blad- en mosgroen voor buiten.

private val Light = lightColorScheme(
    primary = Color(0xFF2C6A48), onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E8CF), onPrimaryContainer = Color(0xFF0C3A1F),
    secondary = Color(0xFF4F7A28), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EDB4), onSecondaryContainer = Color(0xFF1E3A06),
    tertiary = Color(0xFF9A520E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7E4CB), onTertiaryContainer = Color(0xFF5E3307),
    background = Color(0xFFF4F5EC), onBackground = Color(0xFF1A2219),
    surface = Color(0xFFF4F5EC), onSurface = Color(0xFF1A2219), onSurfaceVariant = Color(0xFF4D5947),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFAFBF4),
    surfaceContainer = Color(0xFFEDF0E3), surfaceContainerHigh = Color(0xFFE6E9DB), surfaceContainerHighest = Color(0xFFDFE3D3),
    outline = Color(0xFFBFC8B4), outlineVariant = Color(0xFFDCE1D0),
    error = Color(0xFFB13A2E), onError = Color.White,
    errorContainer = Color(0xFFF8DAD5), onErrorContainer = Color(0xFF5C120A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF94D5A8), onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF1F4A30), onPrimaryContainer = Color(0xFFC8EFD2),
    secondary = Color(0xFFB5D98A), onSecondary = Color(0xFF213A05),
    secondaryContainer = Color(0xFF34521A), onSecondaryContainer = Color(0xFFD6EDB4),
    tertiary = Color(0xFFF2B26C), onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF3E2A12), onTertiaryContainer = Color(0xFFF7D8B3),
    background = Color(0xFF0F150F), onBackground = Color(0xFFE2E8DC),
    surface = Color(0xFF0F150F), onSurface = Color(0xFFE2E8DC), onSurfaceVariant = Color(0xFFAEB9A6),
    surfaceContainerLowest = Color(0xFF0A0F0A), surfaceContainerLow = Color(0xFF151C15),
    surfaceContainer = Color(0xFF192119), surfaceContainerHigh = Color(0xFF212A20), surfaceContainerHighest = Color(0xFF2A3429),
    outline = Color(0xFF3D4A3A), outlineVariant = Color(0xFF2C362A),
    error = Color(0xFFF29C92), onError = Color(0xFF5C120A),
    errorContainer = Color(0xFF4E1D17), onErrorContainer = Color(0xFFF8DAD5),
)

/** Kleuren die Material niet kent: binnen (terracotta), buiten (blad) en de kop van het startscherm. */
@Immutable
data class PkColors(
    val indoor: Color, val onIndoor: Color, val indoorContainer: Color, val onIndoorContainer: Color,
    val outdoor: Color, val onOutdoor: Color, val outdoorContainer: Color, val onOutdoorContainer: Color,
    val hero: List<Color>, val onHero: Color, val sun: Color,
    /** Kaarten: in licht wit op crème, in donker net lichter dan de achtergrond zodat ze niet wegzakken. */
    val card: Color,
)

private val LightPk = PkColors(
    indoor = Color(0xFFB45A34), onIndoor = Color.White, indoorContainer = Color(0xFFF6DCCD), onIndoorContainer = Color(0xFF5A2410),
    outdoor = Color(0xFF4F7A28), onOutdoor = Color.White, outdoorContainer = Color(0xFFD6EDB4), onOutdoorContainer = Color(0xFF1E3A06),
    hero = listOf(Color(0xFF1F4D33), Color(0xFF2C6A48), Color(0xFF6E9A5A)), onHero = Color(0xFFF3F8EC), sun = Color(0xFFF2C14E),
    card = Color(0xFFFFFFFF),
)

private val DarkPk = PkColors(
    indoor = Color(0xFFF0A27F), onIndoor = Color(0xFF4A1A06), indoorContainer = Color(0xFF4E2716), onIndoorContainer = Color(0xFFF9D6C4),
    outdoor = Color(0xFFB5D98A), onOutdoor = Color(0xFF213A05), outdoorContainer = Color(0xFF34521A), onOutdoorContainer = Color(0xFFD6EDB4),
    hero = listOf(Color(0xFF123320), Color(0xFF1F4D33), Color(0xFF3E6B3C)), onHero = Color(0xFFEAF4E2), sun = Color(0xFFF2C14E),
    card = Color(0xFF182018),
)

val LocalPkColors = staticCompositionLocalOf { LightPk }

/** Snelle toegang: `pk.indoor`, `pk.hero`, ... */
val pk: PkColors @Composable get() = LocalPkColors.current

// ---------- Letters ----------
// Fraunces (zachte, eigenwijze serif) voor koppen, Figtree voor lopende tekst. Beide variabele fonts.

@OptIn(ExperimentalTextApi::class)
private fun fraunces(weight: Int, opsz: Float) = Font(
    R.font.fraunces, FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opsz),
        FontVariation.Setting("SOFT", 100f),
        FontVariation.Setting("WONK", 1f),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: Int) = Font(R.font.figtree, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val FrauncesDisplay = FontFamily(fraunces(500, 96f), fraunces(600, 96f), fraunces(700, 96f))
val FrauncesText = FontFamily(fraunces(500, 24f), fraunces(600, 24f), fraunces(700, 24f))
val Figtree = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700))

private fun display(size: Int, line: Int, weight: Int = 600, spacing: Float = -0.5f) =
    TextStyle(fontFamily = FrauncesDisplay, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

private fun body(size: Float, line: Int, weight: Int = 400, spacing: Float = 0f) =
    TextStyle(fontFamily = Figtree, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

private val PkTypography = Typography(
    displayLarge = display(56, 60),
    displayMedium = display(46, 50),
    displaySmall = display(38, 42),
    headlineLarge = display(36, 40),
    headlineMedium = display(30, 34),
    headlineSmall = display(26, 30, spacing = -0.3f),
    titleLarge = TextStyle(fontFamily = FrauncesText, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = body(17f, 24, 600),
    titleSmall = body(15f, 20, 600),
    bodyLarge = body(16f, 24),
    bodyMedium = body(14.5f, 21),
    bodySmall = body(13f, 18),
    labelLarge = body(15f, 20, 600, 0.1f),
    labelMedium = body(13f, 16, 600, 0.2f),
    labelSmall = body(11.5f, 14, 600, 0.4f),
)

// ---------- Ruimte ----------

/** Vaste afstanden. Schermrand = [l], binnenkant kaart = [l], tussen kaarten = [m], binnen een groep = [s]. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Minimale hoogte van alles wat je aantikt (toegankelijkheid). */
val TouchTarget = 48.dp

// ---------- Vorm ----------
// small = maandvakjes, medium = tegels en velden, large = kaarten, extraLarge = vel en dialogen.

private val PkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

/** Bladvorm: twee ronde en twee bijna scherpe hoeken, zoals een blad. */
fun leaf(big: Dp, small: Dp = big / 4) = RoundedCornerShape(topStart = big, topEnd = small, bottomEnd = big, bottomStart = small)

/** Eén bladvorm voor alle knoppen. */
val ButtonShape = leaf(22.dp, 8.dp)

@Composable
fun PlantKeeperTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalPkColors provides if (dark) DarkPk else LightPk) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = PkTypography, shapes = PkShapes, content = content)
    }
}

// ---------- Status ----------

/** Achtergrond- en tekstkleur van het statuslabel. */
@Composable
fun statusColors(status: PlantStatus): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    val (light, night) = when (status) {
        PlantStatus.GEZAAID -> (0xFFEFE2CF to 0xFF6E4E26) to (0xFF3A2D1C to 0xFFE9CFA8)
        PlantStatus.ONTKIEMD -> (0xFFDDEDC8 to 0xFF3F6B17) to (0xFF273A16 to 0xFFC5E59E)
        PlantStatus.UITGEPLANT -> (0xFFC8E8CF to 0xFF1C5233) to (0xFF1D4029 to 0xFFA9DDB9)
        PlantStatus.GEOOGST -> (0xFFF5E5B6 to 0xFF775300) to (0xFF3D3010 to 0xFFF1D27E)
        PlantStatus.MISLUKT -> (0xFFF6D9D3 to 0xFF8E2A1F) to (0xFF45201B to 0xFFF4AFA5)
        PlantStatus.OVERLEDEN -> (0xFFE2E6E2 to 0xFF4F5852) to (0xFF262C28 to 0xFFAEB7B1)
    }
    val (bg, fg) = if (dark) night else light
    return Color(bg) to Color(fg)
}

val PlantStatus.icon: ImageVector
    get() = when (this) {
        PlantStatus.GEZAAID -> Icons.Outlined.Grain
        PlantStatus.ONTKIEMD -> Icons.Outlined.Spa
        PlantStatus.UITGEPLANT -> Icons.Outlined.Park
        PlantStatus.GEOOGST -> Icons.Outlined.Agriculture
        PlantStatus.MISLUKT -> Icons.Outlined.Block
        PlantStatus.OVERLEDEN -> Icons.Outlined.HeartBroken
    }
