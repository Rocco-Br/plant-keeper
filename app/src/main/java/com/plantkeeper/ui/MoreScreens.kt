package com.plantkeeper.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.datastore.preferences.core.edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import com.plantkeeper.data.SettingKeys
import com.plantkeeper.data.nextReminderDate
import com.plantkeeper.data.reminderMonths
import com.plantkeeper.data.settings
import com.plantkeeper.reminders.Reminders
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/** Of meldingen mogen, plus een actie om toestemming te vragen. Wordt bij terugkeren naar de app opnieuw gecontroleerd. */
class NotificationPermission(val granted: Boolean, val request: () -> Unit)

@Composable
fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Reminders.canNotify(context)) }
    LifecycleResumeEffect(Unit) {
        granted = Reminders.canNotify(context)
        onPauseOrDispose {}
    }
    val openSettings = {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = Reminders.canNotify(context)
        // Twee keer geweigerd: Android toont de vraag niet meer, dus naar de instellingen.
        if (!ok && Build.VERSION.SDK_INT >= 33 &&
            !(context as Activity).shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        ) openSettings()
    }
    return NotificationPermission(granted) {
        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else if (!Reminders.canNotify(context)) openSettings()
    }
}

@Composable
fun MoreScreen(nav: NavController) {
    val context = LocalContext.current
    val seeds by app().db.dao().seeds().collectAsState(initial = emptyList())
    val lastExport by remember { context.settings.data.map { it[SettingKeys.LAST_EXPORT] } }.collectAsState(initial = null)
    val name by remember { context.settings.data.map { it[SettingKeys.USER_NAME].orEmpty() } }.collectAsState(initial = "")
    var editName by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (editName) {
        NameDialog(name, onDismiss = { editName = false }) { new ->
            editName = false
            scope.launch { context.settings.edit { it[SettingKeys.USER_NAME] = new.trim() } }
        }
    }

    PkScaffold(title = "Meer", eyebrow = "Instellingen") { _ ->
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MenuRow(
                Icons.Filled.Face, "Jouw naam", name.ifBlank { "Nog niet ingesteld" },
                MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer,
            ) { editName = true }
            MenuRow(
                Icons.Filled.Notifications, "Herinneringen", "${seeds.count { it.reminder }} van ${seeds.size} zaden staan aan",
                MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
            ) { nav.navigate(Routes.REMINDERS) }
            MenuRow(
                Icons.Filled.FolderZip, "Backup", lastExport?.let { "Laatste export: ${fmtTimestamp(it)}" } ?: "Nog nooit geëxporteerd",
                pk.indoorContainer, pk.onIndoorContainer,
            ) { nav.navigate(Routes.BACKUP) }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Hint("Plant Keeper 0.1. Alles staat alleen op dit toestel. Geen account, geen cloud.")
            }
        }
    }
}

/** Naam wijzigen; de app gebruikt hem in de begroeting op het startscherm. */
@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hoe mag ik je noemen?", style = MaterialTheme.typography.headlineSmall) },
        text = {
            TextField(
                value = value, onValueChange = { value = it.take(40) }, singleLine = true,
                placeholder = { Text("Je voornaam") }, shape = FieldShape, colors = pkFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Opslaan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
        shape = MaterialTheme.shapes.extraLarge,
    )
}

fun fmtTimestamp(millis: Long): String = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("nl")).format(Date(millis))

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBlob(icon, bg, fg, 52.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Chevron()
    }
}

@Composable
fun Note(icon: ImageVector, text: String, warn: Boolean = false, action: @Composable () -> Unit = {}) {
    val bg = if (warn) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val fg = if (warn) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(bg).padding(16.dp)) {
        Icon(icon, null, tint = fg)
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, color = fg, style = MaterialTheme.typography.bodyMedium)
            action()
        }
    }
}

@Composable
fun RemindersScreen(nav: NavController) {
    val dao = app().db.dao()
    val seeds by dao.seeds().collectAsState(initial = emptyList())
    val permission = rememberNotificationPermission()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val withPeriod = seeds.filter { it.reminderMonths().isNotEmpty() }

    PkScaffold(title = "Herinneringen", onBack = { nav.popBackStack() }) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Note(Icons.Outlined.Info, "Je krijgt een melding op de eerste dag van de maand waarin een zaaiperiode begint. Begint binnen en buiten in dezelfde maand, dan krijg je één melding.")
                    if (!permission.granted) {
                        Note(Icons.Outlined.NotificationsPaused, "Meldingen staan uit voor Plant Keeper. Zonder toestemming komen er geen herinneringen.", warn = true) {
                            Button(onClick = permission.request) { Text("Meldingen toestaan") }
                        }
                    }
                }
            }
            item {
                Text(
                    "Per zaad · ${seeds.count { it.reminder }} aan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            if (withPeriod.isEmpty()) item { EmptyState(Icons.Outlined.Grain, "Nog geen zaden met een zaaiperiode.") }
            items(withPeriod, key = { it.id }) { s ->
                ListRow(
                    title = s.name,
                    subtitle = if (s.reminder) "Melding op ${fmtLongDate(s.nextReminderDate(today)!!)}" else "Uit",
                    leading = { Thumb(s.name, s.photo, Icons.Outlined.Grain, 40.dp) },
                    onClick = null,
                    trailing = {
                        Switch(s.reminder, { on ->
                            scope.launch { dao.setReminder(s.id, on) }
                            if (on && !permission.granted) permission.request()
                        })
                    },
                )
            }
        }
    }
}
