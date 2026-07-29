package com.example.feedbackmanagement

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentFeedbackActivity : AppCompatActivity() {

    private lateinit var formContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var formIdInput: EditText

    // Stores the input View for each question
    private val answerViews = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_feedback)

        formContainer = findViewById(R.id.formContainer)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        formIdInput = findViewById(R.id.formIdInput)

        val loadButton =
            findViewById<Button>(R.id.loadFormButton)

        val deepLink = intent?.data

        if (
            deepLink != null &&
            deepLink.scheme == "feedbackapp" &&
            deepLink.host == "form"
        ) {
            val formId = deepLink.lastPathSegment

            if (!formId.isNullOrEmpty()) {

                // Hide manual testing controls
                formIdInput.visibility = View.GONE
                loadButton.visibility = View.GONE

                loadForm(formId)
            }
        }

        loadButton.setOnClickListener {

            val formId = formIdInput.text.toString().trim()

            if (formId.isEmpty()) {
                statusText.text = "Enter Form ID"
                return@setOnClickListener
            }

            loadForm(formId)
        }
    }

    private fun loadForm(formId: String) {

        progressBar.visibility = View.VISIBLE
        statusText.text = ""

        ApiClient.apiService
            .getPublicForm(formId)
            .enqueue(object : Callback<PublicFormResponse> {

                override fun onResponse(
                    call: Call<PublicFormResponse>,
                    response: Response<PublicFormResponse>
                ) {
                    progressBar.visibility = View.GONE

                    if (
                        !response.isSuccessful ||
                        response.body() == null
                    ) {
                        statusText.text =
                            "Failed to load form (${response.code()})"
                        return
                    }

                    showForm(
                        response.body()!!.form,
                        formId
                    )
                }

                override fun onFailure(
                    call: Call<PublicFormResponse>,
                    t: Throwable
                ) {
                    progressBar.visibility = View.GONE

                    statusText.text =
                        "Connection error: ${t.message}"
                }
            })
    }

    private fun showForm(
        form: PublicForm,
        formId: String
    ) {

        formContainer.removeAllViews()
        answerViews.clear()

        // FORM TITLE
        val title = TextView(this)
        title.text = form.title
        title.textSize = 24f

        formContainer.addView(title)

        // DESCRIPTION
        val description = TextView(this)

        description.text = form.description ?: ""
        description.textSize = 16f
        description.setPadding(0, 8, 0, 24)

        formContainer.addView(description)

        // STUDENT DETAILS

        val nameInput = EditText(this)
        nameInput.hint = "Student Name"

        formContainer.addView(nameInput)

        val enrollmentInput = EditText(this)
        enrollmentInput.hint = "Enrollment Number"

        formContainer.addView(enrollmentInput)

        val batchInput = EditText(this)
        batchInput.hint = "Batch"

        formContainer.addView(batchInput)

        // ATTENDANCE STATUS

        val attendanceTitle = TextView(this)
        attendanceTitle.text = "Attendance Status *"
        attendanceTitle.textSize = 18f
        attendanceTitle.setPadding(0, 28, 0, 8)

        formContainer.addView(attendanceTitle)

        val attendanceGroup = RadioGroup(this)

        val presentButton = RadioButton(this)
        presentButton.text = "Present"

        val absentButton = RadioButton(this)
        absentButton.text = "Absent"

        attendanceGroup.addView(presentButton)
        attendanceGroup.addView(absentButton)

        formContainer.addView(attendanceGroup)

        // QUESTIONS

        form.questions.forEach { question ->

            val questionText = TextView(this)

            questionText.text =
                question.questionText +
                        if (question.required) " *" else ""

            questionText.textSize = 18f
            questionText.setPadding(
                0,
                28,
                0,
                8
            )

            formContainer.addView(questionText)

            when (question.type) {

                // SHORT ANSWER
                "short" -> {

                    val input = EditText(this)

                    input.hint = "Your answer"

                    formContainer.addView(input)

                    answerViews[question._id] = input
                }

                // PARAGRAPH
                "paragraph" -> {

                    val input = EditText(this)

                    input.hint = "Your answer"

                    input.inputType =
                        InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_FLAG_MULTI_LINE

                    input.minLines = 3
                    input.maxLines = 6

                    formContainer.addView(input)

                    answerViews[question._id] = input
                }

                // MCQ
                "mcq" -> {

                    val radioGroup = RadioGroup(this)

                    question.options.forEach { option ->

                        val radioButton =
                            RadioButton(this)

                        radioButton.text = option

                        radioGroup.addView(radioButton)
                    }

                    formContainer.addView(radioGroup)

                    answerViews[question._id] =
                        radioGroup
                }

                // CHECKBOX
                "checkbox" -> {

                    val checkboxContainer =
                        LinearLayout(this)

                    checkboxContainer.orientation =
                        LinearLayout.VERTICAL

                    question.options.forEach { option ->

                        val checkBox =
                            CheckBox(this)

                        checkBox.text = option

                        checkboxContainer.addView(checkBox)
                    }

                    formContainer.addView(
                        checkboxContainer
                    )

                    answerViews[question._id] =
                        checkboxContainer
                }

                // DROPDOWN
                "dropdown" -> {

                    val spinner = Spinner(this)

                    val options =
                        mutableListOf("Select option")

                    options.addAll(question.options)

                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        options
                    )

                    adapter.setDropDownViewResource(
                        android.R.layout
                            .simple_spinner_dropdown_item
                    )

                    spinner.adapter = adapter

                    formContainer.addView(spinner)

                    answerViews[question._id] =
                        spinner
                }

                // STAR RATING
                "star_rating" -> {

                    val ratingBar = RatingBar(
                        this,
                        null,
                        android.R.attr.ratingBarStyleSmall
                    )

                    ratingBar.numStars = question.maxStars
                    ratingBar.stepSize = 1f
                    ratingBar.rating = 0f
                    ratingBar.setIsIndicator(false)

                    ratingBar.layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                    formContainer.addView(ratingBar)

                    answerViews[question._id] = ratingBar
                }

                // YES / NO
                "yes_no" -> {

                    val radioGroup =
                        RadioGroup(this)

                    val yes =
                        RadioButton(this)

                    yes.text = "Yes"

                    val no =
                        RadioButton(this)

                    no.text = "No"

                    radioGroup.addView(yes)
                    radioGroup.addView(no)

                    formContainer.addView(
                        radioGroup
                    )

                    answerViews[question._id] =
                        radioGroup
                }
            }
        }

        // LOW RATING REASON

        val lowRatingReasonInput = EditText(this)

        lowRatingReasonInput.hint =
            "Reason for rating below 8 (if applicable)"

        lowRatingReasonInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE

        lowRatingReasonInput.minLines = 2
        lowRatingReasonInput.maxLines = 4

        formContainer.addView(lowRatingReasonInput)



        // SUBMIT BUTTON

        val submitButton = Button(this)

        submitButton.text = "Submit Feedback"

        submitButton.setOnClickListener {

            val name =
                nameInput.text.toString().trim()

            val enrollment =
                enrollmentInput.text.toString().trim()

            val batch =
                batchInput.text.toString().trim()

            if (
                name.isEmpty() ||
                enrollment.isEmpty() ||
                batch.isEmpty()
            ) {
                statusText.text =
                    "Student details are required"

                return@setOnClickListener
            }

            val answers =
                mutableListOf<StudentAnswer>()

            val attendanceStatus =
                when (attendanceGroup.checkedRadioButtonId) {

                    presentButton.id -> "Present"

                    absentButton.id -> "Absent"

                    else -> ""
                }

            if (attendanceStatus.isEmpty()) {

                statusText.text =
                    "Please select Present or Absent"

                return@setOnClickListener
            }

            if (attendanceStatus == "Present") {

                for (question in form.questions) {

                    val view =
                        answerViews[question._id]

                    val answer: Any =
                        when (question.type) {

                            "short",
                            "paragraph" -> {

                                (view as EditText)
                                    .text
                                    .toString()
                                    .trim()
                            }

                            "mcq",
                            "yes_no" -> {

                                val group =
                                    view as RadioGroup

                                val selectedId =
                                    group.checkedRadioButtonId

                                if (selectedId == -1) {
                                    ""
                                } else {

                                    findViewById<RadioButton>(
                                        selectedId
                                    ).text.toString()
                                }
                            }

                            "checkbox" -> {

                                val container =
                                    view as LinearLayout

                                val selected =
                                    mutableListOf<String>()

                                for (
                                i in 0 until
                                        container.childCount
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
                                    spinner.selectedItemPosition
                                    == 0
                                ) {
                                    ""
                                } else {
                                    spinner.selectedItem
                                        .toString()
                                }
                            }

                            "star_rating" -> {

                                val ratingBar =
                                    view as RatingBar

                                ratingBar.rating.toInt()
                            }

                            else -> ""
                        }

                    // REQUIRED VALIDATION
                    val empty =
                        when (answer) {

                            is String ->
                                answer.isBlank()

                            is List<*> ->
                                answer.isEmpty()

                            is Int ->
                                answer == 0

                            else -> false
                        }

                    if (question.required && empty) {

                        statusText.text =
                            "Please answer: ${question.questionText}"

                        return@setOnClickListener
                    }

                    answers.add(
                        StudentAnswer(
                            questionId = question._id,
                            answer = answer
                        )
                    )
                }
            }

            val lowRatingReason =
                lowRatingReasonInput.text
                    .toString()
                    .trim()
                    .ifEmpty { null }

            if (attendanceStatus == "Present") {

                val hasLowRating =
                    form.questions.any { question ->

                        if (question.type != "star_rating") {
                            false
                        } else {
                            val ratingBar =
                                answerViews[question._id] as? RatingBar

                            (ratingBar?.rating ?: 0f) in 1f..7f
                        }
                    }

                if (
                    hasLowRating &&
                    lowRatingReason.isNullOrBlank()
                ) {
                    statusText.text =
                        "Please enter a reason for rating below 8"

                    return@setOnClickListener
                }
            }

            submitFeedback(
                formId,
                name,
                enrollment,
                batch,
                attendanceStatus,
                lowRatingReason,
                answers
            )
        }

        formContainer.addView(submitButton)
    }




    private fun submitFeedback(
        formId: String,
        name: String,
        enrollment: String,
        batch: String,
        attendanceStatus: String,
        lowRatingReason: String?,
        answers: List<StudentAnswer>
    ){

        progressBar.visibility = View.VISIBLE
        statusText.text = ""

        val request =
            SubmitFeedbackRequest(
                studentName = name,
                batch = batch,
                enrollmentNumber = enrollment,
                attendanceStatus = attendanceStatus,
                lowRatingReason = lowRatingReason,
                answers = answers
            )





        ApiClient.apiService
            .submitFeedback(
                formId,
                request
            )
            .enqueue(
                object :
                    Callback<SubmitFeedbackResponse> {

                    override fun onResponse(
                        call: Call<SubmitFeedbackResponse>,
                        response:
                        Response<SubmitFeedbackResponse>
                    ) {

                        progressBar.visibility =
                            View.GONE

                        if (response.isSuccessful) {

                            statusText.text =
                                "Feedback submitted successfully"

                            formContainer
                                .removeAllViews()

                        } else {

                            statusText.text =
                                "Submission failed (${response.code()})"
                        }
                    }

                    override fun onFailure(
                        call:
                        Call<SubmitFeedbackResponse>,
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
}