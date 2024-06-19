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

package com.android.wallpaper.picker.preview.ui.viewmodel

import android.app.WallpaperColors
import android.app.WallpaperInfo
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.domain.interactor.WallpaperPreviewInteractor
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.testing.FakeLiveWallpaperDownloader
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
class StaticWallpaperPreviewViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private val testScope: TestScope = TestScope(testDispatcher)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var viewModel: StaticWallpaperPreviewViewModel
    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    private lateinit var wallpaperRepository: WallpaperRepository
    private lateinit var interactor: WallpaperPreviewInteractor

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var wallpaperPreferences: TestWallpaperPreferences
    @Inject lateinit var wallpaperClient: FakeWallpaperClient
    @Inject lateinit var liveWallpaperDownloader: FakeLiveWallpaperDownloader

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
        scenario.onActivity { setEverything(it) }
    }

    private fun setEverything(activity: PreviewTestActivity) {
        wallpaperRepository =
            WallpaperRepository(
                testScope.backgroundScope,
                wallpaperClient,
                wallpaperPreferences,
                testDispatcher,
            )
        wallpaperPreviewRepository = WallpaperPreviewRepository(wallpaperPreferences)
        interactor =
            WallpaperPreviewInteractor(
                wallpaperPreviewRepository,
                wallpaperRepository,
            )
        viewModel =
            StaticWallpaperPreviewViewModel(
                interactor,
                appContext,
                wallpaperPreferences,
                testDispatcher,
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
    fun staticWallpaperModel_withStaticWallpaper_shouldEmitNonNullValue() {
        testScope.runTest {
            val staticWallpaperModel = collectLastValue(viewModel.staticWallpaperModel)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )

            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)

            val actual = staticWallpaperModel()
            assertThat(actual).isNotNull()
            assertThat(actual).isEqualTo(testStaticWallpaperModel)
        }
    }

    @Test
    fun staticWallpaperModel_withLiveWallpaper_shouldNotEmit() {
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

            wallpaperPreviewRepository.setWallpaperModel(liveWallpaperModel)

            // Assert that no value is collected
            assertThat(staticWallpaperModel()).isNull()
        }
    }

    @Test
    fun lowResBitmap_withStaticWallpaper_shouldEmitNonNullValue() {
        testScope.runTest {
            val lowResBitmap = collectLastValue(viewModel.lowResBitmap)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )

            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)

            assertThat(lowResBitmap()).isNotNull()
            assertThat(lowResBitmap()).isInstanceOf(Bitmap::class.java)
        }
    }

    @Test
    fun fullResWallpaperViewModel_withStaticWallpaperAndNullCropHints_shouldEmitNonNullValue() {
        testScope.runTest {
            val fullResWallpaperViewModel = collectLastValue(viewModel.fullResWallpaperViewModel)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )

            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(fullResWallpaperViewModel()).isNotNull()
            assertThat(fullResWallpaperViewModel())
                .isInstanceOf(FullResWallpaperViewModel::class.java)
        }
    }

    @Test
    fun fullResWallpaperViewModel_withStaticWallpaperAndCropHints_shouldEmitNonNullValue() {
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

            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            viewModel.updateCropHintsInfo(cropHintsInfo)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(fullResWallpaperViewModel()).isNotNull()
            assertThat(fullResWallpaperViewModel())
                .isInstanceOf(FullResWallpaperViewModel::class.java)
            assertThat(fullResWallpaperViewModel()?.fullPreviewCropModels).isEqualTo(cropHintsInfo)
        }
    }

    @Test
    fun subsamplingScaleImageViewModel_withStaticWallpaperAndCropHints_shouldEmitNonNullValue() {
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

            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            viewModel.updateCropHintsInfo(cropHintsInfo)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(subsamplingScaleImageViewModel()).isNotNull()
            assertThat(subsamplingScaleImageViewModel())
                .isInstanceOf(FullResWallpaperViewModel::class.java)
            assertThat(subsamplingScaleImageViewModel()?.fullPreviewCropModels)
                .isEqualTo(cropHintsInfo)
        }
    }

    @Test
    fun wallpaperColors_withStoredColorsAndNullCropHints_returnsColorsStoredInPreferences() {
        testScope.runTest {
            val WALLPAPER_ID = "testWallpaperId"
            val storedWallpaperColors =
                WallpaperColors(
                    Color.valueOf(Color.RED),
                    Color.valueOf(Color.GREEN),
                    Color.valueOf(Color.BLUE)
                )
            val wallpaperColors = collectLastValue(viewModel.wallpaperColors)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = WALLPAPER_ID,
                    collectionId = "testCollection",
                )

            wallpaperPreferences.storeWallpaperColors(WALLPAPER_ID, storedWallpaperColors)
            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(wallpaperColors()).isInstanceOf(WallpaperColorsModel.Loaded::class.java)
            val actualWallpaperColors = (wallpaperColors() as WallpaperColorsModel.Loaded).colors
            assertThat(actualWallpaperColors).isNotNull()
            assertThat(actualWallpaperColors?.primaryColor)
                .isEqualTo(storedWallpaperColors.primaryColor)
            assertThat(actualWallpaperColors?.secondaryColor)
                .isEqualTo(storedWallpaperColors.secondaryColor)
            assertThat(actualWallpaperColors?.tertiaryColor)
                .isEqualTo(storedWallpaperColors.tertiaryColor)
            assertThat(actualWallpaperColors?.colorHints?.and(WallpaperColors.HINT_FROM_BITMAP))
                .isNotEqualTo(0)
        }
    }

    @Test
    fun wallpaperColors_withNoStoredColorsAndNullCropHints_returnsClientWallpaperColors() {
        testScope.runTest {
            val clientWallpaperColors =
                WallpaperColors(
                    Color.valueOf(Color.CYAN),
                    Color.valueOf(Color.MAGENTA),
                    Color.valueOf(Color.YELLOW)
                )
            val wallpaperColors = collectLastValue(viewModel.wallpaperColors)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollection",
                )

            wallpaperClient.setWallpaperColors(clientWallpaperColors)
            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(wallpaperColors()).isInstanceOf(WallpaperColorsModel.Loaded::class.java)
            val actualWallpaperColors = (wallpaperColors() as WallpaperColorsModel.Loaded).colors
            assertThat(actualWallpaperColors).isNotNull()
            assertThat(actualWallpaperColors?.primaryColor)
                .isEqualTo(clientWallpaperColors.primaryColor)
            assertThat(actualWallpaperColors?.secondaryColor)
                .isEqualTo(clientWallpaperColors.secondaryColor)
            assertThat(actualWallpaperColors?.tertiaryColor)
                .isEqualTo(clientWallpaperColors.tertiaryColor)
        }
    }

    @Test
    fun wallpaperColors_withStoredColorsAndNonNullCropHints_returnsClientWallpaperColors() {
        testScope.runTest {
            val WALLPAPER_ID = "testWallpaperId"
            val storedWallpaperColors =
                WallpaperColors(
                    Color.valueOf(Color.RED),
                    Color.valueOf(Color.GREEN),
                    Color.valueOf(Color.BLUE)
                )
            val clientWallpaperColors =
                WallpaperColors(
                    Color.valueOf(Color.CYAN),
                    Color.valueOf(Color.MAGENTA),
                    Color.valueOf(Color.YELLOW)
                )
            val wallpaperColors = collectLastValue(viewModel.wallpaperColors)
            val testStaticWallpaperModel =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = WALLPAPER_ID,
                    collectionId = "testCollection",
                )
            val cropHintsInfo =
                mapOf(
                    createPreviewCropModel(
                        displaySize = Point(1000, 1000),
                        cropHint = Rect(100, 200, 300, 400)
                    ),
                )

            wallpaperPreferences.storeWallpaperColors(WALLPAPER_ID, storedWallpaperColors)
            wallpaperClient.setWallpaperColors(clientWallpaperColors)
            viewModel.updateCropHintsInfo(cropHintsInfo)
            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(wallpaperColors()).isInstanceOf(WallpaperColorsModel.Loaded::class.java)
            val actualWallpaperColors = (wallpaperColors() as WallpaperColorsModel.Loaded).colors
            assertThat(actualWallpaperColors).isNotNull()
            assertThat(actualWallpaperColors?.primaryColor)
                .isEqualTo(clientWallpaperColors.primaryColor)
            assertThat(actualWallpaperColors?.secondaryColor)
                .isEqualTo(clientWallpaperColors.secondaryColor)
            assertThat(actualWallpaperColors?.tertiaryColor)
                .isEqualTo(clientWallpaperColors.tertiaryColor)
        }
    }

    @Test
    fun wallpaperColors_withNoStoredColorsAndNonNullCropHints_returnsClientWallpaperColors() {
        testScope.runTest {
            val clientWallpaperColors =
                WallpaperColors(
                    Color.valueOf(Color.CYAN),
                    Color.valueOf(Color.MAGENTA),
                    Color.valueOf(Color.YELLOW)
                )
            val wallpaperColors = collectLastValue(viewModel.wallpaperColors)
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

            wallpaperClient.setWallpaperColors(clientWallpaperColors)
            viewModel.updateCropHintsInfo(cropHintsInfo)
            wallpaperPreviewRepository.setWallpaperModel(testStaticWallpaperModel)
            // Run TestAsset.decodeRawDimensions & decodeBitmap handler.post to unblock assetDetail
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(wallpaperColors()).isInstanceOf(WallpaperColorsModel.Loaded::class.java)
            val actualWallpaperColors = (wallpaperColors() as WallpaperColorsModel.Loaded).colors
            assertThat(actualWallpaperColors).isNotNull()
            assertThat(actualWallpaperColors?.primaryColor)
                .isEqualTo(clientWallpaperColors.primaryColor)
            assertThat(actualWallpaperColors?.secondaryColor)
                .isEqualTo(clientWallpaperColors.secondaryColor)
            assertThat(actualWallpaperColors?.tertiaryColor)
                .isEqualTo(clientWallpaperColors.tertiaryColor)
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
