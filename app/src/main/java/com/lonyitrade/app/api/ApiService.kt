package com.lonyitrade.app.api

import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.AdRequest
import com.lonyitrade.app.data.models.AuthResponse
import com.lonyitrade.app.data.models.LoginRequest
import com.lonyitrade.app.data.models.RegisterRequest
import com.lonyitrade.app.data.models.TokenResponse
import com.lonyitrade.app.data.models.UserProfile
// We will create these models in the next step
import com.lonyitrade.app.data.models.Rental
import com.lonyitrade.app.data.models.RentalRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Auth ---
    @POST("api/auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<TokenResponse>

    @GET("api/auth/me")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfile>


    // --- Adverts ---
    @GET("api/adverts")
    suspend fun getAdverts(): Response<List<Ad>>

    @GET("api/adverts/my")
    suspend fun getMyAdverts(@Header("Authorization") token: String): Response<List<Ad>>

    @POST("api/adverts")
    suspend fun postAdvert(@Header("Authorization") token: String, @Body adRequest: AdRequest): Response<Ad>

    @Multipart
    @POST("api/adverts/{id}/upload-photo")
    suspend fun uploadAdPhoto(@Header("Authorization") token: String, @Path("id") adId: String, @Part photo: MultipartBody.Part): Response<Unit>

    @DELETE("api/adverts/{id}")
    suspend fun deleteAdvert(@Header("Authorization") token: String, @Path("id") adId: String): Response<Unit>

    @PUT("api/adverts/{id}")
    suspend fun updateAdvert(@Header("Authorization") token: String, @Path("id") adId: String, @Body adRequest: AdRequest): Response<Ad>

    @GET("api/adverts")
    suspend fun searchAdverts(
        @Query("q") query: String?,
        @Query("district") district: String?,
        @Query("min_price") minPrice: String?,
        @Query("max_price") maxPrice: String?
    ): Response<List<Ad>>


    // --- Rentals ---
    @POST("api/rentals")
    suspend fun postRental(@Header("Authorization") token: String, @Body rentalRequest: RentalRequest): Response<Rental>

    @GET("api/rentals")
    suspend fun getRentals(): Response<List<Rental>>

    @Multipart
    @POST("api/rentals/{id}/upload-photo")
    suspend fun uploadRentalPhotos(@Header("Authorization") token: String, @Path("id") rentalId: String, @Part photos: List<MultipartBody.Part>): Response<Unit>
}