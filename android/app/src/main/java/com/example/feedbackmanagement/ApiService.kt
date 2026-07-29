package com.example.feedbackmanagement

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.PATCH
import retrofit2.http.PUT
import okhttp3.ResponseBody


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
        @Header("Authorization") token: String,
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("limit") limit: Int = 10,
        @retrofit2.http.Query("teacherId") teacherId: String? = null
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

    @GET("api/responses/form/{formId}")
    fun getFormResponses(
        @Header("Authorization") token: String,
        @Path("formId") formId: String
    ): Call<FormResponsesResponse>

    @GET("api/responses/form/{formId}/summary")
    fun getResponseSummary(
        @Header("Authorization") token: String,
        @Path("formId") formId: String
    ): Call<SummaryResponse>

    @GET("api/responses/form/{formId}/lower-feedback")
    fun getLowerFeedback(
        @Header("Authorization") token: String,
        @Path("formId") formId: String
    ): Call<LowerFeedbackResponse>

    @GET("api/responses/{responseId}/refeedback")
    fun getReFeedback(
        @Header("Authorization") token: String,
        @Path("responseId") responseId: String
    ): Call<ReFeedbackResponse>


    @PUT("api/responses/{responseId}/refeedback")
    fun submitReFeedback(
        @Header("Authorization") token: String,
        @Path("responseId") responseId: String,
        @Body request: ReFeedbackRequest
    ): Call<ReFeedbackSubmitResponse>


    @GET("api/responses/form/{formId}/export")
    fun exportResponses(
        @Header("Authorization") token: String,
        @Path("formId") formId: String
    ): Call<ResponseBody>

    @PATCH("api/forms/{id}")
    fun updateForm(
        @Header("Authorization") token: String,
        @Path("id") formId: String,
        @Body request: CreateFormRequest
    ): Call<CreateFormResponse>

    @GET("api/forms/{id}")
    fun getFormById(
        @Header("Authorization") token: String,
        @Path("id") formId: String
    ): Call<CreateFormResponse>

    @DELETE("api/forms/{id}")
    fun deleteForm(
        @Header("Authorization") token: String,
        @Path("id") formId: String
    ): Call<MessageResponse>

    @PATCH("api/forms/{id}/deactivate")
    fun deactivateForm(
        @Header("Authorization") token: String,
        @Path("id") formId: String
    ): Call<CreateFormResponse>

}