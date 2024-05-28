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

package com.android.wallpaper.picker.preview.domain.interactor

import android.content.Context
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.preview.data.repository.CreativeEffectsRepository
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.data.util.FakeLiveWallpaperDownloader
import com.android.wallpaper.testing.FakeImageEffectsRepository
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowWallpaperInfo::class])
class PreviewActionsInteractorTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var previewActionsInteractor: PreviewActionsInteractor
    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    private lateinit var creativeEffectsRepository: CreativeEffectsRepository

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var liveWallpaperDownloader: FakeLiveWallpaperDownloader
    @Inject lateinit var wallpaperPreferences: TestWallpaperPreferences
    @Inject lateinit var imageEffectsRepository: FakeImageEffectsRepository

    @Before
    fun setUp() {
        hiltRule.inject()

        InjectorProvider.setInjector(testInjector)
        Dispatchers.setMain(testDispatcher)

        wallpaperPreviewRepository =
            WallpaperPreviewRepository(
                liveWallpaperDownloader,
                wallpaperPreferences,
                testDispatcher
            )
        creativeEffectsRepository = CreativeEffectsRepository(appContext, testDispatcher)
        previewActionsInteractor =
            PreviewActionsInteractor(
                wallpaperPreviewRepository,
                imageEffectsRepository,
                creativeEffectsRepository
            )
    }

    @Test
    fun isDownloading_trueWhenDownloading() = runTest {
        val downloading = collectLastValue(previewActionsInteractor.isDownloadingWallpaper)

        // Request a download and progress until we're blocked waiting for the result
        backgroundScope.launch { previewActionsInteractor.downloadWallpaper() }
        advanceUntilIdle()
        assertThat(downloading()).isTrue()

        // Set the result and be sure downloading status updates
        liveWallpaperDownloader.setWallpaperDownloadResult(null)
        advanceUntilIdle()
        assertThat(downloading()).isFalse()
    }
}
