package com.example.feedbackmanagement

import android.content.Intent
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

class MyFormsActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var formsContainer: LinearLayout
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_forms)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

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
                        statusText.text = ""
                        formsContainer.addView(
                            UiKit.emptyState(
                                this@MyFormsActivity,
                                R.drawable.ic_description,
                                "No forms yet",
                                "Create your first feedback form to get started"
                            )
                        )
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

        // RESPONSES
        actions.addView(
            UiKit.actionChip(this, "Responses", R.drawable.ic_bar_chart, primary = true) {
                val intent = Intent(this, ResponsesActivity::class.java)
                intent.putExtra("FORM_ID", form._id)
                startActivity(intent)
            }
        )

        // EDIT
        actions.addView(
            UiKit.actionChip(this, "Edit", R.drawable.ic_edit) {
                val intent = Intent(this, CreateFormActivity::class.java)
                intent.putExtra("FORM_ID", form._id)
                startActivity(intent)
            }
        )

        // ACTIVATE / DEACTIVATE (approved forms only)
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

        // SHARE + QR (active forms only)
        if (form.isActive) {
            actions.addView(
                UiKit.actionChip(this, "Share", R.drawable.ic_share) {
                    val link = "feedbackapp://form/${form._id}"

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Please submit your feedback:\n$link"
                        )
                    }

                    startActivity(
                        Intent.createChooser(shareIntent, "Share Feedback Form")
                    )
                }
            )

            actions.addView(
                UiKit.actionChip(this, "QR Code", R.drawable.ic_qr_code) {
                    val link = "feedbackapp://form/${form._id}"
                    QrHelper.show(this, link, form.title, "Scan to open feedback form")
                }
            )
        }

        // DELETE
        actions.addView(
            UiKit.actionChip(this, "Delete", R.drawable.ic_delete, destructive = true) {
                AlertDialog.Builder(this)
                    .setTitle("Delete Form")
                    .setMessage(
                        "Delete \"${form.title}\"? " +
                                "All responses for this form " +
                                "will also be permanently deleted."
                    )
                    .setPositiveButton("Delete") { _, _ ->

                        ApiClient.apiService
                            .deleteForm("Bearer $token", form._id)
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
        )

        inner.addView(actions)
        formsContainer.addView(card)
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
}
