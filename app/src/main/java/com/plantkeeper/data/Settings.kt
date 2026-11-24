package com.plantkeeper.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.settings by preferencesDataStore("settings")

object SettingKeys {
    /** Maand (yyyy-MM) waarvoor de herinneringen al verstuurd zijn. */
    val LAST_NOTIFIED_MONTH = stringPreferencesKey("lastNotifiedMonth")
    /** Tijdstip van de laatste export, in epoch-millis. */
    val LAST_EXPORT = longPreferencesKey("lastExport")
    /** Naam voor de begroeting op het startscherm. Leeg als de gebruiker het welkomstscherm oversloeg. */
    val USER_NAME = stringPreferencesKey("userName")
    /** Of het welkomstscherm al is doorlopen. */
    val WELCOMED = booleanPreferencesKey("welcomed")
}
