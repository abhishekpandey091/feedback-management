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
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val questionEditText = findViewById<EditText>(R.id.questionEditText)
        val questionTypeSpinner = findViewById<Spinner>(R.id.questionTypeSpinner)
        val optionsEditText = findViewById<EditText>(R.id.optionsEditText)

        val addQuestionButton = findViewById<Button>(R.id.addQuestionButton)
        val createButton = findViewById<Button>(R.id.createFormButton)

        val questionsAddedText = findViewById<TextView>(R.id.questionsAddedText)
        val statusText = findViewById<TextView>(R.id.createStatusText)
        val progressBar = findViewById<ProgressBar>(R.id.createProgressBar)

        val sessionManager = SessionManager(this)

        val addedQuestions = mutableListOf<CreateQuestion>()

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

        // ADD QUESTION
        addQuestionButton.setOnClickListener {

            val question = questionEditText.text.toString().trim()
            val questionType = questionTypeSpinner.selectedItem.toString()

            val options = optionsEditText.text.toString()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (question.isEmpty()) {
                statusText.text = "Enter a question"
                return@setOnClickListener
            }

            if (
                questionType in listOf("mcq", "checkbox", "dropdown") &&
                options.size < 2
            ) {
                statusText.text =
                    "Enter at least 2 comma-separated options"

                return@setOnClickListener
            }

            addedQuestions.add(
                CreateQuestion(
                    questionText = question,
                    type = questionType,
                    options = options,
                    maxStars = 10,
                    required = true
                )
            )

            questionsAddedText.text =
                "Questions added: ${addedQuestions.size}"

            questionEditText.text.clear()
            optionsEditText.text.clear()

            statusText.text = "Question added"
        }

        // CREATE FORM
        createButton.setOnClickListener {

            val title = titleEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            if (title.isEmpty()) {
                statusText.text = "Form title is required"
                return@setOnClickListener
            }

            if (addedQuestions.isEmpty()) {
                statusText.text = "Add at least one question first"
                return@setOnClickListener
            }

            val token = sessionManager.getToken()

            if (token.isNullOrEmpty()) {
                statusText.text = "Please login again"
                return@setOnClickListener
            }

            // IMPORTANT: send questions BEFORE clearing the list
            val request = CreateFormRequest(
                title = title,
                description = description,
                questions = addedQuestions.toList()
            )

            progressBar.visibility = View.VISIBLE
            createButton.isEnabled = false
            addQuestionButton.isEnabled = false
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
                        addQuestionButton.isEnabled = true

                        if (
                            response.isSuccessful &&
                            response.body() != null
                        ) {
                            statusText.text =
                                "Form created successfully - waiting for admin approval"

                            titleEditText.text.clear()
                            descriptionEditText.text.clear()
                            questionEditText.text.clear()
                            optionsEditText.text.clear()

                            // Clear ONLY after successful creation
                            addedQuestions.clear()

                            questionsAddedText.text =
                                "Questions added: 0"

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
                        addQuestionButton.isEnabled = true

                        statusText.text =
                            "Connection error: ${t.message}"
                    }
                })
        }
    }
}