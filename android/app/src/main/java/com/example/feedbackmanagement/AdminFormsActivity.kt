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
class AdminFormsActivity : AppCompatActivity() {

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

        loadForms()
    }

    private fun loadForms() {

        val token = sessionManager.getToken()

        if (token.isNullOrEmpty()) {
            statusText.text = "Please login again"
            return
        }

        progressBar.visibility = View.VISIBLE

        ApiClient.apiService
            .getAllForms("Bearer $token")
            .enqueue(object : Callback<FormsResponse> {

                override fun onResponse(
                    call: Call<FormsResponse>,
                    response: Response<FormsResponse>
                ) {
                    progressBar.visibility = View.GONE

                    if (!response.isSuccessful || response.body() == null) {
                        statusText.text =
                            "Failed to load forms (${response.code()})"
                        return
                    }

                    val forms = response.body()!!.forms

                    formsContainer.removeAllViews()

                    if (forms.isEmpty()) {
                        statusText.text = "No forms found"
                        return
                    }

                    statusText.text = ""

                    forms.forEach { form ->
                        addFormView(form, token)
                    }
                }

                override fun onFailure(
                    call: Call<FormsResponse>,
                    t: Throwable
                ) {
                    progressBar.visibility = View.GONE
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

        // Approved + inactive → Activate
        if (
            form.approvalStatus == "approved" &&
            !form.isActive
        ) {
            val activateButton = Button(this)
            activateButton.text = "Activate"

            activateButton.setOnClickListener {
                activateForm(form._id, token)
            }

            formsContainer.addView(activateButton)
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