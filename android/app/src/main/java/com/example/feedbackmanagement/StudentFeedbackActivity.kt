package com.example.feedbackmanagement

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
    private var lowRatingReasonLayout: TextInputLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_feedback)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        formContainer = findViewById(R.id.formContainer)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        formIdInput = findViewById(R.id.formIdInput)

        val formIdCard = findViewById<MaterialCardView>(R.id.formIdCard)
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
                formIdCard.visibility = View.GONE

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

    private fun textField(hint: String, multiline: Boolean = false): Pair<TextInputLayout, TextInputEditText> {
        val layout = TextInputLayout(this)
        layout.hint = hint
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = UiKit.dp(this, 12)
        layout.layoutParams = params

        val edit = TextInputEditText(this)
        if (multiline) {
            edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            edit.minLines = 3
            edit.maxLines = 6
        }
        layout.addView(edit)

        return Pair(layout, edit)
    }

    private fun showForm(
        form: PublicForm,
        formId: String
    ) {

        formContainer.removeAllViews()
        answerViews.clear()
        lowRatingReasonLayout = null

        // FORM HEADER
        val (headerCard, headerInner) = UiKit.card(this)
        headerInner.addView(UiKit.screenTitle(this, form.title))
        if (!form.description.isNullOrBlank()) {
            val desc = UiKit.bodyText(this, form.description)
            headerInner.addView(desc)
        }
        formContainer.addView(headerCard)

        // STUDENT INFORMATION CARD
        val (studentCard, studentInner) = UiKit.card(this)
        studentInner.addView(UiKit.cardTitle(this, "Student Information"))

        val (nameLayout, nameInput) = textField("Student Name")
        studentInner.addView(nameLayout)

        val (enrollmentLayout, enrollmentInput) = textField("Enrollment Number")
        studentInner.addView(enrollmentLayout)

        val (batchLayout, batchInput) = textField("Batch")
        studentInner.addView(batchLayout)

        formContainer.addView(studentCard)

        // ATTENDANCE
        val (attendanceCard, attendanceInner) = UiKit.card(this)
        attendanceInner.addView(UiKit.cardTitle(this, "Attendance Status *"))

        val attendanceGroup = RadioGroup(this)
        attendanceGroup.orientation = RadioGroup.HORIZONTAL
        val attendanceParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        attendanceParams.topMargin = UiKit.dp(this, 10)
        attendanceGroup.layoutParams = attendanceParams

        val presentButton = RadioButton(this)
        presentButton.text = "Present"
        presentButton.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )

        val absentButton = RadioButton(this)
        absentButton.text = "Absent"
        absentButton.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )

        attendanceGroup.addView(presentButton)
        attendanceGroup.addView(absentButton)

        attendanceInner.addView(attendanceGroup)
        formContainer.addView(attendanceCard)

        // QUESTIONS
        form.questions.forEach { question ->

            val (qCard, qInner) = UiKit.card(this)

            qInner.addView(
                UiKit.questionLabel(this, question.questionText, question.required)
            )

            when (question.type) {

                // SHORT ANSWER
                "short" -> {
                    val (layout, input) = textField("Your answer")
                    qInner.addView(layout)
                    answerViews[question._id] = input
                }

                // PARAGRAPH
                "paragraph" -> {
                    val (layout, input) = textField("Your answer", multiline = true)
                    qInner.addView(layout)
                    answerViews[question._id] = input
                }

                // MCQ
                "mcq" -> {
                    val radioGroup = RadioGroup(this)
                    radioGroup.orientation = RadioGroup.VERTICAL

                    question.options.forEach { option ->
                        val radioButton = RadioButton(this)
                        radioButton.text = option
                        radioGroup.addView(radioButton)
                    }

                    qInner.addView(radioGroup)
                    answerViews[question._id] = radioGroup
                }

                // CHECKBOX
                "checkbox" -> {
                    val checkboxContainer = LinearLayout(this)
                    checkboxContainer.orientation = LinearLayout.VERTICAL

                    question.options.forEach { option ->
                        val checkBox = CheckBox(this)
                        checkBox.text = option
                        checkboxContainer.addView(checkBox)
                    }

                    qInner.addView(checkboxContainer)
                    answerViews[question._id] = checkboxContainer
                }

                // DROPDOWN
                "dropdown" -> {
                    val spinner = Spinner(this)
                    spinner.setPadding(0, UiKit.dp(this, 8), 0, UiKit.dp(this, 8))

                    val options = mutableListOf("Select option")
                    options.addAll(question.options)

                    val spinnerAdapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        options
                    )

                    spinnerAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                    )

                    spinner.adapter = spinnerAdapter

                    qInner.addView(spinner)
                    answerViews[question._id] = spinner
                }

                // STAR RATING
                "star_rating" -> {
                    val starView = StarRatingView(this)
                    starView.maxStars = question.maxStars
                    starView.rating = 0
                    val starParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    starParams.topMargin = UiKit.dp(this, 6)
                    starView.layoutParams = starParams

                    starView.onRatingChanged = { refreshLowRatingVisibility(form) }

                    qInner.addView(starView)
                    answerViews[question._id] = starView
                }

                // YES / NO
                "yes_no" -> {
                    val radioGroup = RadioGroup(this)
                    radioGroup.orientation = RadioGroup.HORIZONTAL

                    val yes = RadioButton(this)
                    yes.text = "Yes"
                    yes.layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )

                    val no = RadioButton(this)
                    no.text = "No"
                    no.layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )

                    radioGroup.addView(yes)
                    radioGroup.addView(no)

                    qInner.addView(radioGroup)
                    answerViews[question._id] = radioGroup
                }
            }

            formContainer.addView(qCard)
        }

        // LOW RATING REASON (revealed only when needed)
        val (reasonLayout, reasonInput) = textField(
            "Reason for rating below 8",
            multiline = true
        )
        reasonLayout.helperText = "Required when any star rating is below 8"
        reasonLayout.visibility = View.GONE
        val reasonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reasonParams.topMargin = UiKit.dp(this, 4)
        reasonLayout.layoutParams = reasonParams
        lowRatingReasonLayout = reasonLayout
        formContainer.addView(reasonLayout)

        // SUBMIT BUTTON
        val submitButton = UiKit.primaryButton(this, "Submit Feedback")
        val submitParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiKit.dp(this, 52)
        )
        submitParams.topMargin = UiKit.dp(this, 20)
        submitButton.layoutParams = submitParams

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

                                    group.findViewById<RadioButton>(
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

                                val starView =
                                    view as StarRatingView

                                starView.rating
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
                reasonInput.text
                    .toString()
                    .trim()
                    .ifEmpty { null }

            if (attendanceStatus == "Present") {

                val hasLowRating =
                    form.questions.any { question ->

                        if (question.type != "star_rating") {
                            false
                        } else {
                            val starView =
                                answerViews[question._id] as? StarRatingView

                            (starView?.rating ?: 0) in 1..7
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

    private fun refreshLowRatingVisibility(form: PublicForm) {
        val hasLowRating = form.questions.any { question ->
            if (question.type != "star_rating") {
                false
            } else {
                val starView = answerViews[question._id] as? StarRatingView
                (starView?.rating ?: 0) in 1..7
            }
        }

        lowRatingReasonLayout?.visibility =
            if (hasLowRating) View.VISIBLE else View.GONE
    }

    private fun submitFeedback(
        formId: String,
        name: String,
        enrollment: String,
        batch: String,
        attendanceStatus: String,
        lowRatingReason: String?,
        answers: List<StudentAnswer>
    ) {

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
