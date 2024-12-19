package com.dicoding.storyoneapp.ui.create

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dicoding.storyoneapp.NetworkUtils
import com.dicoding.storyoneapp.createTempFile
import com.dicoding.storyoneapp.data.pref.UserPreference
import com.dicoding.storyoneapp.databinding.ActivityCreateBinding
import com.dicoding.storyoneapp.reduceFileImage
import com.dicoding.storyoneapp.ui.ViewModelFactory
import com.dicoding.storyoneapp.uriToFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

@Suppress("DEPRECATION")
class CreateActivity : AppCompatActivity() {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

    private lateinit var binding: ActivityCreateBinding
    private val createViewModel: CreateViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private val requestCodePermissions = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi binding
        binding = ActivityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Periksa izin
        if (!checkPermissions()) {
            requestPermissions()
        }

        // Observe loading indicator
        createViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error response
        createViewModel.errorResponse.observe(this) { errorResponse ->
            if (errorResponse.error == true) {
                Toast.makeText(this, "Error: ${errorResponse.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Set pratinjau jika ada
        createViewModel.previewBitmap?.let {
            binding.ivPreview.setImageBitmap(it)
        }

        // Setup listener
        setupListeners()
    }

    private fun checkPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, requestCodePermissions)
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted
            } else {
                Toast.makeText(this, "Permissions are required to use this feature", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnCamera.setOnClickListener { openCamera() }
            btnGallery.setOnClickListener { openGallery() }
            btnUpload.setOnClickListener { uploadStory() }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photoFile = createTempFile(this)
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                bitmap?.let {
                    photoFile.outputStream().use { output ->
                        it.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    }
                }
                createViewModel.selectedFile = photoFile
                createViewModel.previewBitmap = bitmap
                binding.ivPreview.setImageBitmap(bitmap)
            }
        }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    createViewModel.selectedFile = uriToFile(it, this)
                    createViewModel.previewBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    binding.ivPreview.setImageURI(uri)
                }
            }
        }

    private fun uploadStory() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No Internet Connection!", Toast.LENGTH_SHORT).show()
            return
        }

        if (createViewModel.selectedFile == null) {
            Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show()
            return
        }

        val descriptionText = binding.etDescription.text.toString()
        if (descriptionText.isBlank()) {
            Toast.makeText(this, "Description cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val description = RequestBody.create("text/plain".toMediaType(), descriptionText)
        val file = reduceFileImage(createViewModel.selectedFile!!)
        val imageMultipart = MultipartBody.Part.createFormData(
            "photo", file.name, RequestBody.create("image/jpeg".toMediaType(), file.readBytes())
        )

        CoroutineScope(Dispatchers.Main).launch {
            val userPreference = UserPreference.getInstance(dataStore)
            val token = userPreference.getSession().first().token

            // Upload story
            createViewModel.uploadStory(token, imageMultipart, description)
            createViewModel.errorResponse.observe(this@CreateActivity) { errorResponse ->
                if (errorResponse.error == false) {
                    Toast.makeText(this@CreateActivity, "Story uploaded successfully!", Toast.LENGTH_SHORT).show()

                    // Set the result to indicate a refresh
                    val intent = Intent().apply {
                        putExtra("refresh", true)  // Send the refresh flag
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                } else {
                    Toast.makeText(this@CreateActivity, "Upload failed: ${errorResponse.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
