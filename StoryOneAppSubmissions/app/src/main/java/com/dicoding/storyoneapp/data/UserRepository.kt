package com.dicoding.storyoneapp.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dicoding.storyoneapp.data.api.ApiService
import com.dicoding.storyoneapp.data.pref.UserModel
import com.dicoding.storyoneapp.data.pref.UserPreference
import com.dicoding.storyoneapp.data.response.CreateResponse
import com.dicoding.storyoneapp.data.response.GetStoriesResponse
import com.dicoding.storyoneapp.data.response.ListStoryItem
import com.dicoding.storyoneapp.data.response.LoginResponse
import com.dicoding.storyoneapp.data.response.RegisterResponse
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import okhttp3.RequestBody

class UserRepository private constructor(
    private val userPreference: UserPreference,
    private val apiService: ApiService
) {

    suspend fun saveUserSession(user: UserModel) {
        userPreference.saveSession(user)
    }


    suspend fun registerUser(name: String, email: String, password: String): RegisterResponse {
        return apiService.register(name, email, password)
    }

    suspend fun logoutUser() {
        userPreference.logout()
    }

    fun getStories(token: String): LiveData<PagingData<ListStoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { StoriesPagingSource(apiService, token) }
        ).flow.asLiveData()
    }




    suspend fun getStoriesWithLocation(token: String) = apiService.getStoriesWithLocation(token)


    suspend fun loginUser(email: String, password: String): LoginResponse {
        return apiService.login(email, password)
    }


    suspend fun createStory(
        token: String,
        file: MultipartBody.Part,
        description: RequestBody,

    ): CreateResponse {
        return apiService.createStory("Bearer $token", file, description)
    }

    companion object {
        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(
            userPreference: UserPreference,
            apiService: ApiService
        ): UserRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserRepository(userPreference,apiService)
                INSTANCE = instance
                instance
            }
        }
    }
}
