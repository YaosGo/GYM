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
        val KEY_SHOW_IMAGE_IMPORT = booleanPreferencesKey("show_image_import")
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "zh"
    }

    val weightUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEIGHT_UNIT] ?: "kg"
    }

    /** 当前体重（kg），0 表示未设置。 */
    val bodyWeightKg: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEIGHT] ?: 0f
    }

    /** 当前身高（cm），0 表示未设置。 */
    val heightCm: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_HEIGHT] ?: 0f
    }

    /** 当前性别，"" 表示未设置。 */
    val gender: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GENDER] ?: ""
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

    /** 单独更新当前体重（kg），训练开始确认体重时同步回写。 */
    suspend fun setBodyWeight(weightKg: Float) {
        context.dataStore.edit { it[KEY_WEIGHT] = weightKg }
    }

    /** 是否显示计划页「从图片导入」入口（默认开）。 */
    val showImageImport: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_IMAGE_IMPORT] ?: true
    }

    suspend fun setShowImageImport(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_IMAGE_IMPORT] = enabled }
    }

    /** 获取当前重量单位显示标签 */
    fun weightLabel(unit: String): String = when (unit) {
        "lb" -> "lb"
        else -> "kg"
    }
}
