package com.enterprise.netconfigdiff.data.remote.api

import com.enterprise.netconfigdiff.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface NetConfigApiService {

    // Auth
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenDto): Response<AuthResponseDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    // Dashboard
    @GET("api/v1/dashboard")
    suspend fun getDashboardStats(): Response<DashboardStatsDto>

    // Upload
    @Multipart
    @POST("api/v1/upload")
    suspend fun uploadConfigs(
        @Part oldFile: MultipartBody.Part,
        @Part newFile: MultipartBody.Part,
        @Part("config_type") configType: RequestBody,
        @Part("cloud_provider") cloudProvider: RequestBody
    ): Response<UploadResponseDto>

    // Diff
    @POST("api/v1/diff")
    suspend fun createDiff(@Body request: CreateDiffRequestDto): Response<DiffJobDto>

    @GET("api/v1/diff/{reviewId}/changes")
    suspend fun getDiffChanges(@Path("reviewId") reviewId: String): Response<DiffResultDto>

    @GET("api/v1/reviews/{reviewId}/status")
    suspend fun getReviewStatus(@Path("reviewId") reviewId: String): Response<ReviewDto> # note: returning ReviewDto or simple wrapper

    // Analysis
    @POST("api/v1/analyze/{reviewId}")
    suspend fun triggerAnalysis(@Path("reviewId") reviewId: String): Response<Unit>

    @GET("api/v1/analyze/{reviewId}")
    suspend fun getAnalysis(@Path("reviewId") reviewId: String): Response<ResponseBody> # returns the full analysis JSON structure

    // Compliance
    @POST("api/v1/compliance/{reviewId}")
    suspend fun runCompliance(@Path("reviewId") reviewId: String): Response<Unit>

    @GET("api/v1/compliance/{reviewId}")
    suspend fun getComplianceReport(@Path("reviewId") reviewId: String): Response<List<ComplianceFindingDto>>

    // Reports
    @POST("api/v1/report/{reviewId}")
    suspend fun generateReport(
        @Path("reviewId") reviewId: String,
        @Query("format") format: String # pdf | markdown | json
    ): Response<ResponseBody>

    // Reviews
    @GET("api/v1/reviews")
    suspend fun getReviews(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String = "created_at:desc"
    ): Response<ReviewListDto>

    @GET("api/v1/reviews/{reviewId}")
    suspend fun getReview(@Path("reviewId") reviewId: String): Response<ReviewDetailDto>

    @GET("api/v1/reviews/{reviewId}/workflow")
    suspend fun getWorkflowStatus(@Path("reviewId") reviewId: String): Response<List<WorkflowStepDto>>

    @PATCH("api/v1/reviews/{reviewId}/approve")
    suspend fun approveReview(
        @Path("reviewId") reviewId: String,
        @Body request: ApprovalActionDto
    ): Response<List<WorkflowStepDto>>

    @PATCH("api/v1/reviews/{reviewId}/reject")
    suspend fun rejectReview(
        @Path("reviewId") reviewId: String,
        @Body request: ApprovalActionDto
    ): Response<List<WorkflowStepDto>>

    @PATCH("api/v1/reviews/{reviewId}/escalate")
    suspend fun escalateReview(
        @Path("reviewId") reviewId: String,
        @Body request: ApprovalActionDto
    ): Response<List<WorkflowStepDto>>

    // Audit Logs
    @GET("api/v1/audit")
    suspend fun getAuditLogs(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("filter") filter: String?,
        @Query("search") search: String?,
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null
    ): Response<AuditLogListDto>
}
