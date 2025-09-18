package com.lonyitrade.app.api

import com.lonyitrade.app.data.models.AuthResponse
import com.lonyitrade.app.data.models.LoginRequest
import com.lonyitrade.app.data.models.RegisterRequest
import com.lonyitrade.app.data.models.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<TokenResponse>
}