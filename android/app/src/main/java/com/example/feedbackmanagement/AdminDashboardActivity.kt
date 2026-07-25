package com.example.feedbackmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val manageTeachersButton =
            findViewById<Button>(R.id.manageTeachersButton)

        val formsButton =
            findViewById<Button>(R.id.formsButton)

        val logoutButton =
            findViewById<Button>(R.id.logoutButton)

        val sessionManager = SessionManager(this)

        logoutButton.setOnClickListener {
            sessionManager.clearSession()

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }

        manageTeachersButton.setOnClickListener {
            startActivity(
                Intent(this, ManageTeachersActivity::class.java)
            )
        }
    }
}