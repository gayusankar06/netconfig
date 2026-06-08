package com.enterprise.netconfigdiff.di

import android.content.Context
import androidx.room.Room
import com.enterprise.netconfigdiff.data.local.db.*
import com.enterprise.netconfigdiff.data.remote.api.NetConfigApiService
import com.enterprise.netconfigdiff.data.remote.interceptors.AuthInterceptor
import com.enterprise.netconfigdiff.data.remote.interceptors.TokenManager
import com.enterprise.netconfigdiff.data.repository.*
import com.enterprise.netconfigdiff.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "netconfig_reviewer_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideReviewDao(db: AppDatabase): ReviewDao = db.reviewDao()

    @Provides
    fun provideDiffChangeDao(db: AppDatabase): DiffChangeDao = db.diffChangeDao()

    @Provides
    fun provideComplianceFindingDao(db: AppDatabase): ComplianceFindingDao = db.complianceFindingDao()

    @Provides
    fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideRemoteKeyDao(db: AppDatabase): RemoteKeyDao = db.remoteKeyDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideNetConfigApiService(okHttpClient: OkHttpClient): NetConfigApiService {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:80/") // emulator default base URL, matching Constants
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NetConfigApiService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: NetConfigApiService,
        userDao: UserDao,
        tokenManager: TokenManager
    ): AuthRepository = AuthRepositoryImpl(apiService, userDao, tokenManager)

    @Provides
    @Singleton
    fun provideReviewRepository(
        apiService: NetConfigApiService,
        reviewDao: ReviewDao,
        diffChangeDao: DiffChangeDao,
        complianceFindingDao: ComplianceFindingDao
    ): ReviewRepository = ReviewRepositoryImpl(apiService, reviewDao, diffChangeDao, complianceFindingDao)

    @Provides
    @Singleton
    fun provideUploadRepository(
        apiService: NetConfigApiService
    ): UploadRepository = UploadRepositoryImpl(apiService)

    @Provides
    @Singleton
    fun provideAuditRepository(
        apiService: NetConfigApiService,
        auditLogDao: AuditLogDao
    ): AuditRepository = AuditRepositoryImpl(apiService, auditLogDao)
}
