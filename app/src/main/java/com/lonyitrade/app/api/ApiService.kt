package com.lonyitrade.app.api

import com.lonyitrade.app.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    @GET("api/users/{id}")
    suspend fun getUserById(@Header("Authorization") token: String, @Path("id") userId: String): Response<UserProfile>

    @Multipart
    @POST("api/users/{id}/upload-profile-picture")
    suspend fun uploadProfilePicture(
        @Header("Authorization") token: String,
        @Path("id") userId: String,
        @Part photo: MultipartBody.Part
    ): Response<UserProfile>

    @POST("api/users/fcm-token")
    suspend fun updateFcmToken(@Body fcmTokenRequest: FcmTokenRequest): Response<Unit>


    // --- Adverts ---
    // MERGED getAdverts and searchAdverts into one function
    @GET("api/adverts")
    suspend fun searchAdverts(
        @Query("q") query: String?,
        @Query("district") district: String?,
        @Query("min_price") minPrice: String?,
        @Query("max_price") maxPrice: String?,
        @Query("type") type: String?,
        @Query("category") category: String?,
        @Query("sort_by") sortBy: String? // Added sortBy
    ): Response<List<Ad>>

    @GET("api/adverts/my")
    suspend fun getMyAdverts(@Header("Authorization") token: String): Response<List<Ad>>

    @GET("api/adverts/{id}")
    suspend fun getAdvertById(@Header("Authorization") token: String, @Path("id") adId: String): Response<Ad>

    @POST("api/adverts")
    suspend fun postAdvert(@Header("Authorization") token: String, @Body adRequest: AdRequest): Response<Ad>

    @Multipart
    @POST("api/adverts/{id}/upload-photos")
    suspend fun uploadAdPhotos(@Header("Authorization") token: String, @Path("id") adId: String, @Part photos: List<MultipartBody.Part>): Response<Unit>

    @DELETE("api/adverts/{id}")
    suspend fun deleteAdvert(@Header("Authorization") token: String, @Path("id") adId: String): Response<Unit>

    @PUT("api/adverts/{id}")
    suspend fun updateAdvert(@Header("Authorization") token: String, @Path("id") adId: String, @Body adRequest: AdRequest): Response<Ad>


    // --- Rentals ---
    @POST("api/rentals")
    suspend fun postRental(@Header("Authorization") token: String, @Body rentalRequest: RentalRequest): Response<Rental>

    @GET("api/rentals")
    suspend fun getRentals(@Query("sort_by") sortBy: String? = null): Response<List<Rental>>

    @GET("api/rentals/my")
    suspend fun getMyRentals(@Header("Authorization") token: String): Response<List<Rental>>

    @Multipart
    @POST("api/rentals/{id}/upload-photo")
    suspend fun uploadRentalPhotos(@Header("Authorization") token: String, @Path("id") rentalId: String, @Part photos: List<MultipartBody.Part>): Response<Unit>

    @PUT("api/rentals/{id}")
    suspend fun updateRental(@Header("Authorization") token: String, @Path("id") rentalId: String, @Body rentalRequest: RentalRequest): Response<Rental>

    @DELETE("api/rentals/{id}")
    suspend fun deleteRental(@Header("Authorization") token: String, @Path("id") rentalId: String): Response<Unit>


    // --- Messages ---
    @POST("api/messages")
    suspend fun postMessage(@Header("Authorization") token: String, @Body message: Message): Response<Message>

    @GET("api/messages/{advertId}/{otherUserId}")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("advertId") advertId: String,
        @Path("otherUserId") otherUserId: String
    ): Response<List<Message>>

    @GET("api/messages/conversations")
    suspend fun getConversations(@Header("Authorization") token: String): Response<List<ConversationSummary>>

    @GET("api/messages/unread/count")
    suspend fun getUnreadMessageCount(@Header("Authorization") token: String): Response<UnreadCountResponse>

    @POST("api/messages/read/{advertId}/{otherUserId}")
    suspend fun markMessagesAsRead(
        @Header("Authorization") token: String,
        @Path("advertId") advertId: String,
        @Path("otherUserId") otherUserId: String
    ): Response<Unit>

    @Multipart
    @POST("api/messages/upload-media")
    suspend fun uploadMessageMedia(
        @Header("Authorization") token: String,
        @Part("advertId") advertId: RequestBody,
        @Part("receiverId") receiverId: RequestBody,
        @Part media: MultipartBody.Part,
        @Part("caption") caption: RequestBody?
    ): Response<Message>

    @Multipart
    @POST("api/messages/upload-audio")
    suspend fun uploadMessageAudio(
        @Header("Authorization") token: String,
        @Part("advertId") advertId: RequestBody,
        @Part("receiverId") receiverId: RequestBody,
        @Part audio: MultipartBody.Part
    ): Response<Message>

    data class UnreadCountResponse(val unreadCount: Int)

    // --- Job Applications ---
    @GET("api/jobs/applications/my")
    suspend fun getMyJobApplications(@Header("Authorization") token: String): Response<List<JobApplication>>

    @POST("api/jobs/applications")
    suspend fun applyForJob(
        @Header("Authorization") token: String,
        @Body jobApplicationRequest: JobApplicationRequest
    ): Response<JobApplication>

    @GET("api/jobs/applicants")
    suspend fun getJobApplicants(
        @Header("Authorization") token: String,
        @Query("jobTitle") jobTitle: String?,
        @Query("education") education: String?
    ): Response<List<JobApplication>>

    // --- Job Listings (New) ---
    @POST("api/jobs/listings")
    suspend fun postJobListing(
        @Header("Authorization") token: String,
        @Body jobListingRequest: JobListingRequest
    ): Response<JobAd>

    @GET("api/jobs/listings")
    suspend fun getJobListings(
        @Query("q") query: String?,
        @Query("location") location: String?
    ): Response<List<JobAd>>

    // --- Reviews ---
    @POST("api/reviews")
    suspend fun postReview(
        @Header("Authorization") token: String,
        @Body reviewRequest: ReviewRequest
    ): Response<Review>

    @GET("api/reviews/{userId}")
    suspend fun getUserReviews(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<List<Review>>
}