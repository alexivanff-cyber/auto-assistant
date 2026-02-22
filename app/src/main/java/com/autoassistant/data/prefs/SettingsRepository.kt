package com.autoassistant.data.prefs
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object PreferencesKeys {
        val ASSISTANT_ENABLED = booleanPreferencesKey("assistant_enabled")
        val BT_ONLY = booleanPreferencesKey("bt_only")
        val CONFIRM_SEND = booleanPreferencesKey("confirm_send")
        val IGNORE_GROUPS = booleanPreferencesKey("ignore_groups")
    }
    val assistantEnabled: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.ASSISTANT_ENABLED] ?: false }
    val btOnly: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.BT_ONLY] ?: false }
    val confirmSend: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.CONFIRM_SEND] ?: true }
    val ignoreGroups: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.IGNORE_GROUPS] ?: true }
    suspend fun setAssistantEnabled(value: Boolean) = update(PreferencesKeys.ASSISTANT_ENABLED, value)
    suspend fun setBtOnly(value: Boolean) = update(PreferencesKeys.BT_ONLY, value)
    suspend fun setConfirmSend(value: Boolean) = update(PreferencesKeys.CONFIRM_SEND, value)
    suspend fun setIgnoreGroups(value: Boolean) = update(PreferencesKeys.IGNORE_GROUPS, value)
    private suspend fun <T> update(key: Preferences.Key<T>, value: T) { context.dataStore.edit { it[key] = value } }
}
