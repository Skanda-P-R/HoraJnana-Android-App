package com.hora.jnana.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hora.jnana.DataStoreManager
import com.hora.jnana.api.AuthService
import com.hora.jnana.api.HoraApiService
import com.hora.jnana.data.AuthRepository
import com.hora.jnana.repository.HoraRepository
import com.hora.jnana.ui.login.LoginViewModel
import com.squareup.moshi.Moshi

class ViewModelFactory(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val authService: AuthService,
    private val horaRepository: HoraRepository,
    private val moshi: Moshi,
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(horaRepository) as T
        }
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authService, authRepository) as T
        }
        if (modelClass.isAssignableFrom(TransitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransitViewModel(horaRepository, context.applicationContext, dataStoreManager) as T
        }
        if (modelClass.isAssignableFrom(BirthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BirthViewModel(horaRepository, context.applicationContext, moshi, dataStoreManager) as T
        }
        if (modelClass.isAssignableFrom(MatchMakingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MatchMakingViewModel(horaRepository, context.applicationContext, moshi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
