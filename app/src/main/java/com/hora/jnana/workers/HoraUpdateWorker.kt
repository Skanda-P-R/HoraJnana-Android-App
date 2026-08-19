package com.hora.jnana.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hora.jnana.BuildConfig
import com.hora.jnana.api.HoraApiService
import com.hora.jnana.repository.HoraRepository
import com.hora.jnana.DataStoreManager
import com.hora.jnana.data.AuthRepository
import com.hora.jnana.utils.WidgetUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first

class HoraUpdateWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (BuildConfig.DEBUG) {
            Log.d("HoraUpdateWorker", "Starting background update...")
        }
        
        val dataStoreManager = DataStoreManager(applicationContext)
        val authRepository = AuthRepository(applicationContext)
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        
        authRepository.getSessionTokenBlocking() ?: run {
            if (BuildConfig.DEBUG) Log.e("HoraUpdateWorker", "Session token missing, skipping update")
            return Result.failure()
        }

        val location = dataStoreManager.locationFlow.first()
        val locationName = dataStoreManager.locationNameFlow.first()
        val lang = dataStoreManager.langFlow.first()
        val apiBase = dataStoreManager.apiBaseFlow.first()
        
        try {
            val api = HoraApiService.create(
                authRepository = authRepository,
                onSessionExpired = {
                    authRepository.notifySessionExpired()
                },
                moshi = moshi,
                baseUrl = apiBase
            )
            val repo = HoraRepository(api, applicationContext, moshi)
            
            val lat = location?.first
            val lon = location?.second

            // These will only hit the network if the Vedic Day has changed
            repo.fetchDay(lat, lon, locationName, lang = lang, force = false)
            repo.fetchPanchanga(lat, lon, locationName, lang = lang, force = false)
            repo.fetchMuhurta(lat, lon, locationName, lang = lang, force = false)
            val horaRes = repo.fetchHora(lat, lon, locationName, lang = lang, force = false)
            
            return if (horaRes.isSuccess) {
                if (BuildConfig.DEBUG) Log.d("HoraUpdateWorker", "Update successful, refreshing widgets")
                WidgetUtils.updateAllWidgets(applicationContext)
                Result.success()
            } else {
                if (BuildConfig.DEBUG) Log.w("HoraUpdateWorker", "Update failed, retrying...")
                Result.retry()
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("HoraUpdateWorker", "Error during update", e)
            return Result.retry()
        }
    }
}
