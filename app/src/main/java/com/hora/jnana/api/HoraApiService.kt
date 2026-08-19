package com.hora.jnana.api

import com.hora.jnana.BuildConfig
import com.hora.jnana.data.AuthRepository
import com.hora.jnana.api.AuthInterceptor
import com.hora.jnana.api.SessionInvalidationInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface HoraApiService {
    @GET("api/v1/all")
    suspend fun getAllRaw(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("lang") lang: String = "en"
    ): ResponseBody

    @GET("api/v1/panchanga")
    suspend fun getPanchanga(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("lang") lang: String = "en"
    ): ResponseBody

    @GET("api/v1/hora")
    suspend fun getHora(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("lang") lang: String = "en"
    ): ResponseBody

    @GET("api/v1/muhurta")
    suspend fun getMuhurta(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("lang") lang: String = "en"
    ): ResponseBody

    @GET("api/v1/day")
    suspend fun getDay(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("lang") lang: String = "en"
    ): ResponseBody

    @GET("api/v1/dasha")
    suspend fun getDasha(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("lang") lang: String = "en",
        @Query("depth") depth: Int? = null
    ): com.hora.jnana.models.DashaResponse

    @GET("api/v1/dasha/birth")
    suspend fun getBirthDasha(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("lang") lang: String = "en",
        @Query("depth") depth: Int? = null
    ): com.hora.jnana.models.DashaResponse

    @GET("api/v1/kundali")
    suspend fun getKundali(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("lang") lang: String = "en"
    ): com.hora.jnana.models.KundaliResponse

    @GET("api/v1/kundali/birth")
    suspend fun getBirthKundali(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("lang") lang: String = "en"
    ): com.hora.jnana.models.KundaliResponse

    @GET("api/v1/kundali/birth/svg")
    suspend fun getBirthKundaliSvg(
        @Query("lat") lat: String? = null,
        @Query("lon") lon: String? = null,
        @Query("location") location: String? = null,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("name") name: String? = null,
        @Query("lang") lang: String = "en",
        @Query("chart_style") chartStyle: String = "south"
    ): ResponseBody

    @POST("api/v1/auth/logout")
    suspend fun logout(): ResponseBody

    @GET("api/v1/locations")
    suspend fun getLocations(): Map<String, @JvmSuppressWildcards Map<String, @JvmSuppressWildcards Any>>

    @POST("api/v1/locations")
    suspend fun addLocation(@Body location: @JvmSuppressWildcards Map<String, @JvmSuppressWildcards Any?>): ResponseBody

    @DELETE("api/v1/locations/{name}")
    suspend fun deleteLocation(@Path("name") name: String): ResponseBody

    companion object {
        fun create(
            authRepository: AuthRepository,
            onSessionExpired: () -> Unit,
            moshi: Moshi,
            baseUrl: String = BuildConfig.BASE_URL
        ): HoraApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(authRepository))
                .addInterceptor(SessionInvalidationInterceptor(onSessionExpired))
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            return retrofit.create(HoraApiService::class.java)
        }
    }
}
