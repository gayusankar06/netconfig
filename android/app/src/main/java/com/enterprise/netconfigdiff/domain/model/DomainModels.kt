package com.enterprise.netconfigdiff.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class DashboardStats(
    val totalReviews: Int,
    val openReviews: Int,
    val highRiskFindings: Int,
    val complianceViolations: Int,
    val pendingApprovals: Int
)

data class DiffChange(
    val id: String,
    val reviewId: String,
    val changeType: String,
    val fieldPath: String,
    val fieldName: String,
    val oldValue: String?,
    val newValue: String?,
    val riskLevel: String,
    val riskScore: Float,
    val aiExplanation: String?,
    val affectedResource: String?,
    val cisControlRef: String?,
    val nistControlRef: String?,
    val orderIndex: Int
)

data class ComplianceFinding(
    val id: String,
    val reviewId: String,
    val framework: String,
    val controlId: String,
    val controlName: String,
    val status: String,
    val findingDescription: String,
    val remediationGuidance: String?,
    val severity: String,
    val evidence: String?
)

data class WorkflowStep(
    val id: String,
    val reviewId: String,
    val stepNumber: Int,
    val status: String,
    val actorName: String?,
    val actorRole: String?,
    val comment: String?,
    val createdAt: String
)

data class Review(
    val id: String,
    val title: String,
    val ticketId: String?,
    val description: String?,
    val configType: String,
    val cloudProvider: String,
    val status: String,
    val overallRiskLevel: String,
    val overallRiskScore: Float?,
    val complianceScore: Float?,
    val createdBy: String,
    val assignedReviewerId: String?,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?
)

data class ReviewDetail(
    val review: Review,
    val creator: User,
    val assignedReviewer: User?,
    val diffChanges: List<DiffChange>,
    val complianceFindings: List<ComplianceFinding>,
    val workflowSteps: List<WorkflowStep>,
    val aiSummary: String?,
    val aiRecommendation: String?,
    val complianceFrameworks: List<String>
)

data class AuditLog(
    val id: String,
    val eventType: String,
    val eventDescription: String,
    val userId: String?,
    val userEmail: String?,
    val userRole: String?,
    val reviewId: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val requestId: String?,
    val createdAt: String
)
