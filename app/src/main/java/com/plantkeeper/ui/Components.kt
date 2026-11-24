package com.plantkeeper.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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

/** Rootschermen (met bottom bar) krijgen een grote kop met een blad als decoratie, subschermen een app bar met terugknop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PkScaffold(
    title: String,
    eyebrow: String? = null,
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
                    navigationIcon = { IconButton(onClick = onBack) { Icon(backIcon, "Terug") } },
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
                    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 10.dp)) {
                        if (eyebrow != null) Eyebrow(eyebrow)
                        Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                content(PaddingValues(bottom = 104.dp))
            }
        } else {
            content(padding)
        }
    }
}

/**
 * Detailscherm met een grote foto (of gekleurde bladtegel) bovenaan tot onder de statusbalk,
 * ronde knoppen erover en een vel met de inhoud dat over de foto heen schuift.
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
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            PhotoHero(name, photo, icon)
            Column(
                Modifier
                    .offset(y = (-32).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
        // Achter de statusbalk en knoppen vult een balk in zodra de foto onder de rand verdwijnt.
        val fill = ((scroll.value - 450f) / 300f).coerceIn(0f, 1f)
        Box(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = fill))
                .statusBarsPadding().height(64.dp),
        )
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Terug", onBack)
            Spacer(Modifier.weight(1f))
            RoundButton(Icons.Outlined.Edit, "Bewerken", onEdit)
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label) }
    }
}

@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 3), color = color)
}

/** Titel op een detailscherm: eyebrow plus naam in de grote serif. */
@Composable
fun DetailTitle(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Eyebrow(eyebrow)
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    IconBlob(icon, tint.copy(alpha = 0.14f), tint, 34.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        }
        content()
    }
}

/** Icoon in een bladvormig vlakje. */
@Composable
fun IconBlob(icon: ImageVector, bg: Color, fg: Color, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(leaf(size * 0.45f)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun Fact(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
            .padding(14.dp),
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(10.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.75f))
        Text(
            value, style = MaterialTheme.typography.titleSmall, color = content, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textDecoration = if (onClick != null) androidx.compose.ui.text.style.TextDecoration.Underline else null,
        )
    }
}

/** Rij in een lijst, als kaartje met een bladvormige foto. */
@Composable
fun ListRow(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit = { Chevron() },
) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
fun Chevron() = Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBlob(icon, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant, 64.dp)
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun Badge(text: String, icon: ImageVector? = null, warn: Boolean = true) {
    val bg = if (warn) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val fg = if (warn) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Pill(text, bg, fg, icon)
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, icon: ImageVector? = null) {
    Row(
        Modifier.clip(CircleShape).background(bg).padding(start = if (icon != null) 8.dp else 11.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
fun StatusPill(status: PlantStatus) {
    val (bg, fg) = statusColors(status)
    Pill(status.label, bg, fg, status.icon)
}

/** Status kiezen met grote gekleurde tegels: elke status zijn eigen kleur en icoon. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusPicker(selected: PlantStatus, onSelect: (PlantStatus) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlantStatus.entries.forEach { s ->
            val (bg, fg) = statusColors(s)
            val on = s == selected
            val container by animateColorAsState(if (on) bg else Color.Transparent, label = "status")
            Row(
                Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(container)
                    .border(1.5.dp, if (on) Color.Transparent else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                    .clickable(role = Role.RadioButton) { onSelect(s) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(s.icon, null, tint = if (on) fg else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.label.capitalized(), style = MaterialTheme.typography.labelLarge, color = if (on) fg else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
fun FormError(text: String?) {
    if (text == null) return
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.errorContainer).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(10.dp))
        Text(text, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
    }
}

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
fun Thumb(name: String, photo: String?, icon: ImageVector, size: Dp = 58.dp) {
    PhotoOrPlaceholder(name, rememberPhoto(photo), icon, Modifier.size(size).clip(leaf(size * 0.42f)), size * 0.48f)
}

@Composable
fun PhotoHero(name: String, photo: String?, icon: ImageVector) {
    Box(Modifier.fillMaxWidth().height(330.dp)) {
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
    val shape = leaf(40.dp, 12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(shape)
            .background(if (image != null) Brush.linearGradient(listOf(Color.Black, Color.Black)) else placeholderBrush(name.ifBlank { "plant" }))
            .clickable(onClickLabel = "Foto kiezen") { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Row(
                Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Andere foto", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            val ink = placeholderInk(name.ifBlank { "plant" })
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AddAPhoto, null, tint = ink, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(if (hasPhoto) "Andere foto" else "Foto kiezen", style = MaterialTheme.typography.titleMedium, color = ink)
                Text("Optioneel, wordt verkleind tot 1024 px", style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f))
            }
        }
    }
}

// ---------- Invoervelden ----------

/** Gevulde velden zonder onderstreping, met ronde hoeken. */
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

val FieldShape: Shape = RoundedCornerShape(16.dp)

/** Groep velden in een kaart met een titel. */
@Composable
fun FormSection(title: String, icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary, content: @Composable ColumnScope.() -> Unit) =
    SectionCard(title, icon, tint = tint, content = content)

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
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(text(it)) }, onClick = { onSelect(it); open = false })
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBlob(icon, container, color, 32.dp)
            Spacer(Modifier.width(10.dp))
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
            if (range != null) {
                TextButton(onClick = { onChange(null, null); awaitingEnd = false }) { Text("Wissen") }
            }
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
                        label = "maand",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                    IconButton(onClick = { onChange(null) }) { Icon(Icons.Outlined.Close, "Wissen") }
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
        Box(Modifier.matchParentSize().padding(end = if (clearable && iso != null) 48.dp else 0.dp).clickable(onClickLabel = label) { open = true })
    }
    if (open) {
        val millis = iso?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val state = rememberDatePickerState(initialSelectedDateMillis = millis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Annuleren") } },
        ) { DatePicker(state) }
    }
}

/** Keuze uit een paar opties als rij chips (bijvoorbeeld de eenheid). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else Color.Transparent, label = "chip")
            Text(
                text(o),
                style = MaterialTheme.typography.labelLarge,
                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bg)
                    .border(BorderStroke(1.5.dp, if (on) Color.Transparent else MaterialTheme.colorScheme.outline), CircleShape)
                    .clickable(role = Role.RadioButton) { onSelect(o) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}
