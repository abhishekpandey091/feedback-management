package com.example.feedbackmanagement

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserData
)

data class UserData(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String
)