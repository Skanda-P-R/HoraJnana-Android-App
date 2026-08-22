package com.hora.jnana.ui.screens

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hora.jnana.repository.HoraRepository
import com.hora.jnana.utils.WidgetUtils
import com.hora.jnana.DataStoreManager
import com.hora.jnana.utils.LocationUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class HomeViewModel(private val repo: HoraRepository) : ViewModel() {
    private val _state = MutableStateFlow(PanchangaState(isLoading = true))
    val state: StateFlow<PanchangaState> = _state

    private var refreshJob: Job? = null
    private var horaJob: Job? = null

    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var lastLocName: String? = null
    private var lastLang: String? = null
    private var lastDate: String? = null

    fun refresh(context: Context, lat: Double?, lon: Double?, locationName: String? = null, force: Boolean = false) {
        viewModelScope.launch {
            // Guard against redundant refreshes with null data (typical during rotation or DataStore emission jitter)
            if (lat == null && lon == null && locationName == null && _state.value.tithi != "--") {
                return@launch
            }

            val dataStore = DataStoreManager(context)
            val lang = dataStore.langFlow.first()
            val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            // Detect if this is a refresh triggered by a settings change (location or language)
            val hasPreviousState = lastLat != null || lastLocName != null || lastLang != null
            val isLocationChanged = if (locationName != null || lastLocName != null) {
                locationName != lastLocName
            } else {
                LocationUtils.isSignificantChange(lastLat, lastLon, lat, lon)
            }
            val isLangChanged = lang != lastLang
            
            // If location or language changed significantly, we MUST force a refresh to bypass cache
            val effectiveForce = force || (hasPreviousState && (isLocationChanged || isLangChanged))

            if (!effectiveForce) {
                val isSameLocation = lat == lastLat && lon == lastLon && locationName == lastLocName
                val isMinorLocChange = !LocationUtils.isSignificantChange(lastLat, lastLon, lat, lon) && locationName == lastLocName
                val isSameSettings = lang == lastLang && todayDate == lastDate

                if ((isSameLocation || isMinorLocChange) && isSameSettings && _state.value.tithi != "--") {
                    lastLat = lat
                    lastLon = lon
                    if (_state.value.isLoading) {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                    return@launch
                }
            }

            refreshJob?.cancel()
            refreshJob = launch {
                lastLat = lat
                lastLon = lon
                lastLocName = locationName
                lastLang = lang
                lastDate = todayDate

                if (effectiveForce || _state.value.tithi == "--") {
                    _state.value = _state.value.copy(isLoading = true)
                }
                try {
                    coroutineScope {
                        val panDeferred = async { repo.fetchPanchanga(lat, lon, locationName, lang = lang, force = effectiveForce) }
                        val muhDeferred = async { repo.fetchMuhurta(lat, lon, locationName, lang = lang, force = effectiveForce) }
                        val dayDeferred = async { repo.fetchDay(lat, lon, locationName, lang = lang, force = effectiveForce) }
                        val horaDeferred = async { repo.fetchHora(lat, lon, locationName, lang = lang, force = effectiveForce) }

                        val panRes = panDeferred.await()
                        val muhRes = muhDeferred.await()
                        val dayRes = dayDeferred.await()
                        val horaRes = horaDeferred.await()

                        val merged = repo.mergeToState(
                            panchangaJson = panRes.getOrNull(),
                            muhurtaJson = muhRes.getOrNull(),
                            dayJson = dayRes.getOrNull(),
                            horaJson = horaRes.getOrNull()
                        )

                        val errorMsg = if (panRes.isFailure) {
                            val ex = panRes.exceptionOrNull()
                            if (ex is CancellationException) null else ex?.message
                        } else null

                        _state.value = merged.copy(
                            isLoading = false,
                            error = errorMsg
                        )
                        
                        if (panRes.isSuccess || horaRes.isSuccess) {
                            WidgetUtils.updateAllWidgets(context.applicationContext)
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("HomeViewModel", "Error refreshing", e)
                    _state.value = _state.value.copy(isLoading = false, error = repo.mapException(e))
                }
            }
        }
    }

    fun refreshHoraOnly(context: Context, lat: Double?, lon: Double?, locationName: String? = null) {
        horaJob?.cancel()
        horaJob = viewModelScope.launch {
            val dataStore = DataStoreManager(context)
            val lang = dataStore.langFlow.first()
            val res = repo.fetchHora(lat, lon, locationName, lang = lang, force = false)
            if (res.isSuccess) {
                val newState = repo.mergeToState(
                    panchangaJson = null,
                    muhurtaJson = null,
                    dayJson = null,
                    horaJson = res.getOrNull()
                )
                _state.value = _state.value.copy(
                    hora = newState.hora,
                    horaSymbol = newState.horaSymbol,
                    horaNext = newState.horaNext,
                    horaEnds = newState.horaEnds,
                    horaEndsAt = newState.horaEndsAt,
                    remaining = newState.remaining
                )
            }
        }
    }
}
