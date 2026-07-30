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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
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

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        container = findViewById(R.id.responsesContainer)
        progressBar = findViewById(R.id.responsesProgressBar)
        statusText = findViewById(R.id.responsesStatusText)

        val summaryButton =
            findViewById<Button>(R.id.summaryButton)

        val individualButton =
            findViewById<Button>(R.id.individualButton)

        val lowerButton =
            findViewById<Button>(R.id.lowerFeedbackButton)

        val tabGroup =
            findViewById<MaterialButtonToggleGroup>(R.id.tabGroup)

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
        tabGroup.check(R.id.summaryButton)
        loadSummary()

        val exportButton =
            findViewById<View>(R.id.exportButton)

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

    private fun addChartCard(
        labels: List<String>,
        values: List<Float>,
        title: String
    ) {

        if (labels.isEmpty() || values.isEmpty()) return

        val (card, inner) = UiKit.card(this)
        inner.addView(UiKit.captionText(this, title.uppercase()))

        val chart = BarChart(this)
        val chartParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiKit.dp(this, 200)
        )
        chartParams.topMargin = UiKit.dp(this, 8)
        chart.layoutParams = chartParams

        val entries = values.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, title)
        dataSet.color = androidx.core.content.ContextCompat.getColor(this, R.color.brand_primary)
        dataSet.valueTextColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)

        val data = BarData(dataSet)
        data.barWidth = 0.6f

        chart.data = data

        chart.xAxis.valueFormatter =
            IndexAxisValueFormatter(labels)

        chart.xAxis.granularity = 1f
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.textColor =
            androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)

        chart.axisLeft.textColor =
            androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)
        chart.axisLeft.setDrawGridLines(false)

        chart.axisRight.isEnabled = false

        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        chart.setFitBars(true)
        chart.setExtraOffsets(4f, 4f, 4f, 8f)
        chart.invalidate()

        inner.addView(chart)
        container.addView(card)
    }

    private fun metricRow(items: List<Pair<String, String>>) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowParams.bottomMargin = UiKit.dp(this, 4)
        row.layoutParams = rowParams

        items.forEachIndexed { index, (value, label) ->
            val (card, inner) = UiKit.card(this, marginBottomDp = 16)
            val params = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            if (index > 0) params.marginStart = UiKit.dp(this, 12)
            card.layoutParams = params

            val valueText = TextView(this)
            valueText.text = value
            valueText.setTextAppearance(R.style.TextAppearance_Feedback_ScreenTitle)
            inner.addView(valueText)
            inner.addView(UiKit.captionText(this, label))

            row.addView(card)
        }

        container.addView(row)
    }

    private fun prepareLoading() {
        container.removeAllViews()
        statusText.text = ""
        progressBar.visibility = View.VISIBLE
    }

    private fun sectionHeader(text: String) {
        container.addView(UiKit.sectionTitle(this, text))
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

                    metricRow(
                        listOf(
                            body.totalResponses.toString() to "Total Responses",
                            body.summary.count { it.type == "star_rating" }.toString() to "Rated Questions"
                        )
                    )

                    if (body.summary.isEmpty()) {
                        container.addView(
                            UiKit.emptyState(
                                this@ResponsesActivity,
                                R.drawable.ic_bar_chart,
                                "No data yet",
                                "Insights will appear once students respond"
                            )
                        )
                        return
                    }

                    body.summary.forEach { item ->

                        val (card, inner) = UiKit.card(this@ResponsesActivity)
                        inner.addView(UiKit.cardTitle(this@ResponsesActivity, item.questionText))
                        inner.addView(
                            UiKit.captionText(
                                this@ResponsesActivity,
                                "${item.totalAnswers} answers"
                            )
                        )

                        if (item.type == "star_rating") {

                            val avgRow = LinearLayout(this@ResponsesActivity)
                            avgRow.orientation = LinearLayout.HORIZONTAL
                            val avgRowParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            avgRowParams.topMargin = UiKit.dp(this@ResponsesActivity, 10)
                            avgRow.layoutParams = avgRowParams

                            avgRow.addView(
                                UiKit.statusChip(
                                    this@ResponsesActivity,
                                    "Avg " + String.format("%.2f", item.average ?: 0.0),
                                    R.color.brand_primary_container,
                                    R.color.brand_primary
                                )
                            )
                            avgRow.addView(
                                UiKit.statusChip(
                                    this@ResponsesActivity,
                                    "Lower <8: ${item.lowerCount ?: 0}",
                                    R.color.semantic_warning_bg,
                                    R.color.semantic_warning
                                )
                            )
                            inner.addView(avgRow)
                        }

                        container.addView(card)

                        if (item.type == "star_rating") {
                            addChartCard(
                                labels = listOf("Average", "Lower <8"),
                                values = listOf(
                                    (item.average ?: 0.0).toFloat(),
                                    (item.lowerCount ?: 0).toFloat()
                                ),
                                title = "Rating Summary"
                            )
                        }

                        item.counts?.let { counts ->
                            addChartCard(
                                labels = counts.keys.toList(),
                                values = counts.values.map { it.toFloat() },
                                title = item.questionText
                            )
                        }
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

                    sectionHeader("Individual Responses (${body.totalResponses})")

                    if (body.responses.isEmpty()) {
                        container.addView(
                            UiKit.emptyState(
                                this@ResponsesActivity,
                                R.drawable.ic_group,
                                "No responses yet",
                                "Responses will appear here once submitted"
                            )
                        )
                        return
                    }

                    body.responses.forEach { item ->

                        val (card, inner) = UiKit.card(this@ResponsesActivity)

                        inner.addView(UiKit.cardTitle(this@ResponsesActivity, item.studentName))
                        inner.addView(
                            UiKit.captionText(
                                this@ResponsesActivity,
                                "Enrollment: ${item.enrollmentNumber}  ·  Batch: ${item.batch}"
                            )
                        )
                        inner.addView(UiKit.divider(this@ResponsesActivity))

                        item.answers.forEach { answer ->
                            inner.addView(
                                UiKit.bodyText(this@ResponsesActivity, "${answer.answer}")
                            )
                        }

                        container.addView(card)
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

                    sectionHeader("Lower Feedback (${body.total})")

                    if (body.responses.isEmpty()) {
                        container.addView(
                            UiKit.emptyState(
                                this@ResponsesActivity,
                                R.drawable.ic_check_circle,
                                "Nothing to review",
                                "No ratings below 8 have been reported"
                            )
                        )
                        return
                    }

                    body.responses.forEach { item ->

                        val (card, inner) = UiKit.card(this@ResponsesActivity)
                        card.setStrokeColor(
                            androidx.core.content.ContextCompat.getColor(
                                this@ResponsesActivity, R.color.semantic_warning
                            )
                        )

                        val headerRow = LinearLayout(this@ResponsesActivity)
                        headerRow.orientation = LinearLayout.HORIZONTAL
                        headerRow.gravity = android.view.Gravity.CENTER_VERTICAL

                        val textCol = LinearLayout(this@ResponsesActivity)
                        textCol.orientation = LinearLayout.VERTICAL
                        textCol.layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                        textCol.addView(UiKit.cardTitle(this@ResponsesActivity, item.studentName))
                        textCol.addView(UiKit.captionText(this@ResponsesActivity, "Batch: ${item.batch}"))
                        headerRow.addView(textCol)
                        headerRow.addView(
                            UiKit.statusChip(
                                this@ResponsesActivity, "Needs review",
                                R.color.semantic_warning_bg, R.color.semantic_warning
                            )
                        )
                        inner.addView(headerRow)
                        inner.addView(UiKit.divider(this@ResponsesActivity))

                        item.answers.forEach { answer ->
                            inner.addView(
                                UiKit.bodyText(this@ResponsesActivity, "${answer.answer}")
                            )
                        }

                        val actions = UiKit.wrapRow(this@ResponsesActivity)

                        actions.addView(
                            UiKit.actionChip(
                                this@ResponsesActivity, "Re-feedback",
                                R.drawable.ic_edit, primary = true
                            ) {
                                val intent = Intent(
                                    this@ResponsesActivity,
                                    ReFeedbackActivity::class.java
                                )
                                intent.putExtra("responseId", item._id)
                                startActivity(intent)
                            }
                        )

                        actions.addView(
                            UiKit.actionChip(
                                this@ResponsesActivity, "Share",
                                R.drawable.ic_share
                            ) {
                                val link = "feedbackapp://refeedback/${item._id}"

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
                        )

                        actions.addView(
                            UiKit.actionChip(
                                this@ResponsesActivity, "QR Code",
                                R.drawable.ic_qr_code
                            ) {
                                val link = "feedbackapp://refeedback/${item._id}"
                                QrHelper.show(
                                    this@ResponsesActivity,
                                    link,
                                    "Re-feedback",
                                    "Scan to submit re-feedback"
                                )
                            }
                        )

                        inner.addView(actions)
                        container.addView(card)
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
