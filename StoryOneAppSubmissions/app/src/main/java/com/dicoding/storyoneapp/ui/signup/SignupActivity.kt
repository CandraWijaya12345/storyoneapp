package com.dicoding.storyoneapp.ui.signup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.storyoneapp.NetworkUtils
import com.dicoding.storyoneapp.R
import com.dicoding.storyoneapp.databinding.ActivitySignupBinding
import com.dicoding.storyoneapp.ui.ViewModelFactory
import com.dicoding.storyoneapp.ui.login.LoginActivity

class SignupActivity : AppCompatActivity() {

    private val viewModel by viewModels<SignupViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupAction()
        observeViewModel()
    }

    private fun setupView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }

    private fun setupAction() {
        binding.buttonRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            when {
                name.isEmpty() -> binding.tilName.error = resources.getString(R.string.name_empty)
                email.isEmpty() -> binding.tilEmail.error = resources.getString(R.string.email_empty)
                password.isEmpty() -> binding.tilPassword.error = resources.getString(R.string.password_empty)
                !NetworkUtils.isInternetAvailable(this) -> {
                    Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.tilName.error = null
                    binding.tilEmail.error = null
                    binding.tilPassword.error = null
                    viewModel.signup(name, email, password)
                }
            }
        }
    }


    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.signupResponse.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, R.string.signup_success, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            result.onFailure { throwable ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.signup_failed)
                    .setMessage(throwable.localizedMessage)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }
}
