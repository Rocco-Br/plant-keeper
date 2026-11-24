package com.plantkeeper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plantkeeper.PlantKeeperApp
import com.plantkeeper.data.MonthRange
import com.plantkeeper.data.PhotoStore
import com.plantkeeper.data.PlantStatus
import com.plantkeeper.data.QuantityUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Het design-systeem van Plant Keeper: alle schermen bouwen uit deze onderdelen, met de tokens uit
// Theme.kt (Space, vormen, kleuren) en de beweging uit Motion.kt.

// ---------- Opmaak ----------

val MONTHS = listOf("januari", "februari", "maart", "april", "mei", "juni", "juli", "augustus", "september", "oktober", "november", "december")
val MONTHS_SHORT = listOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec")
val MONTH_LETTERS = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

fun fmtNumber(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else d.toString().replace('.', ',')

fun qtyText(quantity: Double?, unit: QuantityUnit): String = when {
    quantity == null -> "Hoeveelheid onbekend"
    quantity == 1.0 -> "1 ${unit.singular}"
    else -> "${fmtNumber(quantity)} ${unit.plural}"
}

fun count(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

fun fmtDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val d = LocalDate.parse(iso)
    return "${d.dayOfMonth} ${MONTHS_SHORT[d.monthValue - 1]} ${d.year}"
}

fun fmtLongDate(d: LocalDate) = "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]} ${d.year}"

fun periodText(r: MonthRange?): String = when {
    r == null -> "geen"
    r.isWholeYear -> "hele jaar"
    else -> "${MONTHS_SHORT[r.start - 1]} t/m ${MONTHS_SHORT[r.end - 1]}"
}

fun String.capitalized() = replaceFirstChar { it.uppercase() }

@Composable
fun app(): PlantKeeperApp = LocalContext.current.applicationContext as PlantKeeperApp

// ---------- Schermopbouw ----------

/** Ruimte onder een lijst op een tabscherm, zodat de toevoegknop niets afdekt. */
val FabClearance = 104.dp

/**
 * Elk scherm begint hier. Tabschermen (zonder [onBack]) krijgen een grote kop met een blad als decoratie,
 * subschermen een app bar met terugknop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PkScaffold(
    title: String,
    eyebrow: String? = null,
    greeting: String? = null,
    onBack: (() -> Unit)? = null,
    backIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        contentWindowInsets = if (onBack == null) WindowInsets.statusBars else WindowInsets.systemBars,
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(backIcon, if (backIcon == Icons.Outlined.Close) "Sluiten" else "Terug") } },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        },
        floatingActionButton = fab,
    ) { padding ->
        if (onBack == null) {
            Column(Modifier.padding(padding)) {
                Box(Modifier.fillMaxWidth()) {
                    // Decoratief blad rechtsboven, puur sfeer.
                    Icon(
                        Icons.Outlined.Spa, null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = (-6).dp).size(124.dp).rotate(-18f),
                    )
                    Column(Modifier.padding(start = Space.l + Space.xs, end = Space.l + Space.xs, top = Space.xl, bottom = Space.s)) {
                        if (greeting != null) {
                            val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
                            AnimatedVisibility(shown, enter = Motion.enterSide) {
                                Text(greeting, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (eyebrow != null) Eyebrow(eyebrow)
                        Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
                    }
                }
                content(PaddingValues(top = Space.xs, bottom = FabClearance))
            }
        } else {
            content(padding)
        }
    }
}

private val HeroHeight = 330.dp
private val BarHeight = 64.dp

/**
 * Detailscherm met een grote foto (of gekleurde tegel) tot onder de statusbalk, ronde knoppen erover en een vel
 * met de inhoud dat over de foto heen schuift. Zodra de foto weg is, vult een balk met de naam de bovenrand.
 */
@Composable
fun DetailScaffold(
    name: String,
    photo: String?,
    icon: ImageVector,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    // De balk is helemaal dicht precies als de bovenrand van het vel de onderkant van de balk raakt.
    val statusBar = WindowInsets.statusBars.getTop(density)
    val sheetTop = with(density) { (HeroHeight - 32.dp).toPx() }
    val barBottom = statusBar + with(density) { BarHeight.toPx() }
    val fillRange = with(density) { 48.dp.toPx() }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // Parallax: de foto schuift langzamer weg dan de inhoud en vervaagt een beetje.
            Box(Modifier.graphicsLayer { translationY = scroll.value * 0.45f; alpha = 1f - (scroll.value / 1400f).coerceIn(0f, 0.5f) }) {
                PhotoHero(name, photo, icon)
            }
            Column(
                Modifier
                    .offset(y = (-32).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(start = Space.l, end = Space.l, top = Space.xl, bottom = Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.m),
                content = content,
            )
        }
        val fill = 1f - ((sheetTop - scroll.value - barBottom) / fillRange).coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().graphicsLayer { alpha = fill }.background(MaterialTheme.colorScheme.surface).statusBarsPadding().height(BarHeight)) {
            Text(
                name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 72.dp, end = 72.dp),
            )
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Space.m, vertical = Space.s)) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Terug", onBack)
            Spacer(Modifier.weight(1f))
            RoundButton(Icons.Outlined.Edit, "Bewerken", onEdit)
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), modifier = Modifier.size(TouchTarget)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label) }
    }
}

/** Klein label in hoofdletters boven een titel. */
@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 3), color = color, modifier = modifier)
}

/** Titel op een detailscherm: eyebrow plus naam in de grote serif. */
@Composable
fun DetailTitle(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Eyebrow(eyebrow)
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    }
}

/** Kop boven een groep lijstrijen, op de schermrand uitgelijnd. */
@Composable
fun ListHeader(text: String) =
    Eyebrow(text, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.padding(start = Space.l + Space.xs, end = Space.l, top = Space.l, bottom = Space.s).semantics { heading() })

// ---------- Kaarten en rijen ----------

/** Icoon in een bladvormig vlakje. */
@Composable
fun IconBlob(icon: ImageVector, bg: Color, fg: Color, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(leaf(size * 0.45f)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * Sectiekop: icoon in een blad, titel, optioneel een regel uitleg en iets rechts (label, schakelaar).
 * [large] is voor koppen direct op de achtergrond (startscherm), anders voor in een kaart.
 */
@Composable
fun SectionTitle(
    title: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    container: Color = tint.copy(alpha = 0.14f),
    large: Boolean = false,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBlob(icon, container, tint, if (large) 44.dp else 36.dp)
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Space.s))
        trailing()
    }
}

/** De enige kaart: ronde hoeken, kaartkleur, vaste binnenruimte. Optioneel klikbaar. */
@Composable
fun PkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = pk.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(interaction, LocalIndication.current, role = Role.Button, onClick = onClick) else Modifier)
            .padding(Space.l),
        verticalArrangement = Arrangement.spacedBy(Space.m),
        content = content,
    )
}

/** Kaart met een sectiekop. */
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    PkCard(modifier) {
        SectionTitle(title, icon, tint, subtitle, trailing = trailing)
        content()
    }
}

/** Rij binnen een kaart of op het vel (bijvoorbeeld een zaaiing of de moederplant). */
@Composable
fun Tile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Space.m + Space.xs, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
        content = content,
    )
}

/** Kaart met feiten onder elkaar (voorraad, houdbaar, locatie...). Vul met [FactRow]. */
@Composable
fun FactsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(pk.card).padding(vertical = Space.s), content = content)
}

/**
 * Eén feit: gekleurd icoon, label links en de waarde rechts. Lange waarden lopen netjes over twee regels
 * in plaats van midden in een woord af te breken. Met [onClick] krijgt de rij een pijltje.
 */
@Composable
fun FactRow(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    container: Color,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Space.l, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBlob(icon, container, tint, 36.dp)
        Spacer(Modifier.width(Space.m))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.width(Space.m))
        Text(
            value, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Rij in een lijst: kaartje met iets links (foto, icoon), titel, ondertitel en iets rechts. Neemt zelf de schermrand mee. */
@Composable
fun ListRow(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = { Chevron() },
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .padding(horizontal = Space.l, vertical = Space.xs)
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(pk.card)
            .then(if (onClick != null) Modifier.clickable(interaction, LocalIndication.current, onClick = onClick) else Modifier)
            .padding(start = Space.m, top = Space.m, bottom = Space.m, end = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(Space.m + Space.xs))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Space.s))
        trailing()
    }
}

@Composable
fun Chevron() = Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)

/** Leeg scherm: de mascotte met een stemming die past bij de situatie, en een korte uitleg. */
@Composable
fun EmptyState(text: String, mood: Mood = Mood.HAPPY, modifier: Modifier = Modifier) {
    Column(
        modifier.appear().fillMaxWidth().padding(horizontal = Space.xxl, vertical = Space.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(Modifier.size(132.dp), mood)
        Spacer(Modifier.height(Space.xs))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ---------- Labels en keuzechips ----------

/** Kleurtoon voor labels en meldingen. */
enum class Tone { Info, Warn, Error }

@Composable
fun toneColors(tone: Tone): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (tone) {
        Tone.Info -> primaryContainer to onPrimaryContainer
        Tone.Warn -> tertiaryContainer to onTertiaryContainer
        Tone.Error -> errorContainer to onErrorContainer
    }
}

/** Het enige label: pilvorm, 28dp hoog, optioneel icoon. Niet klikbaar. */
@Composable
fun Label(text: String, colors: Pair<Color, Color>, icon: ImageVector? = null) {
    val (bg, fg) = colors
    Row(
        Modifier.heightIn(min = 28.dp).clip(CircleShape).background(bg).padding(start = if (icon != null) Space.s else Space.m - 2.dp, end = Space.m - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(Space.xs))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun Label(text: String, tone: Tone, icon: ImageVector? = null) = Label(text, toneColors(tone), icon)

/** Statuslabel met eigen kleur en icoon; springt zacht om als de status verandert. */
@Composable
fun StatusLabel(status: PlantStatus) {
    AnimatedContent(
        targetState = status,
        transitionSpec = { (fadeIn(Motion.tween()) + scaleIn(Motion.pop(), initialScale = 0.8f)) togetherWith fadeOut(Motion.tween(Motion.SHORT)) },
        label = "status",
    ) { s -> Label(s.label, statusColors(s), s.icon) }
}

/**
 * De enige kiesbare chip: pilvorm met rand als hij uit staat, gevuld met [colors] als hij aan staat.
 * Visueel 40dp hoog, tikvlak 48dp.
 */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    colors: Pair<Color, Color> = MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary,
    count: Int? = null,
    role: Role = Role.RadioButton,
) {
    val (bg, fg) = colors
    val container by animateColorAsState(if (selected) bg else Color.Transparent, Motion.tween(), label = "chip")
    val ink = if (selected) fg else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(role = role, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(start = if (icon != null) Space.m else Space.l, end = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = ink)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text("$count", style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.7f))
        }
    }
}

/** Keuze uit een paar opties als rij chips (eenheid, samenvoegen of vervangen). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        options.forEach { o -> SelectChip(text(o), o == selected, { onSelect(o) }) }
    }
}

/** Status kiezen: elke status zijn eigen kleur en icoon. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusPicker(selected: PlantStatus, onSelect: (PlantStatus) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        PlantStatus.entries.forEach { s -> SelectChip(s.label.capitalized(), s == selected, { onSelect(s) }, s.icon, statusColors(s)) }
    }
}

// ---------- Knoppen ----------

enum class ButtonKind { Primary, Tonal, Text, Danger }

/**
 * De enige knop. Primary en Tonal zijn breed en 56dp hoog in de bladvorm; Text en Danger zijn tekstknoppen van 48dp.
 */
@Composable
fun PkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
) {
    val label: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.s))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val big = modifier.fillMaxWidth().heightIn(min = 56.dp)
    when (kind) {
        ButtonKind.Primary -> Button(onClick, big, enabled, shape = ButtonShape, content = label)
        ButtonKind.Tonal -> FilledTonalButton(onClick, big, enabled, shape = ButtonShape, content = label)
        ButtonKind.Text -> TextButton(onClick, modifier.heightIn(min = TouchTarget), enabled, content = label)
        ButtonKind.Danger -> TextButton(
            onClick, modifier.heightIn(min = TouchTarget), enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), content = label,
        )
    }
}

/** Opslaan-knop rechtsboven in een formulier: compacte variant van de hoofdknop. */
@Composable
fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.padding(end = Space.s), shape = ButtonShape) {
        Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Opslaan", style = MaterialTheme.typography.labelLarge)
    }
}

/** Toevoegknop in bladvorm; klapt in tot het plusje zodra je scrolt en springt de eerste keer zacht in beeld. */
@Composable
fun AddFab(label: String, expanded: Boolean = true, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        modifier = Modifier.popIn(),
        icon = { Icon(Icons.Outlined.Add, null) },
        text = { Text(label, style = MaterialTheme.typography.titleMedium) },
        shape = leaf(26.dp, 10.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

// ---------- Meldingen en dialogen ----------

/** Gekleurd blok met icoon en tekst, optioneel met een knop eronder. Voor uitleg, waarschuwingen en fouten. */
@Composable
fun Notice(icon: ImageVector, text: String, tone: Tone = Tone.Info, action: (@Composable () -> Unit)? = null) {
    val (bg, fg) = toneColors(tone)
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(bg).padding(Space.l)) {
        Icon(icon, null, tint = fg)
        Spacer(Modifier.width(Space.m))
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Text(text, color = fg, style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}

@Composable
fun FormError(text: String?) {
    if (text != null) Notice(Icons.Outlined.ErrorOutline, text, Tone.Error)
}

/** De enige dialoog: ronde hoeken, serif-titel, tekstknoppen. [danger] kleurt de bevestigknop rood. */
@Composable
fun PkDialog(
    title: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = content,
        confirmButton = { PkButton(confirm, onConfirm, kind = if (danger) ButtonKind.Danger else ButtonKind.Text, enabled = confirmEnabled) },
        dismissButton = { PkButton("Annuleren", onDismiss, kind = ButtonKind.Text) },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) =
    PkDialog(title, confirm, onConfirm, onDismiss, danger = true) { Text(text, style = MaterialTheme.typography.bodyMedium) }

// ---------- Foto's ----------

/** Zachte tweekleurige tegel, afgeleid van de naam zodat elk item zijn eigen tint houdt. */
@Composable
fun placeholderBrush(name: String): Brush {
    val hue = (((name.hashCode() % 360) + 360) % 360).toFloat()
    val hue2 = (hue + 28f) % 360f
    return if (isSystemInDarkTheme()) Brush.linearGradient(listOf(Color.hsl(hue, 0.30f, 0.32f), Color.hsl(hue2, 0.30f, 0.20f)))
    else Brush.linearGradient(listOf(Color.hsl(hue, 0.45f, 0.82f), Color.hsl(hue2, 0.40f, 0.64f)))
}

@Composable
fun placeholderInk(name: String): Color {
    val hue = (((name.hashCode() % 360) + 360) % 360).toFloat()
    return if (isSystemInDarkTheme()) Color.hsl(hue, 0.40f, 0.86f) else Color.hsl(hue, 0.45f, 0.22f)
}

@Composable
fun rememberPhoto(photo: String?): ImageBitmap? {
    val store = app().photos
    return produceState<ImageBitmap?>(null, photo) {
        value = photo?.let { withContext(Dispatchers.IO) { android.graphics.BitmapFactory.decodeFile(store.file(it).path)?.asImageBitmap() } }
    }.value
}

@Composable
fun rememberUriPhoto(uri: String?): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(null, uri) {
        value = uri?.let {
            withContext(Dispatchers.IO) { runCatching { PhotoStore.decodeScaled(context, Uri.parse(it), 512).asImageBitmap() }.getOrNull() }
        }
    }.value
}

@Composable
private fun PhotoOrPlaceholder(name: String, image: ImageBitmap?, icon: ImageVector, modifier: Modifier, iconSize: Dp) {
    Box(modifier.background(placeholderBrush(name)), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(image, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(icon, null, tint = placeholderInk(name), modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun Thumb(name: String, photo: String?, icon: ImageVector, size: Dp = 52.dp) {
    PhotoOrPlaceholder(name, rememberPhoto(photo), icon, Modifier.size(size).clip(leaf(size * 0.42f)), size * 0.48f)
}

@Composable
fun PhotoHero(name: String, photo: String?, icon: ImageVector) {
    Box(Modifier.fillMaxWidth().height(HeroHeight)) {
        PhotoOrPlaceholder(name, rememberPhoto(photo), icon, Modifier.matchParentSize(), 112.dp)
        if (photo == null) {
            // Twee zachte cirkels geven de lege tegel wat diepte.
            Box(Modifier.size(260.dp).offset((-90).dp, 150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
            Box(Modifier.size(150.dp).align(Alignment.TopEnd).offset(40.dp, 60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
        }
    }
}

/** Formulierveld "Foto kiezen" met preview. [picked] is een gekozen maar nog niet opgeslagen foto (content-URI). */
@Composable
fun PhotoPickerField(name: String, current: String?, picked: String?, onPick: (String) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPick(uri.toString())
    }
    val image = if (picked != null) rememberUriPhoto(picked) else rememberPhoto(current)
    val hasPhoto = picked != null || current != null
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.large)
            .background(if (image != null) Brush.linearGradient(listOf(Color.Black, Color.Black)) else placeholderBrush(name.ifBlank { "plant" }))
            .clickable(role = Role.Button, onClickLabel = "Foto kiezen") { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image, "Gekozen foto", Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Row(
                Modifier.align(Alignment.BottomEnd).padding(Space.m).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = Space.l, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Andere foto", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            val ink = placeholderInk(name.ifBlank { "plant" })
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AddAPhoto, null, tint = ink, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(Space.xs))
                Text(if (hasPhoto) "Andere foto" else "Foto kiezen", style = MaterialTheme.typography.titleMedium, color = ink)
                Text("Optioneel, wordt verkleind tot 1024 px", style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.85f))
            }
        }
    }
}

// ---------- Invoervelden ----------

/** Gevulde velden zonder onderstreping, met de vorm van een tegel. */
@Composable
fun pkFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
)

/** Zelfde hoek als MaterialTheme.shapes.medium. */
val FieldShape = RoundedCornerShape(18.dp)

/** Formulierveld. Eénregelig springt Enter naar het volgende veld; tekst begint met een hoofdletter. */
@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    singleLine: Boolean = true,
    imeAction: ImeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard, imeAction = imeAction,
            capitalization = if (keyboard == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        ),
        keyboardActions = if (onImeAction != null) KeyboardActions(onDone = { onImeAction() }) else KeyboardActions.Default,
        shape = FieldShape,
        colors = pkFieldColors(),
        modifier = modifier,
    )
}

/** Zoekveld bovenaan een lijst. */
@Composable
fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Zoek op naam") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, "Zoekopdracht wissen") } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = CircleShape,
        colors = pkFieldColors(),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.l, vertical = Space.s),
    )
}

@Composable
fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        TextField(
            value = text(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true,
            shape = FieldShape,
            colors = pkFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = open, onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.medium, containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            options.forEach {
                DropdownMenuItem(text = { Text(text(it), maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { onSelect(it); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, iso: String?, onChange: (String?) -> Unit, modifier: Modifier = Modifier, clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextField(
            value = fmtDate(iso),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                if (clearable && iso != null) {
                    IconButton(onClick = { onChange(null) }) { Icon(Icons.Outlined.Close, "$label wissen") }
                } else {
                    Icon(Icons.Outlined.Event, null)
                }
            },
            singleLine = true,
            shape = FieldShape,
            colors = pkFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        // Klikvlak over het veld, behalve het wisknopje.
        Box(Modifier.matchParentSize().padding(end = if (clearable && iso != null) TouchTarget else 0.dp).clickable(role = Role.Button, onClickLabel = "$label kiezen") { open = true }
                .semantics { contentDescription = "$label: ${fmtDate(iso).ifBlank { "geen datum" }}" },
        )
    }
    if (open) {
        val millis = iso?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val state = rememberDatePickerState(initialSelectedDateMillis = millis ?: System.currentTimeMillis())
        val chosen = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                PkButton("Kiezen", { chosen?.let { onChange(it.toString()) }; open = false }, kind = ButtonKind.Text, enabled = chosen != null)
            },
            dismissButton = { PkButton("Annuleren", { open = false }, kind = ButtonKind.Text) },
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            DatePicker(
                state,
                title = { Eyebrow(label, modifier = Modifier.padding(start = Space.xl, end = Space.m, top = Space.l)) },
                headline = {
                    Text(
                        chosen?.let { fmtLongDate(it) } ?: "Geen datum",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = Space.xl, end = Space.m, bottom = Space.m),
                    )
                },
                showModeToggle = false,
            )
        }
    }
}

/**
 * Zaaiperiode kiezen op een kalender van 12 maanden: tik de eerste maand, dan de laatste.
 * Een periode over de jaarwissel kan gewoon (eerst november, dan maart).
 */
@Composable
fun MonthRangePicker(
    title: String,
    icon: ImageVector,
    color: Color,
    onColor: Color,
    container: Color,
    start: Int?,
    end: Int?,
    onChange: (Int?, Int?) -> Unit,
) {
    var awaitingEnd by remember { mutableStateOf(false) }
    val range = if (start != null && end != null) MonthRange(start, end) else null
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBlob(icon, container, color, 32.dp)
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        awaitingEnd -> "Tik nu op de laatste maand"
                        range == null -> "Tik op de eerste maand"
                        else -> periodText(range).capitalized()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (awaitingEnd) color else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (range != null) PkButton("Wissen", { onChange(null, null); awaitingEnd = false }, kind = ButtonKind.Text)
        }
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0..5) {
                    val m = row * 6 + col + 1
                    val inRange = range?.contains(m) == true
                    val edge = m == start || m == end
                    val bg by animateColorAsState(
                        when {
                            edge -> color
                            inRange -> container
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        Motion.tween(),
                        label = "maand",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(TouchTarget)
                            .clip(MaterialTheme.shapes.small)
                            .background(bg)
                            .clickable(onClickLabel = "Kies ${MONTHS[m - 1]}") {
                                if (!awaitingEnd || start == null) { onChange(m, m); awaitingEnd = true } else { onChange(start, m); awaitingEnd = false }
                            }
                            .semantics {
                                selected = inRange
                                contentDescription = "${MONTHS[m - 1]}${if (inRange) ", in zaaiperiode" else ""}"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            MONTHS_SHORT[m - 1],
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (edge) FontWeight.Bold else FontWeight.SemiBold,
                            color = when {
                                edge -> onColor
                                inRange -> color
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
