package com.example.feedbackmanagement


data class ReFeedbackResponse(
    val message: String,
    val form: ReFeedbackForm,
    val response: ReFeedbackStudentResponse
)

data class ReFeedbackForm(
    val _id: String,
    val title: String,
    val description: String?,
    val questions: List<PublicQuestion>
)

data class ReFeedbackStudentResponse(
    val _id: String,
    val studentName: String,
    val enrollmentNumber: String,
    val batch: String,
    val attendanceStatus: String?,
    val lowRatingReason: String?,
    val answers: List<StudentAnswer>
)

data class ReFeedbackRequest(
    val answers: List<StudentAnswer>,
    val lowRatingReason: String?
)

data class ReFeedbackSubmitResponse(
    val message: String,
    val response: ReFeedbackStudentResponse?
)