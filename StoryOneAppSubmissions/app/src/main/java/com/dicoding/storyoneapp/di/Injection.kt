package com.dicoding.storyoneapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dicoding.storyoneapp.data.UserRepository
import com.dicoding.storyoneapp.data.api.ApiConfig
import com.dicoding.storyoneapp.data.api.ApiService
import com.dicoding.storyoneapp.data.pref.UserPreference
import com.dicoding.storyoneapp.ui.maps.MapsActivity

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object Injection {
    fun provideRepository(context: Context): UserRepository {
        val pref = UserPreference.getInstance(context.dataStore)
        val apiService = ApiConfig.getApiService()
        return UserRepository.getInstance(pref, apiService)
    }

    fun provideApiService(mapsActivity: MapsActivity): ApiService {
        return ApiConfig.getApiService()
    }
}