package com.dicoding.storyoneapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dicoding.storyoneapp.data.UserRepository
import com.dicoding.storyoneapp.data.response.ListStoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: UserRepository) : ViewModel() {

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.logoutUser()
            viewModelScope.launch(Dispatchers.Main) {
                onLogoutSuccess()
            }
        }
    }

    fun getStories(token: String): LiveData<PagingData<ListStoryItem>> {
        return repository.getStories(token)
            .cachedIn(viewModelScope)
    }

}
