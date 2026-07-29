package com.example.feedbackmanagement

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReFeedbackActivity : AppCompatActivity() {

    private lateinit var formContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private val answerViews =
        mutableMapOf<String, View>()

    private lateinit var currentForm: ReFeedbackForm
    private lateinit var oldResponse: ReFeedbackStudentResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_re_feedback)

        formContainer =
            findViewById(R.id.reFeedbackFormContainer)

        progressBar =
            findViewById(R.id.reFeedbackProgress)

        statusText =
            findViewById(R.id.reFeedbackStatus)

        val responseId =
            intent.getStringExtra("responseId")
                ?: intent.data?.lastPathSegment

        if (responseId.isNullOrEmpty()) {
            statusText.text = "Response ID missing"
            return
        }

        loadReFeedback(responseId)
    }

    private fun loadReFeedback(responseId: String) {

        val token = SessionManager(this).getToken()

        if (token.isNullOrEmpty()) {
            statusText.text = "Please login again"
            return
        }

        progressBar.visibility = View.VISIBLE

        ApiClient.apiService
            .getReFeedback(
                "Bearer $token",
                responseId
            )
            .enqueue(
                object : Callback<ReFeedbackResponse> {

                    override fun onResponse(
                        call: Call<ReFeedbackResponse>,
                        response: Response<ReFeedbackResponse>
                    ) {
                        progressBar.visibility = View.GONE

                        if (
                            !response.isSuccessful ||
                            response.body() == null
                        ) {
                            statusText.text =
                                "Failed to load re-feedback (${response.code()})"
                            return
                        }

                        currentForm = response.body()!!.form
                        oldResponse = response.body()!!.response

                        showForm(responseId)
                    }

                    override fun onFailure(
                        call: Call<ReFeedbackResponse>,
                        t: Throwable
                    ) {
                        progressBar.visibility = View.GONE
                        statusText.text =
                            "Connection error: ${t.message}"
                    }
                }
            )
    }

    private fun showForm(responseId: String) {

        formContainer.removeAllViews()
        answerViews.clear()

        addText(currentForm.title, 24f)

        addText(
            "Student: ${oldResponse.studentName}",
            16f
        )

        addText(
            "Enrollment: ${oldResponse.enrollmentNumber}",
            16f
        )

        addText(
            "Batch: ${oldResponse.batch}",
            16f
        )

        currentForm.questions.forEach { question ->

            addText(
                question.questionText +
                        if (question.required) " *" else "",
                18f
            )

            val oldAnswer =
                oldResponse.answers.find {
                    it.questionId == question._id
                }?.answer

            val view =
                createQuestionView(
                    question,
                    oldAnswer
                )

            formContainer.addView(view)

            answerViews[question._id] = view
        }

        val reasonInput = EditText(this)

        reasonInput.hint =
            "Reason for rating below 8"

        reasonInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE

        reasonInput.minLines = 2

        reasonInput.setText(
            oldResponse.lowRatingReason ?: ""
        )

        formContainer.addView(reasonInput)

        val submitButton = Button(this)

        submitButton.text = "Submit Re-feedback"

        submitButton.setOnClickListener {

            val answers =
                collectAnswers() ?: return@setOnClickListener

            val reason =
                reasonInput.text
                    .toString()
                    .trim()
                    .ifEmpty { null }

            val hasLowRating =
                currentForm.questions.any { question ->

                    if (question.type != "star_rating") {
                        false
                    } else {

                        val rating =
                            answers.find {
                                it.questionId == question._id
                            }?.answer

                        val number =
                            (rating as? Number)?.toInt() ?: 0

                        number in 1..7
                    }
                }

            if (
                hasLowRating &&
                reason.isNullOrBlank()
            ) {
                statusText.text =
                    "Reason is required for rating below 8"

                return@setOnClickListener
            }

            submitReFeedback(
                responseId,
                answers,
                reason
            )
        }

        formContainer.addView(submitButton)
    }

    private fun createQuestionView(
        question: PublicQuestion,
        oldAnswer: Any?
    ): View {

        return when (question.type) {

            "short",
            "paragraph" -> {

                EditText(this).apply {

                    hint = "Your answer"

                    setText(oldAnswer?.toString() ?: "")

                    if (question.type == "paragraph") {
                        minLines = 3

                        inputType =
                            InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    }
                }
            }

            "mcq",
            "yes_no" -> {

                val group = RadioGroup(this)

                val options =
                    if (question.type == "yes_no")
                        listOf("Yes", "No")
                    else
                        question.options

                options.forEach { option ->

                    val button = RadioButton(this)

                    button.text = option

                    if (oldAnswer?.toString() == option) {
                        button.isChecked = true
                    }

                    // Assignment rule:
                    // old Yes cannot become No
                    if (
                        question.type == "yes_no" &&
                        oldAnswer?.toString() == "Yes" &&
                        option == "No"
                    ) {
                        button.isEnabled = false
                    }

                    group.addView(button)
                }

                group
            }

            "checkbox" -> {

                val container = LinearLayout(this)

                container.orientation =
                    LinearLayout.VERTICAL

                val oldValues =
                    (oldAnswer as? List<*>)
                        ?.map { it.toString() }
                        ?: emptyList()

                question.options.forEach { option ->

                    val checkBox = CheckBox(this)

                    checkBox.text = option

                    checkBox.isChecked =
                        oldValues.contains(option)

                    container.addView(checkBox)
                }

                container
            }

            "dropdown" -> {

                val spinner = Spinner(this)

                val options =
                    mutableListOf("Select option")

                options.addAll(question.options)

                spinner.adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        options
                    ).apply {
                        setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )
                    }

                val oldIndex =
                    question.options.indexOf(
                        oldAnswer?.toString()
                    )

                if (oldIndex >= 0) {
                    spinner.setSelection(oldIndex + 1)
                }

                spinner
            }

            "star_rating" -> {

                RatingBar(
                    this,
                    null,
                    android.R.attr.ratingBarStyleSmall
                ).apply {

                    numStars = question.maxStars
                    stepSize = 1f

                    rating =
                        (oldAnswer as? Number)
                            ?.toFloat() ?: 0f
                }
            }

            else -> EditText(this)
        }
    }

    private fun collectAnswers(): List<StudentAnswer>? {

        val answers =
            mutableListOf<StudentAnswer>()

        for (question in currentForm.questions) {

            val view =
                answerViews[question._id] ?: continue

            val answer: Any =
                when (question.type) {

                    "short",
                    "paragraph" ->
                        (view as EditText)
                            .text.toString().trim()

                    "mcq",
                    "yes_no" -> {

                        val group = view as RadioGroup

                        val id =
                            group.checkedRadioButtonId

                        if (id == -1) {
                            ""
                        } else {
                            group.findViewById<RadioButton>(id)
                                .text.toString()
                        }
                    }

                    "checkbox" -> {

                        val container =
                            view as LinearLayout

                        val selected =
                            mutableListOf<String>()

                        for (
                        i in 0 until container.childCount
                        ) {
                            val checkbox =
                                container.getChildAt(i)
                                        as CheckBox

                            if (checkbox.isChecked) {
                                selected.add(
                                    checkbox.text.toString()
                                )
                            }
                        }

                        selected
                    }

                    "dropdown" -> {

                        val spinner =
                            view as Spinner

                        if (
                            spinner.selectedItemPosition == 0
                        ) {
                            ""
                        } else {
                            spinner.selectedItem.toString()
                        }
                    }

                    "star_rating" ->
                        (view as RatingBar)
                            .rating.toInt()

                    else -> ""
                }

            val empty =
                when (answer) {
                    is String -> answer.isBlank()
                    is List<*> -> answer.isEmpty()
                    is Int -> answer == 0
                    else -> false
                }

            if (question.required && empty) {
                statusText.text =
                    "Please answer: ${question.questionText}"

                return null
            }

            answers.add(
                StudentAnswer(
                    questionId = question._id,
                    answer = answer
                )
            )
        }

        return answers
    }

    private fun submitReFeedback(
        responseId: String,
        answers: List<StudentAnswer>,
        reason: String?
    ) {

        val token = SessionManager(this).getToken()

        if (token.isNullOrEmpty()) {
            statusText.text = "Please login again"
            return
        }

        progressBar.visibility = View.VISIBLE

        val request =
            ReFeedbackRequest(
                answers = answers,
                lowRatingReason = reason
            )

        ApiClient.apiService
            .submitReFeedback(
                "Bearer $token",
                responseId,
                request
            )
            .enqueue(
                object :
                    Callback<ReFeedbackSubmitResponse> {

                    override fun onResponse(
                        call: Call<ReFeedbackSubmitResponse>,
                        response:
                        Response<ReFeedbackSubmitResponse>
                    ) {
                        progressBar.visibility =
                            View.GONE

                        if (response.isSuccessful) {
                            statusText.text =
                                "Re-feedback submitted successfully"
                        } else {
                            statusText.text =
                                "Re-feedback failed (${response.code()})"
                        }
                    }

                    override fun onFailure(
                        call: Call<ReFeedbackSubmitResponse>,
                        t: Throwable
                    ) {
                        progressBar.visibility =
                            View.GONE

                        statusText.text =
                            "Connection error: ${t.message}"
                    }
                }
            )
    }

    private fun addText(
        text: String,
        size: Float
    ) {

        val textView = TextView(this)

        textView.text = text
        textView.textSize = size
        textView.setPadding(0, 12, 0, 8)

        formContainer.addView(textView)
    }
}