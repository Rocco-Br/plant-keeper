package com.plantkeeper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Seed::class, Plant::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PlantKeeperDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "plantkeeper.db").build()
    }
}
