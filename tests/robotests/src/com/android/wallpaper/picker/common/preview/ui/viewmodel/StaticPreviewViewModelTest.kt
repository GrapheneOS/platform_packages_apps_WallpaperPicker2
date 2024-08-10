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

import android.app.WallpaperInfo
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.model.Screen
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.common.preview.data.repository.BasePreviewRepository
import com.android.wallpaper.picker.common.preview.domain.interactor.BasePreviewInteractor
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestInjector
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class StaticPreviewViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private val testScope: TestScope = TestScope(testDispatcher)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var viewModel: StaticPreviewViewModel
    private lateinit var basePreviewRepository: BasePreviewRepository
    private lateinit var wallpaperRepository: WallpaperRepository
    private lateinit var interactor: BasePreviewInteractor

    @Inject @ApplicationContext lateinit var appContext: Context
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
            wallpaperRepository =
                WallpaperRepository(
                    testScope.backgroundScope,
                    wallpaperClient,
                    wallpaperPreferences,
                    testDispatcher,
                )
            basePreviewRepository = BasePreviewRepository()
            interactor =
                BasePreviewInteractor(
                    basePreviewRepository,
                    wallpaperRepository,
                )
            setViewModel(Screen.HOME_SCREEN)
        }
    }

    private fun setViewModel(screen: Screen) {
        viewModel =
            StaticPreviewViewModel(
                interactor,
                appContext,
                testDispatcher,
                screen,
                testScope.backgroundScope,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun staticWallpaperPreviewViewModel_isNotNull() {
        assertThat(viewModel).isNotNull()
    }

    @Test
    fun homeStaticWallpaperModel_withStaticHomeScreenAndNoPreviewWallpaper_shouldEmitHomeScreen() {
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
            setViewModel(Screen.HOME_SCREEN)

            val actual = collectLastValue(viewModel.staticWallpaperModel)()
            assertThat(actual).isNotNull()
            assertThat(actual).isEqualTo(homeStaticWallpaperModel)
        }
    }

    @Test
    fun homeStaticWallpaperModel_withLiveHomeScreenAndNoPreviewWallpaper_shouldEmitNull() {
        testScope.runTest {
            val resolveInfo =
                ResolveInfo().apply {
                    serviceInfo = ServiceInfo()
                    serviceInfo.packageName = "com.google.android.apps.wallpaper.nexus"
                    serviceInfo.splitName = "wallpaper_cities_ny"
                    serviceInfo.name = "NewYorkWallpaper"
                    serviceInfo.flags = PackageManager.GET_META_DATA
                }
            // ShadowWallpaperInfo allows the creation of this object
            val wallpaperInfo = WallpaperInfo(appContext, resolveInfo)
            val liveWallpaperModel =
                WallpaperModelUtils.getLiveWallpaperModel(
                    wallpaperId = "liveWallpaperId",
                    collectionId = "liveCollection",
                    systemWallpaperInfo = wallpaperInfo,
                )

            // Current wallpaper models need to be set up before the view model is run.
            wallpaperClient.setCurrentWallpaperModels(liveWallpaperModel, null)
            setViewModel(Screen.HOME_SCREEN)

            val actual = collectLastValue(viewModel.staticWallpaperModel)()
            assertThat(actual).isNull()
        }
    }

    @Test
    fun lockStaticWallpaperModel_withStaticLockScreenAndNoPreviewWallpaper_shouldEmitLockScreen() {
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
            setViewModel(Screen.LOCK_SCREEN)

            val actual = collectLastValue(viewModel.staticWallpaperModel)()
            assertThat(actual).isNotNull()
            assertThat(actual).isEqualTo(lockStaticWallpaperModel)
        }
    }

    @Test
    fun lockStaticWallpaperModel_withNullLockScreenAndNoPreviewWallpaper_shouldEmitNull() {
        testScope.runTest {
            val homeStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "homeWallpaperId",
                    collectionId = "homeCollection",
                )

            // Current wallpaper models need to be set up before the view model is run.
            wallpaperClient.setCurrentWallpaperModels(homeStaticWallpaperModel, null)
            setViewModel(Screen.LOCK_SCREEN)

            val actual = collectLastValue(viewModel.staticWallpaperModel)()
            assertThat(actual).isNull()
        }
    }

    @Test
    fun staticWallpaperModel_withStaticPreview_shouldEmitNonNullValue() {
        testScope.runTest {
            val staticWallpaperModel = collectLastValue(viewModel.staticWallpaperModel)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )

            basePreviewRepository.setWallpaperModel(testStaticWallpaperModel)

            val actual = staticWallpaperModel()
            assertThat(actual).isNotNull()
            assertThat(actual).isEqualTo(testStaticWallpaperModel)
        }
    }

    @Test
    fun staticWallpaperModel_withLivePreview_shouldEmitNull() {
        testScope.runTest {
            val staticWallpaperModel = collectLastValue(viewModel.staticWallpaperModel)
            val resolveInfo =
                ResolveInfo().apply {
                    serviceInfo = ServiceInfo()
                    serviceInfo.packageName = "com.google.android.apps.wallpaper.nexus"
                    serviceInfo.splitName = "wallpaper_cities_ny"
                    serviceInfo.name = "NewYorkWallpaper"
                    serviceInfo.flags = PackageManager.GET_META_DATA
                }
            // ShadowWallpaperInfo allows the creation of this object
            val wallpaperInfo = WallpaperInfo(appContext, resolveInfo)
            val liveWallpaperModel =
                WallpaperModelUtils.getLiveWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                    systemWallpaperInfo = wallpaperInfo,
                )

            basePreviewRepository.setWallpaperModel(liveWallpaperModel)

            // Assert that no value is collected
            assertThat(staticWallpaperModel()).isNull()
        }
    }

    @Test
    fun staticWallpaperModel_setModelWithCropHints_shouldUpdateCropHintsInfo() {
        testScope.runTest {
            val cropHints = listOf(Point(1000, 1000) to Rect(100, 200, 300, 400))
            val cropHintsInfo = cropHints.associate { createPreviewCropModel(it.first, it.second) }
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                    cropHints = cropHints.toMap()
                )
            // Create an empty collector for the wallpaper model so the flow runs
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.staticWallpaperModel.collect {}
            }

            basePreviewRepository.setWallpaperModel(testStaticWallpaperModel)

            assertThat(viewModel.cropHintsInfo.value).isNotNull()
            assertThat(viewModel.cropHintsInfo.value).containsExactlyEntriesIn(cropHintsInfo)
        }
    }

    @Test
    fun staticWallpaperModel_setModelWithCropHintsTwice_shouldClearPreviousCropHintsInfo() {
        testScope.runTest {
            val cropHints1 = listOf(Point(1000, 1000) to Rect(100, 200, 300, 400))
            val cropHints2 = listOf(Point(1500, 1500) to Rect(200, 400, 600, 800))
            val cropHintsInfo = cropHints2.associate { createPreviewCropModel(it.first, it.second) }
            val testStaticWallpaperModel1 =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                    cropHints = cropHints1.toMap()
                )
            val testStaticWallpaperModel2 =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                    cropHints = cropHints2.toMap()
                )
            // Create an empty collector for the wallpaper model so the flow runs
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.staticWallpaperModel.collect {}
            }

            basePreviewRepository.setWallpaperModel(testStaticWallpaperModel1)
            basePreviewRepository.setWallpaperModel(testStaticWallpaperModel2)

            assertThat(viewModel.cropHintsInfo.value).isNotNull()
            assertThat(viewModel.cropHintsInfo.value).containsExactlyEntriesIn(cropHintsInfo)
        }
    }

    @Test
    fun lowResBitmap_withStaticPreview_shouldEmitNonNullValue() {
        testScope.runTest {
            val lowResBitmap = collectLastValue(viewModel.lowResBitmap)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )

            basePreviewRepository.setWallpaperModel(testStaticWallpaperModel)

            assertThat(lowResBitmap()).isNotNull()
            assertThat(lowResBitmap()).isInstanceOf(Bitmap::class.java)
        }
    }

    @Test
    fun fullResWallpaperViewModel_withStaticPreviewAndNullCropHints_shouldEmitNonNullValue() {
        testScope.runTest {
            val fullResWallpaperViewModel = collectLastValue(viewModel.fullResWallpaperViewModel)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )

            basePreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(fullResWallpaperViewModel()).isNotNull()
            assertThat(fullResWallpaperViewModel())
                .isInstanceOf(FullResWallpaperViewModel::class.java)
        }
    }

    @Test
    fun fullResWallpaperViewModel_withStaticPreviewAndCropHints_shouldEmitNonNullValue() {
        testScope.runTest {
            val fullResWallpaperViewModel = collectLastValue(viewModel.fullResWallpaperViewModel)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )
            val cropHintsInfo =
                mapOf(
                    createPreviewCropModel(
                        displaySize = Point(1000, 1000),
                        cropHint = Rect(100, 200, 300, 400)
                    ),
                )

            basePreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            viewModel.updateCropHintsInfo(cropHintsInfo)

            assertThat(fullResWallpaperViewModel()).isNotNull()
            assertThat(fullResWallpaperViewModel())
                .isInstanceOf(FullResWallpaperViewModel::class.java)
            assertThat(fullResWallpaperViewModel()?.fullPreviewCropModels).isEqualTo(cropHintsInfo)
        }
    }

    @Test
    fun subsamplingScaleImageViewModel_withStaticPreviewAndCropHints_shouldEmitNonNullValue() {
        testScope.runTest {
            val subsamplingScaleImageViewModel =
                collectLastValue(viewModel.subsamplingScaleImageViewModel)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )
            val cropHintsInfo =
                mapOf(
                    createPreviewCropModel(
                        displaySize = Point(1000, 1000),
                        cropHint = Rect(100, 200, 300, 400)
                    ),
                )

            basePreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            viewModel.updateCropHintsInfo(cropHintsInfo)

            assertThat(subsamplingScaleImageViewModel()).isNotNull()
            assertThat(subsamplingScaleImageViewModel())
                .isInstanceOf(FullResWallpaperViewModel::class.java)
            assertThat(subsamplingScaleImageViewModel()?.fullPreviewCropModels)
                .isEqualTo(cropHintsInfo)
        }
    }

    @Test
    fun updateCropHintsInfo_updateDefaultCropTrue_onlyAddsNewCropHints() {
        val cropHintA =
            createPreviewCropModel(
                displaySize = Point(1000, 1000),
                cropHint = Rect(100, 200, 300, 400)
            )
        val cropHintB =
            createPreviewCropModel(
                displaySize = Point(500, 1500),
                cropHint = Rect(100, 100, 100, 100)
            )
        val cropHintB2 =
            createPreviewCropModel(
                displaySize = Point(500, 1500),
                cropHint = Rect(400, 300, 200, 100)
            )
        val cropHintC =
            createPreviewCropModel(
                displaySize = Point(400, 600),
                cropHint = Rect(200, 200, 200, 200)
            )
        val cropHintsInfo = mapOf(cropHintA, cropHintB)
        val additionalCropHintsInfo = mapOf(cropHintB2, cropHintC)
        val expectedCropHintsInfo = mapOf(cropHintA, cropHintB, cropHintC)

        viewModel.updateCropHintsInfo(cropHintsInfo)
        assertThat(viewModel.fullPreviewCropModels).containsExactlyEntriesIn(cropHintsInfo)
        viewModel.updateCropHintsInfo(additionalCropHintsInfo, updateDefaultCrop = true)
        assertThat(viewModel.fullPreviewCropModels).containsExactlyEntriesIn(expectedCropHintsInfo)
    }

    @Test
    fun updateCropHintsInfo_updateDefaultCropFalse_addsAndReplacesPreviousCropHints() {
        val cropHintA =
            createPreviewCropModel(
                displaySize = Point(1000, 1000),
                cropHint = Rect(100, 200, 300, 400)
            )
        val cropHintB =
            createPreviewCropModel(
                displaySize = Point(500, 1500),
                cropHint = Rect(100, 100, 100, 100)
            )
        val cropHintB2 =
            createPreviewCropModel(
                displaySize = Point(500, 1500),
                cropHint = Rect(400, 300, 200, 100)
            )
        val cropHintC =
            createPreviewCropModel(
                displaySize = Point(400, 600),
                cropHint = Rect(200, 200, 200, 200)
            )
        val cropHintsInfo = mapOf(cropHintA, cropHintB)
        val additionalCropHintsInfo = mapOf(cropHintB2, cropHintC)
        val expectedCropHintsInfo = mapOf(cropHintA, cropHintB2, cropHintC)

        viewModel.updateCropHintsInfo(cropHintsInfo)
        assertThat(viewModel.fullPreviewCropModels).containsExactlyEntriesIn(cropHintsInfo)
        viewModel.updateCropHintsInfo(additionalCropHintsInfo, updateDefaultCrop = false)
        assertThat(viewModel.fullPreviewCropModels).containsExactlyEntriesIn(expectedCropHintsInfo)
    }

    @Test
    fun updateDefaultCropModel_existingDisplaySize_resultsInNoUpdates() {
        val cropHintA =
            createPreviewCropModel(
                displaySize = Point(1000, 1000),
                cropHint = Rect(100, 200, 300, 400)
            )
        val cropHintB =
            createPreviewCropModel(
                displaySize = Point(500, 1500),
                cropHint = Rect(100, 100, 100, 100)
            )
        val cropHintB2 =
            createPreviewCropModel(
                displaySize = Point(500, 1500),
                cropHint = Rect(400, 300, 200, 100)
            )
        val cropHintsInfo = mapOf(cropHintA, cropHintB)

        viewModel.updateCropHintsInfo(cropHintsInfo)
        assertThat(viewModel.fullPreviewCropModels).containsExactlyEntriesIn(cropHintsInfo)
        viewModel.updateDefaultPreviewCropModel(cropHintB2.first, cropHintB2.second)
        assertThat(viewModel.fullPreviewCropModels).containsExactlyEntriesIn(cropHintsInfo)
    }

    @Test
    fun updateDefaultCropModel_newDisplaySize_addsNewDisplaySize() {
        val cropHintA =
            createPreviewCropModel(
                displaySize = Point(1000, 1000),
                cropHint = Rect(100, 200, 300, 400)
            )
        val cropHintB =
            createPreviewCropModel(
                displaySize = Point(500, 1500),
                cropHint = Rect(100, 100, 100, 100)
            )
        val cropHintC =
            createPreviewCropModel(
                displaySize = Point(400, 600),
                cropHint = Rect(200, 200, 200, 200)
            )
        val cropHintsInfo = mapOf(cropHintA, cropHintB)
        val expectedCropHintsInfo = mapOf(cropHintA, cropHintB, cropHintC)

        viewModel.updateCropHintsInfo(cropHintsInfo)
        assertThat(viewModel.fullPreviewCropModels).containsExactlyEntriesIn(cropHintsInfo)
        viewModel.updateDefaultPreviewCropModel(cropHintC.first, cropHintC.second)
        assertThat(viewModel.fullPreviewCropModels).containsExactlyEntriesIn(expectedCropHintsInfo)
    }

    private fun createPreviewCropModel(
        displaySize: Point,
        cropHint: Rect
    ): Pair<Point, FullPreviewCropModel> {
        return Pair(
            displaySize,
            FullPreviewCropModel(
                cropHint = cropHint,
                cropSizeModel = null,
            ),
        )
    }
}
