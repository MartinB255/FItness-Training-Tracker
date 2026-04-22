package com.fittrack.app.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton Retrofit client.
 *
 * Call RetrofitClient.init(context) once on app startup,
 * then use RetrofitClient.api anywhere to make requests.
 *
 * The auth interceptor automatically adds the token header
 * to every request if the user is logged in.
 */
object RetrofitClient {

    // 10.0.2.2 is the Android emulator's alias for localhost
    // If testing on a real device, use your PC's local IP instead
    // (e.g. "http://192.168.1.42:8000/api/")
    private const val BASE_URL =
        "http://10.0.0.25:8000/api/"
       // "http://10.0.2.2:8000/api/"

    lateinit var api: FitTrackApi
        private set

    private lateinit var appContext: Context

    /**
     * Initialize the client. Call this in Application.onCreate()
     * or in your launcher activity before any API calls.
     */
    fun init(context: Context) {
        appContext = context.applicationContext

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Interceptor that attaches "Authorization: Token xxx" header
        val authInterceptor = Interceptor { chain ->
            val token = getToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Token $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(FitTrackApi::class.java)
    }

    // ── Token management via SharedPreferences ──────────────────

    private const val PREFS_NAME = "fittrack_prefs"
    private const val KEY_TOKEN = "auth_token"

    fun saveToken(token: String) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(): String? {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }

    fun clearToken() {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TOKEN)
            .apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null
}
