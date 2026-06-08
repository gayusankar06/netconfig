package com.enterprise.netconfigdiff.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("role") val role: String
)

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("user") val user: UserDto
)

data class RefreshTokenDto(
    @SerializedName("refresh_token") val refreshToken: String
)

data class DashboardStatsDto(
    @SerializedName("total_reviews") val totalReviews: Int,
    @SerializedName("open_reviews") val openReviews: Int,
    @SerializedName("high_risk_findings") val highRiskFindings: Int,
    @SerializedName("compliance_violations") val complianceViolations: Int,
    @SerializedName("pending_approvals") val pendingApprovals: Int
)

data class UploadResponseDto(
    @SerializedName("upload_id") val uploadId: String
)

data class CreateDiffRequestDto(
    @SerializedName("upload_id") val uploadId: String,
    @SerializedName("compliance_frameworks") val complianceFrameworks: List<String>,
    @SerializedName("title") val title: String,
    @SerializedName("ticket_id") val ticketId: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("auto_approve_if_low_risk") val autoApprove: Boolean = false,
    @SerializedName("notify_manager") val notifyManager: Boolean = true
)

data class DiffJobDto(
    @SerializedName("review_id") val reviewId: String,
    @SerializedName("status") val status: String
)

data class DiffChangeDto(
    @SerializedName("id") val id: String,
    @SerializedName("review_id") val reviewId: String,
    @SerializedName("change_type") val changeType: String,
    @SerializedName("field_path") val fieldPath: String,
    @SerializedName("field_name") val fieldName: String,
    @SerializedName("old_value") val oldValue: String?,
    @SerializedName("new_value") val newValue: String?,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("risk_score") val riskScore: Float,
    @SerializedName("ai_explanation") val aiExplanation: String?,
    @SerializedName("affected_resource") val affectedResource: String?,
    @SerializedName("cis_control_ref") val cisControlRef: String?,
    @SerializedName("nist_control_ref") val nistControlRef: String?,
    @SerializedName("order_index") val orderIndex: Int
)

typealias DiffResultDto = List<DiffChangeDto>

data class ReviewDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("ticket_id") val ticketId: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("config_type") val configType: String,
    @SerializedName("cloud_provider") val cloudProvider: String,
    @SerializedName("status") val status: String,
    @SerializedName("overall_risk_level") val overallRiskLevel: String,
    @SerializedName("overall_risk_score") val overallRiskScore: Float?,
    @SerializedName("compliance_score") val complianceScore: Float?,
    @SerializedName("created_by") val createdBy: String,
    @SerializedName("assigned_reviewer_id") val assignedReviewerId: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("completed_at") val completedAt: String?
)

typealias ReviewListDto = List<ReviewDto>

data class ComplianceFindingDto(
    @SerializedName("id") val id: String,
    @SerializedName("review_id") val reviewId: String,
    @SerializedName("framework") val framework: String,
    @SerializedName("control_id") val controlId: String,
    @SerializedName("control_name") val controlName: String,
    @SerializedName("status") val status: String,
    @SerializedName("finding_description") val findingDescription: String,
    @SerializedName("remediation_guidance") val remediationGuidance: String?,
    @SerializedName("severity") val severity: String,
    @SerializedName("evidence") val evidence: String?
)

data class WorkflowStepDto(
    @SerializedName("id") val id: String,
    @SerializedName("review_id") val reviewId: String,
    @SerializedName("step_number") val stepNumber: Int,
    @SerializedName("status") val status: String,
    @SerializedName("actor_id") val actorId: String?,
    @SerializedName("actor_name") val actorName: String?,
    @SerializedName("actor_role") val actorRole: String?,
    @SerializedName("comment") val comment: String?,
    @SerializedName("created_at") val createdAt: String
)

data class ReviewDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("ticket_id") val ticketId: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("config_type") val configType: String,
    @SerializedName("cloud_provider") val cloudProvider: String,
    @SerializedName("status") val status: String,
    @SerializedName("overall_risk_level") val overallRiskLevel: String,
    @SerializedName("overall_risk_score") val overallRiskScore: Float?,
    @SerializedName("compliance_score") val complianceScore: Float?,
    @SerializedName("created_by") val createdBy: String,
    @SerializedName("assigned_reviewer_id") val assignedReviewerId: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("completed_at") val completedAt: String?,
    @SerializedName("creator") val creator: UserDto,
    @SerializedName("assigned_reviewer") val assignedReviewer: UserDto?,
    @SerializedName("diff_changes") val diffChanges: List<DiffChangeDto> = emptyList(),
    @SerializedName("compliance_findings") val complianceFindings: List<ComplianceFindingDto> = emptyList(),
    @SerializedName("workflow_steps") val workflowSteps: List<WorkflowStepDto> = emptyList(),
    @SerializedName("ai_summary") val aiSummary: String?,
    @SerializedName("ai_recommendation") val aiRecommendation: String?,
    @SerializedName("compliance_frameworks") val complianceFrameworks: List<String> = emptyList()
)

data class ApprovalActionDto(
    @SerializedName("comment") val comment: String
)

data class AuditLogDto(
    @SerializedName("id") val id: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("event_description") val eventDescription: String,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("user_email") val userEmail: String?,
    @SerializedName("user_role") val userRole: String?,
    @SerializedName("review_id") val reviewId: String?,
    @SerializedName("ip_address") val ipAddress: String?,
    @SerializedName("user_agent") val userAgent: String?,
    @SerializedName("request_id") val requestId: String?,
    @SerializedName("created_at") val createdAt: String
)

data class AuditLogListDto(
    @SerializedName("total") val total: Int,
    @SerializedName("items") val items: List<AuditLogDto>
)
