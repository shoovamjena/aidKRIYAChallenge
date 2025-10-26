package com.example.aidkriyachallenge.common

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private val USER_EMAIL = stringPreferencesKey("user_email")
    private val USER_ID = stringPreferencesKey("user_id")

    private val IS_WANDERER = booleanPreferencesKey("wanderer_or_walker")
    private val SESSION_ID = stringPreferencesKey("session_id")
    private val REQUEST_ID = stringPreferencesKey("request_id")

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

    suspend fun saveRole(isWanderer: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_WANDERER] = isWanderer
        }
    }

    fun getUserRole(): Flow<Boolean?> {
        return context.dataStore.data.map { prefs ->
            prefs[IS_WANDERER]
        }
    }

    suspend fun clearUserRole() {
        context.dataStore.edit { prefs ->
            prefs.remove(IS_WANDERER)
        }
    }

    suspend fun saveSessionId(sessionId: String) {
        context.dataStore.edit { prefs ->
            prefs[SESSION_ID] = sessionId
        }
    }

    fun getSessionId(): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[SESSION_ID]
        }
    }

    suspend fun clearSessionId() {
        context.dataStore.edit { prefs ->
            prefs.remove(SESSION_ID)
        }
    }

    suspend fun saveSessionInfo(sessionId: String, requestId: String) {
        context.dataStore.edit { prefs ->
            prefs[SESSION_ID] = sessionId
            prefs[REQUEST_ID] = requestId
        }
    }

    fun getSessionInfo(): Flow<Pair<String?, String?>> {
        return context.dataStore.data.map { prefs ->
            Pair(prefs[SESSION_ID], prefs[REQUEST_ID])
        }
    }

    suspend fun clearSessionInfo() {
        context.dataStore.edit { prefs ->
            prefs.remove(SESSION_ID)
            prefs.remove(REQUEST_ID)
        }
    }
}