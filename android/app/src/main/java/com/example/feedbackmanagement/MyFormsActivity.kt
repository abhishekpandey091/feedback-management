package com.example.feedbackmanagement

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyFormsActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var formsContainer: LinearLayout
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_forms)

        progressBar = findViewById(R.id.formsProgressBar)
        statusText = findViewById(R.id.formsStatusText)
        formsContainer = findViewById(R.id.formsContainer)

        sessionManager = SessionManager(this)

        loadForms()
    }

    override fun onResume() {
        super.onResume()

        if (::sessionManager.isInitialized) {
            loadForms()
        }
    }

    private fun loadForms() {

        val token = sessionManager.getToken()

        if (token.isNullOrEmpty()) {
            statusText.text = "Please login again"
            return
        }

        progressBar.visibility = View.VISIBLE

        ApiClient.apiService
            .getMyForms("Bearer $token")
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

    private fun addFormView(
        form: FormData,
        token: String
    ) {

        // FORM INFORMATION
        val formText = TextView(this)

        formText.text = """
            ${form.title}
            ${form.description ?: ""}
            Status: ${form.approvalStatus}
            Active: ${if (form.isActive) "Yes" else "No"}
        """.trimIndent()

        formText.textSize = 18f
        formText.setPadding(16, 24, 16, 12)

        formsContainer.addView(formText)


        // =========================
        // EDIT
        // =========================

        val editButton = Button(this)
        editButton.text = "Edit Form"

        editButton.setOnClickListener {

            val intent = Intent(
                this,
                CreateFormActivity::class.java
            )

            intent.putExtra("FORM_ID", form._id)

            startActivity(intent)
        }

        formsContainer.addView(editButton)


        // =========================
        // ACTIVATE / DEACTIVATE
        // Only approved forms
        // =========================

        if (form.approvalStatus == "approved") {

            val activeButton = Button(this)

            if (form.isActive) {

                activeButton.text = "Deactivate"

                activeButton.setOnClickListener {
                    deactivateForm(
                        form._id,
                        token
                    )
                }

            } else {

                activeButton.text = "Activate"

                activeButton.setOnClickListener {
                    activateForm(
                        form._id,
                        token
                    )
                }
            }

            formsContainer.addView(activeButton)
        }


        // =========================
        // SHARE
        // Only active forms
        // =========================

        if (form.isActive) {

            val shareButton = Button(this)
            shareButton.text = "Share Form"

            shareButton.setOnClickListener {

                val link =
                    "feedbackapp://form/${form._id}"

                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {

                        type = "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Please submit your feedback:\n$link"
                        )
                    }

                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "Share Feedback Form"
                    )
                )
            }

            formsContainer.addView(shareButton)


            // =========================
            // QR CODE
            // =========================

            val qrButton = Button(this)
            qrButton.text = "Show QR Code"

            qrButton.setOnClickListener {
                showQrCode(
                    form._id,
                    form.title
                )
            }

            formsContainer.addView(qrButton)
        }


        // =========================
        // RESPONSES
        // =========================

        val responsesButton = Button(this)
        responsesButton.text = "Responses"

        responsesButton.setOnClickListener {

            val intent = Intent(
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


        // =========================
        // DELETE
        // =========================

        val deleteButton = Button(this)
        deleteButton.text = "Delete Form"

        deleteButton.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Delete Form")
                .setMessage(
                    "Delete \"${form.title}\"? " +
                            "All responses for this form " +
                            "will also be permanently deleted."
                )
                .setPositiveButton("Delete") { _, _ ->

                    ApiClient.apiService
                        .deleteForm(
                            "Bearer $token",
                            form._id
                        )
                        .enqueue(
                            object : Callback<MessageResponse> {

                                override fun onResponse(
                                    call: Call<MessageResponse>,
                                    response: Response<MessageResponse>
                                ) {

                                    if (response.isSuccessful) {

                                        Toast.makeText(
                                            this@MyFormsActivity,
                                            "Form deleted successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        loadForms()

                                    } else {

                                        Toast.makeText(
                                            this@MyFormsActivity,
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
                                        this@MyFormsActivity,
                                        "Connection error: ${t.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        formsContainer.addView(deleteButton)
    }


    // =========================
    // ACTIVATE
    // =========================

    private fun activateForm(
        formId: String,
        token: String
    ) {

        progressBar.visibility = View.VISIBLE

        ApiClient.apiService
            .activateForm(
                "Bearer $token",
                formId
            )
            .enqueue(
                formActionCallback(
                    "Form activated successfully"
                )
            )
    }


    // =========================
    // DEACTIVATE
    // =========================

    private fun deactivateForm(
        formId: String,
        token: String
    ) {

        progressBar.visibility = View.VISIBLE

        ApiClient.apiService
            .deactivateForm(
                "Bearer $token",
                formId
            )
            .enqueue(
                formActionCallback(
                    "Form deactivated successfully"
                )
            )
    }


    // =========================
    // COMMON CALLBACK
    // =========================

    private fun formActionCallback(
        successMessage: String
    ): Callback<CreateFormResponse> {

        return object : Callback<CreateFormResponse> {

            override fun onResponse(
                call: Call<CreateFormResponse>,
                response: Response<CreateFormResponse>
            ) {

                progressBar.visibility = View.GONE

                if (response.isSuccessful) {

                    Toast.makeText(
                        this@MyFormsActivity,
                        successMessage,
                        Toast.LENGTH_SHORT
                    ).show()

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

                progressBar.visibility = View.GONE

                statusText.text =
                    "Connection error: ${t.message}"
            }
        }
    }


    // =========================
    // QR CODE
    // =========================

    private fun showQrCode(
        formId: String,
        title: String
    ) {

        try {

            val link =
                "feedbackapp://form/$formId"

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
                            Color.BLACK
                        else
                            Color.WHITE
                    )
                }
            }

            val imageView = ImageView(this)

            imageView.setImageBitmap(bitmap)

            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                    "Scan to open feedback form"
                )
                .setView(imageView)
                .setPositiveButton(
                    "Close",
                    null
                )
                .show()

        } catch (e: Exception) {

            statusText.text =
                "Could not generate QR: ${e.message}"
        }
    }
}