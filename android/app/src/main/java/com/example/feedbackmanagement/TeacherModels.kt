package com.example.feedbackmanagement

data class TeachersResponse(
    val message: String,
    val teachers: List<TeacherData>
)

data class TeacherResponse(
    val message: String,
    val teacher: TeacherData?
)

data class TeacherData(
    val _id: String,
    val fullName: String,
    val email: String,
    val role: String,
    val isActive: Boolean
)

data class CreateTeacherRequest(
    val fullName: String,
    val email: String,
    val password: String
)

data class MessageResponse(
    val message: String
)