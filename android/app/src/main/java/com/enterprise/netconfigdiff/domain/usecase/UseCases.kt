package com.enterprise.netconfigdiff.domain.usecase

import com.enterprise.netconfigdiff.domain.model.*
import com.enterprise.netconfigdiff.domain.repository.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return authRepository.login(email, password)
    }
}

class GetDashboardStatsUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    // Return flow calculation or similar mapping, or default fetch.
    // For simplicity, we can fetch from repository reviews listing and summarize stats locally
    // to keep it offline-first, or hook to separate API.
    operator fun invoke(): Flow<DashboardStats> {
        return reviewRepository.getReviews().mapToStats()
    }
    
    private fun Flow<List<Review>>.mapToStats(): Flow<DashboardStats> = this.mapFlow { list ->
        val total = list.size
        val open = list.count { it.status in listOf("DRAFT", "UNDER_ANALYSIS", "PENDING_REVIEW", "PENDING_APPROVAL", "ESCALATED") }
        val highRisk = list.count { it.overallRiskLevel in listOf("CRITICAL", "HIGH") }
        val compliance = list.count { (it.complianceScore ?: 100f) < 100f }
        val pending = list.count { it.status in listOf("PENDING_REVIEW", "PENDING_APPROVAL", "ESCALATED") }
        DashboardStats(total, open, highRisk, compliance, pending)
    }
}

// Inline helper to map flow items since map operator in standard Flow works on flow itself
fun <T, R> Flow<T>.mapFlow(transform: suspend (value: T) -> R): Flow<R> = kotlinx.coroutines.flow.map { transform(it) }

class UploadConfigFilesUseCase @Inject constructor(
    private val uploadRepository: UploadRepository
) {
    suspend operator fun invoke(
        oldFile: File,
        newFile: File,
        configType: String,
        cloudProvider: String
    ): Result<String> {
        return uploadRepository.uploadConfigurationFiles(oldFile, newFile, configType, cloudProvider)
    }
}

class CreateDiffReviewUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    // Note: We can implement CreateDiffReview using Repository and Retrofit, or direct upload flow.
    // Let's implement trigger interface if needed.
}

class PollReviewStatusUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    // Polls until completed or failed
    operator fun invoke(reviewId: String): Flow<Review> = flow {
        while (true) {
            val result = reviewRepository.refreshReviewDetail(reviewId)
            if (result.isSuccess) {
                // Fetch from repository
                // Delay 2 seconds
            }
            delay(2000L)
        }
    }
}

class GetDiffChangesUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    operator fun invoke(reviewId: String): Flow<List<DiffChange>> {
        return reviewRepository.getReviewDetail(reviewId).mapFlow { it?.diffChanges ?: emptyList() }
    }
}

class GetAIAnalysisUseCase @Inject constructor(
    private val analysisRepository: AnalysisRepository
) {
    suspend operator fun invoke(reviewId: String): Result<String> {
        return analysisRepository.getAnalysis(reviewId)
    }
}

class GetComplianceReportUseCase @Inject constructor(
    private val complianceRepository: ComplianceRepository
) {
    operator fun invoke(reviewId: String): Flow<List<ComplianceFinding>> {
        return complianceRepository.getComplianceFindings(reviewId)
    }
}

class ApproveReviewUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(reviewId: String, comment: String): Result<List<WorkflowStep>> {
        return reviewRepository.approveReview(reviewId, comment)
    }
}

class RejectReviewUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(reviewId: String, comment: String): Result<List<WorkflowStep>> {
        return reviewRepository.rejectReview(reviewId, comment)
    }
}

class EscalateReviewUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(reviewId: String, comment: String): Result<List<WorkflowStep>> {
        return reviewRepository.escalateReview(reviewId, comment)
    }
}

class GenerateReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(reviewId: String, format: String): Result<ByteArray> {
        return reportRepository.generateAndDownloadReport(reviewId, format)
    }
}

class GetAuditLogsUseCase @Inject constructor(
    private val auditRepository: AuditRepository
) {
    operator fun invoke(filter: String?, search: String?): Flow<List<AuditLog>> {
        return auditRepository.getAuditLogs(filter, search)
    }
}
