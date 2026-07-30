package com.example.feedbackmanagement

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminFormsActivity : AppCompatActivity() {

    private lateinit var loadMoreButton: Button

    private var currentPage = 1
    private var hasMore = false
    private val pageSize = 10

    private lateinit var teacherFilterSpinner: Spinner

    private var selectedTeacherId: String? = null
    private var teachers: List<TeacherData> = emptyList()

    private lateinit var formsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_forms)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        formsContainer = findViewById(R.id.formsContainer)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        sessionManager = SessionManager(this)

        loadMoreButton = findViewById(R.id.loadMoreButton)

        loadMoreButton.setOnClickListener {
            if (hasMore) {
                currentPage++
                loadForms(append = true)
            }
        }

        teacherFilterSpinner =
            findViewById(R.id.teacherFilterSpinner)

        loadTeacherFilter()

        loadForms()
    }

    private fun loadTeacherFilter() {

        val token = sessionManager.getToken() ?: return

        ApiClient.apiService
            .getTeachers("Bearer $token")
            .enqueue(object : Callback<TeachersResponse> {

                override fun onResponse(
                    call: Call<TeachersResponse>,
                    response: Response<TeachersResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) {
                        return
                    }

                    teachers = response.body()!!.teachers

                    val names = mutableListOf("All Teachers")

                    names.addAll(
                        teachers.map { it.fullName }
                    )

                    val adapter = ArrayAdapter(
                        this@AdminFormsActivity,
                        android.R.layout.simple_spinner_item,
                        names
                    )

                    adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                    )

                    teacherFilterSpinner.adapter = adapter

                    teacherFilterSpinner.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {

                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                selectedTeacherId =
                                    if (position == 0) {
                                        null
                                    } else {
                                        teachers[position - 1]._id
                                    }

                                currentPage = 1
                                loadForms()
                            }

                            override fun onNothingSelected(
                                parent: AdapterView<*>?
                            ) {
                            }
                        }
                }

                override fun onFailure(
                    call: Call<TeachersResponse>,
                    t: Throwable
                ) {

                }
            })
    }

    private fun loadForms(append: Boolean = false) {

        val token = sessionManager.getToken()

        if (token.isNullOrEmpty()) {
            statusText.text = "Please login again"
            return
        }

        if (!append) {
            currentPage = 1
            formsContainer.removeAllViews()
        }

        progressBar.visibility = View.VISIBLE
        loadMoreButton.isEnabled = false

        ApiClient.apiService
            .getAllForms(
                token = "Bearer $token",
                page = currentPage,
                limit = pageSize,
                teacherId = selectedTeacherId
            )
            .enqueue(object : Callback<FormsResponse> {

                override fun onResponse(
                    call: Call<FormsResponse>,
                    response: Response<FormsResponse>
                ) {
                    progressBar.visibility = View.GONE
                    loadMoreButton.isEnabled = true

                    if (!response.isSuccessful || response.body() == null) {
                        statusText.text =
                            "Failed to load forms (${response.code()})"
                        return
                    }

                    val body = response.body()!!
                    val forms = body.forms

                    if (!append) {
                        formsContainer.removeAllViews()
                    }

                    if (forms.isEmpty() && currentPage == 1) {
                        statusText.text = ""
                        formsContainer.addView(
                            UiKit.emptyState(
                                this@AdminFormsActivity,
                                R.drawable.ic_description,
                                "No forms found",
                                "Try a different teacher filter"
                            )
                        )
                    } else {
                        statusText.text = ""

                        forms.forEach { form ->
                            addFormView(form, token)
                        }
                    }

                    hasMore = body.pagination?.hasMore ?: false

                    loadMoreButton.visibility =
                        if (hasMore) View.VISIBLE
                        else View.GONE
                }

                override fun onFailure(
                    call: Call<FormsResponse>,
                    t: Throwable
                ) {
                    progressBar.visibility = View.GONE
                    loadMoreButton.isEnabled = true

                    statusText.text =
                        "Connection error: ${t.message}"
                }
            })
    }

    private fun addFormView(form: FormData, token: String) {

        val (card, inner) = UiKit.card(this)

        inner.addView(UiKit.cardTitle(this, form.title))

        if (!form.description.isNullOrBlank()) {
            inner.addView(UiKit.bodyText(this, form.description))
        }

        val chipRow = LinearLayout(this)
        chipRow.orientation = LinearLayout.HORIZONTAL
        chipRow.gravity = Gravity.CENTER_VERTICAL
        val chipRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        chipRowParams.topMargin = UiKit.dp(this, 12)
        chipRow.layoutParams = chipRowParams
        chipRow.addView(UiKit.approvalChip(this, form.approvalStatus))
        chipRow.addView(UiKit.activeChip(this, form.isActive))
        inner.addView(chipRow)

        val actions = UiKit.wrapRow(this)

        actions.addView(
            UiKit.actionChip(this, "Responses", R.drawable.ic_bar_chart, primary = true) {
                val intent = android.content.Intent(this, ResponsesActivity::class.java)
                intent.putExtra("FORM_ID", form._id)
                startActivity(intent)
            }
        )

        actions.addView(
            UiKit.actionChip(this, "Edit", R.drawable.ic_edit) {
                val intent = android.content.Intent(this, CreateFormActivity::class.java)
                intent.putExtra("FORM_ID", form._id)
                startActivity(intent)
            }
        )

        if (form.approvalStatus == "pending") {
            actions.addView(
                UiKit.actionChip(this, "Approve", R.drawable.ic_check_circle) {
                    approveForm(form._id, token)
                }
            )
            actions.addView(
                UiKit.actionChip(this, "Reject", R.drawable.ic_cancel, destructive = true) {
                    showRejectDialog(form._id, token)
                }
            )
        }

        if (form.approvalStatus == "approved") {
            if (form.isActive) {
                actions.addView(
                    UiKit.actionChip(this, "Deactivate", R.drawable.ic_cancel) {
                        deactivateForm(form._id, token)
                    }
                )
            } else {
                actions.addView(
                    UiKit.actionChip(this, "Activate", R.drawable.ic_check_circle) {
                        activateForm(form._id, token)
                    }
                )
            }
        }

        if (form.isActive) {
            actions.addView(
                UiKit.actionChip(this, "Share", R.drawable.ic_share) {
                    val link = "feedbackapp://form/${form._id}"

                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        type = "text/plain"

                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "Please submit your feedback:\n$link"
                        )
                    }

                    startActivity(
                        android.content.Intent.createChooser(
                            shareIntent,
                            "Share Feedback Form"
                        )
                    )
                }
            )
        }

        actions.addView(
            UiKit.actionChip(this, "QR Code", R.drawable.ic_qr_code) {
                val link = "feedbackapp://form/${form._id}"
                QrHelper.show(this, link, form.title, "Scan to open feedback form")
            }
        )

        actions.addView(
            UiKit.actionChip(this, "Delete", R.drawable.ic_delete, destructive = true) {
                AlertDialog.Builder(this)
                    .setTitle("Delete Form")
                    .setMessage(
                        "Delete \"${form.title}\"? All responses will also be permanently deleted."
                    )
                    .setPositiveButton("Delete") { _, _ ->

                        ApiClient.apiService
                            .deleteForm("Bearer $token", form._id)
                            .enqueue(object : Callback<MessageResponse> {

                                override fun onResponse(
                                    call: Call<MessageResponse>,
                                    response: Response<MessageResponse>
                                ) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(
                                            this@AdminFormsActivity,
                                            "Form deleted successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        loadForms()
                                    } else {
                                        Toast.makeText(
                                            this@AdminFormsActivity,
                                            "Delete failed (${response.code()})",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                                override fun onFailure(
                                    call: Call<MessageResponse>,
                                    t: Throwable
                                ) {
                                    Toast.makeText(
                                        this@AdminFormsActivity,
                                        "Connection error: ${t.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        inner.addView(actions)
        formsContainer.addView(card)
    }

    private fun approveForm(formId: String, token: String) {

        ApiClient.apiService
            .approveForm("Bearer $token", formId)
            .enqueue(simpleCallback("Form approved"))
    }

    private fun activateForm(formId: String, token: String) {

        ApiClient.apiService
            .activateForm("Bearer $token", formId)
            .enqueue(simpleCallback("Form activated"))
    }

    private fun deactivateForm(
        formId: String,
        token: String
    ) {

        ApiClient.apiService
            .deactivateForm(
                "Bearer $token",
                formId
            )
            .enqueue(
                simpleCallback("Form deactivated")
            )
    }

    private fun showRejectDialog(
        formId: String,
        token: String
    ) {

        val input = EditText(this)
        input.hint = "Rejection reason"
        val pad = UiKit.dp(this, 20)
        input.setPadding(pad, UiKit.dp(this, 8), pad, 0)

        AlertDialog.Builder(this)
            .setTitle("Reject Form")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->

                val reason = input.text.toString().trim()

                if (reason.isEmpty()) {
                    statusText.text =
                        "Rejection reason is required"
                    return@setPositiveButton
                }

                ApiClient.apiService
                    .rejectForm(
                        "Bearer $token",
                        formId,
                        RejectFormRequest(reason)
                    )
                    .enqueue(simpleCallback("Form rejected"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun simpleCallback(
        successMessage: String
    ): Callback<CreateFormResponse> {

        return object : Callback<CreateFormResponse> {

            override fun onResponse(
                call: Call<CreateFormResponse>,
                response: Response<CreateFormResponse>
            ) {
                if (response.isSuccessful) {
                    statusText.text = successMessage
                    loadForms()
                } else {
                    statusText.text =
                        "Operation failed (${response.code()})"
                }
            }

            override fun onFailure(
                call: Call<CreateFormResponse>,
                t: Throwable
            ) {
                statusText.text =
                    "Connection error: ${t.message}"
            }
        }
    }
}
