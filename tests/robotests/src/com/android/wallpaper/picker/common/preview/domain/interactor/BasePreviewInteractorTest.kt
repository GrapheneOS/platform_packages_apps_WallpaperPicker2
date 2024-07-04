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

package com.android.wallpaper.picker.common.preview.domain.interactor

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.model.WallpaperModelsPair
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.testing.collectLastValue
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BasePreviewInteractorTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    private lateinit var wallpaperRepository: WallpaperRepository
    private lateinit var interactor: BasePreviewInteractor

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var wallpaperPreferences: TestWallpaperPreferences
    @Inject lateinit var wallpaperClient: FakeWallpaperClient

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
        shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
        scenario = ActivityScenario.launch(PreviewTestActivity::class.java)
        scenario.onActivity {
            val activityScopeEntryPoint =
                EntryPointAccessors.fromActivity(it, ActivityScopeEntryPoint::class.java)
            wallpaperPreviewRepository = activityScopeEntryPoint.wallpaperPreviewRepository()
            wallpaperRepository = activityScopeEntryPoint.wallpaperRepository()
            interactor = activityScopeEntryPoint.basePreviewInteractor()
        }
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface ActivityScopeEntryPoint {
        fun wallpaperPreviewRepository(): WallpaperPreviewRepository

        fun wallpaperRepository(): WallpaperRepository

        fun basePreviewInteractor(): BasePreviewInteractor
    }

    @Test
    fun wallpapers_withHomeAndLockScreenAndPreviewWallpapers_shouldEmitPreview() {
        testScope.runTest {
            val homeStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "homeWallpaperId",
                    collectionId = "homeCollection",
                )
            val lockStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "lockWallpaperId",
                    collectionId = "lockCollection",
                )
            val previewStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "previewWallpaperId",
                    collectionId = "previewCollection",
                )

            // Current wallpaper models need to be set up before the view model is run.
            wallpaperClient.setCurrentWallpaperModels(
                homeStaticWallpaperModel,
                lockStaticWallpaperModel
            )
            wallpaperPreviewRepository.setWallpaperModel(previewStaticWallpaperModel)

            val actual = collectLastValue(interactor.wallpapers)()
            assertThat(actual).isNotNull()
            assertThat(actual).isEqualTo(WallpaperModelsPair(previewStaticWallpaperModel, null))
        }
    }

    @Test
    fun wallpapers_withHomeAndLockScreenAndNoPreviewWallpapers_shouldEmitCurrentHomeAndLock() {
        testScope.runTest {
            val homeStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "homeWallpaperId",
                    collectionId = "homeCollection",
                )
            val lockStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "lockWallpaperId",
                    collectionId = "lockCollection",
                )

            // Current wallpaper models need to be set up before the view model is run.
            wallpaperClient.setCurrentWallpaperModels(
                homeStaticWallpaperModel,
                lockStaticWallpaperModel
            )

            val actual = collectLastValue(interactor.wallpapers)()
            assertThat(actual).isNotNull()
            assertThat(actual)
                .isEqualTo(WallpaperModelsPair(homeStaticWallpaperModel, lockStaticWallpaperModel))
        }
    }
}
