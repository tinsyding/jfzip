package com.tinsyding.jfzip.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val name: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[NAME_KEY].orEmpty()
    }

    suspend fun setName(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[NAME_KEY] = value
        }
    }

    private companion object {
        val NAME_KEY = stringPreferencesKey("name")
    }
}
