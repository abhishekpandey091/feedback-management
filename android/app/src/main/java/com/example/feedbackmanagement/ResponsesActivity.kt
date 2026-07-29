package com.example.feedbackmanagement

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

class ResponsesActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private lateinit var formId: String
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_responses)

        container = findViewById(R.id.responsesContainer)
        progressBar = findViewById(R.id.responsesProgressBar)
        statusText = findViewById(R.id.responsesStatusText)

        val summaryButton =
            findViewById<Button>(R.id.summaryButton)

        val individualButton =
            findViewById<Button>(R.id.individualButton)

        val lowerButton =
            findViewById<Button>(R.id.lowerFeedbackButton)

        formId = intent.getStringExtra("FORM_ID") ?: ""

        val sessionManager = SessionManager(this)
        token = sessionManager.getToken() ?: ""

        if (formId.isEmpty() || token.isEmpty()) {
            statusText.text = "Unable to load responses"
            return
        }

        summaryButton.setOnClickListener {
            loadSummary()
        }

        individualButton.setOnClickListener {
            loadResponses()
        }

        lowerButton.setOnClickListener {
            loadLowerFeedback()
        }

        // Default view
        loadSummary()

        val exportButton =
            findViewById<Button>(R.id.exportButton)

        exportButton.setOnClickListener {
            exportResponses()
        }
    }

    private fun exportResponses() {

        statusText.text = "Preparing export..."
        progressBar.visibility = View.VISIBLE

        ApiClient.apiService
            .exportResponses(
                "Bearer $token",
                formId
            )
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    progressBar.visibility = View.GONE

                    if (!response.isSuccessful || response.body() == null) {
                        statusText.text =
                            "Export failed (${response.code()})"
                        return
                    }

                    try {
                        val exportDir =
                            File(cacheDir, "exports")

                        if (!exportDir.exists()) {
                            exportDir.mkdirs()
                        }

                        val file =
                            File(
                                exportDir,
                                "feedback_responses.csv"
                            )

                        FileOutputStream(file).use { output ->
                            response.body()!!.byteStream().use { input ->
                                input.copyTo(output)
                            }
                        }

                        val uri =
                            FileProvider.getUriForFile(
                                this@ResponsesActivity,
                                "${packageName}.fileprovider",
                                file
                            )

                        val shareIntent =
                            Intent(Intent.ACTION_SEND).apply {

                                type = "text/csv"

                                putExtra(
                                    Intent.EXTRA_STREAM,
                                    uri
                                )

                                addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }

                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share Responses CSV"
                            )
                        )

                        statusText.text =
                            "Export created successfully"

                    } catch (e: Exception) {
                        statusText.text =
                            "Could not create export: ${e.message}"
                    }
                }

                override fun onFailure(
                    call: Call<ResponseBody>,
                    t: Throwable
                ) {
                    progressBar.visibility = View.GONE

                    statusText.text =
                        "Export error: ${t.message}"
                }
            })
    }

    private fun addBarChart(
        labels: List<String>,
        values: List<Float>,
        title: String
    ) {

        if (labels.isEmpty() || values.isEmpty()) return

        val chart = BarChart(this)

        val entries = values.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, title)

        val data = BarData(dataSet)
        data.barWidth = 0.7f

        chart.data = data

        chart.xAxis.valueFormatter =
            IndexAxisValueFormatter(labels)

        chart.xAxis.granularity = 1f
        chart.xAxis.setDrawGridLines(false)

        chart.axisRight.isEnabled = false

        chart.description.isEnabled = false
        chart.legend.isEnabled = true

        chart.minimumHeight = 500

        chart.setFitBars(true)
        chart.invalidate()

        container.addView(chart)
    }

    private fun prepareLoading() {
        container.removeAllViews()
        statusText.text = ""
        progressBar.visibility = View.VISIBLE
    }

    private fun addText(text: String, size: Float = 16f) {
        val view = TextView(this)
        view.text = text
        view.textSize = size
        view.setPadding(8, 12, 8, 12)
        container.addView(view)
    }

    private fun loadSummary() {

        prepareLoading()

        ApiClient.apiService
            .getResponseSummary(
                "Bearer $token",
                formId
            )
            .enqueue(object : Callback<SummaryResponse> {

                override fun onResponse(
                    call: Call<SummaryResponse>,
                    response: Response<SummaryResponse>
                ) {
                    progressBar.visibility = View.GONE

                    val body = response.body()

                    if (!response.isSuccessful || body == null) {
                        statusText.text =
                            "Failed to load summary (${response.code()})"
                        return
                    }

                    addText(body.title, 22f)
                    addText(
                        "Total Responses: ${body.totalResponses}",
                        18f
                    )

                    body.summary.forEach { item ->

                        addText(item.questionText, 18f)

                        addText(
                            "Answers: ${item.totalAnswers}"
                        )

                        if (item.type == "star_rating") {

                            addText(
                                "Average Rating: ${
                                    String.format(
                                        "%.2f",
                                        item.average ?: 0.0
                                    )
                                }"
                            )

                            addText(
                                "Lower Feedback (<8): ${
                                    item.lowerCount ?: 0
                                }"
                            )

                            addBarChart(
                                labels = listOf("Average", "Lower <8"),
                                values = listOf(
                                    (item.average ?: 0.0).toFloat(),
                                    (item.lowerCount ?: 0).toFloat()
                                ),
                                title = "Rating Summary"
                            )

                        }


                        item.counts?.let { counts ->

                            counts.forEach { entry ->
                                addText(
                                    "${entry.key}: ${entry.value}"
                                )
                            }

                            addBarChart(
                                labels = counts.keys.toList(),
                                values = counts.values.map { it.toFloat() },
                                title = item.questionText
                            )
                        }

                        addText("--------------------")
                    }
                }

                override fun onFailure(
                    call: Call<SummaryResponse>,
                    t: Throwable
                ) {
                    progressBar.visibility = View.GONE
                    statusText.text =
                        "Connection error: ${t.message}"
                }
            })
    }

    private fun loadResponses() {

        prepareLoading()

        ApiClient.apiService
            .getFormResponses(
                "Bearer $token",
                formId
            )
            .enqueue(object :
                Callback<FormResponsesResponse> {

                override fun onResponse(
                    call: Call<FormResponsesResponse>,
                    response: Response<FormResponsesResponse>
                ) {
                    progressBar.visibility = View.GONE

                    val body = response.body()

                    if (!response.isSuccessful || body == null) {
                        statusText.text =
                            "Failed to load responses (${response.code()})"
                        return
                    }

                    addText(
                        "Individual Responses (${body.totalResponses})",
                        22f
                    )

                    if (body.responses.isEmpty()) {
                        addText("No responses yet")
                        return
                    }

                    body.responses.forEach { item ->

                        addText(item.studentName, 18f)

                        addText(
                            "Enrollment: ${item.enrollmentNumber}"
                        )

                        addText("Batch: ${item.batch}")

                        item.answers.forEach { answer ->
                            addText(
                                "Answer: ${answer.answer}"
                            )
                        }

                        addText("====================")
                    }
                }

                override fun onFailure(
                    call: Call<FormResponsesResponse>,
                    t: Throwable
                ) {
                    progressBar.visibility = View.GONE
                    statusText.text =
                        "Connection error: ${t.message}"
                }
            })
    }

    private fun loadLowerFeedback() {

        prepareLoading()

        ApiClient.apiService
            .getLowerFeedback(
                "Bearer $token",
                formId
            )
            .enqueue(object :
                Callback<LowerFeedbackResponse> {

                override fun onResponse(
                    call: Call<LowerFeedbackResponse>,
                    response: Response<LowerFeedbackResponse>
                ) {
                    progressBar.visibility = View.GONE

                    val body = response.body()

                    if (!response.isSuccessful || body == null) {
                        statusText.text =
                            "Failed to load lower feedback (${response.code()})"
                        return
                    }

                    addText(
                        "Lower Feedback (${body.total})",
                        22f
                    )

                    if (body.responses.isEmpty()) {
                        addText("No lower feedback")
                        return
                    }

                    body.responses.forEach { item ->

                        addText(item.studentName, 18f)
                        addText("Batch: ${item.batch}")

                        item.answers.forEach { answer ->
                            addText(
                                "Answer: ${answer.answer}"
                            )
                        }

                        val reFeedbackButton = Button(this@ResponsesActivity)
                        reFeedbackButton.text = "Re-feedback"

                        reFeedbackButton.setOnClickListener {

                            val intent =
                                Intent(
                                    this@ResponsesActivity,
                                    ReFeedbackActivity::class.java
                                )

                            intent.putExtra(
                                "responseId",
                                item._id
                            )

                            startActivity(intent)
                        }

                        container.addView(reFeedbackButton)

                        addText("====================")

                        val shareReFeedbackButton =
                            Button(this@ResponsesActivity)

                        shareReFeedbackButton.text = "Share Re-feedback"

                        shareReFeedbackButton.setOnClickListener {

                            val link =
                                "feedbackapp://refeedback/${item._id}"

                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"

                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Please submit your re-feedback:\n$link"
                                )
                            }

                            startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share Re-feedback"
                                )
                            )
                        }

                        container.addView(shareReFeedbackButton)

                        val qrButton =
                            Button(this@ResponsesActivity)

                        qrButton.text = "Re-feedback QR"

                        qrButton.setOnClickListener {

                            val link =
                                "feedbackapp://refeedback/${item._id}"

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

                                val imageView =
                                    ImageView(this@ResponsesActivity)

                                imageView.setImageBitmap(bitmap)

                                AlertDialog.Builder(
                                    this@ResponsesActivity
                                )
                                    .setTitle("Re-feedback")
                                    .setMessage(
                                        "Scan to submit re-feedback"
                                    )
                                    .setView(imageView)
                                    .setPositiveButton(
                                        "Close",
                                        null
                                    )
                                    .show()

                            } catch (e: Exception) {

                                Toast.makeText(
                                    this@ResponsesActivity,
                                    "Could not generate QR: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        container.addView(qrButton)
                    }


                }

                override fun onFailure(
                    call: Call<LowerFeedbackResponse>,
                    t: Throwable
                ) {
                    progressBar.visibility = View.GONE
                    statusText.text =
                        "Connection error: ${t.message}"
                }
            })
    }
}