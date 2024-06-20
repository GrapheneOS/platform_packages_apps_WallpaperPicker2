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

package com.android.wallpaper.picker.preview.data.repository

import android.app.WallpaperInfo
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.shared.model.DownloadStatus
import com.android.wallpaper.picker.preview.shared.model.DownloadableWallpaperModel
import com.android.wallpaper.testing.FakeLiveWallpaperDownloader
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for {@link WallpaperPreviewRepository}.
 *
 * WallpaperPreviewRepository cannot be injected in setUp() because it is annotated with scope
 * ActivityRetainedScoped. We make an instance available via TestActivity, which can inject the SUT
 * and expose it for testing.
 */
@HiltAndroidTest
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class DownloadableWallpaperRepositoryTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var resultWallpaper: WallpaperModel.LiveWallpaperModel
    private lateinit var underTest: DownloadableWallpaperRepository

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var liveWallpaperDownloader: FakeLiveWallpaperDownloader

    @Before
    fun setUp() {
        hiltRule.inject()

        resultWallpaper = getTestLiveWallpaperModel()
        underTest =
            DownloadableWallpaperRepository(liveWallpaperDownloader = liveWallpaperDownloader)
    }

    @Test
    fun downloadableWallpaperModel_downloadSuccess() = runTest {
        val downloadableWallpaperModel = collectLastValue(underTest.downloadableWallpaperModel)

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.DOWNLOAD_NOT_AVAILABLE, null))

        liveWallpaperDownloader.initiateDownloadableServiceByPass()

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.READY_TO_DOWNLOAD, null))

        underTest.downloadWallpaper {}

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.DOWNLOADING, null))

        liveWallpaperDownloader.proceedToDownloadSuccess(resultWallpaper)

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.DOWNLOADED, resultWallpaper))
    }

    @Test
    fun downloadableWallpaperModel_downloadFailed() = runTest {
        val downloadableWallpaperModel = collectLastValue(underTest.downloadableWallpaperModel)

        liveWallpaperDownloader.initiateDownloadableServiceByPass()
        underTest.downloadWallpaper {}
        liveWallpaperDownloader.proceedToDownloadFailed()

        assertThat(downloadableWallpaperModel())
            .isEqualTo(DownloadableWallpaperModel(DownloadStatus.READY_TO_DOWNLOAD, null))
    }

    @Test
    fun downloadableWallpaperModel_cancelDownloadWallpaper() = runTest {
        val downloadableWallpaperModel = collectLastValue(underTest.downloadableWallpaperModel)

        liveWallpaperDownloader.initiateDownloadableServiceByPass()
        underTest.downloadWallpaper {}
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
