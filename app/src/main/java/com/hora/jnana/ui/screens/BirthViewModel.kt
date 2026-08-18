package com.hora.jnana.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hora.jnana.repository.HoraRepository
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import coil.imageLoader
import coil.request.ImageRequest
import com.hora.jnana.models.DashaResponse
import com.hora.jnana.models.KundaliResponse
import com.hora.jnana.models.DashaPeriod
import com.hora.jnana.models.LocationData
import com.hora.jnana.utils.EncryptionUtils
import com.hora.jnana.utils.LocationUtils
import com.hora.jnana.utils.NetworkUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URLEncoder

data class SavedKundali(
    val name: String,
    val date: String,
    val time: String,
    val locationName: String?,
    val lat: Double?,
    val lon: Double?,
    val dashaResponse: DashaResponse,
    val kundaliResponse: KundaliResponse? = null,
    val chartUrl: String?,
     val svgContent: String? = null,
    val chartStyle: String = "south"
)

data class SavedKundaliMeta(
    val name: String,
    val date: String,
    val time: String,
    val locationName: String?,
    val uri: Uri
)

data class BirthState(
    val isLoading: Boolean = false,
    val isListingFiles: Boolean = false,
    val dashaResponse: DashaResponse? = null,
    val kundaliResponse: KundaliResponse? = null,
    val chartUrl: String? = null,
    val svgContent: String? = null,
    val error: String? = null,
    val inputName: String = "",
    val inputDate: String = "",
    val inputTime: String = "",
    val inputLocationName: String? = null,
    val inputLat: Double? = null,
    val inputLon: Double? = null,
    val locations: List<LocationData> = emptyList(),
    val savedKundalis: List<SavedKundaliMeta> = emptyList(),
    val isFetchingLocations: Boolean = false,
    val chartStyle: String = "south"
)

class BirthViewModel(
    private val repo: HoraRepository,
    private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(BirthState())
    val state: StateFlow<BirthState> = _state

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val savedKundaliAdapter = moshi.adapter(SavedKundali::class.java)

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
    private var lastPersonName: String? = null
    private var lastChartStyle: String? = null
    private var lastDepth: Int? = null

    fun fetchData(
        lat: Double?,
        lon: Double?,
        location: String?,
        date: String,
        time: String,
        name: String,
        lang: String,
        apiBase: String,
        depth: Int,
        chartStyle: String,
        sessionToken: String?
    ) {
        val sameParams = lat == lastLat && lon == lastLon && location == lastLocName && 
                         date == lastDate && time == lastTime && name == lastPersonName &&
                         chartStyle == lastChartStyle && depth == lastDepth
        if (sameParams) return

        if (!NetworkUtils.isOnline(context)) {
            _state.value = _state.value.copy(error = "Internet connection is required to fetch new information")
            return
        }

        // Check if only chart style changed
        val onlyChartStyleChanged = lat == lastLat && lon == lastLon && location == lastLocName && 
                                   date == lastDate && time == lastTime && name == lastPersonName &&
                                   depth == lastDepth && chartStyle != lastChartStyle

        if (onlyChartStyleChanged) {
            updateOnlyChart(lat, lon, location, date, time, name, lang, apiBase, chartStyle, sessionToken)
            lastChartStyle = chartStyle
            return
        }

        // Skip if only minor coordinate drift and nothing else changed
        if (!LocationUtils.isSignificantChange(lastLat, lastLon, lat, lon) && 
            location == lastLocName && date == lastDate && time == lastTime && 
            name == lastPersonName && chartStyle == lastChartStyle) {
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
            lastPersonName = name
            lastChartStyle = chartStyle
            lastDepth = depth

            _state.value = _state.value.copy(
                isLoading = true, 
                error = null,
                inputName = name,
                inputDate = date,
                inputTime = time,
                inputLocationName = location,
                inputLat = lat,
                inputLon = lon,
                chartStyle = chartStyle
            )
            _chartLoaded.value = false
            _selectedL1.value = null
            _selectedL2.value = null

            val apiLang = if (lang == "kn") "kan" else "en"
            val normalizedBase = if (apiBase.endsWith("/")) apiBase else "$apiBase/"

            coroutineScope {
                val chartUrl = buildString {
                    append("${normalizedBase}api/v1/kundali/birth/svg?")
                    if (location != null) {
                        append("location=${URLEncoder.encode(location, "UTF-8")}")
                    } else if (lat != null && lon != null) {
                        val fLat = LocationUtils.formatCoord(lat)
                        val fLon = LocationUtils.formatCoord(lon)
                        append("lat=$fLat&lon=$fLon")
                    } else {
                        append("lat=12.9716&lon=77.5946")
                    }
                    append("&date=$date")
                    append("&time=$time")
                    if (name.isNotEmpty()) {
                        append("&name=${URLEncoder.encode(name, "UTF-8")}")
                    }
                    append("&lang=$apiLang")
                    append("&chart_style=$chartStyle")
                }

                val dashaDeferred = async { 
                    repo.fetchBirthDasha(lat, lon, location, date, time, lang, depth)
                }

                val kundaliDeferred = async {
                    repo.fetchBirthKundali(lat, lon, location, date, time, lang)
                }
                
                val chartJob = async {
                    repo.fetchBirthKundaliSvg(lat, lon, location, date, time, name, lang, chartStyle)
                }

                val dashaResult = dashaDeferred.await()
                val kundaliResult = kundaliDeferred.await()
                val svgResult = chartJob.await()
                _chartLoaded.value = true

                if (dashaResult.isSuccess) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        dashaResponse = dashaResult.getOrNull(),
                        kundaliResponse = kundaliResult.getOrNull(),
                        chartUrl = chartUrl,
                        svgContent = svgResult.getOrNull()
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
        name: String,
        lang: String,
        apiBase: String,
        chartStyle: String,
        sessionToken: String?
    ) {
        val apiLang = if (lang == "kn") "kan" else "en"
        val normalizedBase = if (apiBase.endsWith("/")) apiBase else "$apiBase/"

        _state.value = _state.value.copy(chartStyle = chartStyle, isLoading = true)
        _chartLoaded.value = false

        viewModelScope.launch {
            val svgResult = repo.fetchBirthKundaliSvg(lat, lon, location, date, time, name, lang, chartStyle)
            _chartLoaded.value = true
            
            val chartUrl = buildString {
                append("${normalizedBase}api/v1/kundali/birth/svg?")
                if (location != null) {
                    append("location=${URLEncoder.encode(location, "UTF-8")}")
                } else if (lat != null && lon != null) {
                    val fLat = LocationUtils.formatCoord(lat)
                    val fLon = LocationUtils.formatCoord(lon)
                    append("lat=$fLat&lon=$fLon")
                } else {
                    append("lat=12.9716&lon=77.5946")
                }
                append("&date=$date")
                append("&time=$time")
                if (name.isNotEmpty()) {
                    append("&name=${URLEncoder.encode(name, "UTF-8")}")
                }
                append("&lang=$apiLang")
                append("&chart_style=$chartStyle")
            }

            _state.value = _state.value.copy(
                isLoading = false,
                chartUrl = chartUrl,
                svgContent = svgResult.getOrNull()
            )
        }
    }

    fun fetchLocations() {
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
                _state.value = _state.value.copy(locations = locList, isFetchingLocations = false)
            } else {
                _state.value = _state.value.copy(isFetchingLocations = false)
            }
        }
    }

    fun saveKundali(saveUri: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val currentState = _state.value
            val dasha = currentState.dashaResponse ?: return@launch
            
            try {
                val savedData = SavedKundali(
                    name = currentState.inputName,
                    date = currentState.inputDate,
                    time = currentState.inputTime,
                    locationName = currentState.inputLocationName,
                    lat = currentState.inputLat,
                    lon = currentState.inputLon,
                    dashaResponse = dasha,
                    kundaliResponse = currentState.kundaliResponse,
                    chartUrl = currentState.chartUrl,
                    svgContent = currentState.svgContent,
                    chartStyle = currentState.chartStyle
                )
                
                val json = savedKundaliAdapter.toJson(savedData)
                val encrypted = EncryptionUtils.encrypt(json)
                val fileName = "${currentState.inputName} - ${currentState.inputDate}.json"
                
                if (saveUri.isNullOrEmpty()) {
                    // Use MediaStore for public Documents/Kundalis on API 29+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = context.contentResolver
                        val contentUri = MediaStore.Files.getContentUri("external")
                        
                        // Cleanup existing file with same name in same path
                        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/Kundalis/"
                        resolver.delete(contentUri, selection, arrayOf(fileName, relativePath))

                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        }

                        val uri = resolver.insert(contentUri, contentValues)
                            ?: throw Exception("Could not create file in MediaStore")
                            
                        resolver.openOutputStream(uri)?.use { output ->
                            OutputStreamWriter(output).use { it.write(encrypted) }
                        }
                        onResult(true, "Saved in Documents/Kundalis")
                    } else {
                        // Fallback for older devices or as a secondary option
                        @Suppress("DEPRECATION")
                        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Kundalis")
                        if (!directory.exists()) directory.mkdirs()
                        val file = File(directory, fileName)
                        file.writeText(encrypted)
                        onResult(true, "Saved in Documents/Kundalis")
                    }
                } else {
                    val treeUri = Uri.parse(saveUri)
                    
                    val rootFolder = DocumentFile.fromTreeUri(context, treeUri)
                    if (rootFolder == null || !rootFolder.canWrite()) {
                        // If custom folder fails, fallback to MediaStore default instead of failing
                        saveKundali(null, onResult)
                        return@launch
                    }
                    
                    // Create or get "Kundalis" subfolder
                    var targetFolder = rootFolder.findFile("Kundalis")
                    if (targetFolder == null || !targetFolder.isDirectory) {
                        targetFolder = rootFolder.createDirectory("Kundalis")
                    }
                    
                    if (targetFolder == null) throw Exception("Could not create Kundalis folder")
                    
                    // Check if file exists to overwrite (SAF doesn't overwrite by default)
                    val existingFile = targetFolder.findFile(fileName)
                    existingFile?.delete()
                    
                    // Create the file
                    val documentFile = targetFolder.createFile("application/json", fileName)
                        ?: throw Exception("Could not create file")
                    
                    context.contentResolver.openOutputStream(documentFile.uri)?.use { output ->
                        OutputStreamWriter(output).use { writer ->
                            writer.write(encrypted)
                        }
                    }
                    onResult(true, "Saved in ${rootFolder.name}/Kundalis")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun loadKundali(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        try {
            val encrypted = context.contentResolver.openInputStream(uri)?.use { input ->
                InputStreamReader(input).use { reader ->
                    reader.readText()
                }
            } ?: throw Exception("Could not read file")
            
            val json = EncryptionUtils.decrypt(encrypted)
            val savedData = savedKundaliAdapter.fromJson(json) ?: throw Exception("Invalid data")
            
            _state.value = _state.value.copy(
                isLoading = false,
                dashaResponse = savedData.dashaResponse,
                kundaliResponse = savedData.kundaliResponse,
                chartUrl = savedData.chartUrl,
                svgContent = savedData.svgContent,
                error = null,
                inputName = savedData.name,
                inputDate = savedData.date,
                inputTime = savedData.time,
                inputLocationName = savedData.locationName,
                inputLat = savedData.lat,
                inputLon = savedData.lon,
                chartStyle = savedData.chartStyle
            )
            lastChartStyle = savedData.chartStyle
            onResult(true, null)
        } catch (e: Exception) {
            onResult(false, e.message)
        }
    }

    fun loadSavedFiles(saveUri: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isListingFiles = true, savedKundalis = emptyList())
            val discovered = mutableListOf<SavedKundaliMeta>()

            try {
                if (!saveUri.isNullOrEmpty()) {
                    // Load from custom SAF folder
                    val treeUri = Uri.parse(saveUri)
                    val rootFolder = DocumentFile.fromTreeUri(context, treeUri)
                    val targetFolder = rootFolder?.findFile("Kundalis")
                    
                    targetFolder?.listFiles()?.forEach { file ->
                        if (file.name?.endsWith(".json") == true) {
                            try {
                                val meta = readMeta(file.uri)
                                if (meta != null) discovered.add(meta)
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    // Load from default Documents/Kundalis
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
                        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                        val selectionArgs = arrayOf("${Environment.DIRECTORY_DOCUMENTS}/Kundalis/%")
                        
                        context.contentResolver.query(
                            MediaStore.Files.getContentUri("external"),
                            projection, selection, selectionArgs, null
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idCol)
                                val uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())
                                try {
                                    val meta = readMeta(uri)
                                    if (meta != null) discovered.add(meta)
                                } catch (_: Exception) {}
                            }
                        }
                    } else {
                        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Kundalis")
                        directory.listFiles()?.forEach { file ->
                            if (file.name.endsWith(".json")) {
                                try {
                                    val meta = readMeta(Uri.fromFile(file))
                                    if (meta != null) discovered.add(meta)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BirthViewModel", "Error listing files", e)
            }

            _state.value = _state.value.copy(
                isListingFiles = false, 
                savedKundalis = discovered.sortedBy { it.name.lowercase() }
            )
        }
    }

    private fun readMeta(uri: Uri): SavedKundaliMeta? {
        return try {
            val encrypted = context.contentResolver.openInputStream(uri)?.use { input ->
                InputStreamReader(input).use { it.readText() }
            } ?: return null
            val json = EncryptionUtils.decrypt(encrypted)
            val data = savedKundaliAdapter.fromJson(json) ?: return null
            SavedKundaliMeta(data.name, data.date, data.time, data.locationName, uri)
        } catch (_: Exception) { null }
    }

    fun deleteSavedKundali(uri: Uri, saveUri: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (saveUri.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.delete(uri, null, null)
                } else {
                    // For SAF or legacy File
                    val file = DocumentFile.fromSingleUri(context, uri)
                    if (file?.delete() == true) {
                        // Success
                    } else {
                        // Fallback to direct File delete if it's a file:// uri
                        if (uri.scheme == "file") {
                            File(uri.path!!).delete()
                        }
                    }
                }
                loadSavedFiles(saveUri)
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
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

    fun resetState() {
        fetchJob?.cancel()
        _state.value = BirthState(locations = _state.value.locations) // Keep locations
        _selectedL1.value = null
        _selectedL2.value = null
        lastLat = null
        lastLon = null
        lastLocName = null
        lastDate = null
        lastTime = null
        lastPersonName = null
        lastChartStyle = null
        lastDepth = null
    }
}
