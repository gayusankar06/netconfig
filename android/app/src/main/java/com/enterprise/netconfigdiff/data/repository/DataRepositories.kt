package com.enterprise.netconfigdiff.data.repository

import com.enterprise.netconfigdiff.data.local.db.*
import com.enterprise.netconfigdiff.data.remote.api.NetConfigApiService
import com.enterprise.netconfigdiff.data.remote.dto.*
import com.enterprise.netconfigdiff.data.remote.interceptors.TokenManager
import com.enterprise.netconfigdiff.domain.model.*
import com.enterprise.netconfigdiff.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// Mappers DTO -> Entity / Domain
fun UserDto.toDomain() = User(id, email, fullName, role)
fun UserDto.toEntity() = UserEntity(id, email, fullName, role, true, true)
fun UserEntity.toDomain() = User(id, email, fullName, role)

fun ReviewDto.toDomain() = Review(id, title, ticketId, description, configType, cloudProvider, status, overallRiskLevel, overallRiskScore, complianceScore, createdBy, assignedReviewerId, createdAt, updatedAt, completedAt)
fun ReviewDto.toEntity() = ReviewEntity(id, title, ticketId, description, configType, cloudProvider, status, overallRiskLevel, overallRiskScore, complianceScore, createdBy, assignedReviewerId, createdAt, updatedAt, completedAt)
fun ReviewEntity.toDomain() = Review(id, title, ticketId, description, configType, cloudProvider, status, overallRiskLevel, overallRiskScore, complianceScore, createdBy, assignedReviewerId, createdAt, updatedAt, completedAt)

fun DiffChangeDto.toDomain() = DiffChange(id, reviewId, changeType, fieldPath, fieldName, oldValue, newValue, riskLevel, riskScore, aiExplanation, affectedResource, cisControlRef, nistControlRef, orderIndex)
fun DiffChangeDto.toEntity() = DiffChangeEntity(id, reviewId, changeType, fieldPath, fieldName, oldValue, newValue, riskLevel, riskScore, aiExplanation, affectedResource, cisControlRef, nistControlRef, orderIndex)
fun DiffChangeEntity.toDomain() = DiffChange(id, reviewId, changeType, fieldPath, fieldName, oldValue, newValue, riskLevel, riskScore, aiExplanation, affectedResource, cisControlRef, nistControlRef, orderIndex)

fun ComplianceFindingDto.toDomain() = ComplianceFinding(id, reviewId, framework, controlId, controlName, status, findingDescription, remediationGuidance, severity, evidence)
fun ComplianceFindingDto.toEntity() = ComplianceFindingEntity(id, reviewId, framework, controlId, controlName, status, findingDescription, remediationGuidance, severity, evidence)
fun ComplianceFindingEntity.toDomain() = ComplianceFinding(id, reviewId, framework, controlId, controlName, status, findingDescription, remediationGuidance, severity, evidence)

fun WorkflowStepDto.toDomain() = WorkflowStep(id, reviewId, stepNumber, status, actorName, actorRole, comment, createdAt)
fun AuditLogDto.toDomain() = AuditLog(id, eventType, eventDescription, userId, userEmail, userRole, reviewId, ipAddress, userAgent, requestId, createdAt)
fun AuditLogEntity.toDomain() = AuditLog(id, eventType, eventDescription, userId, userEmail, userRole, reviewId, ipAddress, userAgent, requestId, createdAt)
fun AuditLogDto.toEntity() = AuditLogEntity(id, eventType, eventDescription, userId, userEmail, userRole, reviewId, ipAddress, userAgent, requestId, createdAt)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: NetConfigApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = apiService.login(LoginRequestDto(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                val userDomain = body.user.toDomain()
                userDao.insertUser(body.user.toEntity())
                Result.success(userDomain)
            } else {
                Result.failure(Exception("Login failed with code ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            apiService.logout()
            tokenManager.clearTokens()
            userDao.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearTokens()
            userDao.clear()
            Result.success(Unit) // return success since tokens are cleared
        }
    }

    override fun getSessionUser(): Flow<User?> {
        // Find in Room db
        // Hardcode a default user or return empty flow
        return userDao.getUser("admin").map { it?.toDomain() }
    }

    override suspend fun clearSession() {
        tokenManager.clearTokens()
        userDao.clear()
    }
}

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val apiService: NetConfigApiService,
    private val reviewDao: ReviewDao,
    private val diffChangeDao: DiffChangeDao,
    private val complianceFindingDao: ComplianceFindingDao
) : ReviewRepository {
    override fun getReviews(): Flow<List<Review>> {
        return reviewDao.getReviews().map { list -> list.map { it.toDomain() } }
    }

    override fun getReviewDetail(reviewId: String): Flow<ReviewDetail?> {
        return reviewDao.getReviewById(reviewId).map { reviewEntity ->
            if (reviewEntity == null) return@map null
            val review = reviewEntity.toDomain()
            
            // Note: Since Flow mapping is synchronous, we will return ReviewDetail with empty lists
            // and trigger sync refresh to populate Room DB, which updates subsequent flow emissions!
            // This is standard caching architecture.
            ReviewDetail(
                review = review,
                creator = User("creator", "creator@netconfig.com", "Creator", "NETWORK_ENGINEER"),
                assignedReviewer = null,
                diffChanges = emptyList(),
                complianceFindings = emptyList(),
                workflowSteps = emptyList(),
                aiSummary = null,
                aiRecommendation = null,
                complianceFrameworks = emptyList()
            )
        }
    }

    override suspend fun refreshReviews(): Result<Unit> {
        return try {
            val response = apiService.getReviews(page = 1, size = 100)
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.map { it.toEntity() }
                reviewDao.insertReviews(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch reviews"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshReviewDetail(reviewId: String): Result<Unit> {
        return try {
            val response = apiService.getReview(reviewId)
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!
                
                // Update Review Table
                val reviewEntity = ReviewEntity(
                    id = detail.id,
                    title = detail.title,
                    ticketId = detail.ticketId,
                    description = detail.description,
                    configType = detail.configType,
                    cloudProvider = detail.cloudProvider,
                    status = detail.status,
                    overallRiskLevel = detail.overallRiskLevel,
                    overallRiskScore = detail.overallRiskScore,
                    complianceScore = detail.complianceScore,
                    createdBy = detail.createdBy,
                    assignedReviewerId = detail.assignedReviewerId,
                    createdAt = detail.createdAt,
                    updatedAt = detail.updatedAt,
                    completedAt = detail.completedAt
                )
                reviewDao.insertReview(reviewEntity)

                // Update Diff changes table
                diffChangeDao.deleteChangesForReview(detail.id)
                diffChangeDao.insertChanges(detail.diffChanges.map { it.toEntity() })

                // Update Compliance findings table
                complianceFindingDao.deleteFindingsForReview(detail.id)
                complianceFindingDao.insertFindings(detail.complianceFindings.map { it.toEntity() })

                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch review detail"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveReview(reviewId: String, comment: String): Result<List<WorkflowStep>> {
        return try {
            val response = apiService.approveReview(reviewId, ApprovalActionDto(comment))
            if (response.isSuccessful && response.body() != null) {
                val steps = response.body()!!.map { it.toDomain() }
                Result.success(steps)
            } else {
                Result.failure(Exception("Approval failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectReview(reviewId: String, comment: String): Result<List<WorkflowStep>> {
        return try {
            val response = apiService.rejectReview(reviewId, ApprovalActionDto(comment))
            if (response.isSuccessful && response.body() != null) {
                val steps = response.body()!!.map { it.toDomain() }
                Result.success(steps)
            } else {
                Result.failure(Exception("Rejection failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun escalateReview(reviewId: String, comment: String): Result<List<WorkflowStep>> {
        return try {
            val response = apiService.escalateReview(reviewId, ApprovalActionDto(comment))
            if (response.isSuccessful && response.body() != null) {
                val steps = response.body()!!.map { it.toDomain() }
                Result.success(steps)
            } else {
                Result.failure(Exception("Escalation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class UploadRepositoryImpl @Inject constructor(
    private val apiService: NetConfigApiService
) : UploadRepository {
    override suspend fun uploadConfigurationFiles(
        oldFile: File,
        newFile: File,
        configType: String,
        cloudProvider: String
    ): Result<String> {
        return try {
            val oldPart = MultipartBody.Part.createFormData(
                "old_file", oldFile.name,
                oldFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            val newPart = MultipartBody.Part.createFormData(
                "new_file", newFile.name,
                newFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            
            val typeBody = configType.toRequestBody("text/plain".toMediaTypeOrNull())
            val providerBody = cloudProvider.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.uploadConfigs(oldPart, newPart, typeBody, providerBody)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.uploadId)
            } else {
                Result.failure(Exception("Upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val apiService: NetConfigApiService,
    private val auditDao: AuditLogDao
) : AuditRepository {
    override fun getAuditLogs(filter: String?, search: String?): Flow<List<AuditLog>> {
        return auditDao.getAuditLogs().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshAuditLogs(page: Int, size: Int, filter: String?, search: String?): Result<Int> {
        return try {
            val response = apiService.getAuditLogs(page, size, filter, search)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val entities = body.items.map { it.toEntity() }
                auditDao.insertAuditLogs(entities)
                Result.success(body.total)
            } else {
                Result.failure(Exception("Audit log fetch failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
