package com.rootguard.di

import com.rootguard.data.repository.AppRepositoryImpl
import com.rootguard.data.repository.LogRepositoryImpl
import com.rootguard.data.repository.RootRepositoryImpl
import com.rootguard.data.repository.SettingsRepositoryImpl
import com.rootguard.domain.repository.AppRepository
import com.rootguard.domain.repository.LogRepository
import com.rootguard.domain.repository.RootRepository
import com.rootguard.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRootRepository(
        rootRepositoryImpl: RootRepositoryImpl
    ): RootRepository

    @Binds
    @Singleton
    abstract fun bindAppRepository(
        appRepositoryImpl: AppRepositoryImpl
    ): AppRepository

    @Binds
    @Singleton
    abstract fun bindLogRepository(
        logRepositoryImpl: LogRepositoryImpl
    ): LogRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
