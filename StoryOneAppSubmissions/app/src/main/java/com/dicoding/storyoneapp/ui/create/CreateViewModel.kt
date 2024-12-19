package com.dicoding.storyoneapp.ui.create

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.storyoneapp.data.UserRepository
import com.dicoding.storyoneapp.data.response.CreateResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.File

class CreateViewModel(private val repository: UserRepository) : ViewModel() {
    var selectedFile: File? = null
    var previewBitmap: Bitmap? = null
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorResponse = MutableLiveData<CreateResponse>()
    val errorResponse: LiveData<CreateResponse> = _errorResponse

    fun uploadStory(
        token: String,
        imageMultipart: MultipartBody.Part,
        description: RequestBody,
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.createStory(token, imageMultipart, description)
                _isLoading.value = false
                _errorResponse.postValue(response)
                Log.d(TAG, "onSuccess: ${response.message}")
                _isLoading.value = false
            } catch (e: HttpException) {
                val jsonInString = e.response()?.errorBody()?.string()
                val errorBody = Gson().fromJson(jsonInString, CreateResponse::class.java)
                val errorMessage = errorBody.message
                _isLoading.postValue(false)
                _errorResponse.postValue(errorBody)
                Log.d(TAG, "onError: $errorMessage")
            }
        }
    }

    companion object {
        private const val TAG = "CreateViewModel"
    }
}
