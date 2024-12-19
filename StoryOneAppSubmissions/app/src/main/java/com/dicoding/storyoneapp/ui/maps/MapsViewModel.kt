package com.dicoding.storyoneapp.ui.maps

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.storyoneapp.data.UserRepository
import com.dicoding.storyoneapp.data.response.GetStoriesResponse
import com.dicoding.storyoneapp.data.response.ListStoryItem
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MapsViewModel(private val repository: UserRepository) : ViewModel() {

    private val _stories = MutableLiveData<List<ListStoryItem>>()
    val stories: LiveData<List<ListStoryItem>> = _stories

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun getStoriesWithLocation(token: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                // Make the API call to get stories with location
                val response = repository.getStoriesWithLocation(token)
                // Log the success message for debugging
                Log.d(TAG, "onSuccess: ${response.message}")

                // Ensure that the response listStory is not null
                val storiesList = response.listStory?.filterNotNull() ?: emptyList()
                // Update LiveData with the filtered list (remove nulls)
                _isLoading.postValue(false)
                _stories.postValue(storiesList)
            } catch (e: HttpException) {
                val jsonInString = e.response()?.errorBody()?.string()
                val errorBody = Gson().fromJson(jsonInString, GetStoriesResponse::class.java)
                val errorMessage = errorBody.message
                _isLoading.postValue(false)
                Log.d(TAG, "onError: $errorMessage")
            }
        }
    }

    companion object {
        private const val TAG = "MapsViewModel"
    }
}
