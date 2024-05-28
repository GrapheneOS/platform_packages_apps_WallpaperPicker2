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
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.Point
import android.stats.style.StyleEnums
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.WallpaperModelUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowWallpaperInfo::class])
class WallpaperPreviewInteractorTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var wallpaperPreviewInteractor: WallpaperPreviewInteractor

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var prefs: TestWallpaperPreferences
    @Inject lateinit var client: FakeWallpaperClient

    @Before
    fun setUp() {
        hiltRule.inject()

        InjectorProvider.setInjector(testInjector)
        Dispatchers.setMain(testDispatcher)

        val activityInfo =
            ActivityInfo().apply {
                name = PreviewTestActivity::class.java.name
                packageName = appContext.packageName
            }
        Shadows.shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
        scenario = ActivityScenario.launch(PreviewTestActivity::class.java)
        scenario.onActivity { setEverything(it) }
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface ActivityScopeEntryPoint {
        fun interactor(): WallpaperPreviewInteractor
    }

    private fun setEverything(activity: PreviewTestActivity) {
        val activityScopeEntryPoint =
            EntryPointAccessors.fromActivity(activity, ActivityScopeEntryPoint::class.java)
        wallpaperPreviewInteractor = activityScopeEntryPoint.interactor()
    }

    @Test
    fun hideSmallPreviewTooltip_succeeds() {
        prefs.setHasSmallPreviewTooltipBeenShown(false)
        assertThat(wallpaperPreviewInteractor.hasSmallPreviewTooltipBeenShown.value).isFalse()

        wallpaperPreviewInteractor.hideSmallPreviewTooltip()

        assertThat(wallpaperPreviewInteractor.hasSmallPreviewTooltipBeenShown.value).isTrue()
    }

    @Test
    fun hideFullPreviewTooltip_succeeds() {
        prefs.setHasFullPreviewTooltipBeenShown(false)
        assertThat(wallpaperPreviewInteractor.hasFullPreviewTooltipBeenShown.value).isFalse()

        wallpaperPreviewInteractor.hideFullPreviewTooltip()

        assertThat(wallpaperPreviewInteractor.hasFullPreviewTooltipBeenShown.value).isTrue()
    }

    @Test
    fun setStaticWallpaper_succeeds() = runTest {
        val asset = WallpaperModelUtils.DEFAULT_ASSET
        val wallpaperModel =
            WallpaperModelUtils.getStaticWallpaperModel(
                wallpaperId = "testWallpaperId",
                collectionId = "testCollectionId",
                placeholderColor = 0,
                asset = asset,
            )

        wallpaperPreviewInteractor.setStaticWallpaper(
            setWallpaperEntryPoint = StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
            destination = WallpaperDestination.HOME,
            wallpaperModel = wallpaperModel,
            bitmap = asset.bitmap,
            wallpaperSize = Point(10, 10),
            asset = asset,
        )
        runCurrent()

        assertThat(client.wallpapersSet[WallpaperDestination.HOME]).containsExactly(wallpaperModel)
        assertThat(client.wallpapersSet[WallpaperDestination.LOCK]).isEmpty()
    }

    @Test
    fun setLiveWallpaper_succeeds() = runTest {
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
        val wallpaperModel =
            WallpaperModelUtils.getLiveWallpaperModel(
                wallpaperId = "testWallpaperId",
                collectionId = "testCollectionId",
                systemWallpaperInfo = wallpaperInfo,
            )

        wallpaperPreviewInteractor.setLiveWallpaper(
            setWallpaperEntryPoint = StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
            destination = WallpaperDestination.HOME,
            wallpaperModel = wallpaperModel,
        )

        assertThat(client.wallpapersSet[WallpaperDestination.HOME]).containsExactly(wallpaperModel)
        assertThat(client.wallpapersSet[WallpaperDestination.LOCK]).isEmpty()
    }
}
