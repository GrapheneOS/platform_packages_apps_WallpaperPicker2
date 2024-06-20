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

import android.app.WallpaperInfo
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.data.repository.CreativeEffectsRepository
import com.android.wallpaper.picker.preview.data.repository.DownloadableWallpaperRepository
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.shared.model.DownloadStatus
import com.android.wallpaper.picker.preview.shared.model.DownloadableWallpaperModel
import com.android.wallpaper.testing.FakeImageEffectsRepository
import com.android.wallpaper.testing.FakeLiveWallpaperDownloader
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
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

    private lateinit var resultWallpaper: WallpaperModel.LiveWallpaperModel
    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    private lateinit var creativeEffectsRepository: CreativeEffectsRepository
    private lateinit var downloadableWallpaperRepository: DownloadableWallpaperRepository
    private lateinit var underTest: PreviewActionsInteractor

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var liveWallpaperDownloader: FakeLiveWallpaperDownloader
    @Inject lateinit var wallpaperPreferences: TestWallpaperPreferences
    @Inject lateinit var imageEffectsRepository: FakeImageEffectsRepository

    @Before
    fun setUp() {
        hiltRule.inject()

        Dispatchers.setMain(testDispatcher)

        resultWallpaper = getTestLiveWallpaperModel()

        wallpaperPreviewRepository = WallpaperPreviewRepository(wallpaperPreferences)
        downloadableWallpaperRepository = DownloadableWallpaperRepository(liveWallpaperDownloader)
        creativeEffectsRepository = CreativeEffectsRepository(appContext, testDispatcher)
        underTest =
            PreviewActionsInteractor(
                wallpaperPreviewRepository,
                imageEffectsRepository,
                creativeEffectsRepository,
                downloadableWallpaperRepository,
            )
    }

    /**
     * Proceeds through all stages of a successful download, from
     * [DownloadStatus.DOWNLOAD_NOT_AVAILABLE] to [DownloadStatus.DOWNLOADED]
     */
    @Test
    fun downloadableWallpaperModel_downloadSuccess() = runTest {
        val downloadableWallpaperModel = collectLastValue(underTest.downloadableWallpaperModel)

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.DOWNLOAD_NOT_AVAILABLE, null))

        liveWallpaperDownloader.initiateDownloadableServiceByPass()

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.READY_TO_DOWNLOAD, null))

        underTest.downloadWallpaper()

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.DOWNLOADING, null))

        liveWallpaperDownloader.proceedToDownloadSuccess(resultWallpaper)

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.DOWNLOADED, resultWallpaper))
    }

    @Test
    fun wallpaperModel_shouldUpdateWhenDownloadSuccess() = runTest {
        val wallpaperModel = collectLastValue(wallpaperPreviewRepository.wallpaperModel)

        assertThat(wallpaperModel()).isNull()

        liveWallpaperDownloader.initiateDownloadableServiceByPass()
        underTest.downloadWallpaper()
        liveWallpaperDownloader.proceedToDownloadSuccess(resultWallpaper)

        assertThat(wallpaperModel()).isEqualTo(resultWallpaper)
    }

    @Test
    fun downloadableWallpaperModel_downloadFailed() = runTest {
        val downloadableWallpaperModel = collectLastValue(underTest.downloadableWallpaperModel)

        liveWallpaperDownloader.initiateDownloadableServiceByPass()
        underTest.downloadWallpaper()
        liveWallpaperDownloader.proceedToDownloadFailed()

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.READY_TO_DOWNLOAD, null))
    }

    @Test
    fun downloadableWallpaperModel_cancelDownloadWallpaper() = runTest {
        val downloadableWallpaperModel = collectLastValue(underTest.downloadableWallpaperModel)

        liveWallpaperDownloader.initiateDownloadableServiceByPass()
        underTest.downloadWallpaper()
        underTest.cancelDownloadWallpaper()

        assertThat(liveWallpaperDownloader.isCancelDownloadWallpaperCalled).isTrue()
    }

    private fun getTestLiveWallpaperModel(): WallpaperModel.LiveWallpaperModel {
        // ShadowWallpaperInfo allows the creation of this object
        val wallpaperInfo =
            WallpaperInfo(
                appContext,
                ResolveInfo().apply {
                    serviceInfo = ServiceInfo()
                    serviceInfo.packageName = "com.google.android.apps.wallpaper.nexus"
                    serviceInfo.splitName = "wallpaper_cities_ny"
                    serviceInfo.name = "NewYorkWallpaper"
                    serviceInfo.flags = PackageManager.GET_META_DATA
                }
            )
        return WallpaperModelUtils.getLiveWallpaperModel(
            wallpaperId = "uniqueId",
            collectionId = "collectionId",
            systemWallpaperInfo = wallpaperInfo
        )
    }
}
