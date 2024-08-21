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
import android.content.res.Resources
import com.android.wallpaper.module.LargeScreenMultiPanesChecker
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.module.NetworkStatusNotifier
import com.android.wallpaper.picker.category.client.LiveWallpapersClient
import com.android.wallpaper.picker.category.data.repository.WallpaperCategoryRepository
import com.android.wallpaper.picker.category.domain.interactor.CategoriesLoadingStatusInteractor
import com.android.wallpaper.picker.category.domain.interactor.CategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.MyPhotosInteractor
import com.android.wallpaper.picker.category.domain.interactor.ThirdPartyCategoryInteractor
import com.android.wallpaper.picker.category.ui.view.providers.IndividualPickerFactory
import com.android.wallpaper.picker.category.ui.view.providers.implementation.DefaultIndividualPickerFactory
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.di.modules.SharedAppModule
import com.android.wallpaper.system.UiModeManagerWrapper
import com.android.wallpaper.testing.FakeCategoriesLoadingStatusInteractor
import com.android.wallpaper.testing.FakeCategoryInteractor
import com.android.wallpaper.testing.FakeCreativeWallpaperInteractor
import com.android.wallpaper.testing.FakeDefaultCategoryFactory
import com.android.wallpaper.testing.FakeDefaultWallpaperCategoryRepository
import com.android.wallpaper.testing.FakeLiveWallpaperClientImpl
import com.android.wallpaper.testing.FakeMyPhotosInteractor
import com.android.wallpaper.testing.FakeThirdPartyCategoryInteractor
import com.android.wallpaper.testing.FakeUiModeManager
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.FakeWallpaperParser
import com.android.wallpaper.testing.TestNetworkStatusNotifier
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
@TestInstallIn(components = [SingletonComponent::class], replaces = [SharedAppModule::class])
internal abstract class SharedAppTestModule {

    // Also use the test dispatcher for work intended for the background thread. This makes tests
    // single-threaded and more deterministic.
    @Binds
    @Singleton
    @BackgroundDispatcher
    abstract fun bindBackgroundDispatcher(impl: TestDispatcher): CoroutineDispatcher

    @Binds
    @Singleton
    abstract fun bindCategoryFactory(impl: FakeDefaultCategoryFactory): CategoryFactory

    @Binds
    @Singleton
    abstract fun bindCategoryInteractor(impl: FakeCategoryInteractor): CategoryInteractor

    @Binds
    @Singleton
    abstract fun bindCreativeCategoryInteractor(
        impl: FakeCreativeWallpaperInteractor
    ): CreativeCategoryInteractor

    // Dispatcher and Scope injection choices are based on documentation at
    // http://go/android-dev/kotlin/coroutines/test. Most tests will not need to inject anything
    // other than the TestDispatcher, for use in Dispatchers.setMain().

    @Binds
    @Singleton
    abstract fun bindFakeDefaultWallpaperCategoryRepository(
        impl: FakeDefaultWallpaperCategoryRepository
    ): WallpaperCategoryRepository

    @Binds
    @Singleton
    abstract fun bindIndividualPickerFactoryFragment(
        impl: DefaultIndividualPickerFactory
    ): IndividualPickerFactory

    @Binds
    @Singleton
    abstract fun bindLiveWallpaperClient(
        impl: FakeLiveWallpaperClientImpl,
    ): LiveWallpapersClient

    @Binds
    @Singleton
    abstract fun bindLoadingStatusInteractor(
        impl: FakeCategoriesLoadingStatusInteractor,
    ): CategoriesLoadingStatusInteractor

    // Use the test dispatcher for work intended for the main thread
    @Binds
    @Singleton
    @MainDispatcher
    abstract fun bindMainDispatcher(impl: TestDispatcher): CoroutineDispatcher

    // Use the test scope as the main scope to match the test dispatcher
    @Binds @Singleton @MainDispatcher abstract fun bindMainScope(impl: TestScope): CoroutineScope

    @Binds
    @Singleton
    abstract fun bindMyPhotosInteractor(impl: FakeMyPhotosInteractor): MyPhotosInteractor

    @Binds
    @Singleton
    abstract fun bindNetworkStatusNotifier(impl: TestNetworkStatusNotifier): NetworkStatusNotifier

    @Binds
    @Singleton
    abstract fun bindThirdPartyCategoryInteractor(
        impl: FakeThirdPartyCategoryInteractor
    ): ThirdPartyCategoryInteractor

    @Binds
    @Singleton
    abstract fun bindUiModeManagerWrapper(impl: FakeUiModeManager): UiModeManagerWrapper

    @Binds @Singleton abstract fun bindWallpaperClient(impl: FakeWallpaperClient): WallpaperClient

    @Binds @Singleton abstract fun bindWallpaperParser(impl: FakeWallpaperParser): WallpaperParser

    companion object {

        // Scope for background work that does not need to finish before a test completes, like
        // continuously reading values from a flow.
        @Provides
        @Singleton
        @BackgroundDispatcher
        fun provideBackgroundScope(impl: TestScope): CoroutineScope {
            return impl.backgroundScope
        }

        @Provides
        @Singleton
        fun provideMultiPanesChecker(): MultiPanesChecker {
            return LargeScreenMultiPanesChecker()
        }

        @Provides
        @Singleton
        fun providePackageManager(@ApplicationContext appContext: Context): PackageManager {
            return appContext.packageManager
        }

        @Provides
        @Singleton
        fun provideResources(@ApplicationContext context: Context): Resources {
            return context.resources
        }

        // This is the most general test dispatcher for use in tests. UnconfinedTestDispatcher
        // is the other choice. The difference is that the unconfined dispatcher starts new
        // coroutines eagerly, which could be easier but could also make tests non-deterministic in
        // some cases.
        @Provides
        @Singleton
        fun provideTestDispatcher(): TestDispatcher {
            return StandardTestDispatcher()
        }

        // Scope corresponding to the test dispatcher and main test thread. Tests will fail if work
        // is still running in this scope after the test completes.
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
    }
}
