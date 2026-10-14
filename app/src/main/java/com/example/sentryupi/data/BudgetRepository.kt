package com.example.sentryupi.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sentry_upi_budget_prefs")

data class BudgetConfig(
    val lastFourDigits: String = "1234",
    val currentBalance: Double = 15000.0,
    val greenYellowThreshold: Double = 10000.0,
    val yellowRedThreshold: Double = 3000.0,
    val minBalanceToMaintain: Double = 2000.0,
    val lastVerifiedTimestamp: Long = System.currentTimeMillis()
) {
    fun getZone(): BudgetZone {
        return when {
            currentBalance > greenYellowThreshold -> BudgetZone.GREEN
            currentBalance in yellowRedThreshold..greenYellowThreshold -> BudgetZone.YELLOW
            else -> BudgetZone.RED
        }
    }

    fun isVerificationDue(): Boolean {
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastVerifiedTimestamp) >= sevenDaysMs
    }
}

enum class BudgetZone { GREEN, YELLOW, RED }

class BudgetRepository(private val context: Context) {

    companion object {
        private val KEY_LAST_FOUR = stringPreferencesKey("last_four_digits")
        private val KEY_BALANCE = doublePreferencesKey("current_balance")
        private val KEY_A1 = doublePreferencesKey("threshold_a1")
        private val KEY_A2 = doublePreferencesKey("threshold_a2")
        private val KEY_MIN_BAL = doublePreferencesKey("min_balance")
        private val KEY_LAST_VERIFIED = longPreferencesKey("last_verified_timestamp")
    }

    val budgetConfigFlow: Flow<BudgetConfig> = context.dataStore.data.map { prefs ->
        BudgetConfig(
            lastFourDigits = prefs[KEY_LAST_FOUR] ?: "1234",
            currentBalance = prefs[KEY_BALANCE] ?: 15000.0,
            greenYellowThreshold = prefs[KEY_A1] ?: 10000.0,
            yellowRedThreshold = prefs[KEY_A2] ?: 3000.0,
            minBalanceToMaintain = prefs[KEY_MIN_BAL] ?: 2000.0,
            lastVerifiedTimestamp = prefs[KEY_LAST_VERIFIED] ?: System.currentTimeMillis()
        )
    }

    suspend fun saveConfig(config: BudgetConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_FOUR] = config.lastFourDigits
            prefs[KEY_BALANCE] = config.currentBalance
            prefs[KEY_A1] = config.greenYellowThreshold
            prefs[KEY_A2] = config.yellowRedThreshold
            prefs[KEY_MIN_BAL] = config.minBalanceToMaintain
            prefs[KEY_LAST_VERIFIED] = System.currentTimeMillis() // Resets 7-day timer on save
        }
    }

    suspend fun deductBalance(amount: Double): Double {
        var newBalance = 0.0
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BALANCE] ?: 15000.0
            newBalance = (current - amount).coerceAtLeast(0.0)
            prefs[KEY_BALANCE] = newBalance
        }
        return newBalance
    }
}