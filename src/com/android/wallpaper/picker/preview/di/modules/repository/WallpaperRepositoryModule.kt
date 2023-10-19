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
package com.android.wallpaper.picker.preview.di.modules.repository

import android.graphics.Bitmap
import android.graphics.Rect
import com.android.wallpaper.dispatchers.BackgroundDispatcher
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.model.wallpaper.ScreenOrientation
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.UserEventLogger.SetWallpaperEntryPoint
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.customization.shared.model.WallpaperModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@InstallIn(SingletonComponent::class)
@Module
class WallpaperRepositoryModule {

    @Provides
    @Singleton
    fun provideWallpaperRepository(
        @BackgroundDispatcher bgDispatcher: CoroutineDispatcher,
        @MainDispatcher mainScope: CoroutineScope,
        wallpaperPreferences: WallpaperPreferences
    ): WallpaperRepository {
        return WallpaperRepository(
            scope = mainScope,
            client =
                object : WallpaperClient {
                    override fun recentWallpapers(
                        destination: WallpaperDestination,
                        limit: Int,
                    ): Flow<List<WallpaperModel>> {
                        return emptyFlow()
                    }

                    override suspend fun setRecentWallpaper(
                        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
                        destination: WallpaperDestination,
                        wallpaperId: String,
                        onDone: () -> Unit,
                    ) {}

                    override suspend fun setStaticWallpaper(
                        setWallpaperEntryPoint: Int,
                        destination: WallpaperDestination,
                        wallpaperModel:
                            com.android.wallpaper.model.wallpaper.WallpaperModel.StaticWallpaperModel,
                        bitmap: Bitmap,
                        cropHints: Map<ScreenOrientation, Rect>,
                        onDone: () -> Unit
                    ) {}

                    override suspend fun loadThumbnail(
                        wallpaperId: String,
                        destination: WallpaperDestination
                    ): Bitmap? {
                        return null
                    }

                    override fun areRecentsAvailable(): Boolean {
                        return false
                    }
                },
            wallpaperPreferences = wallpaperPreferences,
            backgroundDispatcher = bgDispatcher,
        )
    }
}
