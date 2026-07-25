package com.example.feedbackmanagement

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyFormsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_forms)

        val progressBar =
            findViewById<ProgressBar>(R.id.formsProgressBar)

        val statusText =
            findViewById<TextView>(R.id.formsStatusText)

        val formsContainer =
            findViewById<LinearLayout>(R.id.formsContainer)

        val sessionManager = SessionManager(this)

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

                    if (response.isSuccessful && response.body() != null) {

                        val forms = response.body()!!.forms

                        if (forms.isEmpty()) {
                            statusText.text = "No forms found"
                            return
                        }

                        statusText.text = ""

                        for (form in forms) {

                            val formText = TextView(this@MyFormsActivity)

                            formText.text = """
                                ${form.title}
                                ${form.description ?: ""}
                                Status: ${form.approvalStatus}
                                Active: ${if (form.isActive) "Yes" else "No"}
                            """.trimIndent()

                            formText.textSize = 18f
                            formText.setPadding(16, 20, 16, 20)

                            formsContainer.addView(formText)
                        }

                    } else {
                        statusText.text =
                            "Failed to load forms (${response.code()})"
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
}