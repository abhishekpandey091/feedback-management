package com.example.feedbackmanagement

data class FormResponsesResponse(
    val message: String,
    val totalResponses: Int,
    val responses: List<FeedbackResponse>
)

data class FeedbackResponse(
    val _id: String,
    val formId: String,
    val studentName: String,
    val batch: String,
    val enrollmentNumber: String,
    val answers: List<ResponseAnswer>,
    val submittedAt: String
)

data class ResponseAnswer(
    val questionId: String,
    val answer: Any?
)

data class SummaryResponse(
    val message: String,
    val formId: String,
    val title: String,
    val totalResponses: Int,
    val summary: List<QuestionSummary>
)

data class QuestionSummary(
    val questionId: String,
    val questionText: String,
    val type: String,
    val totalAnswers: Int,
    val average: Double?,
    val lowerCount: Int?,
    val counts: Map<String, Int>?
)

data class LowerFeedbackResponse(
    val message: String,
    val total: Int,
    val responses: List<FeedbackResponse>
)