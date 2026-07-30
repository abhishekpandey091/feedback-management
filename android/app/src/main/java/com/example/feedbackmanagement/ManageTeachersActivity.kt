package com.example.feedbackmanagement

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageTeachersActivity : AppCompatActivity() {

    private lateinit var teachersContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_teachers)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val addTeacherButton = findViewById<Button>(R.id.addTeacherButton)

        teachersContainer = findViewById(R.id.teachersContainer)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)

        sessionManager = SessionManager(this)

        loadTeachers()

        addTeacherButton.setOnClickListener {

            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                statusText.text = "Fill all fields"
                return@setOnClickListener
            }

            val token = sessionManager.getToken()

            if (token.isNullOrEmpty()) {
                statusText.text = "Please login again"
                return@setOnClickListener
            }

            val request = CreateTeacherRequest(
                fullName = name,
                email = email,
                password = password
            )

            progressBar.visibility = View.VISIBLE

            ApiClient.apiService
                .createTeacher("Bearer $token", request)
                .enqueue(object : Callback<TeacherResponse> {

                    override fun onResponse(
                        call: Call<TeacherResponse>,
                        response: Response<TeacherResponse>
                    ) {
                        progressBar.visibility = View.GONE

                        if (response.isSuccessful) {

                            statusText.text = "Teacher created"

                            nameInput.text.clear()
                            emailInput.text.clear()
                            passwordInput.text.clear()

                            loadTeachers()

                        } else {
                            statusText.text =
                                "Failed (${response.code()})"
                        }
                    }

                    override fun onFailure(
                        call: Call<TeacherResponse>,
                        t: Throwable
                    ) {
                        progressBar.visibility = View.GONE
                        statusText.text =
                            "Connection error: ${t.message}"
                    }
                })
        }
    }

    private fun loadTeachers() {

        val token = sessionManager.getToken()

        if (token.isNullOrEmpty()) {
            statusText.text = "Please login again"
            return
        }

        progressBar.visibility = View.VISIBLE

        ApiClient.apiService
            .getTeachers("Bearer $token")
            .enqueue(object : Callback<TeachersResponse> {

                override fun onResponse(
                    call: Call<TeachersResponse>,
                    response: Response<TeachersResponse>
                ) {
                    progressBar.visibility = View.GONE

                    if (
                        response.isSuccessful &&
                        response.body() != null
                    ) {

                        teachersContainer.removeAllViews()

                        val teachers = response.body()!!.teachers

                        if (teachers.isEmpty()) {
                            statusText.text = ""
                            teachersContainer.addView(
                                UiKit.emptyState(
                                    this@ManageTeachersActivity,
                                    R.drawable.ic_group,
                                    "No teachers yet",
                                    "Teachers you add will appear here"
                                )
                            )
                            return
                        }

                        statusText.text = ""

                        for (teacher in teachers) {
                            addTeacherCard(teacher, token)
                        }

                    } else {
                        statusText.text =
                            "Failed to load teachers (${response.code()})"
                    }
                }

                override fun onFailure(
                    call: Call<TeachersResponse>,
                    t: Throwable
                ) {
                    progressBar.visibility = View.GONE
                    statusText.text =
                        "Connection error: ${t.message}"
                }
            })
    }

    private fun addTeacherCard(teacher: TeacherData, token: String) {

        val (card, inner) = UiKit.card(this)

        val headerRow = LinearLayout(this)
        headerRow.orientation = LinearLayout.HORIZONTAL
        headerRow.gravity = android.view.Gravity.CENTER_VERTICAL

        val textCol = LinearLayout(this)
        textCol.orientation = LinearLayout.VERTICAL
        val textColParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        textCol.layoutParams = textColParams

        textCol.addView(UiKit.cardTitle(this, teacher.fullName))
        textCol.addView(UiKit.bodyText(this, teacher.email))

        headerRow.addView(textCol)
        headerRow.addView(UiKit.activeChip(this, teacher.isActive))

        inner.addView(headerRow)

        val actions = UiKit.wrapRow(this)

        actions.addView(
            UiKit.actionChip(
                this,
                if (teacher.isActive) "Deactivate" else "Activate",
                iconRes = if (teacher.isActive) R.drawable.ic_cancel else R.drawable.ic_check_circle,
                destructive = teacher.isActive
            ) {
                ApiClient.apiService
                    .toggleTeacherStatus(
                        "Bearer $token",
                        teacher._id
                    )
                    .enqueue(object : Callback<TeacherResponse> {

                        override fun onResponse(
                            call: Call<TeacherResponse>,
                            response: Response<TeacherResponse>
                        ) {
                            if (response.isSuccessful) {
                                statusText.text = "Teacher status updated"
                                loadTeachers()
                            } else {
                                statusText.text =
                                    "Failed to update (${response.code()})"
                            }
                        }

                        override fun onFailure(
                            call: Call<TeacherResponse>,
                            t: Throwable
                        ) {
                            statusText.text =
                                "Connection error: ${t.message}"
                        }
                    })
            }
        )

        inner.addView(actions)
        teachersContainer.addView(card)
    }
}
