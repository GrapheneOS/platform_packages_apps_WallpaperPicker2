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

package com.android.wallpaper.picker.common.preview.ui.viewmodel

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.testing.collectLastValue
import com.android.wallpaper.util.WallpaperConnection
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
class BasePreviewViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var basePreviewViewModel: BasePreviewViewModel
    private lateinit var staticPreviewViewModel: StaticPreviewViewModel
    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    private lateinit var basePreviewViewModelFactory: BasePreviewViewModel.Factory

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
            basePreviewViewModelFactory = activityScopeEntryPoint.basePreviewViewModelFactory()
            basePreviewViewModel = basePreviewViewModelFactory.create(testScope.backgroundScope)
            staticPreviewViewModel = basePreviewViewModel.staticWallpaperPreviewViewModel
        }
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface ActivityScopeEntryPoint {
        fun wallpaperPreviewRepository(): WallpaperPreviewRepository

        fun basePreviewViewModelFactory(): BasePreviewViewModel.Factory
    }

    @Test
    fun wallpaper_setWallpaperModelAndWhichPreview_emitsMatchingValues() {
        testScope.runTest {
            val wallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            val whichPreview = WallpaperConnection.WhichPreview.PREVIEW_CURRENT

            wallpaperPreviewRepository.setWallpaperModel(wallpaperModel)
            basePreviewViewModel.setWhichPreview(whichPreview)

            val wallpaper = collectLastValue(basePreviewViewModel.wallpaper)()
            assertThat(wallpaper).isNotNull()
            val (actualWallpaperModel, actualWhichPreview) = wallpaper!!
            assertThat(actualWallpaperModel).isEqualTo(wallpaperModel)
            assertThat(actualWhichPreview).isEqualTo(whichPreview)
        }
    }
}
