package com.dicoding.storyoneapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.storyoneapp.adapter.LoadingStateAdapter
import com.dicoding.storyoneapp.adapter.MainActivityAdapter
import com.dicoding.storyoneapp.data.UserRepository
import com.dicoding.storyoneapp.data.api.ApiConfig
import com.dicoding.storyoneapp.data.pref.UserPreference
import com.dicoding.storyoneapp.databinding.ActivityMainBinding
import com.dicoding.storyoneapp.ui.ViewModelFactory
import com.dicoding.storyoneapp.ui.create.CreateActivity
import com.dicoding.storyoneapp.ui.login.LoginActivity
import com.dicoding.storyoneapp.ui.maps.MapsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")

class MainActivity : AppCompatActivity() {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels {
        ViewModelFactory(
            UserRepository.getInstance(
                UserPreference.getInstance(applicationContext.dataStore),
                ApiConfig.getApiService()
            )
        )
    }

    private val createStoryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val refresh = result.data?.getBooleanExtra("refresh", false) ?: false
                if (refresh) {
                    // Perbarui daftar cerita setelah berhasil upload
                    refreshStories()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView with PagingDataAdapter and LoadingStateAdapter
        val adapter = MainActivityAdapter().apply {
            withLoadStateHeaderAndFooter(
                header = LoadingStateAdapter { this.retry() },
                footer = LoadingStateAdapter { this.retry() }
            )
        }

        binding.rvStories.layoutManager = LinearLayoutManager(this)
        binding.rvStories.adapter = adapter

        // Tambahkan listener untuk LoadState
        adapter.addLoadStateListener { loadState ->
            // Tampilkan progress bar utama jika data sedang dimuat
            binding.progressBar.isVisible = loadState.source.refresh is LoadState.Loading

            // Tampilkan pesan error global jika terjadi kesalahan saat memuat
            val isError = loadState.source.refresh is LoadState.Error
            if (isError) {
                val error = (loadState.source.refresh as LoadState.Error).error
                Toast.makeText(this, "Terjadi kesalahan: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
            }

            // Tampilkan list kosong jika tidak ada data
            val isEmpty = loadState.source.refresh is LoadState.NotLoading && adapter.itemCount == 0
            binding.emptyState.isVisible = isEmpty
        }

        // Observe PagingData
        CoroutineScope(Dispatchers.Main).launch {
            val userPreference = UserPreference.getInstance(applicationContext.dataStore)
            val session = userPreference.getSession().first()
            val token = session.token

            // Observe LiveData
            mainViewModel.getStories(token).observe(this@MainActivity) { pagingData ->
                adapter.submitData(lifecycle, pagingData)
            }
        }

        // FAB Listeners
        binding.fabLogout.setOnClickListener { showLogoutConfirmation() }
        binding.fabMaps.setOnClickListener { navigateToMaps() }
        binding.fabAddStory.setOnClickListener { navigateToCreateStory() }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this).apply {
            setTitle("Logout")
            setMessage("Apakah Anda yakin ingin logout?")
            setPositiveButton("Ya") { _, _ ->
                mainViewModel.logout {
                    Toast.makeText(this@MainActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            }
            setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
            create()
            show()
        }
    }

    private fun refreshStories() {
        CoroutineScope(Dispatchers.Main).launch {
            val userPreference = UserPreference.getInstance(applicationContext.dataStore)
            val session = userPreference.getSession().first()
            val token = session.token

            // Re-fetch the stories after an upload
            mainViewModel.getStories(token).observe(this@MainActivity) { pagingData ->
                (binding.rvStories.adapter as? MainActivityAdapter)?.submitData(lifecycle, pagingData)
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToMaps() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCreateStory() {
        val intent = Intent(this, CreateActivity::class.java)
        createStoryLauncher.launch(intent)
    }
}

