package com.hora.jnana.ui.screens

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hora.jnana.models.*
import com.hora.jnana.repository.HoraRepository
import com.hora.jnana.utils.EncryptionUtils
import com.hora.jnana.utils.NetworkUtils
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class MatchMakingState(
    val isLoading: Boolean = false,
    val groom: SavedKundali? = null,
    val bride: SavedKundali? = null,
    val result: MatchMakingResponse? = null,
    val error: String? = null,
    val success: String? = null,
    val isListingFiles: Boolean = false,
    val savedProfiles: List<SavedKundaliMeta> = emptyList(),
    val locations: List<LocationData> = emptyList()
)

class MatchMakingViewModel(
    private val repo: HoraRepository,
    private val context: Context,
    private val moshi: Moshi
) : ViewModel() {
    private val _state = MutableStateFlow(MatchMakingState())
    val state: StateFlow<MatchMakingState> = _state

    private val savedKundaliAdapter by lazy { moshi.adapter(SavedKundali::class.java) }

    private val internalDir = File(context.filesDir, "saved_kundalis").apply { if (!exists()) mkdirs() }

    fun selectGroom(uri: Uri) {
        viewModelScope.launch {
            val profile = loadProfile(uri)
            if (profile != null) {
                _state.value = _state.value.copy(groom = profile, error = null)
            }
        }
    }

    fun selectBride(uri: Uri) {
        viewModelScope.launch {
            val profile = loadProfile(uri)
            if (profile != null) {
                _state.value = _state.value.copy(bride = profile, error = null)
            }
        }
    }

    private fun loadProfile(uri: Uri): SavedKundali? {
        return try {
            val encrypted = context.contentResolver.openInputStream(uri)?.use { input ->
                InputStreamReader(input).use { it.readText() }
            } ?: return null
            val json = EncryptionUtils.decrypt(encrypted)
            savedKundaliAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    fun loadSavedFiles(savePath: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isListingFiles = true, savedProfiles = emptyList())
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
                if (!savePath.isNullOrEmpty()) {
                    val treeUri = Uri.parse(savePath)
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
                android.util.Log.e("MatchMakingViewModel", "Error listing files", e)
            }

            _state.value = _state.value.copy(
                isListingFiles = false, 
                savedProfiles = discovered.distinctBy { it.name + it.date }.sortedBy { it.name.lowercase() }
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
    
    fun savePartialProfile(n: String, d: String, t: String, p: String) {
        viewModelScope.launch {
            try {
                val savedData = SavedKundali(
                    name = n,
                    date = d,
                    time = t,
                    locationName = p,
                    lat = null,
                    lon = null
                )
                
                val json = savedKundaliAdapter.toJson(savedData)
                val encrypted = EncryptionUtils.encrypt(json)
                val fileName = "$n - $d.json"
                
                // Save to Private Internal Storage ONLY
                val file = File(internalDir, fileName)
                file.writeText(encrypted)
                
                _state.value = _state.value.copy(success = "Profile $n added Successfully")
                loadSavedFiles(null)
            } catch (_: Exception) {}
        }
    }

    fun updatePartialProfile(uri: Uri, n: String, d: String, t: String, p: String, savePath: String?) {
        viewModelScope.launch {
            try {
                val encrypted = context.contentResolver.openInputStream(uri)?.use { input ->
                    InputStreamReader(input).use { it.readText() }
                } ?: return@launch
                
                val json = EncryptionUtils.decrypt(encrypted)
                val existingData = savedKundaliAdapter.fromJson(json) ?: return@launch

                val updatedData = existingData.copy(
                    name = n,
                    date = d,
                    time = t,
                    locationName = p
                )
                
                val newJson = savedKundaliAdapter.toJson(updatedData)
                val newEncrypted = EncryptionUtils.encrypt(newJson)
                val newFileName = "$n - $d.json"
                
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
                
                _state.value = _state.value.copy(success = "Profile $n updated successfully")
                loadSavedFiles(savePath)
            } catch (_: Exception) {}
        }
    }

    fun deleteSavedProfile(uri: Uri, savePath: String?) {
        viewModelScope.launch {
            try {
                if (uri.scheme == "file") {
                    File(uri.path!!).delete()
                } else if (savePath.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.delete(uri, null, null)
                } else {
                    // For SAF
                    val file = DocumentFile.fromSingleUri(context, uri)
                    file?.delete()
                }
                loadSavedFiles(savePath)
            } catch (_: Exception) {}
        }
    }

    fun submit(lang: String) {
        val groom = _state.value.groom ?: return
        val bride = _state.value.bride ?: return

        if (!NetworkUtils.isOnline(context)) {
            _state.value = _state.value.copy(error = "Internet required to use")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val request = MatchMakingRequest(
                groom = MatchMakingProfile(
                    name = groom.name,
                    dob = groom.date,
                    tob = formatTime(groom.time),
                    pob = groom.locationName ?: "${groom.lat}, ${groom.lon}"
                ),
                bride = MatchMakingProfile(
                    name = bride.name,
                    dob = bride.date,
                    tob = formatTime(bride.time),
                    pob = bride.locationName ?: "${bride.lat}, ${bride.lon}"
                ),
                lang = if (lang == "kn") "kan" else "en"
            )

            val result = repo.matchMaking(request)
            if (result.isSuccess) {
                _state.value = _state.value.copy(isLoading = false, result = result.getOrNull())
            } else {
                _state.value = _state.value.copy(
                    isLoading = false, 
                    error = result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }

    private fun formatTime(time: String): String {
        // Ensure HH:MM:SS
        return if (time.count { it == ':' } == 1) "$time:00" else time
    }

    fun resetResult() {
        _state.value = _state.value.copy(result = null, error = null)
    }

    fun fetchLocations() {
        viewModelScope.launch {
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
                _state.value = _state.value.copy(locations = locList, error = null)
            } else {
                _state.value = _state.value.copy(error = res.exceptionOrNull()?.message)
            }
        }
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(success = null)
    }
}
