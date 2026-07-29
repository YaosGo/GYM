package com.gym.app.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_settings")

/** 应用设置管理 */
class AppSettings(private val context: Context) {

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")       // "zh" | "en"
        val KEY_WEIGHT_UNIT = stringPreferencesKey("weight_unit")  // "kg" | "lb"
        val KEY_GENDER = stringPreferencesKey("gender")            // "male" | "female"
        val KEY_HEIGHT = floatPreferencesKey("height_cm")
        val KEY_WEIGHT = floatPreferencesKey("weight_kg")
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "zh"
    }

    val weightUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEIGHT_UNIT] ?: "kg"
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setWeightUnit(unit: String) {
        context.dataStore.edit { it[KEY_WEIGHT_UNIT] = unit }
    }

    suspend fun setBodyInfo(gender: String?, heightCm: Float?, weightKg: Float?) {
        context.dataStore.edit { prefs ->
            gender?.let { prefs[KEY_GENDER] = it }
            heightCm?.let { prefs[KEY_HEIGHT] = it }
            weightKg?.let { prefs[KEY_WEIGHT] = it }
        }
    }

    /** 获取当前重量单位显示标签 */
    fun weightLabel(unit: String): String = when (unit) {
        "lb" -> "lb"
        else -> "kg"
    }
}
