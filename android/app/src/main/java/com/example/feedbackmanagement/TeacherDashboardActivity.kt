package com.example.feedbackmanagement

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class TeacherDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        val logoutButton = findViewById<View>(R.id.logoutButton)
        val myFormsButton = findViewById<View>(R.id.myFormsButton)
        val createFormButton = findViewById<View>(R.id.createFormButton)

        val sessionManager = SessionManager(this)

        createFormButton.setOnClickListener {
            val intent = Intent(
                this,
                CreateFormActivity::class.java
            )
            startActivity(intent)
        }

        myFormsButton.setOnClickListener {
            val intent = Intent(
                this,
                MyFormsActivity::class.java
            )

            startActivity(intent)
        }

        logoutButton.setOnClickListener {

            // Delete saved JWT and role
            sessionManager.clearSession()

            // Return to login
            val intent = Intent(
                this,
                MainActivity::class.java
            )

            startActivity(intent)
            finish()
        }
    }
}
