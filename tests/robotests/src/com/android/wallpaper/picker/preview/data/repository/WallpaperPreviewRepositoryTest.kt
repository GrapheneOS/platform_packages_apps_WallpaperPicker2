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
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.data.util.FakeLiveWallpaperDownloader
import com.android.wallpaper.picker.preview.shared.model.LiveWallpaperDownloadResultCode
import com.android.wallpaper.picker.preview.shared.model.LiveWallpaperDownloadResultModel
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.testing.WallpaperModelUtils.Companion.getStaticWallpaperModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
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
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class WallpaperPreviewRepositoryTest {

    private lateinit var context: Context
    private lateinit var testDispatcher: CoroutineDispatcher
    private lateinit var testScope: TestScope
    private lateinit var underTest: WallpaperPreviewRepository
    private lateinit var prefs: WallpaperPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        prefs = TestWallpaperPreferences()
    }

    @Test
    fun setWallpaperModel() {
        underTest =
            WallpaperPreviewRepository(
                liveWallpaperDownloader = FakeLiveWallpaperDownloader(),
                preferences = prefs,
                bgDispatcher = testDispatcher,
            )

        val wallpaperModel =
            getStaticWallpaperModel(
                wallpaperId = "aaa",
                collectionId = "testCollection",
            )
        assertThat(underTest.wallpaperModel.value).isNull()

        underTest.setWallpaperModel(wallpaperModel)

        assertThat(underTest.wallpaperModel.value).isEqualTo(wallpaperModel)
    }

    @Test
    fun dismissSmallTooltip() {
        prefs.setHasSmallPreviewTooltipBeenShown(false)
        prefs.setHasFullPreviewTooltipBeenShown(false)
        underTest =
            WallpaperPreviewRepository(
                liveWallpaperDownloader = FakeLiveWallpaperDownloader(),
                preferences = prefs,
                bgDispatcher = testDispatcher,
            )
        assertThat(underTest.hasSmallPreviewTooltipBeenShown.value).isFalse()
        assertThat(underTest.hasFullPreviewTooltipBeenShown.value).isFalse()

        underTest.hideSmallPreviewTooltip()

        assertThat(prefs.getHasSmallPreviewTooltipBeenShown()).isTrue()
        assertThat(underTest.hasSmallPreviewTooltipBeenShown.value).isTrue()
        assertThat(prefs.getHasFullPreviewTooltipBeenShown()).isFalse()
        assertThat(underTest.hasFullPreviewTooltipBeenShown.value).isFalse()
    }

    @Test
    fun dismissFullTooltip() {
        prefs.setHasSmallPreviewTooltipBeenShown(false)
        prefs.setHasFullPreviewTooltipBeenShown(false)
        underTest =
            WallpaperPreviewRepository(
                liveWallpaperDownloader = FakeLiveWallpaperDownloader(),
                preferences = prefs,
                bgDispatcher = testDispatcher,
            )
        assertThat(underTest.hasSmallPreviewTooltipBeenShown.value).isFalse()
        assertThat(underTest.hasFullPreviewTooltipBeenShown.value).isFalse()

        underTest.hideFullPreviewTooltip()

        assertThat(prefs.getHasSmallPreviewTooltipBeenShown()).isFalse()
        assertThat(underTest.hasSmallPreviewTooltipBeenShown.value).isFalse()
        assertThat(prefs.getHasFullPreviewTooltipBeenShown()).isTrue()
        assertThat(underTest.hasFullPreviewTooltipBeenShown.value).isTrue()
    }

    @Test
    fun downloadWallpaper_fails() {
        val liveWallpaperDownloader = FakeLiveWallpaperDownloader()
        liveWallpaperDownloader.setWallpaperDownloadResult(
            LiveWallpaperDownloadResultModel(LiveWallpaperDownloadResultCode.FAIL, null)
        )
        underTest =
            WallpaperPreviewRepository(
                liveWallpaperDownloader = liveWallpaperDownloader,
                preferences = prefs,
                bgDispatcher = testDispatcher,
            )

        testScope.runTest {
            val result = underTest.downloadWallpaper()

            assertThat(result).isNotNull()
            val (code, wallpaperModel) = result!!
            assertThat(code).isEqualTo(LiveWallpaperDownloadResultCode.FAIL)
            assertThat(wallpaperModel).isNull()
        }
    }

    @Test
    fun downloadWallpaper_succeeds() {
        val liveWallpaperDownloader = FakeLiveWallpaperDownloader()
        val resultWallpaper = getTestLiveWallpaperModel()
        liveWallpaperDownloader.setWallpaperDownloadResult(
            LiveWallpaperDownloadResultModel(
                code = LiveWallpaperDownloadResultCode.SUCCESS,
                wallpaperModel = resultWallpaper,
            )
        )
        underTest =
            WallpaperPreviewRepository(
                liveWallpaperDownloader = liveWallpaperDownloader,
                preferences = prefs,
                bgDispatcher = testDispatcher,
            )

        testScope.runTest {
            val result = underTest.downloadWallpaper()

            assertThat(result).isNotNull()
            val (code, wallpaperModel) = result!!
            assertThat(code).isEqualTo(LiveWallpaperDownloadResultCode.SUCCESS)
            assertThat(wallpaperModel).isEqualTo(resultWallpaper)
        }
    }

    private fun getTestLiveWallpaperModel(): WallpaperModel.LiveWallpaperModel {
        // ShadowWallpaperInfo allows the creation of this object
        val wallpaperInfo =
            WallpaperInfo(
                context,
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
