package com.enterprise.netconfigdiff.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUser(id: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clear()
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun getReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    fun getReviewById(id: String): Flow<ReviewEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReviewById(id: String)

    @Query("DELETE FROM reviews")
    suspend fun clear()
}

@Dao
interface DiffChangeDao {
    @Query("SELECT * FROM diff_changes WHERE reviewId = :reviewId ORDER BY orderIndex ASC")
    fun getChangesForReview(reviewId: String): Flow<List<DiffChangeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChanges(changes: List<DiffChangeEntity>)

    @Query("DELETE FROM diff_changes WHERE reviewId = :reviewId")
    suspend fun deleteChangesForReview(reviewId: String)
}

@Dao
interface ComplianceFindingDao {
    @Query("SELECT * FROM compliance_findings WHERE reviewId = :reviewId")
    fun getFindingsForReview(reviewId: String): Flow<List<ComplianceFindingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<ComplianceFindingEntity>)

    @Query("DELETE FROM compliance_findings WHERE reviewId = :reviewId")
    suspend fun deleteFindingsForReview(reviewId: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    fun getAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAll()
}

@Dao
interface RemoteKeyDao {
    @Query("SELECT * FROM remote_keys WHERE label = :label LIMIT 1")
    suspend fun getRemoteKey(label: String): RemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemoteKey(remoteKey: RemoteKeyEntity)

    @Query("DELETE FROM remote_keys WHERE label = :label")
    suspend fun deleteRemoteKey(label: String)
}
