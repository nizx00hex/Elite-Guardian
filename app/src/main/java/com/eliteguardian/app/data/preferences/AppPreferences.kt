package com.eliteguardian.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "elite_guardian_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_PROTECTION_ENABLED = booleanPreferencesKey("is_protection_enabled")
        val KEY_SAFESEARCH_ENABLED = booleanPreferencesKey("is_safesearch_enabled")
        val KEY_FOCUS_MODE_ACTIVE = booleanPreferencesKey("is_focus_mode_active")
        val KEY_FOCUS_END_TIME = longPreferencesKey("focus_end_time_millis")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
        val KEY_COOLDOWN_EXPIRY = longPreferencesKey("cooldown_expiry_millis")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val KEY_DEVICE_ADMIN_ENABLED = booleanPreferencesKey("is_device_admin_enabled")
        val KEY_TOTAL_BLOCKED_COUNT = longPreferencesKey("total_blocked_count")
        val KEY_LAST_FILTER_INIT_TIME = longPreferencesKey("last_filter_init_time")
    }

    val isProtectionEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_PROTECTION_ENABLED] ?: false
    }

    val isSafeSearchEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_SAFESEARCH_ENABLED] ?: true
    }

    val isFocusModeActive: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_FOCUS_MODE_ACTIVE] ?: false
    }

    val focusEndTime: Flow<Long> = context.dataStore.data.map {
        it[KEY_FOCUS_END_TIME] ?: 0L
    }

    val pinHash: Flow<String?> = context.dataStore.data.map {
        it[KEY_PIN_HASH]
    }

    val pinSalt: Flow<String?> = context.dataStore.data.map {
        it[KEY_PIN_SALT]
    }

    val cooldownMinutes: Flow<Int> = context.dataStore.data.map {
        it[KEY_COOLDOWN_MINUTES] ?: 0
    }

    val cooldownExpiry: Flow<Long> = context.dataStore.data.map {
        it[KEY_COOLDOWN_EXPIRY] ?: 0L
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isDeviceAdminEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_DEVICE_ADMIN_ENABLED] ?: false
    }

    val totalBlockedCount: Flow<Long> = context.dataStore.data.map {
        it[KEY_TOTAL_BLOCKED_COUNT] ?: 0L
    }

    val lastFilterInitTime: Flow<Long> = context.dataStore.data.map {
        it[KEY_LAST_FILTER_INIT_TIME] ?: 0L
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PROTECTION_ENABLED] = enabled }
    }

    suspend fun setSafeSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SAFESEARCH_ENABLED] = enabled }
    }

    suspend fun setFocusMode(active: Boolean, endTimeMillis: Long = 0L) {
        context.dataStore.edit {
            it[KEY_FOCUS_MODE_ACTIVE] = active
            it[KEY_FOCUS_END_TIME] = endTimeMillis
        }
    }

    suspend fun setPin(hash: String?, salt: String?) {
        context.dataStore.edit {
            if (hash == null || salt == null) {
                it.remove(KEY_PIN_HASH)
                it.remove(KEY_PIN_SALT)
            } else {
                it[KEY_PIN_HASH] = hash
                it[KEY_PIN_SALT] = salt
            }
        }
    }

    suspend fun setCooldownMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_COOLDOWN_MINUTES] = minutes }
    }

    suspend fun startCooldown(expiryMillis: Long) {
        context.dataStore.edit { it[KEY_COOLDOWN_EXPIRY] = expiryMillis }
    }

    suspend fun clearCooldown() {
        context.dataStore.edit { it[KEY_COOLDOWN_EXPIRY] = 0L }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setDeviceAdminEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEVICE_ADMIN_ENABLED] = enabled }
    }

    suspend fun incrementBlockedCount() {
        context.dataStore.edit {
            val current = it[KEY_TOTAL_BLOCKED_COUNT] ?: 0L
            it[KEY_TOTAL_BLOCKED_COUNT] = current + 1
        }
    }

    suspend fun setLastFilterInitTime(time: Long) {
        context.dataStore.edit { it[KEY_LAST_FILTER_INIT_TIME] = time }
    }
}

