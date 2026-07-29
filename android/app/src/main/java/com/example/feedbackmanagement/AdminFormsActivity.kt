package com.example.feedbackmanagement

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.graphics.Bitmap
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.widget.*
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
                    // Forms can still load without filter
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
                        statusText.text = "No forms found"
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

        val title = TextView(this)

        title.text = """
            ${form.title}
            ${form.description ?: ""}
            Status: ${form.approvalStatus}
            Active: ${if (form.isActive) "Yes" else "No"}
        """.trimIndent()

        title.textSize = 18f
        title.setPadding(16, 24, 16, 12)

        formsContainer.addView(title)

        val editButton = Button(this)
        editButton.text = "Edit Form"

        editButton.setOnClickListener {

            val intent = android.content.Intent(
                this,
                CreateFormActivity::class.java
            )

            intent.putExtra("FORM_ID", form._id)

            startActivity(intent)
        }

        val deleteButton = Button(this)
        deleteButton.text = "Delete Form"

        deleteButton.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Delete Form")
                .setMessage(
                    "Delete \"${form.title}\"? All responses will also be permanently deleted."
                )
                .setPositiveButton("Delete") { _, _ ->

                    ApiClient.apiService
                        .deleteForm(
                            "Bearer $token",
                            form._id
                        )
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

            val deleteButton = Button(this)
            deleteButton.text = "Delete Form"

            deleteButton.setOnClickListener {

                AlertDialog.Builder(this)
                    .setTitle("Delete Form")
                    .setMessage(
                        "Delete \"${form.title}\"? All responses for this form will also be deleted."
                    )
                    .setPositiveButton("Delete") { _, _ ->

                        ApiClient.apiService
                            .deleteForm(
                                "Bearer $token",
                                form._id
                            )
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

            formsContainer.addView(deleteButton)
        }



        formsContainer.addView(deleteButton)

        formsContainer.addView(editButton)

        // Pending forms → Approve / Reject
        if (form.approvalStatus == "pending") {

            val approveButton = Button(this)
            approveButton.text = "Approve"

            approveButton.setOnClickListener {
                approveForm(form._id, token)
            }

            formsContainer.addView(approveButton)

            val rejectButton = Button(this)
            rejectButton.text = "Reject"

            rejectButton.setOnClickListener {
                showRejectDialog(form._id, token)
            }

            formsContainer.addView(rejectButton)
        }

        if (form.approvalStatus == "approved") {

            val activeButton = Button(this)

            if (form.isActive) {

                activeButton.text = "Deactivate"

                activeButton.setOnClickListener {
                    deactivateForm(form._id, token)
                }

            } else {

                activeButton.text = "Activate"

                activeButton.setOnClickListener {
                    activateForm(form._id, token)
                }
            }

            formsContainer.addView(activeButton)
        }

        // Active form → Share
        if (form.isActive) {

            val shareButton = Button(this)
            shareButton.text = "Share Form"

            shareButton.setOnClickListener {

                val link =
                    "feedbackapp://form/${form._id}"

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

            formsContainer.addView(shareButton)
        }

        val qrButton = Button(this)
        qrButton.text = "Show QR Code"

        qrButton.setOnClickListener {

            val link = "feedbackapp://form/${form._id}"

            try {
                val writer = QRCodeWriter()

                val bitMatrix = writer.encode(
                    link,
                    BarcodeFormat.QR_CODE,
                    700,
                    700
                )

                val bitmap = Bitmap.createBitmap(
                    700,
                    700,
                    Bitmap.Config.RGB_565
                )

                for (x in 0 until 700) {
                    for (y in 0 until 700) {
                        bitmap.setPixel(
                            x,
                            y,
                            if (bitMatrix[x, y])
                                android.graphics.Color.BLACK
                            else
                                android.graphics.Color.WHITE
                        )
                    }
                }

                val imageView = ImageView(this)
                imageView.setImageBitmap(bitmap)

                AlertDialog.Builder(this)
                    .setTitle(form.title)
                    .setMessage("Scan to open feedback form")
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .show()

            } catch (e: Exception) {
                statusText.text =
                    "Could not generate QR: ${e.message}"
            }
        }

        formsContainer.addView(qrButton)


        val responsesButton = Button(this)
        responsesButton.text = "Responses"

        responsesButton.setOnClickListener {

            val intent = android.content.Intent(
                this,
                ResponsesActivity::class.java
            )

            intent.putExtra(
                "FORM_ID",
                form._id
            )

            startActivity(intent)
        }

        formsContainer.addView(responsesButton)
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