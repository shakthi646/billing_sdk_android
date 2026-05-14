package com.billingsdk.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.billingsdk.model.PremiumStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.billingSdkPremiumDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "billing_sdk_premium_cache",
)

/**
 * Persists a client-side premium snapshot for offline UX and faster startup.
 */
@Singleton
class PremiumCacheStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val ds = context.billingSdkPremiumDataStore

    val premiumStatus: Flow<PremiumStatus> = ds.data.map { prefs ->
        PremiumStatus(
            isPremium = prefs[KEY_IS_PREMIUM] ?: false,
            activePlan = prefs[KEY_ACTIVE_PLAN],
            expiryDateMillis = prefs[KEY_EXPIRY]?.takeIf { it > 0L },
            purchaseToken = prefs[KEY_PURCHASE_TOKEN],
            isLifetime = prefs[KEY_IS_LIFETIME] ?: false,
            lastSyncedMillis = prefs[KEY_LAST_SYNCED] ?: 0L,
        )
    }

    suspend fun write(status: PremiumStatus) {
        ds.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = status.isPremium
            if (status.activePlan != null) prefs[KEY_ACTIVE_PLAN] = status.activePlan
            else prefs.remove(KEY_ACTIVE_PLAN)
            if (status.expiryDateMillis != null) prefs[KEY_EXPIRY] = status.expiryDateMillis
            else prefs.remove(KEY_EXPIRY)
            if (status.purchaseToken != null) prefs[KEY_PURCHASE_TOKEN] = status.purchaseToken
            else prefs.remove(KEY_PURCHASE_TOKEN)
            prefs[KEY_IS_LIFETIME] = status.isLifetime
            prefs[KEY_LAST_SYNCED] = status.lastSyncedMillis
        }
    }

    private companion object {
        val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        val KEY_ACTIVE_PLAN = stringPreferencesKey("active_plan")
        val KEY_EXPIRY = longPreferencesKey("expiry_date_ms")
        val KEY_PURCHASE_TOKEN = stringPreferencesKey("purchase_token")
        val KEY_IS_LIFETIME = booleanPreferencesKey("is_lifetime")
        val KEY_LAST_SYNCED = longPreferencesKey("last_synced_ms")
    }
}
