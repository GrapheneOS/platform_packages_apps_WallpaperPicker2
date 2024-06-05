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
package com.android.wallpaper.modules

import android.content.Context
import com.android.wallpaper.module.DefaultPartnerProvider
import com.android.wallpaper.module.DefaultWallpaperPreferences
import com.android.wallpaper.module.Injector
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.module.WallpaperPicker2Injector
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.NoOpUserEventLogger
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.preview.data.util.DefaultLiveWallpaperDownloader
import com.android.wallpaper.picker.preview.data.util.LiveWallpaperDownloader
import com.android.wallpaper.picker.preview.ui.util.DefaultImageEffectDialogUtil
import com.android.wallpaper.picker.preview.ui.util.ImageEffectDialogUtil
import com.android.wallpaper.util.converter.DefaultWallpaperModelFactory
import com.android.wallpaper.util.converter.WallpaperModelFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WallpaperPicker2AppModule {
    @Binds @Singleton abstract fun bindInjector(impl: WallpaperPicker2Injector): Injector

    @Binds
    @Singleton
    abstract fun bindWallpaperModelFactory(
        impl: DefaultWallpaperModelFactory
    ): WallpaperModelFactory

    @Binds
    @Singleton
    abstract fun bindLiveWallpaperDownloader(
        impl: DefaultLiveWallpaperDownloader
    ): LiveWallpaperDownloader

    @Binds
    @Singleton
    abstract fun bindPartnerProvider(impl: DefaultPartnerProvider): PartnerProvider

    @Binds
    @Singleton
    abstract fun bindEffectsWallpaperDialogUtil(
        impl: DefaultImageEffectDialogUtil
    ): ImageEffectDialogUtil

    @Binds
    @Singleton
    abstract fun bindCustomizationOptionsBinder(
        impl: DefaultCustomizationOptionsBinder
    ): CustomizationOptionsBinder

    companion object {
        @Provides
        @Singleton
        fun provideWallpaperPreferences(
            @ApplicationContext context: Context
        ): WallpaperPreferences {
            return DefaultWallpaperPreferences(context)
        }

        @Provides
        @Singleton
        fun provideUserEventLogger(): UserEventLogger {
            return NoOpUserEventLogger()
        }
    }
}
