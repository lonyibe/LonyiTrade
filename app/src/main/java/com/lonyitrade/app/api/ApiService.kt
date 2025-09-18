package com.lonyitrade.app.api

import com.lonyitrade.app.data.models.AuthResponse
import com.lonyitrade.app.data.models.LoginRequest
import com.lonyitrade.app.data.models.RegisterRequest
import com.lonyitrade.app.data.models.TokenResponse
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.AdRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {

    @POST("api/auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<TokenResponse>

    @GET("api/adverts")
    suspend fun getAdverts(): Response<List<Ad>>

    @POST("api/adverts")
    suspend fun postAdvert(@Header("Authorization") token: String, @Body adRequest: AdRequest): Response<Ad>
}