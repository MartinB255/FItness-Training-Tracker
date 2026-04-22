package com.fittrack.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.R
import com.fittrack.app.data.api.RetrofitClient
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.ui.dashboard.DashboardActivity
import com.fittrack.app.util.UserStore
import kotlinx.coroutines.launch

/**
 * Login / Register screen. Hits `/auth/login/` or `/auth/register/`;
 * stores the returned token (for the auth interceptor) and the user id/name
 * (for greetings). In Register mode also asks for email + confirm-password.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnAction: Button
    private lateinit var tvToggle: TextView
    private var progress: ProgressBar? = null

    private var registerMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // If the token is still valid, skip straight to the dashboard.
        if (RetrofitClient.isLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnAction = findViewById(R.id.btnAction)
        tvToggle = findViewById(R.id.tvToggle)
        progress = findViewById(R.id.progress)

        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()

        btnAction.setOnClickListener { submit() }
        tvToggle.setOnClickListener { toggleMode() }

        applyMode()
    }

    private fun toggleMode() {
        registerMode = !registerMode
        applyMode()
    }

    private fun applyMode() {
        val registerVisibility = if (registerMode) View.VISIBLE else View.GONE
        etEmail.visibility = registerVisibility
        etConfirmPassword.visibility = registerVisibility
        btnAction.text = if (registerMode) "Register" else "Login"
        tvToggle.text = if (registerMode) {
            "Already have an account? Login"
        } else {
            "Don't have an account? Register"
        }
    }

    private fun submit() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in username and password.")
            return
        }

        if (registerMode) {
            val email = etEmail.text.toString().trim()
            val confirm = etConfirmPassword.text.toString()
            if (email.isEmpty()) {
                showError("Please provide an email address.")
                return
            }
            if (password != confirm) {
                showError("Passwords do not match.")
                return
            }
            // Django's RegisterSerializer enforces min_length=8; fail fast here.
            if (password.length < 8) {
                showError("Password must be at least 8 characters.")
                return
            }
            callBackend(username, etEmail.text.toString().trim(), password, true)
        } else {
            callBackend(username, null, password, false)
        }
    }

    private fun callBackend(
        username: String, email: String?, password: String, register: Boolean,
    ) {
        setLoading(true)
        lifecycleScope.launch {
            val result = if (register) {
                FitTrackRepository.register(username, email ?: "", password)
            } else {
                FitTrackRepository.login(username, password)
            }
            setLoading(false)
            result
                .onSuccess { auth ->
                    RetrofitClient.saveToken(auth.token)
                    UserStore.save(this@LoginActivity, auth.user.id, auth.user.username)
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                }
                .onFailure { showError(it.message ?: "Login failed.") }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnAction.isEnabled = !loading
        progress?.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Oops")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
