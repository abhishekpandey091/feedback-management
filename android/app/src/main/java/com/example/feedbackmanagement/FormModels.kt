package com.example.feedbackmanagement

data class FormsResponse(
    val message: String,
    val forms: List<FormData>
)

data class FormData(
    val _id: String,
    val title: String,
    val description: String?,
    val approvalStatus: String,
    val isActive: Boolean,
    val createdByRole: String
)

// ADD BELOW THIS LINE

data class CreateFormRequest(
    val title: String,
    val description: String,
    val questions: List<CreateQuestion>
)

data class CreateQuestion(
    val questionText: String,
    val type: String,
    val options: List<String> = emptyList(),
    val maxStars: Int = 10,
    val required: Boolean = true
)

data class CreateFormResponse(
    val message: String,
    val form: FormData
)

data class RejectFormRequest(
    val reason: String
)