package com.peakconnect.app.data.api

import com.peakconnect.app.data.dto.ActivityResponse
import com.peakconnect.app.data.dto.AuthResponse
import com.peakconnect.app.data.dto.BookingConfirmDto
import com.peakconnect.app.data.dto.BookingRequestDto
import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.dto.GuideProfileUpdateRequest
import com.peakconnect.app.data.dto.GuideResponse
import com.peakconnect.app.data.dto.LoginRequest
import com.peakconnect.app.data.dto.MatchedGuideResponse
import com.peakconnect.app.data.dto.RegisterRequest
import com.peakconnect.app.data.dto.WaitlistMessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PeakConnectApi {

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // Public Activities
    @GET("api/activities")
    suspend fun getActivities(
        @Query("location") location: String? = null,
        @Query("date") date: String? = null // yyyy-MM-dd
    ): Response<List<ActivityResponse>>

    @GET("api/activities/{id}")
    suspend fun getActivity(
        @Path("id") id: String
    ): Response<ActivityResponse>

    // Bookings (Trekker)
    @POST("api/bookings/request")
    suspend fun requestBooking(
        @Body request: BookingRequestDto
    ): Response<List<MatchedGuideResponse>>

    @POST("api/bookings/confirm")
    suspend fun confirmBooking(
        @Body request: BookingConfirmDto
    ): Response<BookingResponse>

    @GET("api/bookings/my")
    suspend fun getMyBookings(): Response<List<BookingResponse>>

    @POST("api/bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Path("id") id: String
    ): Response<BookingResponse>

    @POST("api/bookings/waitlist")
    suspend fun joinWaitlist(
        @Body request: BookingConfirmDto
    ): Response<WaitlistMessageResponse>

    // Guide
    @GET("api/guides/me")
    suspend fun getMyGuideProfile(): Response<GuideResponse>

    @PUT("api/guides/me")
    suspend fun updateMyGuideProfile(
        @Body request: GuideProfileUpdateRequest
    ): Response<GuideResponse>

    @GET("api/guides/me/bookings")
    suspend fun getGuideBookings(): Response<List<BookingResponse>>

    @POST("api/bookings/{id}/accept")
    suspend fun acceptBooking(
        @Path("id") id: String
    ): Response<BookingResponse>

    @POST("api/bookings/{id}/decline")
    suspend fun declineBooking(
        @Path("id") id: String
    ): Response<BookingResponse>

    @POST("api/bookings/{id}/no-show")
    suspend fun markNoShow(
        @Path("id") id: String
    ): Response<BookingResponse>
}
