package com.hora.jnana.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hora.jnana.DataStoreManager
import com.hora.jnana.repository.HoraRepository
import com.hora.jnana.models.DashaResponse
import com.hora.jnana.models.KundaliResponse
import com.hora.jnana.models.DashaPeriod
import com.hora.jnana.models.LocationData
import com.hora.jnana.utils.LocationUtils
import com.hora.jnana.utils.NetworkUtils
import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.*

data class TransitState(
    val isLoading: Boolean = false,
    val dashaResponse: DashaResponse? = null,
    val kundaliResponse: KundaliResponse? = null,
    val chartUrl: String? = null,
    val error: String? = null,
    val locations: List<LocationData> = emptyList(),
    val isFetchingLocations: Boolean = false
)

class TransitViewModel(
    private val repo: HoraRepository,
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _state = MutableStateFlow(TransitState())
    val state: StateFlow<TransitState> = _state

    private val _chartLoaded = MutableStateFlow(false)
    val chartLoaded: StateFlow<Boolean> = _chartLoaded

    // Hierarchical navigation for Dasha tab
    private val _selectedL1 = MutableStateFlow<DashaPeriod?>(null)
    val selectedL1: StateFlow<DashaPeriod?> = _selectedL1

    private val _selectedL2 = MutableStateFlow<DashaPeriod?>(null)
    val selectedL2: StateFlow<DashaPeriod?> = _selectedL2

    private var fetchJob: Job? = null
    
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var lastLocName: String? = null
    private var lastDate: String? = null
    private var lastTime: String? = null
    private var lastChartStyle: String? = null
    private var lastDepth: Int? = null

    fun fetchData(
        lat: Double?,
        lon: Double?,
        location: String?,
        date: String,
        time: String,
        lang: String,
        apiBase: String,
        depth: Int,
        chartStyle: String,
        sessionToken: String?
    ) {
        // Guard against redundant fetching with null location data if we already have a response
        if (lat == null && lon == null && location == null && _state.value.dashaResponse != null) {
            return
        }

        val sameParams = lat == lastLat && lon == lastLon && location == lastLocName && 
                         date == lastDate && time == lastTime && chartStyle == lastChartStyle &&
                         depth == lastDepth
        if (sameParams) return

        if (!NetworkUtils.isOnline(context)) {
            _state.value = _state.value.copy(error = "Internet required to use")
            return
        }

        // Check if only chart style changed
        val onlyChartStyleChanged = lat == lastLat && lon == lastLon && location == lastLocName && 
                                   date == lastDate && time == lastTime && depth == lastDepth &&
                                   chartStyle != lastChartStyle

        if (onlyChartStyleChanged) {
            updateOnlyChart(lat, lon, location, date, time, lang, apiBase, chartStyle, sessionToken)
            lastChartStyle = chartStyle
            return
        }

        // Skip if only minor coordinate drift and nothing else changed
        if (!LocationUtils.isSignificantChange(lastLat, lastLon, lat, lon) && 
            location == lastLocName && date == lastDate && time == lastTime && chartStyle == lastChartStyle) {
            lastLat = lat
            lastLon = lon
            return
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            lastLat = lat
            lastLon = lon
            lastLocName = location
            lastDate = date
            lastTime = time
            lastChartStyle = chartStyle
            lastDepth = depth

            _state.value = _state.value.copy(isLoading = true, error = null)
            _chartLoaded.value = false
            _selectedL1.value = null
            _selectedL2.value = null

            val apiLang = if (lang == "kn") "kan" else "en"
            val normalizedBase = if (apiBase.endsWith("/")) apiBase else "$apiBase/"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()

            coroutineScope {
                val chartUrl = buildString {
                    append("${normalizedBase}api/v1/kundali/svg?")
                    if (location != null) {
                        append("location=${java.net.URLEncoder.encode(location, "UTF-8")}")
                    } else if (lat != null && lon != null) {
                        val fLat = LocationUtils.formatCoord(lat)
                        val fLon = LocationUtils.formatCoord(lon)
                        append("lat=$fLat&lon=$fLon")
                    } else {
                        append("lat=12.9716&lon=77.5946")
                    }
                    append("&date=$date")
                    append("&time=$time")
                    append("&lang=$apiLang")
                    append("&chart_style=$chartStyle")
                    append("&ayanamsa=$ayanamsa")
                }

                // Pre-fetch chart image
                val imageRequest = ImageRequest.Builder(context)
                    .data(chartUrl)
                    .apply {
                        if (sessionToken != null) {
                            addHeader("Authorization", "Bearer $sessionToken")
                        }
                    }
                    .build()
                
                val chartJob = async {
                    context.imageLoader.execute(imageRequest)
                    _chartLoaded.value = true
                }

                val dashaDeferred = async { 
                    repo.fetchDasha(lat, lon, location, date, time, lang, depth)
                }

                val kundaliDeferred = async {
                    repo.fetchKundali(lat, lon, location, date, time, lang)
                }

                val dashaResult = dashaDeferred.await()
                val kundaliResult = kundaliDeferred.await()
                chartJob.await()

                if (dashaResult.isSuccess) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        dashaResponse = dashaResult.getOrNull(),
                        kundaliResponse = kundaliResult.getOrNull(),
                        chartUrl = chartUrl
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = dashaResult.exceptionOrNull()?.message ?: "Unknown error",
                        chartUrl = chartUrl
                    )
                }
            }
        }
    }

    fun selectL1(period: DashaPeriod?) {
        _selectedL1.value = period
        _selectedL2.value = null
    }

    fun selectL2(period: DashaPeriod?) {
        _selectedL2.value = period
    }

    private fun updateOnlyChart(
        lat: Double?,
        lon: Double?,
        location: String?,
        date: String,
        time: String,
        lang: String,
        apiBase: String,
        chartStyle: String,
        sessionToken: String?
    ) {
        val apiLang = if (lang == "kn") "kan" else "en"
        val normalizedBase = if (apiBase.endsWith("/")) apiBase else "$apiBase/"

        _state.value = _state.value.copy(chartUrl = null, isLoading = true)
        _chartLoaded.value = false

        viewModelScope.launch {
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val chartUrl = buildString {
                append("${normalizedBase}api/v1/kundali/svg?")
                if (location != null) {
                    append("location=${java.net.URLEncoder.encode(location, "UTF-8")}")
                } else if (lat != null && lon != null) {
                    val fLat = LocationUtils.formatCoord(lat)
                    val fLon = LocationUtils.formatCoord(lon)
                    append("lat=$fLat&lon=$fLon")
                } else {
                    append("lat=12.9716&lon=77.5946")
                }
                append("&date=$date")
                append("&time=$time")
                append("&lang=$apiLang")
                append("&chart_style=$chartStyle")
                append("&ayanamsa=$ayanamsa")
            }

            val imageRequest = ImageRequest.Builder(context)
                .data(chartUrl)
                .apply {
                    if (sessionToken != null) {
                        addHeader("Authorization", "Bearer $sessionToken")
                    }
                }
                .build()
            
            context.imageLoader.execute(imageRequest)
            _chartLoaded.value = true
            _state.value = _state.value.copy(chartUrl = chartUrl, isLoading = false)
        }
    }

    fun fetchLocations() {
        if (_state.value.isFetchingLocations) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingLocations = true)
            val res = repo.fetchLocations()
            if (res.isSuccess) {
                val map = res.getOrNull() ?: emptyMap()
                val locList = map.map { (name, data) ->
                    LocationData(
                        name = name,
                        latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                        longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                        timezone = data["timezone"]?.toString(),
                        description = data["description"]?.toString()
                    )
                }.sortedBy { it.name }
                _state.value = _state.value.copy(locations = locList, isFetchingLocations = false, error = null)
            } else {
                _state.value = _state.value.copy(
                    isFetchingLocations = false,
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }

    fun formatDecimalYears(decimalYears: Double): String {
        val years = decimalYears.toInt()
        val remainingAfterYears = (decimalYears - years) * 12
        val months = remainingAfterYears.toInt()
        val remainingAfterMonths = (remainingAfterYears - months) * 30
        val days = remainingAfterMonths.toInt()
        
        return "${years}y ${months}m ${days}d"
    }

    fun formatDegrees(decimalDegrees: Double): String {
        val degrees = decimalDegrees.toInt()
        val minutesDecimal = (decimalDegrees - degrees) * 60
        val minutes = minutesDecimal.toInt()
        return "$degrees° $minutes'"
    }
}
