package com.example.aidkriyachallenge.common

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPreferences {
    private val USER_EMAIL = stringPreferencesKey("user_email")

    suspend fun saveUserEmail(context: Context, email: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_EMAIL] = email
        }
    }

    fun getUserEmail(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[USER_EMAIL]
        }
    }

    suspend fun clearUserEmail(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_EMAIL)
        }
    }
}