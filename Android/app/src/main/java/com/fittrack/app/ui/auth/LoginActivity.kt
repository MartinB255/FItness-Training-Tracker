package com.fittrack.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.R
import com.fittrack.app.data.api.RetrofitClient
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch

/**
 * Login / Registration screen.
 * First screen the user sees if not logged in.
 * On success, saves the auth token and navigates to DashboardActivity.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var tvToggle: TextView
    private lateinit var progressBar: ProgressBar

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already logged in, skip to dashboard
        if (RetrofitClient.isLoggedIn()) {
            startDashboard()
            return
        }

        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        tvToggle = findViewById(R.id.tvToggle)
        progressBar = findViewById(R.id.progressBar)

        updateMode()

        btnLogin.setOnClickListener { performLogin() }
        btnRegister.setOnClickListener { performRegister() }

        tvToggle.setOnClickListener {
            isLoginMode = !isLoginMode
            updateMode()
        }
    }

    /** Switch between login and register form. */
    private fun updateMode() {
        if (isLoginMode) {
            etEmail.visibility = View.GONE
            btnLogin.visibility = View.VISIBLE
            btnRegister.visibility = View.GONE
            tvToggle.text = "Don't have an account? Register"
        } else {
            etEmail.visibility = View.VISIBLE
            btnLogin.visibility = View.GONE
            btnRegister.visibility = View.VISIBLE
            tvToggle.text = "Already have an account? Login"
        }
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = FitTrackRepository.login(username, password)
            setLoading(false)
            result.onSuccess { auth ->
                RetrofitClient.saveToken(auth.token)
                startDashboard()
            }.onFailure { e ->
                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performRegister() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = FitTrackRepository.register(username, email, password)
            setLoading(false)
            result.onSuccess { auth ->
                RetrofitClient.saveToken(auth.token)
                startDashboard()
            }.onFailure { e ->
                Toast.makeText(this@LoginActivity, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnRegister.isEnabled = !loading
    }

    private fun startDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
