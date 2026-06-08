package com.enterprise.netconfigdiff.domain.repository

import com.enterprise.netconfigdiff.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    fun getSessionUser(): Flow<User?>
    suspend fun clearSession()
}

interface ReviewRepository {
    fun getReviews(): Flow<List<Review>>
    fun getReviewDetail(reviewId: String): Flow<ReviewDetail?>
    suspend fun refreshReviews(): Result<Unit>
    suspend fun refreshReviewDetail(reviewId: String): Result<Unit>
    suspend fun approveReview(reviewId: String, comment: String): Result<List<WorkflowStep>>
    suspend fun rejectReview(reviewId: String, comment: String): Result<List<WorkflowStep>>
    suspend fun escalateReview(reviewId: String, comment: String): Result<List<WorkflowStep>>
}

interface UploadRepository {
    suspend fun uploadConfigurationFiles(
        oldFile: File,
        newFile: File,
        configType: String,
        cloudProvider: String
    ): Result<String>
}

interface AnalysisRepository {
    suspend fun triggerAnalysis(reviewId: String): Result<Unit>
    suspend fun getAnalysis(reviewId: String): Result<String>
}

interface ComplianceRepository {
    suspend fun triggerCompliance(reviewId: String): Result<Unit>
    fun getComplianceFindings(reviewId: String): Flow<List<ComplianceFinding>>
    suspend fun refreshComplianceFindings(reviewId: String): Result<Unit>
}

interface ReportRepository {
    suspend fun generateAndDownloadReport(reviewId: String, format: String): Result<ByteArray>
}

interface AuditRepository {
    fun getAuditLogs(filter: String?, search: String?): Flow<List<AuditLog>>
    suspend fun refreshAuditLogs(page: Int, size: Int, filter: String?, search: String?): Result<Int>
}
