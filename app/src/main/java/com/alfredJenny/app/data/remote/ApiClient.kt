package com.alfredJenny.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"

/**
 * In-memory store for the JWT token, user role, and configurable base URL.
 * Populated by AuthRepository on login/session restore.
 * Read by interceptors on every request — no Retrofit rebuild needed.
 */
@Singleton
class TokenStore @Inject constructor() {
    @Volatile var token: String = ""
    @Volatile var role: String = ""
    @Volatile var baseUrl: String = DEFAULT_BASE_URL
}

/**
 * Adds "Authorization: Bearer <token>" to every request when a token is present.
 * Skips the auth/login endpoint to avoid sending an empty header on first login.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (tokenStore.token.isBlank() || original.url.encodedPath.endsWith("auth/login")) {
            return chain.proceed(original)
        }
        return chain.proceed(
            original.newBuilder()
                .header("Authorization", "Bearer ${tokenStore.token}")
                .build()
        )
    }
}

/**
 * Rewrites the host/scheme/port of every request to match the user-configured base URL.
 * This lets us swap the backend URL at runtime without recreating Retrofit.
 */
class DynamicUrlInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val base = tokenStore.baseUrl.trimEnd('/').toHttpUrlOrNull()
            ?: return chain.proceed(original)
        val newUrl = original.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
