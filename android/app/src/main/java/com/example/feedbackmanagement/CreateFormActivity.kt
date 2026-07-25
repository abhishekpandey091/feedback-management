package com.example.feedbackmanagement

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateFormActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_form)

        val titleEditText = findViewById<EditText>(R.id.titleEditText)
        val descriptionEditText =
            findViewById<EditText>(R.id.descriptionEditText)

        val questionEditText =
            findViewById<EditText>(R.id.questionEditText)

        val questionTypeSpinner =
            findViewById<Spinner>(R.id.questionTypeSpinner)

        val createButton =
            findViewById<Button>(R.id.createFormButton)

        val progressBar =
            findViewById<ProgressBar>(R.id.createProgressBar)

        val statusText =
            findViewById<TextView>(R.id.createStatusText)

        val sessionManager = SessionManager(this)

        // These must match the enum in Form.js
        val questionTypes = listOf(
            "short",
            "paragraph",
            "mcq",
            "checkbox",
            "dropdown",
            "star_rating",
            "yes_no"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            questionTypes
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        questionTypeSpinner.adapter = adapter

        createButton.setOnClickListener {

            val title = titleEditText.text.toString().trim()
            val description =
                descriptionEditText.text.toString().trim()

            val question =
                questionEditText.text.toString().trim()

            val questionType =
                questionTypeSpinner.selectedItem.toString()

            if (title.isEmpty() || question.isEmpty()) {
                statusText.text =
                    "Title and question are required"
                return@setOnClickListener
            }

            val token = sessionManager.getToken()

            if (token.isNullOrEmpty()) {
                statusText.text = "Please login again"
                return@setOnClickListener
            }

            val request = CreateFormRequest(
                title = title,
                description = description,
                questions = listOf(
                    CreateQuestion(
                        questionText = question,
                        type = questionType
                    )
                )
            )

            progressBar.visibility = View.VISIBLE
            createButton.isEnabled = false
            statusText.text = ""

            ApiClient.apiService
                .createForm(
                    "Bearer $token",
                    request
                )
                .enqueue(object : Callback<CreateFormResponse> {

                    override fun onResponse(
                        call: Call<CreateFormResponse>,
                        response: Response<CreateFormResponse>
                    ) {
                        progressBar.visibility = View.GONE
                        createButton.isEnabled = true

                        if (
                            response.isSuccessful &&
                            response.body() != null
                        ) {
                            statusText.text =
                                "Form created successfully - waiting for admin approval"

                            titleEditText.text.clear()
                            descriptionEditText.text.clear()
                            questionEditText.text.clear()

                        } else {
                            statusText.text =
                                "Failed to create form (${response.code()})"
                        }
                    }

                    override fun onFailure(
                        call: Call<CreateFormResponse>,
                        t: Throwable
                    ) {
                        progressBar.visibility = View.GONE
                        createButton.isEnabled = true

                        statusText.text =
                            "Connection error: ${t.message}"
                    }
                })
        }
    }
}