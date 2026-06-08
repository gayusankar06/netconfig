package com.enterprise.netconfigdiff.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ReviewEntity::class,
        DiffChangeEntity::class,
        ComplianceFindingEntity::class,
        AuditLogEntity::class,
        UserEntity::class,
        RemoteKeyEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
    abstract fun diffChangeDao(): DiffChangeDao
    abstract fun complianceFindingDao(): ComplianceFindingDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun userDao(): UserDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}
