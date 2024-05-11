/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.wallpaper.di.modules

import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import com.android.wallpaper.module.LargeScreenMultiPanesChecker
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.di.modules.DispatchersModule
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.di.modules.SharedAppModule
import com.android.wallpaper.system.UiModeManagerWrapper
import com.android.wallpaper.testing.FakeDefaultCategoryFactory
import com.android.wallpaper.testing.FakeUiModeManager
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.FakeWallpaperParser
import com.android.wallpaper.util.WallpaperParser
import com.android.wallpaper.util.converter.category.CategoryFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SharedAppModule::class, DispatchersModule::class]
)
internal abstract class SharedTestModule {
    @Binds @Singleton abstract fun bindUiModeManager(impl: FakeUiModeManager): UiModeManagerWrapper

    @Binds
    @Singleton
    abstract fun bindWallpaperXMLParser(impl: FakeWallpaperParser): WallpaperParser

    @Binds
    @Singleton
    abstract fun bindCategoryFactory(impl: FakeDefaultCategoryFactory): CategoryFactory

    @Binds @Singleton abstract fun bindWallpaperClient(impl: FakeWallpaperClient): WallpaperClient

    @Binds
    @Singleton
    @MainDispatcher
    abstract fun bindMainDispatcher(impl: TestDispatcher): CoroutineDispatcher

    @Binds @Singleton @MainDispatcher abstract fun bindMainScope(impl: TestScope): CoroutineScope

    @Binds
    @Singleton
    @BackgroundDispatcher
    abstract fun bindBackgroundDispatcher(impl: TestDispatcher): CoroutineDispatcher

    @Binds
    @Singleton
    @BackgroundDispatcher
    abstract fun bindBackgroundScope(impl: TestScope): CoroutineScope

    companion object {
        @Provides
        @Singleton
        fun provideTestDispatcher(): TestDispatcher {
            return StandardTestDispatcher()
        }

        @Provides
        @Singleton
        fun provideTestScope(testDispatcher: TestDispatcher): TestScope {
            return TestScope(testDispatcher)
        }

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
    }
}
