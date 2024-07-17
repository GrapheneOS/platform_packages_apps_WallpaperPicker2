/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.picker.di.modules

import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import com.android.wallpaper.module.DefaultNetworkStatusNotifier
import com.android.wallpaper.module.LargeScreenMultiPanesChecker
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.module.NetworkStatusNotifier
import com.android.wallpaper.network.Requester
import com.android.wallpaper.network.WallpaperRequester
import com.android.wallpaper.picker.category.domain.interactor.CategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.MyPhotosInteractor
import com.android.wallpaper.picker.category.domain.interactor.implementations.CategoryInteractorImpl
import com.android.wallpaper.picker.category.domain.interactor.implementations.CreativeCategoryInteractorImpl
import com.android.wallpaper.picker.category.domain.interactor.implementations.MyPhotosInteractorImpl
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.customization.data.content.WallpaperClientImpl
import com.android.wallpaper.system.UiModeManagerImpl
import com.android.wallpaper.system.UiModeManagerWrapper
import com.android.wallpaper.util.WallpaperParser
import com.android.wallpaper.util.WallpaperParserImpl
import com.android.wallpaper.util.converter.category.CategoryFactory
import com.android.wallpaper.util.converter.category.DefaultCategoryFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SharedAppModule {
    @Binds @Singleton abstract fun bindUiModeManager(impl: UiModeManagerImpl): UiModeManagerWrapper

    @Binds
    @Singleton
    abstract fun bindNetworkStatusNotifier(
        impl: DefaultNetworkStatusNotifier
    ): NetworkStatusNotifier

    @Binds @Singleton abstract fun bindWallpaperRequester(impl: WallpaperRequester): Requester

    @Binds
    @Singleton
    abstract fun bindWallpaperXMLParser(impl: WallpaperParserImpl): WallpaperParser

    @Binds
    @Singleton
    abstract fun bindCategoryFactory(impl: DefaultCategoryFactory): CategoryFactory

    @Binds @Singleton abstract fun bindWallpaperClient(impl: WallpaperClientImpl): WallpaperClient

    @Binds
    @Singleton
    abstract fun bindCategoryInteractor(impl: CategoryInteractorImpl): CategoryInteractor

    @Binds
    @Singleton
    abstract fun bindCreativeCategoryInteractor(
        impl: CreativeCategoryInteractorImpl
    ): CreativeCategoryInteractor

    @Binds
    @Singleton
    abstract fun bindMyPhotosInteractor(impl: MyPhotosInteractorImpl): MyPhotosInteractor

    companion object {
        @Provides
        @Singleton
        fun provideWallpaperManager(@ApplicationContext appContext: Context): WallpaperManager {
            return WallpaperManager.getInstance(appContext)
        }

        @Provides
        @Singleton
        fun providePackageManager(@ApplicationContext appContext: Context): PackageManager {
            return appContext.packageManager
        }

        @Provides
        @Singleton
        fun provideMultiPanesChecker(): MultiPanesChecker {
            return LargeScreenMultiPanesChecker()
        }

        @Provides
        @Singleton
        fun provideResources(@ApplicationContext context: Context): Resources {
            return context.resources
        }
    }
}
