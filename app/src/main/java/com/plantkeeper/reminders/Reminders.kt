package com.plantkeeper.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.plantkeeper.MainActivity
import com.plantkeeper.PlantKeeperApp
import com.plantkeeper.R
import com.plantkeeper.data.Seed
import com.plantkeeper.data.SettingKeys
import com.plantkeeper.data.indoor
import com.plantkeeper.data.outdoor
import com.plantkeeper.data.seedsToNotify
import com.plantkeeper.data.settings
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

private const val CHANNEL = "zaaiherinneringen"

object Reminders {
    fun setUp(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Zaaiherinneringen", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Melding op de eerste dag van de maand waarin een zaaiperiode begint"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "dagelijkse-zaaicontrole",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun notify(context: Context, seed: Seed, month: Int) {
        if (!canNotify(context)) return
        val where = listOfNotNull(
            "binnen".takeIf { seed.indoor?.start == month },
            "buiten".takeIf { seed.outdoor?.start == month },
        ).joinToString(" en ")
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zaaitijd: ${seed.name}")
            .setContentText("Vanaf deze maand kun je ${seed.name} $where zaaien.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(seed.id.hashCode(), n)
        } catch (_: SecurityException) {
            // Permissie net ingetrokken, dan geen melding.
        }
    }
}

/** Dagelijkse controle. Meldt één keer per maand, voor zaden waarvan een zaaiperiode deze maand begint. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val store = applicationContext.settings
        val last = store.data.first()[SettingKeys.LAST_NOTIFIED_MONTH]?.let(YearMonth::parse)
        // Eerste run na installatie: niet meteen melden voor de lopende maand.
        if (last != null) {
            val seeds = (applicationContext as PlantKeeperApp).db.dao().allSeeds()
            seedsToNotify(seeds, today, last).forEach { Reminders.notify(applicationContext, it, today.monthValue) }
        }
        store.edit { it[SettingKeys.LAST_NOTIFIED_MONTH] = YearMonth.from(today).toString() }
        return Result.success()
    }
}
