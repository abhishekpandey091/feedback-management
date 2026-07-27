package com.example.feedbackmanagement

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sessionManager = SessionManager(this)

        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passwordInput = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val statusText = findViewById<TextView>(R.id.statusText)
        val studentFeedbackButton =
            findViewById<Button>(R.id.studentFeedbackButton)

        loginButton.setOnClickListener {

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                statusText.text = "Please enter email and password"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            statusText.text = ""
            loginButton.isEnabled = false

            val request = LoginRequest(
                email = email,
                password = password
            )

            ApiClient.apiService.login(request)
                .enqueue(object : Callback<LoginResponse> {

                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        progressBar.visibility = View.GONE
                        loginButton.isEnabled = true

                        if (response.isSuccessful && response.body() != null) {

                            val loginResponse = response.body()!!

                            sessionManager.saveToken(loginResponse.token)
                            sessionManager.saveRole(loginResponse.user.role)

                            if (loginResponse.user.role == "teacher") {

                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        TeacherDashboardActivity::class.java
                                    )
                                )

                                finish()

                            } else if (loginResponse.user.role == "admin") {

                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        AdminDashboardActivity::class.java
                                    )
                                )

                                finish()

                            } else {
                                statusText.text = "Unknown user role"
                            }
                        } else {
                            statusText.text =
                                "Login failed (${response.code()})"
                        }
                    }

                    override fun onFailure(
                        call: Call<LoginResponse>,
                        t: Throwable
                    ) {
                        progressBar.visibility = View.GONE
                        loginButton.isEnabled = true

                        statusText.text =
                            "Connection error: ${t.message}"
                    }
                })
        }

        studentFeedbackButton.setOnClickListener {
            val intent = Intent(
                this,
                StudentFeedbackActivity::class.java
            )
            startActivity(intent)
        }
    }
}