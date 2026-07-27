package com.example.feedbackmanagement

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.PATCH

interface ApiService {

    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @GET("api/forms/my-forms")
    fun getMyForms(
        @Header("Authorization") token: String
    ): Call<FormsResponse>

    @POST("api/forms")
    fun createForm(
        @Header("Authorization") authorization: String,
        @Body request: CreateFormRequest
    ): Call<CreateFormResponse>

    @GET("api/teachers")
    fun getTeachers(
        @Header("Authorization") token: String
    ): Call<TeachersResponse>

    @POST("api/teachers")
    fun createTeacher(
        @Header("Authorization") token: String,
        @Body request: CreateTeacherRequest
    ): Call<TeacherResponse>

    @DELETE("api/teachers/{id}")
    fun deleteTeacher(
        @Header("Authorization") token: String,
        @Path("id") teacherId: String
    ): Call<MessageResponse>

    @PATCH("api/teachers/{id}/toggle-active")
    fun toggleTeacherStatus(
        @Header("Authorization") token: String,
        @Path("id") teacherId: String
    ): Call<TeacherResponse>

    @GET("api/forms")
    fun getAllForms(
        @Header("Authorization") token: String
    ): Call<FormsResponse>

    @PATCH("api/forms/{id}/approve")
    fun approveForm(
        @Header("Authorization") token: String,
        @Path("id") formId: String
    ): Call<CreateFormResponse>

    @PATCH("api/forms/{id}/reject")
    fun rejectForm(
        @Header("Authorization") token: String,
        @Path("id") formId: String,
        @Body request: RejectFormRequest
    ): Call<CreateFormResponse>

    @PATCH("api/forms/{id}/activate")
    fun activateForm(
        @Header("Authorization") token: String,
        @Path("id") formId: String
    ): Call<CreateFormResponse>

    @GET("api/public/forms/{formId}")
    fun getPublicForm(
        @Path("formId") formId: String
    ): Call<PublicFormResponse>

    @POST("api/public/forms/{formId}/responses")
    fun submitFeedback(
        @Path("formId") formId: String,
        @Body request: SubmitFeedbackRequest
    ): Call<SubmitFeedbackResponse>
}