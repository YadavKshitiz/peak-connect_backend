package com.peakconnect.app.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface TokenStore {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    fun getCachedToken(): String?
    fun getTokenFlow(): Flow<String?>
    suspend fun clearToken()
}

@Singleton
class DataStoreTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher
) : TokenStore {

    companion object {
        val KEY_ENCRYPTED_TOKEN = stringPreferencesKey("encrypted_jwt_token")
    }

    private val cachedToken = AtomicReference<String?>(null)

    override suspend fun saveToken(token: String) {
        cachedToken.set(token)
        val encrypted = cipher.encrypt(token)
        dataStore.edit { prefs ->
            prefs[KEY_ENCRYPTED_TOKEN] = encrypted
        }
    }

    override suspend fun getToken(): String? {
        val inMemory = cachedToken.get()
        if (inMemory != null) return inMemory

        val encrypted = dataStore.data.map { it[KEY_ENCRYPTED_TOKEN] }.firstOrNull() ?: return null
        val decrypted = cipher.decrypt(encrypted)
        cachedToken.set(decrypted)
        return decrypted
    }

    override fun getCachedToken(): String? {
        val inMemory = cachedToken.get()
        if (inMemory != null) return inMemory

        return runBlocking {
            getToken()
        }
    }

    override fun getTokenFlow(): Flow<String?> {
        return dataStore.data.map { prefs ->
            val encrypted = prefs[KEY_ENCRYPTED_TOKEN] ?: return@map null
            val decrypted = cipher.decrypt(encrypted)
            cachedToken.set(decrypted)
            decrypted
        }
    }

    override suspend fun clearToken() {
        cachedToken.set(null)
        dataStore.edit { prefs ->
            prefs.remove(KEY_ENCRYPTED_TOKEN)
        }
    }
}
