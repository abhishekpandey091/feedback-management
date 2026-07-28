package com.example.feedbackmanagement

data class PublicFormResponse(
    val message: String,
    val form: PublicForm
)

data class PublicForm(
    val _id: String,
    val title: String,
    val description: String?,
    val questions: List<PublicQuestion>,
    val allowedBatches: List<String>
)

data class PublicQuestion(
    val _id: String,
    val questionText: String,
    val type: String,
    val options: List<String>,
    val maxStars: Int,
    val required: Boolean
)

data class SubmitFeedbackRequest(
    val studentName: String,
    val batch: String,
    val enrollmentNumber: String,
    val attendanceStatus: String,
    val lowRatingReason: String?,
    val answers: List<StudentAnswer>
)

data class StudentAnswer(
    val questionId: String,
    val answer: Any
)

data class SubmitFeedbackResponse(
    val message: String,
    val responseId: String?
)