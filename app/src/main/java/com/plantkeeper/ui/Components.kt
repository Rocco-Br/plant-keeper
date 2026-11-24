package com.plantkeeper.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

/** Rootschermen (met bottom bar) krijgen een grote kop, subschermen een app bar met terugknop. */
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
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(backIcon, "Terug") } },
                    actions = actions,
                )
            }
        },
        floatingActionButton = fab,
    ) { padding ->
        if (onBack == null) {
            Column(Modifier.padding(padding)) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                    if (eyebrow != null) Eyebrow(eyebrow)
                    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                content(PaddingValues(bottom = 88.dp))
            }
        } else {
            content(padding)
        }
    }
}

@Composable
fun Eyebrow(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
}

@Composable
fun SectionCard(title: String? = null, icon: ImageVector? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            content()
        }
    }
}

@Composable
fun Fact(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

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
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Badge(text: String, icon: ImageVector? = null, warn: Boolean = true) {
    val bg = if (warn) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val fg = if (warn) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusPill(status: PlantStatus) {
    val (bg, fg) = statusColors(status)
    Text(
        status.label,
        Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}

@Composable
fun FormError(text: String?) {
    if (text == null) return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------- Foto's ----------

/** Zachte placeholderkleur, afgeleid van de naam zodat elk item zijn eigen tint houdt. */
@Composable
fun placeholderColor(name: String): Color {
    val hue = ((name.hashCode() % 360) + 360) % 360
    return if (isSystemInDarkTheme()) Color.hsl(hue.toFloat(), 0.25f, 0.22f) else Color.hsl(hue.toFloat(), 0.35f, 0.85f)
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
    Box(modifier.background(placeholderColor(name)), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(image, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun Thumb(name: String, photo: String?, icon: ImageVector, size: Dp = 52.dp) {
    PhotoOrPlaceholder(name, rememberPhoto(photo), icon, Modifier.size(size).clip(RoundedCornerShape(14.dp)), 26.dp)
}

@Composable
fun PhotoHero(name: String, photo: String?, icon: ImageVector) {
    PhotoOrPlaceholder(name, rememberPhoto(photo), icon, Modifier.fillMaxWidth().height(220.dp), 64.dp)
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
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(placeholderColor(name))
            .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) Image(image, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Column(
            Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.AddAPhoto, null)
            Text(if (hasPhoto) "Andere foto" else "Foto kiezen", fontWeight = FontWeight.SemiBold)
            if (!hasPhoto) Text("Optioneel, wordt verkleind tot 1024 px", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ---------- Invoervelden ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = text(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(text(it)) }, onClick = { onSelect(it); open = false })
            }
        }
    }
}

@Composable
fun MonthField(label: String, month: Int?, onChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    DropdownField(label, listOf<Int?>(null) + (1..12), month, { if (it == null) "Geen" else MONTHS[it - 1].capitalized() }, onChange, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, iso: String?, onChange: (String?) -> Unit, modifier: Modifier = Modifier, clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
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
            modifier = Modifier.fillMaxWidth(),
        )
        // Klikvlak over het veld, behalve het wisknopje.
        Box(Modifier.matchParentSize().padding(end = if (clearable && iso != null) 48.dp else 0.dp).clickable { open = true })
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
