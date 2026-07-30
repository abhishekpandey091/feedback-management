package com.example.feedbackmanagement

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateFormActivity : AppCompatActivity() {

    private val addedQuestions = mutableListOf<CreateQuestion>()
    private lateinit var questionsContainer: LinearLayout
    private lateinit var questionsAddedText: TextView

    private val typeLabels = mapOf(
        "short" to "Short Answer",
        "paragraph" to "Paragraph",
        "mcq" to "Multiple Choice",
        "checkbox" to "Checkbox",
        "dropdown" to "Dropdown",
        "star_rating" to "Star Rating",
        "yes_no" to "Yes / No"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_form)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val titleEditText = findViewById<EditText>(R.id.titleEditText)
        val descriptionEditText =
            findViewById<EditText>(R.id.descriptionEditText)

        val questionEditText =
            findViewById<EditText>(R.id.questionEditText)

        val questionTypeSpinner =
            findViewById<Spinner>(R.id.questionTypeSpinner)

        val optionsEditText =
            findViewById<EditText>(R.id.optionsEditText)

        val addQuestionButton =
            findViewById<Button>(R.id.addQuestionButton)

        val createButton =
            findViewById<Button>(R.id.createFormButton)

        questionsAddedText =
            findViewById(R.id.questionsAddedText)

        questionsContainer =
            findViewById(R.id.questionsContainer)

        val statusText =
            findViewById<TextView>(R.id.createStatusText)

        val progressBar =
            findViewById<ProgressBar>(R.id.createProgressBar)

        val sessionManager = SessionManager(this)

        // If FORM_ID exists -> Edit mode
        val formId = intent.getStringExtra("FORM_ID")
        val isEditMode = !formId.isNullOrEmpty()

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
            questionTypes.map { typeLabels[it] ?: it }
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        questionTypeSpinner.adapter = adapter

        if (isEditMode) {
            findViewById<MaterialToolbar>(R.id.toolbar).title = "Edit Feedback Form"
            createButton.text = "Update Form"

            val token = sessionManager.getToken()

            if (!token.isNullOrEmpty()) {

                progressBar.visibility = View.VISIBLE

                ApiClient.apiService
                    .getFormById(
                        "Bearer $token",
                        formId!!
                    )
                    .enqueue(object : Callback<CreateFormResponse> {

                        override fun onResponse(
                            call: Call<CreateFormResponse>,
                            response: Response<CreateFormResponse>
                        ) {
                            progressBar.visibility = View.GONE

                            if (
                                response.isSuccessful &&
                                response.body()?.form != null
                            ) {
                                val form = response.body()!!.form

                                titleEditText.setText(form.title)
                                descriptionEditText.setText(
                                    form.description ?: ""
                                )

                                addedQuestions.clear()

                                form.questions.forEach { question ->

                                    addedQuestions.add(
                                        CreateQuestion(
                                            questionText =
                                            question.questionText,
                                            type = question.type,
                                            options = question.options,
                                            maxStars = question.maxStars,
                                            required = question.required
                                        )
                                    )
                                }

                                renderQuestionsList()

                            } else {
                                statusText.text =
                                    "Failed to load form (${response.code()})"
                            }
                        }

                        override fun onFailure(
                            call: Call<CreateFormResponse>,
                            t: Throwable
                        ) {
                            progressBar.visibility = View.GONE

                            statusText.text =
                                "Connection error: ${t.message}"
                        }
                    })
            }
        }

        // ADD QUESTION
        addQuestionButton.setOnClickListener {

            val question =
                questionEditText.text.toString().trim()

            val questionType =
                questionTypes[questionTypeSpinner.selectedItemPosition]

            val options =
                optionsEditText.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

            if (question.isEmpty()) {
                statusText.text = "Enter a question"
                return@setOnClickListener
            }

            if (
                questionType in listOf(
                    "mcq",
                    "checkbox",
                    "dropdown"
                ) &&
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

            renderQuestionsList()

            questionEditText.text.clear()
            optionsEditText.text.clear()

            statusText.text = "Question added"
        }

        // CREATE / UPDATE
        createButton.setOnClickListener {

            val title =
                titleEditText.text.toString().trim()

            val description =
                descriptionEditText.text.toString().trim()

            if (title.isEmpty()) {
                statusText.text = "Form title is required"
                return@setOnClickListener
            }

            if (addedQuestions.isEmpty()) {
                statusText.text =
                    "Add at least one question first"
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
                questions = addedQuestions.toList()
            )

            progressBar.visibility = View.VISIBLE
            createButton.isEnabled = false
            addQuestionButton.isEnabled = false
            statusText.text = ""

            val apiCall =
                if (isEditMode) {
                    ApiClient.apiService.updateForm(
                        "Bearer $token",
                        formId!!,
                        request
                    )
                } else {
                    ApiClient.apiService.createForm(
                        "Bearer $token",
                        request
                    )
                }

            apiCall.enqueue(
                object : Callback<CreateFormResponse> {

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

                            if (isEditMode) {
                                Toast.makeText(
                                    this@CreateFormActivity,
                                    "Form updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                finish()

                            } else {
                                statusText.text =
                                    "Form created successfully - waiting for admin approval"

                                titleEditText.text.clear()
                                descriptionEditText.text.clear()
                                questionEditText.text.clear()
                                optionsEditText.text.clear()

                                addedQuestions.clear()
                                renderQuestionsList()
                            }

                        } else {
                            statusText.text =
                                if (isEditMode)
                                    "Failed to update form (${response.code()})"
                                else
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
                }
            )
        }

        renderQuestionsList()
    }

    private fun renderQuestionsList() {

        questionsAddedText.text =
            "Questions added: ${addedQuestions.size}"

        questionsContainer.removeAllViews()

        if (addedQuestions.isEmpty()) {
            questionsContainer.addView(
                UiKit.emptyState(
                    this,
                    R.drawable.ic_description,
                    "No questions yet",
                    "Questions you add will appear here"
                )
            )
            return
        }

        addedQuestions.forEachIndexed { index, question ->

            val (card, inner) = UiKit.card(this, marginBottomDp = 12)

            val headerRow = LinearLayout(this)
            headerRow.orientation = LinearLayout.HORIZONTAL
            headerRow.gravity = Gravity.TOP

            val textCol = LinearLayout(this)
            textCol.orientation = LinearLayout.VERTICAL
            val textColParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            textCol.layoutParams = textColParams

            textCol.addView(
                UiKit.cardTitle(this, "${index + 1}. ${question.questionText}")
            )

            val meta = typeLabels[question.type] ?: question.type
            textCol.addView(UiKit.captionText(this, meta.uppercase()))

            if (question.options.isNotEmpty()) {
                textCol.addView(
                    UiKit.bodyText(this, question.options.joinToString(" · "))
                )
            }

            headerRow.addView(textCol)

            val removeButton = ImageButton(this)
            removeButton.setImageResource(R.drawable.ic_delete)
            removeButton.background = null
            removeButton.setColorFilter(
                androidx.core.content.ContextCompat.getColor(this, R.color.semantic_error)
            )
            removeButton.contentDescription = "Remove question"
            val removeParams = LinearLayout.LayoutParams(
                UiKit.dp(this, 36),
                UiKit.dp(this, 36)
            )
            removeButton.layoutParams = removeParams

            removeButton.setOnClickListener {
                addedQuestions.removeAt(index)
                renderQuestionsList()
            }

            headerRow.addView(removeButton)
            inner.addView(headerRow)

            questionsContainer.addView(card)
        }
    }
}
