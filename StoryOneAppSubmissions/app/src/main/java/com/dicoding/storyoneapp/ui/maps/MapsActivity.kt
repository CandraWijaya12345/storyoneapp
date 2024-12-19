package com.dicoding.storyoneapp.ui.maps

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.dicoding.storyoneapp.R
import com.dicoding.storyoneapp.data.UserRepository
import com.dicoding.storyoneapp.data.api.ApiService
import com.dicoding.storyoneapp.data.pref.UserPreference

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.dicoding.storyoneapp.databinding.ActivityMapsBinding
import com.dicoding.storyoneapp.di.Injection
import com.dicoding.storyoneapp.ui.ViewModelFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private lateinit var userPreference: UserPreference
    private lateinit var apiService: ApiService

    // Create a ViewModel instance
    private lateinit var viewModel: MapsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize preferences and API service
        userPreference = UserPreference.getInstance(dataStore)
        apiService = Injection.provideApiService(this)

        // Inflate the layout
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the ViewModel
        val viewModelFactory = ViewModelFactory(UserRepository.getInstance(userPreference, apiService))
        viewModel = viewModelFactory.create(MapsViewModel::class.java)

        // Fetch the token and make the API call
        lifecycleScope.launch {
            userPreference.getSession().collect { user ->
                val token = user.token
                if (!token.isNullOrEmpty()) {
                    val formattedToken = "Bearer $token"
                    viewModel.getStoriesWithLocation(formattedToken)
                } else {
                    Log.e("MapsActivity", "Token is null or empty")
                }
            }
        }

        // Observe stories LiveData and update markers on the map
        viewModel.stories.observe(this) { stories ->
            mMap.clear() // Clear previous markers before adding new ones
            for (story in stories) {
                val location = LatLng(story.lat ?: 0.0, story.lon ?: 0.0)
                mMap.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title(story.name)
                        .snippet(story.description)
                )
            }
            // If there are stories, move the camera to the first story's location
            if (stories.isNotEmpty()) {
                val firstStory = stories.first()
                val firstLocation = LatLng(firstStory.lat ?: 0.0, firstStory.lon ?: 0.0)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10f))
            }
        }

        // Observe error message and show it as a Toast
        viewModel.errorMessage.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    // Handle map types from the options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.map_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.normal_type -> {
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }
            R.id.satellite_type -> {
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            }
            R.id.terrain_type -> {
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            }
            R.id.hybrid_type -> {
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    // Permission handling for location
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getMyLocation()
            }
        }

    private fun getMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Convert vector drawable to BitmapDescriptor
    private fun vectorToBitmap(@DrawableRes id: Int, @ColorInt color: Int): BitmapDescriptor {
        val vectorDrawable = ResourcesCompat.getDrawable(resources, id, null)
        if (vectorDrawable == null) {
            Log.e("BitmapHelper", "Resource not found")
            return BitmapDescriptorFactory.defaultMarker()
        }
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        DrawableCompat.setTint(vectorDrawable, color)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // onMapReady callback to initialize map
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        // Call getMyLocation to enable location services if needed
        getMyLocation()
    }
}
