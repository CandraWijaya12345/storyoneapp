package com.dicoding.storyoneapp.ui.signup

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.storyoneapp.data.UserRepository
import com.dicoding.storyoneapp.data.response.RegisterResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SignupViewModel(private val repository: UserRepository) : ViewModel() {

    companion object {
        private const val TAG = "SignupViewModel"
    }

    private val _signupResponse = MutableLiveData<Result<RegisterResponse>>()
    val signupResponse: LiveData<Result<RegisterResponse>> = _signupResponse

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun signup(name: String, email: String, password: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val response = repository.registerUser(name, email, password)
                _isLoading.postValue(false)
                _signupResponse.postValue(Result.success(response))
                Log.d(TAG, "onSuccess: ${response.message}")
            } catch (e: HttpException) {
                val jsonInString = e.response()?.errorBody()?.string()
                val errorBody = Gson().fromJson(jsonInString, RegisterResponse::class.java)
                _isLoading.postValue(false)
                _signupResponse.postValue(Result.failure(Exception(errorBody.message)))
                Log.d(TAG, "onError: ${errorBody.message}")
            }
        }
    }

}
