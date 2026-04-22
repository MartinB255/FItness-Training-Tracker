package com.fittrack.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.fittrack.app.R
import com.fittrack.app.ui.dashboard.DashboardActivity
import com.fittrack.app.util.UserStore

/**
 * Login / Register screen.
 * Since the backend isn't wired yet, any non-empty credentials pass.
 * In Register mode, also asks for email + confirm-password.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnAction: Button
    private lateinit var tvToggle: TextView

    private var registerMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnAction = findViewById(R.id.btnAction)
        tvToggle = findViewById(R.id.tvToggle)

        // Instant character masking — no reveal-last-char delay.
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
            if (password.length < 6) {
                showError("Password must be at least 6 characters.")
                return
            }
        }

        // Backend not wired yet — remember the username locally and go to Dashboard.
        UserStore.saveUsername(this, username)
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Oops")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
