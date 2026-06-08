package com.enterprise.netconfigdiff.data.remote.interceptors

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "auth_prefs")
    private val tokenKey = stringPreferencesKey("jwt_access_token")
    private val refreshTokenKey = stringPreferencesKey("jwt_refresh_token")

    fun getAccessToken(): String? = runBlocking {
        context.dataStore.data.first()[tokenKey]
    }

    fun saveTokens(accessToken: String, refreshToken: String) = runBlocking {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
        }
    }

    fun clearTokens() = runBlocking {
        context.dataStore.edit { prefs ->
            prefs.remove(tokenKey)
            prefs.remove(refreshTokenKey)
        }
    }
}

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getAccessToken()

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("X-Request-ID", java.util.UUID.randomUUID().toString())
                .build()
        } else {
            originalRequest.newBuilder()
                .header("X-Request-ID", java.util.UUID.randomUUID().toString())
                .build()
        }

        return chain.proceed(newRequest)
    }
}
