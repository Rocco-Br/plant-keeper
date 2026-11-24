package com.plantkeeper.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.plantkeeper.backup.Backup
import com.plantkeeper.backup.BackupData
import com.plantkeeper.backup.BackupException
import com.plantkeeper.data.SettingKeys
import com.plantkeeper.data.settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun BackupScreen(nav: NavController) {
    val context = LocalContext.current
    val app = app()
    val dao = app.db.dao()
    val scope = rememberCoroutineScope()
    val seeds by dao.seeds().collectAsState(initial = emptyList())
    val plants by dao.plants().collectAsState(initial = emptyList())
    val lastExport by remember { context.settings.data.map { it[SettingKeys.LAST_EXPORT] } }.collectAsState(initial = null)
    val photoCount = (seeds.map { it.photo } + plants.map { it.photo }).count { it != null }

    var zip by rememberSaveable { mutableStateOf<String?>(null) }
    var replace by rememberSaveable { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val data = BackupData(exportedAt = LocalDateTime.now().toString(), seeds = dao.allSeeds(), plants = dao.allPlants())
                    context.contentResolver.openOutputStream(uri)!!.use { Backup.write(it, data, app.photos.dir) }
                }
                context.settings.edit { it[SettingKeys.LAST_EXPORT] = System.currentTimeMillis() }
                message = "Export klaar: ${count(seeds.size, "zaad", "zaden")}, ${count(plants.size, "plant", "planten")}."; error = null
            } catch (e: Exception) {
                error = "Exporteren mislukt: ${e.message}"
            }
            busy = false
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { zip = uri.toString(); message = null; error = null }
    }

    fun doImport() {
        val uri = Uri.parse(zip ?: return)
        busy = true
        scope.launch {
            val staging = File(context.cacheDir, "import")
            try {
                val data = withContext(Dispatchers.IO) {
                    staging.deleteRecursively()
                    context.contentResolver.openInputStream(uri)!!.use { Backup.read(it, staging) }
                }
                withContext(Dispatchers.IO) { Backup.apply(dao, app.photos, data, staging, replace) }
                message = "Import klaar: ${count(data.seeds.size, "zaad", "zaden")} en ${count(data.plants.size, "plant", "planten")} ${if (replace) "teruggezet" else "samengevoegd"}."
                error = null
                zip = null
            } catch (e: BackupException) {
                error = e.message
            } catch (e: Exception) {
                error = "Importeren mislukt: ${e.message}"
            } finally {
                staging.deleteRecursively()
                busy = false
            }
        }
    }

    PkScaffold(title = "Backup", onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FormError(error)
            message?.let { Note(Icons.Outlined.FolderZip, it) }
            SectionCard("Exporteren", Icons.Outlined.Download, tint = pk.outdoor) {
                Text("Eén ZIP met al je zaden, planten en foto's. Bewaar hem op een veilige plek, het bestand is niet versleuteld.")
                Hint("${count(seeds.size, "zaad", "zaden")} · ${count(plants.size, "plant", "planten")} · ${count(photoCount, "foto", "foto's")}")
                BigButton("Exporteer ZIP", Icons.Outlined.Download, { exportLauncher.launch("plant-keeper-${LocalDate.now()}.zip") }, enabled = !busy)
                lastExport?.let { Hint("Laatste export: ${fmtTimestamp(it)}") }
            }
            SectionCard("Importeren", Icons.Outlined.Upload, tint = pk.indoor) {
                Text("Kies een eerder geëxporteerde ZIP en bepaal wat er met je huidige data gebeurt.")
                FilledTonalButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text(zip?.let { displayName(context, Uri.parse(it)) } ?: "Kies ZIP-bestand")
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(!replace, { replace = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Samenvoegen") }
                    SegmentedButton(replace, { replace = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Vervangen") }
                }
                Hint(
                    if (replace) "Al je huidige zaden, planten en foto's worden gewist en vervangen door de inhoud van de ZIP."
                    else "Items die al bestaan worden bijgewerkt, nieuwe items worden toegevoegd. Er gaat niets verloren.",
                )
                BigButton("Importeer", Icons.Outlined.Upload, { if (replace) confirmReplace = true else doImport() }, enabled = zip != null && !busy)
            }
            Note(Icons.Outlined.Warning, "Verwijder je de app zonder te exporteren, dan ben je al je gegevens kwijt. Er is geen cloud die ze bewaart.", warn = true)
        }
    }

    if (confirmReplace) {
        ConfirmDialog(
            title = "Alles vervangen?",
            text = "Je huidige ${count(seeds.size, "zaad", "zaden")}, ${count(plants.size, "plant", "planten")} en hun foto's worden gewist. Dit kan niet ongedaan worden gemaakt.",
            confirm = "Vervangen",
            onConfirm = { confirmReplace = false; doImport() },
            onDismiss = { confirmReplace = false },
        )
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: "Gekozen bestand"
