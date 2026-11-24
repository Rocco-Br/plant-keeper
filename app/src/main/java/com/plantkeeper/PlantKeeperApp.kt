package com.plantkeeper

import android.app.Application
import com.plantkeeper.data.AppDatabase
import com.plantkeeper.data.PhotoStore
import com.plantkeeper.reminders.Reminders
import java.io.File

class PlantKeeperApp : Application() {
    val db by lazy { AppDatabase.create(this) }
    val photos by lazy { PhotoStore(File(filesDir, "photos")) }

    override fun onCreate() {
        super.onCreate()
        Reminders.setUp(this)
    }
}
