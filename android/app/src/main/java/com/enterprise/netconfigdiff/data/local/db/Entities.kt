package com.enterprise.netconfigdiff.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean,
    val isVerified: Boolean
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "diff_changes")
data class DiffChangeEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "compliance_findings")
data class ComplianceFindingEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val label: String,
    val nextKey: Int?
)
