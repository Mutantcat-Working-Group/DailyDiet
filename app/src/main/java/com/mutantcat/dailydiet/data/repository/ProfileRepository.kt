package com.mutantcat.dailydiet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mutantcat.dailydiet.core.units.EnergyUnit
import com.mutantcat.dailydiet.core.units.MassUnit
import com.mutantcat.dailydiet.domain.model.ActivityLevel
import com.mutantcat.dailydiet.domain.model.Sex
import com.mutantcat.dailydiet.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val profileFlow: Flow<UserProfile> = dataStore.data.map { preferences ->
        UserProfile(
            sex = preferences[Keys.SEX].toEnum(Sex.MALE),
            age = preferences[Keys.AGE] ?: 30,
            heightCm = preferences[Keys.HEIGHT_CM] ?: 170.0,
            currentWeightKg = preferences[Keys.CURRENT_WEIGHT_KG] ?: 70.0,
            targetWeightKg = preferences[Keys.TARGET_WEIGHT_KG] ?: 65.0,
            targetDays = preferences[Keys.TARGET_DAYS] ?: 90,
            activityLevel = preferences[Keys.ACTIVITY_LEVEL].toEnum(ActivityLevel.LIGHT),
        )
    }

    val profileConfiguredFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.PROFILE_CONFIGURED] ?: false
    }

    val energyUnitFlow: Flow<EnergyUnit> = dataStore.data.map { preferences ->
        preferences[Keys.ENERGY_UNIT].toEnum(EnergyUnit.KILOCALORIE)
    }

    val massUnitFlow: Flow<MassUnit> = dataStore.data.map { preferences ->
        preferences[Keys.MASS_UNIT].toEnum(MassUnit.GRAM)
    }

    suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[Keys.SEX] = profile.sex.name
            preferences[Keys.AGE] = profile.age
            preferences[Keys.HEIGHT_CM] = profile.heightCm
            preferences[Keys.CURRENT_WEIGHT_KG] = profile.currentWeightKg
            preferences[Keys.TARGET_WEIGHT_KG] = profile.targetWeightKg
            preferences[Keys.TARGET_DAYS] = profile.targetDays
            preferences[Keys.ACTIVITY_LEVEL] = profile.activityLevel.name
            preferences[Keys.PROFILE_CONFIGURED] = true
        }
    }

    suspend fun saveEnergyUnit(unit: EnergyUnit) {
        dataStore.edit { it[Keys.ENERGY_UNIT] = unit.name }
    }

    suspend fun saveMassUnit(unit: MassUnit) {
        dataStore.edit { it[Keys.MASS_UNIT] = unit.name }
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
        if (this == null) fallback else runCatching { enumValueOf<T>(this) }.getOrDefault(fallback)

    private object Keys {
        val SEX = stringPreferencesKey("profile_sex")
        val AGE = intPreferencesKey("profile_age")
        val HEIGHT_CM = doublePreferencesKey("profile_height_cm")
        val CURRENT_WEIGHT_KG = doublePreferencesKey("profile_current_weight_kg")
        val TARGET_WEIGHT_KG = doublePreferencesKey("profile_target_weight_kg")
        val TARGET_DAYS = intPreferencesKey("profile_target_days")
        val ACTIVITY_LEVEL = stringPreferencesKey("profile_activity_level")
        val PROFILE_CONFIGURED = booleanPreferencesKey("profile_configured")
        val ENERGY_UNIT = stringPreferencesKey("unit_energy")
        val MASS_UNIT = stringPreferencesKey("unit_mass")
    }
}

