package com.hora.jnana.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hora.jnana.DataStoreManager
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
import com.hora.jnana.models.*
import com.hora.jnana.utils.EncryptionUtils
import com.hora.jnana.utils.LocationUtils
import com.hora.jnana.utils.NetworkUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URLEncoder

data class BirthState(
    val isLoading: Boolean = false,
    val isListingFiles: Boolean = false,
    val dashaResponse: DashaResponse? = null,
    val kundaliResponse: KundaliResponse? = null,
    val chartUrl: String? = null,
    val svgContent: String? = null,
    val error: String? = null,
    val success: String? = null,
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
    private val context: Context,
    private val moshi: Moshi,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _state = MutableStateFlow(BirthState())
    val state: StateFlow<BirthState> = _state

    private val savedKundaliAdapter by lazy { moshi.adapter(SavedKundali::class.java) }

    private val internalDir = File(context.filesDir, "saved_kundalis").apply { if (!exists()) mkdirs() }

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
        // Guard against redundant fetching with null location data if we already have a response
        if (lat == null && lon == null && location == null && _state.value.dashaResponse != null) {
            return
        }

        val sameParams = lat == lastLat && lon == lastLon && location == lastLocName && 
                         date == lastDate && time == lastTime && name == lastPersonName &&
                         chartStyle == lastChartStyle && depth == lastDepth
        if (sameParams) return

        if (!NetworkUtils.isOnline(context)) {
            _state.value = _state.value.copy(error = "Internet required to use")
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
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()

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
                    append("&ayanamsa=$ayanamsa")
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
            val ayanamsa = dataStoreManager.ayanamsaFlow.first()
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
                append("&ayanamsa=$ayanamsa")
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
                _state.value = _state.value.copy(locations = locList, isFetchingLocations = false, error = null)
            } else {
                _state.value = _state.value.copy(
                    isFetchingLocations = false,
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }

    fun saveKundali(onResult: (Boolean, String?) -> Unit) {
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
                
                // Save to Private Internal Storage ONLY
                val file = File(internalDir, fileName)
                file.writeText(encrypted)
                onResult(true, "Saved to App Vault")
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun savePartialProfile(
        name: String,
        date: String,
        time: String,
        locationName: String?,
        lat: Double?,
        lon: Double?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val savedData = SavedKundali(
                    name = name,
                    date = date,
                    time = time,
                    locationName = locationName,
                    lat = lat,
                    lon = lon
                )
                
                val json = savedKundaliAdapter.toJson(savedData)
                val encrypted = EncryptionUtils.encrypt(json)
                val fileName = "$name - $date.json"
                
                // Save to Private Internal Storage ONLY
                val file = File(internalDir, fileName)
                file.writeText(encrypted)
                _state.value = _state.value.copy(success = "Profile $name added Successfully")
                onResult(true, "Profile Saved to App Vault")
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun updatePartialProfile(
        uri: Uri,
        name: String,
        date: String,
        time: String,
        locationName: String?,
        saveUri: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val encrypted = context.contentResolver.openInputStream(uri)?.use { input ->
                    InputStreamReader(input).use { it.readText() }
                } ?: throw Exception("Could not read file")
                
                val json = EncryptionUtils.decrypt(encrypted)
                val existingData = savedKundaliAdapter.fromJson(json) ?: throw Exception("Invalid data")

                val updatedData = existingData.copy(
                    name = name,
                    date = date,
                    time = time,
                    locationName = locationName
                )
                
                val newJson = savedKundaliAdapter.toJson(updatedData)
                val newEncrypted = EncryptionUtils.encrypt(newJson)
                val newFileName = "$name - $date.json"
                
                val oldFileName = if (uri.scheme == "file") {
                    File(uri.path!!).name
                } else {
                    DocumentFile.fromSingleUri(context, uri)?.name
                }

                // Save updated to internal
                val internalFile = File(internalDir, newFileName)
                internalFile.writeText(newEncrypted)

                // If filename changed OR it was not an internal file, delete the old one
                if (oldFileName != newFileName || uri.scheme != "file") {
                    if (uri.scheme == "file") {
                        File(uri.path!!).delete()
                    } else {
                        DocumentFile.fromSingleUri(context, uri)?.delete()
                    }
                }
                
                loadSavedFiles(saveUri)
                _state.value = _state.value.copy(success = "Profile $name updated successfully")
                onResult(true, "Profile Updated")
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
            // Update tracking variables to prevent redundant API calls
            lastLat = savedData.lat
            lastLon = savedData.lon
            lastLocName = savedData.locationName
            lastDate = savedData.date
            lastTime = savedData.time
            lastPersonName = savedData.name
            lastChartStyle = savedData.chartStyle
            lastDepth = 3 // Default depth used in BirthKundaliScreen

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
                // 1. Always load from Internal Storage
                internalDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".json")) {
                        try {
                            val meta = readMeta(Uri.fromFile(file))
                            if (meta != null) discovered.add(meta)
                        } catch (_: Exception) {}
                    }
                }

                // 2. Load from Custom Path (if set)
                if (!saveUri.isNullOrEmpty()) {
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
                }
            } catch (e: Exception) {
                android.util.Log.e("BirthViewModel", "Error listing files", e)
            }

            _state.value = _state.value.copy(
                isListingFiles = false, 
                savedKundalis = discovered.distinctBy { it.name + it.date }.sortedBy { it.name.lowercase() }
            )
        }
    }

    fun backupKundalis(saveUri: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val files = internalDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
                if (files.isEmpty()) {
                    onResult(true, "No files to backup")
                    return@launch
                }

                if (saveUri.isNullOrEmpty()) {
                    // Backup to default Documents/Kundalis
                    files.forEach { file ->
                        val fileName = file.name
                        val content = file.readText()
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val resolver = context.contentResolver
                            val contentUri = MediaStore.Files.getContentUri("external")
                            val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/Kundalis/"
                            
                            // Delete existing
                            resolver.delete(contentUri, "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?", arrayOf(fileName, relativePath))
                            
                            val values = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                            }
                            resolver.insert(contentUri, values)?.let { uri ->
                                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Kundalis")
                            if (!dir.exists()) dir.mkdirs()
                            File(dir, fileName).writeText(content)
                        }
                    }
                    onResult(true, "Backup completed to Documents/Kundalis")
                } else {
                    // Backup to custom SAF folder
                    val root = DocumentFile.fromTreeUri(context, Uri.parse(saveUri))
                    var target = root?.findFile("Kundalis") ?: root?.createDirectory("Kundalis")
                    
                    files.forEach { file ->
                        target?.let { t ->
                            t.findFile(file.name)?.delete()
                            t.createFile("application/json", file.name)?.let { doc ->
                                context.contentResolver.openOutputStream(doc.uri)?.use { it.write(file.readBytes()) }
                            }
                        }
                    }
                    onResult(true, "Backup completed to ${root?.name}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun restoreKundalis(saveUri: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                var restoredCount = 0
                val targetUri = saveUri?.let { Uri.parse(it) }
                
                if (targetUri != null) {
                    // 1. Restore from custom SAF folder
                    val root = DocumentFile.fromTreeUri(context, targetUri)
                    val target = root?.findFile("Kundalis") ?: root // Check both root and subfolder
                    target?.listFiles()?.forEach { doc ->
                        if (doc.name?.endsWith(".json") == true) {
                            context.contentResolver.openInputStream(doc.uri)?.use { input ->
                                File(internalDir, doc.name!!).writeBytes(input.readBytes())
                                restoredCount++
                            }
                        }
                    }
                } else {
                    // 2. Fallback: On Android 10+, MediaStore is owner-restricted. 
                    // To restore everything, the user MUST have a path set.
                    // If no path is set, we try the legacy File API as a last resort (works on some devices/configs)
                    @Suppress("DEPRECATION")
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Kundalis")
                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles()?.forEach { file ->
                            if (file.name.endsWith(".json")) {
                                try {
                                    file.copyTo(File(internalDir, file.name), overwrite = true)
                                    restoredCount++
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    // 3. Also try MediaStore for files the app DOES own
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                        val args = arrayOf("${Environment.DIRECTORY_DOCUMENTS}/Kundalis/%")
                        context.contentResolver.query(MediaStore.Files.getContentUri("external"), arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME), selection, args, null)?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idCol)
                                val name = cursor.getString(nameCol)
                                if (File(internalDir, name).exists()) continue // Don't re-copy if already done by File API
                                
                                val uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    File(internalDir, name).writeBytes(input.readBytes())
                                    restoredCount++
                                }
                            }
                        }
                    }
                }
                
                if (restoredCount == 0 && saveUri.isNullOrEmpty()) {
                    onResult(false, "No files found. If you have old files, please select the folder in Settings first.")
                } else {
                    loadSavedFiles(saveUri)
                    onResult(true, "Restored $restoredCount files")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
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
                if (uri.scheme == "file") {
                    File(uri.path!!).delete()
                } else if (saveUri.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.delete(uri, null, null)
                } else {
                    // For SAF
                    val file = DocumentFile.fromSingleUri(context, uri)
                    file?.delete()
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

    fun clearSuccess() {
        _state.value = _state.value.copy(success = null)
    }
}
