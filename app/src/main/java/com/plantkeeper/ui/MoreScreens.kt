package com.plantkeeper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.text.input.ImeAction
import androidx.datastore.preferences.core.edit
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

    PkScaffold(title = "Meer", eyebrow = "Instellingen") { padding ->
        Column(Modifier.padding(padding)) {
            MenuRow(Icons.Outlined.Face, "Jouw naam", name.ifBlank { "Nog niet ingesteld" }, MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer, 0) { editName = true }
            MenuRow(
                Icons.Outlined.Notifications, "Herinneringen", "${seeds.count { it.reminder }} van ${seeds.size} zaden staan aan",
                MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer, 1,
            ) { nav.navigate(Routes.REMINDERS) }
            MenuRow(
                Icons.Outlined.FolderZip, "Backup", lastExport?.let { "Laatste export: ${fmtTimestamp(it)}" } ?: "Nog nooit geëxporteerd",
                pk.indoorContainer to pk.onIndoorContainer, 2,
            ) { nav.navigate(Routes.BACKUP) }
            Row(Modifier.appear(3).padding(horizontal = Space.l + Space.xs, vertical = Space.l), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Space.s))
                Hint("Plant Keeper 0.1. Alles staat alleen op dit toestel. Geen account, geen cloud.")
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, colors: Pair<Color, Color>, index: Int, onClick: () -> Unit) =
    ListRow(title, subtitle, { IconBlob(icon, colors.first, colors.second, 52.dp) }, onClick, Modifier.appear(index))

/** Naam wijzigen; de app gebruikt hem in de begroeting op het startscherm. */
@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    PkDialog("Hoe mag ik je noemen?", "Opslaan", { onSave(value) }, onDismiss) {
        Field(
            "Voornaam", value, { value = it.take(40) },
            imeAction = ImeAction.Done, onImeAction = { onSave(value) },
        )
    }
}

fun fmtTimestamp(millis: Long): String = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("nl")).format(Date(millis))

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
                Column(Modifier.padding(horizontal = Space.l, vertical = Space.s), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    Notice(Icons.Outlined.Info, "Je krijgt een melding op de eerste dag van de maand waarin een zaaiperiode begint. Begint binnen en buiten in dezelfde maand, dan krijg je één melding.")
                    if (!permission.granted) {
                        Notice(Icons.Outlined.NotificationsPaused, "Meldingen staan uit voor Plant Keeper. Zonder toestemming komen er geen herinneringen.", Tone.Warn) {
                            PkButton("Meldingen toestaan", permission.request, icon = Icons.Outlined.Notifications)
                        }
                    }
                }
            }
            item { ListHeader("Per zaad · ${seeds.count { it.reminder }} aan") }
            if (withPeriod.isEmpty()) item(key = "leeg") { EmptyState("Nog geen zaden met een zaaiperiode.", Mood.SLEEPY, Modifier.animateItem()) }
            itemsIndexed(withPeriod, key = { _, it -> it.id }) { index, s ->
                val toggle = { on: Boolean ->
                    scope.launch { dao.setReminder(s.id, on) }
                    if (on && !permission.granted) permission.request()
                }
                ListRow(
                    title = s.name,
                    subtitle = if (s.reminder) "Melding op ${fmtLongDate(s.nextReminderDate(today)!!)}" else "Uit",
                    leading = { Thumb(s.name, s.photo, Icons.Outlined.Grain) },
                    // De hele rij schakelt; de schakelaar zelf is alleen weergave, zo is er één tikdoel.
                    onClick = { toggle(!s.reminder) },
                    modifier = Modifier.animateItem().appear(index),
                    trailing = { Switch(s.reminder, onCheckedChange = null) },
                )
            }
        }
    }
}
