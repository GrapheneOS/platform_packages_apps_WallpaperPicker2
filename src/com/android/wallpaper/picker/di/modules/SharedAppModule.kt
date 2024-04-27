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
import com.android.wallpaper.system.UiModeManagerImpl
import com.android.wallpaper.system.UiModeManagerWrapper
import com.android.wallpaper.util.WallpaperXMLParser
import com.android.wallpaper.util.WallpaperXMLParserInterface
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
    abstract fun bindWallpaperXMLParser(impl: WallpaperXMLParser): WallpaperXMLParserInterface

    @Binds
    @Singleton
    abstract fun bindCategoryFactory(impl: DefaultCategoryFactory): CategoryFactory

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
    }
}
