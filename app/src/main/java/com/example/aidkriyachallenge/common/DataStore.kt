package com.example.aidkriyachallenge.common

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private val USER_EMAIL = stringPreferencesKey("user_email")
    private val USER_ID = stringPreferencesKey("user_id")

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_EMAIL] = email
        }
    }

    fun getUserEmail(): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[USER_EMAIL]
        }
    }

    suspend fun clearUserEmail() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_EMAIL)
        }
    }

    suspend fun saveUserID(uid: String){
        context.dataStore.edit {prefs->
            prefs[USER_ID] = uid
        }
    }

    fun getUserid(): Flow<String?>{
        return context.dataStore.data.map {prefs->
            prefs[USER_ID]
        }

    }

    suspend fun clearUid(){
        context.dataStore.edit { prefs->
            prefs.remove(USER_ID)
        }
    }
}