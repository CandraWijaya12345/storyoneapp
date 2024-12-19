package com.dicoding.storyoneapp.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.storyoneapp.data.UserRepository
import com.dicoding.storyoneapp.data.pref.UserModel
import com.dicoding.storyoneapp.data.response.LoginResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel(private val repository: UserRepository) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val response = repository.loginUser(email, password)
                response.loginResult?.let { loginResult ->
                    saveUser(
                        UserModel(
                            loginResult.userId.toString(),
                            loginResult.name.toString(),
                            email,
                            loginResult.token.toString(),
                            true
                        )
                    )
                } ?: run {
                    Log.e(TAG, "LoginResult is null")
                }

                _isLoading.postValue(false)
                _loginResult.postValue(Result.success(response))
                Log.d(TAG, "onSuccess: ${response.message}")
            } catch (e: HttpException) {
                _isLoading.postValue(false)
                val jsonInString = e.response()?.errorBody()?.string()
                val errorBody = Gson().fromJson(jsonInString, LoginResponse::class.java)
                _loginResult.postValue(e.response()?.let { HttpException(it) }
                    ?.let { Result.failure(it) })
                Log.d(TAG, "onError: ${errorBody.message}")
            }
        }
    }

    fun saveUser(user: UserModel) {
        viewModelScope.launch {
            repository.saveUserSession(user)
        }
    }
}
