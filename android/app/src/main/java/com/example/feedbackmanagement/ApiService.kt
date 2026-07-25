package com.example.feedbackmanagement

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header

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
}