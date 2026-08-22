package com.hora.jnana.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hora.jnana.BuildConfig
import com.hora.jnana.api.AuthService
import com.hora.jnana.api.models.ApiErrorResponse
import com.hora.jnana.api.models.LoginRequest
import com.hora.jnana.data.AuthRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel(
    private val authService: AuthService,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val tag = "LoginViewModel"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val errorAdapter = moshi.adapter(ApiErrorResponse::class.java)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(uuid: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = authService.login(LoginRequest(uuid))
                authRepository.saveSessionToken(response.token)
                _uiState.value = LoginUiState.Success
                onLoginSuccess()
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val apiError = try {
                    errorBody?.let { errorAdapter.fromJson(it) }?.error
                } catch (_: Exception) {
                    null
                }

                val errorMsg = apiError?.message ?: "Unable to connect to backend server, please try later"
                _uiState.value = LoginUiState.Error(errorMsg)
            } catch (e: IOException) {
                if (BuildConfig.DEBUG) Log.e(tag, "Network error", e)
                _uiState.value = LoginUiState.Error("Internet required to use")
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(tag, "Unexpected error during login", e)
                _uiState.value = LoginUiState.Error("Unable to connect to backend server, please try later")
            }
        }
    }
}
