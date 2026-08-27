package com.hora.jnana.repository

import android.content.Context
import android.util.Log
import com.hora.jnana.DataStoreManager
import com.hora.jnana.api.HoraApiService
import com.hora.jnana.CacheManager
import com.hora.jnana.ui.screens.PanchangaState
import com.hora.jnana.utils.NetworkUtils
import com.hora.jnana.utils.LocationUtils
import com.hora.jnana.utils.DateUtils
import com.hora.jnana.models.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HoraRepository(
    private val api: HoraApiService, 
    private val context: Context,
    private val moshi: Moshi,
    private val dataStoreManager: DataStoreManager
) {
    private val cache = CacheManager(context)

    private fun isToday(dateStr: String?): Boolean {
        if (dateStr == null) return true
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return dateStr == today
    }

    private fun isNow(dateStr: String?, timeStr: String?): Boolean {
        if (dateStr == null && timeStr == null) return true
        val now = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val dateMatch = dateStr == null || dateStr == sdfDate.format(now.time)
        val timeMatch = timeStr == null || timeStr == sdfTime.format(now.time)
        
        if (dateMatch && timeStr != null) {
            try {
                val parts = timeStr.split(":")
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                }
                return Math.abs(target.timeInMillis - now.timeInMillis) < 300000 
            } catch (e: Exception) {}
        }
        
        return dateMatch && timeMatch
    }

    private fun isVedicDayActive(): Boolean {
        val cached = cache.readJson("day.json") ?: return false
        return try {
            val obj = JSONObject(cached)
            val sunriseAtStr = obj.optString("sunrise_at", "")
            val nextSunriseAtStr = obj.optString("next_sunrise_at", "")
            val vedicDayDate = obj.optString("vedic_day_date", "")
            
            val now = System.currentTimeMillis()
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (sunriseAtStr.isNotEmpty() && nextSunriseAtStr.isNotEmpty()) {
                val start = DateUtils.parseToMillis(sunriseAtStr)
                val end = DateUtils.parseToMillis(nextSunriseAtStr)
                if (start > 0 && end > 0) {
                    if (now in (start - 60000)..(end - 60000)) return true
                }
            }
            
            if (vedicDayDate.isNotEmpty()) {
                return vedicDayDate == todayDate
            }
            
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun getCachedVedicDayDate(): String? {
        val cached = cache.readJson("day.json") ?: return null
        return try {
            JSONObject(cached).optString("vedic_day_date", "")
        } catch (e: Exception) {
            null
        }
    }

    private fun isCacheValidForDay(json: String?, currentVedicDayDate: String?): Boolean {
        if (json == null || currentVedicDayDate == null || currentVedicDayDate.isEmpty()) return false
        return try {
            val obj = JSONObject(json)
            val cacheVedicDate = obj.optString("vedic_day_date", "")
            cacheVedicDate.isNotEmpty() && cacheVedicDate == currentVedicDayDate
        } catch (e: Exception) {
            false
        }
    }

    fun mapException(e: Exception): String {
        val msg = e.message ?: ""
        return if (msg.contains("timeout", ignoreCase = true) || 
            msg.contains("connection", ignoreCase = true) || 
            msg.contains("failed to connect", ignoreCase = true) ||
            msg.contains("unable to resolve host", ignoreCase = true) ||
            msg.contains("Network is unreachable", ignoreCase = true) ||
            e is java.net.SocketTimeoutException ||
            e is java.net.ConnectException ||
            e is java.net.UnknownHostException ||
            e is retrofit2.HttpException ||
            e is java.io.IOException) {
            "Unable to connect to backend server, please try later"
        } else {
            "Unable to connect to backend server, please try later"
        }
    }

    suspend fun fetchPanchanga(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        lang: String,
        force: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheName = "panchanga.json"
        val online = NetworkUtils.isOnline(context)
        val today = isToday(date)
        val isCurrentVedicDay = today && isVedicDayActive()

        if (!force && isCurrentVedicDay) {
            val cached = cache.readJson(cacheName)
            val currentDayDate = getCachedVedicDayDate()
            if (isCacheValidForDay(cached, currentDayDate)) {
                return@withContext Result.success(cached!!)
            }
        }

        if (online) {
            return@withContext try {
                val apiLang = if (lang == "kn") "kan" else "en"
                val ayanamsa = dataStoreManager.ayanamsaFlow.first()
                val json = api.getPanchanga(
                    LocationUtils.formatCoord(lat), 
                    LocationUtils.formatCoord(lon), 
                    location, date, apiLang, ayanamsa
                ).string()
                if (today) cache.saveJson(cacheName, json)
                Result.success(json)
            } catch (e: Exception) {
                val cached = cache.readJson(cacheName)
                if (today && cached != null) Result.success(cached) else Result.failure(Exception(mapException(e)))
            }
        } else {
            val cached = cache.readJson(cacheName)
            return@withContext if (today && cached != null) Result.success(cached) 
            else Result.failure(Exception("Internet required to use"))
        }
    }

    suspend fun fetchMuhurta(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        lang: String,
        force: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheName = "muhurta.json"
        val online = NetworkUtils.isOnline(context)
        val today = isToday(date)
        val isCurrentVedicDay = today && isVedicDayActive()

        if (!force && isCurrentVedicDay) {
            val cached = cache.readJson(cacheName)
            val currentDayDate = getCachedVedicDayDate()
            if (isCacheValidForDay(cached, currentDayDate)) {
                return@withContext Result.success(cached!!)
            }
        }

        if (online) {
            return@withContext try {
                val apiLang = if (lang == "kn") "kan" else "en"
                val ayanamsa = dataStoreManager.ayanamsaFlow.first()
                val json = api.getMuhurta(
                    LocationUtils.formatCoord(lat), 
                    LocationUtils.formatCoord(lon), 
                    location, date, apiLang, ayanamsa
                ).string()
                if (today) cache.saveJson(cacheName, json)
                Result.success(json)
            } catch (e: Exception) {
                val cached = cache.readJson(cacheName)
                if (today && cached != null) Result.success(cached) else Result.failure(Exception(mapException(e)))
            }
        } else {
            val cached = cache.readJson(cacheName)
            return@withContext if (today && cached != null) Result.success(cached) 
            else Result.failure(Exception("Internet required to use"))
        }
    }

    suspend fun fetchDay(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        lang: String,
        force: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheName = "day.json"
        val online = NetworkUtils.isOnline(context)
        val today = isToday(date)
        val isCurrentVedicDay = today && isVedicDayActive()

        if (!force && isCurrentVedicDay) {
            val cached = cache.readJson(cacheName)
            if (cached != null) return@withContext Result.success(cached)
        }

        if (online) {
            return@withContext try {
                val apiLang = if (lang == "kn") "kan" else "en"
                val ayanamsa = dataStoreManager.ayanamsaFlow.first()
                val json = api.getDay(
                    LocationUtils.formatCoord(lat), 
                    LocationUtils.formatCoord(lon), 
                    location, date, apiLang, ayanamsa
                ).string()
                if (today) cache.saveJson(cacheName, json)
                Result.success(json)
            } catch (e: Exception) {
                val cached = cache.readJson(cacheName)
                if (today && cached != null) Result.success(cached) else Result.failure(Exception(mapException(e)))
            }
        } else {
            val cached = cache.readJson(cacheName)
            return@withContext if (today && cached != null) Result.success(cached) 
            else Result.failure(Exception("Internet required to use"))
        }
    }

    suspend fun fetchHora(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
        lang: String,
        force: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheName = "hora.json"
        val online = NetworkUtils.isOnline(context)
        val today = isToday(date)
        val isCurrentVedicDay = today && isVedicDayActive()
        val isNow = isNow(date, time)

        if (!force && isCurrentVedicDay) {
            val cached = cache.readJson(cacheName)
            val currentDayDate = getCachedVedicDayDate()
            if (isCacheValidForDay(cached, currentDayDate)) {
                return@withContext Result.success(cached!!)
            }
        }

        if (online) {
            return@withContext try {
                val apiLang = if (lang == "kn") "kan" else "en"
                val ayanamsa = dataStoreManager.ayanamsaFlow.first()
                val json = api.getHora(
                    LocationUtils.formatCoord(lat), 
                    LocationUtils.formatCoord(lon), 
                    location, date, time, apiLang, ayanamsa
                ).string()
                if (isNow) cache.saveJson(cacheName, json)
                Result.success(json)
            } catch (e: Exception) {
                val cached = cache.readJson(cacheName)
                if (isNow && cached != null) Result.success(cached) else Result.failure(Exception(mapException(e)))
            }
        } else {
            val cached = cache.readJson(cacheName)
            return@withContext if (isNow && cached != null) Result.success(cached) 
            else Result.failure(Exception("Internet required to use"))
        }
    }

    suspend fun fetchAllRaw(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
        lang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val respBody = api.getAllRaw(
                LocationUtils.formatCoord(lat), 
                LocationUtils.formatCoord(lon), 
                location, date, time, apiLang, ayanamsa
            )
            val json = respBody.string()
            if (date == null && time == null) {
                cache.saveJson("all.json", json)
            }
            Result.success(json)
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error fetching all data", e)
            val cached = cache.readJson("all.json")
            if (cached != null && date == null && time == null) {
                Result.success(cached)
            } else {
                Result.failure(Exception(mapException(e)))
            }
        }
    }

    suspend fun fetchPanchangaRaw(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        lang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val respBody = api.getPanchanga(
                LocationUtils.formatCoord(lat), 
                LocationUtils.formatCoord(lon), 
                location, date, apiLang, ayanamsa
            )
            Result.success(respBody.string())
        } catch (e: Exception) {
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun fetchHoraRaw(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
        lang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val respBody = api.getHora(
                LocationUtils.formatCoord(lat), 
                LocationUtils.formatCoord(lon), 
                location, date, time, apiLang, ayanamsa
            )
            Result.success(respBody.string())
        } catch (e: Exception) {
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun fetchMuhurtaRaw(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        lang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val respBody = api.getMuhurta(
                LocationUtils.formatCoord(lat), 
                LocationUtils.formatCoord(lon), 
                location, date, apiLang, ayanamsa
            )
            Result.success(respBody.string())
        } catch (e: Exception) {
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun fetchDasha(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
        lang: String,
        depth: Int? = null
    ): Result<DashaResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val resp = api.getDasha(
                LocationUtils.formatCoord(lat), 
                LocationUtils.formatCoord(lon), 
                location, date, time, apiLang, depth, ayanamsa
            )
            Result.success(resp)
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error fetching dasha", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun fetchBirthDasha(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
        lang: String,
        depth: Int? = null
    ): Result<DashaResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val resp = api.getBirthDasha(
                LocationUtils.formatCoord(lat), 
                LocationUtils.formatCoord(lon), 
                location, date, time, apiLang, depth, ayanamsa
            )
            Result.success(resp)
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error fetching birth dasha", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun fetchKundali(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
        lang: String
    ): Result<KundaliResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val resp = api.getKundali(
                LocationUtils.formatCoord(lat),
                LocationUtils.formatCoord(lon),
                location, date, time, apiLang, ayanamsa
            )
            Result.success(resp)
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error fetching kundali data", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun fetchBirthKundali(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
        lang: String
    ): Result<KundaliResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val resp = api.getBirthKundali(
                LocationUtils.formatCoord(lat),
                LocationUtils.formatCoord(lon),
                location, date, time, apiLang, ayanamsa
            )
            Result.success(resp)
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error fetching birth kundali data", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun fetchBirthKundaliSvg(
        lat: Double? = null,
        lon: Double? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
        name: String? = null,
        lang: String,
        chartStyle: String = "south"
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiLang = if (lang == "kn") "kan" else "en"
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val respBody = api.getBirthKundaliSvg(
                LocationUtils.formatCoord(lat),
                LocationUtils.formatCoord(lon),
                location, date, time, name, apiLang, chartStyle, ayanamsa
            )
            Result.success(respBody.string())
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error fetching birth kundali svg", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun fetchLocations(): Result<Map<String, Map<String, Any>>> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!NetworkUtils.isOnline(context)) {
                return@withContext Result.failure(Exception("Internet required to use"))
            }
            Result.success(api.getLocations())
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error fetching locations", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun addLocation(location: Map<String, Any?>): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!NetworkUtils.isOnline(context)) {
                return@withContext Result.failure(Exception("Internet required to use"))
            }
            val response = api.addLocation(location)
            Result.success(response.string())
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error adding location: $location", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun deleteLocation(name: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!NetworkUtils.isOnline(context)) {
                return@withContext Result.failure(Exception("Internet required to use"))
            }
            val response = api.deleteLocation(name)
            Result.success(response.string())
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error deleting location: $name", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun matchMaking(request: MatchMakingRequest): Result<MatchMakingResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!NetworkUtils.isOnline(context)) {
                return@withContext Result.failure(Exception("Internet required to use"))
            }
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
            val finalRequest = request.copy(ayanamsa = request.ayanamsa ?: ayanamsa)
            Result.success(api.matchMaking(finalRequest))
        } catch (e: Exception) {
            Log.e("HoraRepository", "Error in matchmaking API call", e)
            Result.failure(Exception(mapException(e)))
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val responseBody = api.logout()
            val json = responseBody.string()
            val obj = JSONObject(json)
            if (obj.optString("status") == "logged_out") {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Logout failed: Unexpected response status"))
            }
        } catch (e: Exception) {
            Log.e("HoraRepository", "Logout error", e)
            Result.failure(e)
        }
    }

    fun mergeToState(
        panchangaJson: String? = null,
        muhurtaJson: String? = null,
        dayJson: String? = null,
        horaJson: String? = null,
        targetTimeMillis: Long? = null
    ): PanchangaState {
        var state = PanchangaState()
        
        panchangaJson?.let { json ->
            try {
                val adapter = moshi.adapter(PanchangaResponse::class.java)
                val resp = adapter.fromJson(json)
                resp?.let {
                    state = state.copy(
                        tithi = it.panchanga["tithi"] ?: "--",
                        nakshatra = it.panchanga["nakshatra"] ?: "--",
                        yoga = it.panchanga["yoga"] ?: "--",
                        karana = it.panchanga["karana"] ?: "--",
                        vara = it.panchanga["vara"] ?: "--",
                        samvatsara = it.panchanga["samvatsara"] ?: "--",
                        ayana = it.panchanga["ayana"] ?: "--",
                        rutu = it.panchanga["rutu"] ?: "--",
                        masa = it.panchanga["masa"] ?: "--",
                        paksha = it.panchanga["paksha"] ?: "--",
                        
                        tithiEnds = getEnd(it.panchangaDetails["tithi"]),
                        nakshatraEnds = getEnd(it.panchangaDetails["nakshatra"]),
                        yogaEnds = getEnd(it.panchangaDetails["yoga"]),
                        karanaEnds = getEnd(it.panchangaDetails["karana"]),
                        
                        moonRasi = it.moon["rasi"]?.toString() ?: "--",
                        sunRasi = it.sun["rasi"]?.toString() ?: "--"
                    )
                }
            } catch (e: Exception) { Log.e("HoraRepository", "Error merging panchanga", e) }
        }

        muhurtaJson?.let { json ->
            try {
                val adapter = moshi.adapter(MuhurtaResponse::class.java)
                val resp = adapter.fromJson(json)
                resp?.let {
                    state = state.copy(
                        rahuKalam = it.muhurta["rahu_kalam"]?.display ?: "--",
                        gulika = it.muhurta["gulika"]?.display ?: "--",
                        yamaganda = it.muhurta["yamaganda"]?.display ?: "--",
                        abhijit = it.muhurta["abhijit"]?.display ?: "--"
                    )
                }
            } catch (e: Exception) { Log.e("HoraRepository", "Error merging muhurta", e) }
        }

        dayJson?.let { json ->
            try {
                val adapter = moshi.adapter(DayResponse::class.java)
                val resp = adapter.fromJson(json)
                resp?.let {
                    state = state.copy(
                        sunrise = it.sunrise,
                        sunset = it.sunset,
                        sunriseAt = it.sunriseAt,
                        sunsetAt = it.sunsetAt,
                        nextSunriseAt = it.nextSunriseAt,
                        solarNoonAt = it.solarNoonAt,
                        daylightMidpointAt = it.daylightMidpointAt,
                        dayDuration = it.dayDurationSeconds.toString(),
                        nightDuration = it.nightDurationSeconds.toString(),
                        lastUpdated = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date()),
                        lastUpdatedMillis = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) { Log.e("HoraRepository", "Error merging day", e) }
        }

        horaJson?.let { json ->
            try {
                val adapter = moshi.adapter(HoraResponse::class.java)
                val resp = adapter.fromJson(json)
                resp?.let {
                    val allHoras = it.dayHora + it.nightHora
                    val timeToCalculate = targetTimeMillis ?: System.currentTimeMillis()
                    
                    val active = allHoras.find { item ->
                        val start = DateUtils.parseToMillis(item.startsAt)
                        val end = DateUtils.parseToMillis(item.endsAt)
                        timeToCalculate in start until end
                    }
                    
                    val nextIdx = if (active != null) allHoras.indexOf(active) + 1 else -1
                    val next = if (nextIdx in allHoras.indices) allHoras[nextIdx].planet else "--"
                    
                    val remainingStr = active?.let { a ->
                        val end = DateUtils.parseToMillis(a.endsAt)
                        val diff = (end - timeToCalculate) / 60000
                        if (diff > 0) "$diff min" else if (diff == 0L) "< 1 min" else "Ended"
                    } ?: it.hora.remaining

                    state = state.copy(
                        hora = active?.planet ?: it.hora.planet,
                        horaSymbol = active?.symbol ?: it.hora.symbol,
                        horaNext = next,
                        horaEnds = active?.ends ?: it.hora.ends,
                        horaEndsAt = active?.endsAt ?: it.hora.endsAt,
                        remaining = remainingStr,
                        dayHoraList = it.dayHora,
                        nightHoraList = it.nightHora
                    )
                }
            } catch (e: Exception) { Log.e("HoraRepository", "Error merging hora", e) }
        }

        return state
    }

    private fun getEnd(obj: Any?): String {
        val detail = obj as? Map<*, *>
        val endsAt = detail?.get("ends_at")?.toString() ?: ""
        if (endsAt.isEmpty()) return ""
        
        return try {
            val millis = DateUtils.parseToMillis(endsAt)
            if (millis > 0) {
                SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(millis))
            } else {
                endsAt.split("T").lastOrNull()?.take(5) ?: ""
            }
        } catch (e: Exception) {
            endsAt.split("T").lastOrNull()?.take(5) ?: ""
        }
    }

    fun parsePanchangaFromJson(json: String): PanchangaState {
        try {
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(json) ?: return PanchangaState()
            
            val horaObj = map["hora"] as? Map<*, *>
            val panSummary = map["panchanga"] as? Map<*, *>
            val panDetails = map["panchanga_details"] as? Map<*, *>
            val moonObj = map["moon"] as? Map<*, *>
            val sunObj = map["sun"] as? Map<*, *>
            
            return PanchangaState(
                hora = horaObj?.get("planet")?.toString() ?: "--",
                horaSymbol = horaObj?.get("symbol")?.toString() ?: "",
                horaNext = horaObj?.get("next")?.toString() ?: "--",
                horaEnds = horaObj?.get("ends")?.toString() ?: "--",
                horaEndsAt = horaObj?.get("ends_at")?.toString(),
                remaining = horaObj?.get("remaining")?.toString() ?: "--",
                
                tithi = panSummary?.get("tithi")?.toString() ?: "--",
                tithiEnds = getEnd(panDetails?.get("tithi")),
                nakshatra = panSummary?.get("nakshatra")?.toString() ?: "--",
                nakshatraEnds = getEnd(panDetails?.get("nakshatra")),
                yoga = panSummary?.get("yoga")?.toString() ?: "--",
                yogaEnds = getEnd(panDetails?.get("yoga")),
                karana = panSummary?.get("karana")?.toString() ?: "--",
                karanaEnds = getEnd(panDetails?.get("karana")),
                vara = panSummary?.get("vara")?.toString() ?: "--",
                
                samvatsara = panSummary?.get("samvatsara")?.toString() ?: "--",
                ayana = panSummary?.get("ayana")?.toString() ?: "--",
                rutu = panSummary?.get("rutu")?.toString() ?: "--",
                masa = panSummary?.get("masa")?.toString() ?: "--",
                paksha = panSummary?.get("paksha")?.toString() ?: "--",
                
                rahuKalam = map["rahu_kalam"]?.toString() ?: "--",
                gulika = map["gulika"]?.toString() ?: "--",
                yamaganda = map["yamaganda"]?.toString() ?: "--",
                abhijit = map["abhijit"]?.toString() ?: "--",
                
                sunrise = map["sunrise"]?.toString() ?: "--",
                sunset = map["sunset"]?.toString() ?: "--",
                sunriseAt = map["sunrise_at"]?.toString() ?: "--",
                sunsetAt = map["sunset_at"]?.toString() ?: "--",
                nextSunriseAt = map["next_sunrise_at"]?.toString() ?: "--",
                solarNoonAt = map["solar_noon_at"]?.toString() ?: "--",
                daylightMidpointAt = map["daylight_midpoint_at"]?.toString() ?: "--",
                dayDuration = map["day_duration_seconds"]?.toString() ?: "--",
                nightDuration = map["night_duration_seconds"]?.toString() ?: "--",
                
                moonRasi = moonObj?.get("rasi")?.toString() ?: "--",
                sunRasi = sunObj?.get("rasi")?.toString() ?: "--",
                lastUpdated = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date()),
                lastUpdatedMillis = System.currentTimeMillis(),
                isLoading = false
            )
        } catch (e: Exception) {
            return PanchangaState(isLoading = false, error = e.message)
        }
    }
}
