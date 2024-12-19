package com.dicoding.storyoneapp.ui.login

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.storyoneapp.MainActivity
import com.dicoding.storyoneapp.NetworkUtils
import com.dicoding.storyoneapp.R
import com.dicoding.storyoneapp.databinding.ActivityLoginBinding
import com.dicoding.storyoneapp.ui.ViewModelFactory

class LoginActivity : AppCompatActivity() {

    private val viewModel by viewModels<LoginViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupAction()
        playAnimation()

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
        binding.myButton.isEnabled = false // Tombol dinonaktifkan secara default

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setMyButtonEnable()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        binding.emailEditText.addTextChangedListener(textWatcher)
        binding.passwordEditText.addTextChangedListener(textWatcher)

        binding.myButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            when {
                email.isEmpty() -> binding.tilEmail.error = resources.getString(R.string.email_empty)
                !isValidEmail(email) -> binding.tilEmail.error = resources.getString(R.string.invalid_email)
                password.isEmpty() -> binding.tilPassword.error = resources.getString(R.string.password_empty)
                password.length < 8 -> binding.tilPassword.error = resources.getString(R.string.password_minimum_length)
                !NetworkUtils.isInternetAvailable(this) -> {
                    Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.tilEmail.error = null
                    binding.tilPassword.error = null
                    viewModel.login(email, password)
                }
            }
        }
    }

    private fun setMyButtonEnable() {
        val password = binding.passwordEditText.text.toString().trim()

        // Tambahkan validasi panjang password
        binding.myButton.isEnabled = password.length >= 8
    }


    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        return email.matches(emailRegex)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { loginResponse ->
                val user = com.dicoding.storyoneapp.data.pref.UserModel(
                    idUser = loginResponse.loginResult?.userId.orEmpty(),
                    name = loginResponse.loginResult?.name.orEmpty(),
                    email = binding.emailEditText.text.toString(),
                    token = loginResponse.loginResult?.token.orEmpty(),
                    isLogin = true
                )

                viewModel.saveUser(user)

                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            result.onFailure { throwable ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.login_failed)
                    .setMessage(throwable.localizedMessage)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun playAnimation() {
        ObjectAnimator.ofFloat(binding.imageView, View.TRANSLATION_X, -30f, 30f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }.start()

        val title = ObjectAnimator.ofFloat(binding.titleTextView, View.ALPHA, 1f).setDuration(100)
        val message = ObjectAnimator.ofFloat(binding.messageTextView, View.ALPHA, 1f).setDuration(100)
        val emailTextView = ObjectAnimator.ofFloat(binding.emailTextView, View.ALPHA, 1f).setDuration(100)
        val emailEditTextLayout = ObjectAnimator.ofFloat(binding.tilEmail, View.ALPHA, 1f).setDuration(100)
        val passwordTextView = ObjectAnimator.ofFloat(binding.passwordTextView, View.ALPHA, 1f).setDuration(100)
        val passwordEditTextLayout = ObjectAnimator.ofFloat(binding.tilPassword, View.ALPHA, 1f).setDuration(100)
        val loginButton = ObjectAnimator.ofFloat(binding.myButton, View.ALPHA, 1f).setDuration(100)

        AnimatorSet().apply {
            playSequentially(
                title,
                message,
                emailTextView,
                emailEditTextLayout,
                passwordTextView,
                passwordEditTextLayout,
                loginButton
            )
            startDelay = 100
        }.start()
    }
}
