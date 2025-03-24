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

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.WallpaperInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityManager
import androidx.activity.viewModels
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.effects.FakeEffectsController
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.BasePreviewActivity.EXTRA_VIEW_AS_HOME
import com.android.wallpaper.picker.BasePreviewActivity.EXTRA_WALLPAPER_INFO
import com.android.wallpaper.picker.BasePreviewActivity.IS_ASSET_ID_PRESENT
import com.android.wallpaper.picker.BasePreviewActivity.IS_NEW_TASK
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.di.modules.HomeScreenPreviewUtils
import com.android.wallpaper.picker.di.modules.LockScreenPreviewUtils
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.Companion.PreviewScreen
import com.android.wallpaper.testing.FakeContentProvider
import com.android.wallpaper.testing.FakeDisplaysProvider
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.FOLDABLE_UNFOLDED_LAND
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.HANDHELD
import com.android.wallpaper.testing.FakeImageEffectsRepository
import com.android.wallpaper.testing.FakeLiveWallpaperDownloader
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.WallpaperInfoUtils
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.testing.collectLastValue
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowLooper

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class WallpaperPreviewViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var wallpaperPreviewViewModel: WallpaperPreviewViewModel
    private lateinit var staticWallpapaperPreviewViewModel: StaticWallpaperPreviewViewModel
    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    private lateinit var startActivityIntent: Intent
    private lateinit var effectsWallpaperInfo: WallpaperInfo
    private lateinit var accessibilityManager: AccessibilityManager

    @HomeScreenPreviewUtils private lateinit var homePreviewUtils: PreviewUtils
    @LockScreenPreviewUtils private lateinit var lockPreviewUtils: PreviewUtils
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope
    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var wallpaperDownloader: FakeLiveWallpaperDownloader
    @Inject lateinit var wallpaperPreferences: TestWallpaperPreferences
    @Inject lateinit var wallpaperClient: FakeWallpaperClient
    @Inject lateinit var contentProvider: FakeContentProvider
    @Inject lateinit var imageEffectsRepository: FakeImageEffectsRepository
    @Inject lateinit var displaysProvider: FakeDisplaysProvider

    @Before
    fun setUp() {
        hiltRule.inject()

        InjectorProvider.setInjector(testInjector)
        Dispatchers.setMain(testDispatcher)

        // Register our testing activity since it's not in the manifest
        val pm = Shadows.shadowOf(appContext.packageManager)
        val activityInfo =
            ActivityInfo().apply {
                name = PreviewTestActivity::class.java.name
                packageName = appContext.packageName
            }
        pm.addOrUpdateActivity(activityInfo)

        accessibilityManager = spy(appContext.getSystemService(AccessibilityManager::class.java))!!

        // Register a fake the content provider
        ShadowContentResolver.registerProviderInternal(
            FakeEffectsController.AUTHORITY,
            contentProvider,
        )

        effectsWallpaperInfo = WallpaperInfoUtils.createWallpaperInfo(context = appContext)

        startActivityIntent =
            Intent.makeMainActivity(ComponentName(appContext, PreviewTestActivity::class.java))
        startActivityForTesting()
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface ActivityScopeEntryPoint {
        @HomeScreenPreviewUtils fun homePreviewUtils(): PreviewUtils

        @LockScreenPreviewUtils fun lockPreviewUtils(): PreviewUtils

        fun wallpaperPreviewRepository(): WallpaperPreviewRepository
    }

    private fun setEverything(activity: PreviewTestActivity) {
        val activityScopeEntryPoint =
            EntryPointAccessors.fromActivity(activity, ActivityScopeEntryPoint::class.java)
        homePreviewUtils = activityScopeEntryPoint.homePreviewUtils()
        lockPreviewUtils = activityScopeEntryPoint.lockPreviewUtils()
        wallpaperPreviewRepository = activityScopeEntryPoint.wallpaperPreviewRepository()

        wallpaperPreviewViewModel = activity.viewModels<WallpaperPreviewViewModel>().value
        staticWallpapaperPreviewViewModel =
            wallpaperPreviewViewModel.staticWallpaperPreviewViewModel
    }

    @Test
    fun testBackPress_onFullPreviewScreen() =
        testScope.runTest {
            val currentPreviewScreen =
                collectLastValue(wallpaperPreviewViewModel.currentPreviewScreen)

            val handled = wallpaperPreviewViewModel.handleBackPressed()

            assertThat(handled).isFalse()
            assertThat(currentPreviewScreen()).isEqualTo(PreviewScreen.SMALL_PREVIEW)
        }

    @Test
    fun testBackPress_onApplyWallpaperScreen() =
        testScope.runTest {
            val currentPreviewScreen =
                collectLastValue(wallpaperPreviewViewModel.currentPreviewScreen)
            val onNextButtonClicked =
                collectLastValue(wallpaperPreviewViewModel.onNextButtonClicked)
            val model =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            wallpaperPreviewRepository.setWallpaperModel(model)
            executePendingWork(this)
            // Navigates to apply wallpaper screen
            onNextButtonClicked()?.invoke()

            val handled = wallpaperPreviewViewModel.handleBackPressed()

            assertThat(handled).isTrue()
            assertThat(currentPreviewScreen()).isEqualTo(PreviewScreen.SMALL_PREVIEW)
        }

    @Test
    fun onApplyWallpaperScreen_shouldEnableClickOnPager() =
        testScope.runTest {
            val shouldEnableClickOnPager =
                collectLastValue(wallpaperPreviewViewModel.shouldEnableClickOnPager)
            val onNextButtonClicked =
                collectLastValue(wallpaperPreviewViewModel.onNextButtonClicked)
            val model =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            wallpaperPreviewRepository.setWallpaperModel(model)
            executePendingWork(this)
            // Navigates to apply wallpaper screen
            onNextButtonClicked()?.invoke()

            assertThat(shouldEnableClickOnPager()).isTrue()
        }

    @Ignore("b/367372434: test shouldEnableClickOnPager when implementing full preview")
    @Test
    fun onFullPreviewScreen_shouldNotEnableClickOnPager() = testScope.runTest {}

    @Test
    fun clickNextButton_setsApplyWallpaperScreen() =
        testScope.runTest {
            val onNextButtonClicked =
                collectLastValue(wallpaperPreviewViewModel.onNextButtonClicked)
            val model =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            wallpaperPreviewRepository.setWallpaperModel(model)
            executePendingWork(this)

            onNextButtonClicked()?.invoke()

            assertThat(wallpaperPreviewViewModel.currentPreviewScreen.value)
                .isEqualTo(PreviewScreen.APPLY_WALLPAPER)
        }

    @Test
    fun clickCancelButton_setsSmallPreviewScreen() =
        testScope.runTest {
            val onCancelButtonClicked =
                collectLastValue(wallpaperPreviewViewModel.onCancelButtonClicked)
            val onNextButtonClicked =
                collectLastValue(wallpaperPreviewViewModel.onNextButtonClicked)
            val model =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            wallpaperPreviewRepository.setWallpaperModel(model)
            executePendingWork(this)
            // Navigates to apply wallpaper screen
            onNextButtonClicked()?.invoke()

            onCancelButtonClicked()?.invoke()

            assertThat(wallpaperPreviewViewModel.currentPreviewScreen.value)
                .isEqualTo(PreviewScreen.SMALL_PREVIEW)
        }

    @Test
    fun navigatesUpOnApplyWallpaperScreen_setsSmallPreviewScreen() =
        testScope.runTest {
            val onNextButtonClicked =
                collectLastValue(wallpaperPreviewViewModel.onNextButtonClicked)
            val model =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            wallpaperPreviewRepository.setWallpaperModel(model)
            executePendingWork(this)
            // Navigates to apply wallpaper screen
            onNextButtonClicked()?.invoke()

            val shouldHandleBackPress = wallpaperPreviewViewModel.handleBackPressed()

            assertThat(shouldHandleBackPress).isTrue()
            assertThat(wallpaperPreviewViewModel.currentPreviewScreen.value)
                .isEqualTo(PreviewScreen.SMALL_PREVIEW)
        }

    @Test
    fun startActivity_withViewAsHome_setsToViewModel() {
        startActivityForTesting(isViewAsHome = true)

        assertThat(wallpaperPreviewViewModel.isViewAsHome).isTrue()
    }

    @Test
    fun smallPreviewClickable_byDefault() =
        testScope.runTest {
            val isClickable = collectLastValue(wallpaperPreviewViewModel.isSmallPreviewClickable)()

            assertThat(isClickable).isTrue()
        }

    @Test
    fun smallPreviewNotClickable_whenEffectInProgress() =
        testScope.runTest {
            imageEffectsRepository.imageEffectsModel.value =
                ImageEffectsModel(EffectStatus.EFFECT_APPLY_IN_PROGRESS)

            val isClickable = collectLastValue(wallpaperPreviewViewModel.isSmallPreviewClickable)()

            assertThat(isClickable).isFalse()
        }

    @Test
    fun clickSmallPreview_isSelectedPreview_updatesFullWorkspacePreviewConfig() =
        testScope.runTest {
            wallpaperPreviewViewModel.setSmallPreviewSelectedTab(Screen.HOME_SCREEN)
            val onHomePreviewClicked =
                collectLastValue(
                    wallpaperPreviewViewModel.onSmallPreviewClicked(
                        Screen.HOME_SCREEN,
                        DeviceDisplayType.UNFOLDED,
                    ) {}
                )

            onHomePreviewClicked()?.invoke()

            val config =
                collectLastValue(wallpaperPreviewViewModel.fullWorkspacePreviewConfigViewModel)()
            assertThat(config?.deviceDisplayType).isEqualTo(DeviceDisplayType.UNFOLDED)
            assertThat(config?.previewUtils).isEqualTo(homePreviewUtils)
        }

    @Test
    fun clickSmallPreview_isSelectedPreview_updatesFullWallpaperPreviewConfig() =
        testScope.runTest {
            val model = WallpaperModelUtils.getStaticWallpaperModel("testId", "testCollection")
            updateFullWallpaperFlow(model, WhichPreview.PREVIEW_CURRENT, listOf(HANDHELD))
            wallpaperPreviewViewModel.setSmallPreviewSelectedTab(Screen.LOCK_SCREEN)
            val onLockPreviewClicked =
                collectLastValue(
                    wallpaperPreviewViewModel.onSmallPreviewClicked(
                        Screen.LOCK_SCREEN,
                        DeviceDisplayType.SINGLE,
                    ) {}
                )

            onLockPreviewClicked()?.invoke()

            val fullWallpaper = collectLastValue(wallpaperPreviewViewModel.fullWallpaper)()
            assertThat(fullWallpaper).isNotNull()
            fullWallpaper?.run {
                assertThat(config.deviceDisplayType).isEqualTo(DeviceDisplayType.SINGLE)
                assertThat(config.screen).isEqualTo(Screen.LOCK_SCREEN)
                assertThat(wallpaper).isEqualTo(model)
                assertThat(displaySize).isEqualTo(HANDHELD.displaySize)
                assertThat(allowUserCropping).isTrue()
                assertThat(whichPreview).isEqualTo(WhichPreview.PREVIEW_CURRENT)
            }
        }

    @Test
    fun clickSmallPreview_isNotSelectedPreview_doesNotUpdateFullWorkspacePreviewConfig() =
        testScope.runTest {
            wallpaperPreviewViewModel.setSmallPreviewSelectedTab(Screen.LOCK_SCREEN)
            val onHomePreviewClicked =
                collectLastValue(
                    wallpaperPreviewViewModel.onSmallPreviewClicked(
                        Screen.HOME_SCREEN,
                        DeviceDisplayType.UNFOLDED,
                    ) {}
                )

            onHomePreviewClicked()?.invoke()

            val config =
                collectLastValue(wallpaperPreviewViewModel.fullWorkspacePreviewConfigViewModel)()
            // Make sure flow does not emit.
            assertThat(config).isNull()
        }

    @Test
    fun clickCropButton_updatesCropHintsInfo() =
        testScope.runTest {
            val newCropRect = Rect(10, 10, 10, 10)
            val model =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            updateFullWallpaperFlow(
                model,
                WhichPreview.PREVIEW_CURRENT,
                listOf(FOLDABLE_UNFOLDED_LAND),
            )
            wallpaperPreviewViewModel.setDefaultFullPreviewConfigViewModel(
                DeviceDisplayType.UNFOLDED
            )

            // Set a crop and confirm via clicking button
            wallpaperPreviewViewModel.staticWallpaperPreviewViewModel.fullPreviewCropModels[
                    FOLDABLE_UNFOLDED_LAND.displaySize] =
                FullPreviewCropModel(cropHint = newCropRect, cropSizeModel = null)
            collectLastValue(wallpaperPreviewViewModel.onCropButtonClick)()?.invoke()

            val cropHintsInfo =
                wallpaperPreviewViewModel.staticWallpaperPreviewViewModel.cropHintsInfo.value
            assertThat(cropHintsInfo).containsKey(FOLDABLE_UNFOLDED_LAND.displaySize)
            assertThat(cropHintsInfo?.get(FOLDABLE_UNFOLDED_LAND.displaySize)?.cropHint)
                .isEqualTo(newCropRect)
        }

    @Test
    fun previewLiveWallpaper_disablesCropping() =
        testScope.runTest {
            val model =
                WallpaperModelUtils.getLiveWallpaperModel(
                    wallpaperId = "testWallpaperId",
                    collectionId = "testCollectionId",
                    systemWallpaperInfo = effectsWallpaperInfo,
                )
            updateFullWallpaperFlow(model, WhichPreview.PREVIEW_CURRENT, listOf(HANDHELD))

            wallpaperPreviewViewModel.onSmallPreviewClicked(
                Screen.HOME_SCREEN,
                DeviceDisplayType.SINGLE,
            ) {}

            val onCropButtonClick = collectLastValue(wallpaperPreviewViewModel.onCropButtonClick)()
            assertThat(onCropButtonClick).isNull()
        }

    @Test
    fun clickSetWallpaperButton_showsSetWallpaperDialog() =
        testScope.runTest {
            val onSetWallpaperButtonClicked =
                collectLastValue(wallpaperPreviewViewModel.onSetWallpaperButtonClicked)
            val newCropRect = Rect(10, 10, 10, 10)
            val model =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            wallpaperPreviewRepository.setWallpaperModel(model)
            wallpaperPreviewViewModel.staticWallpaperPreviewViewModel.updateCropHintsInfo(
                mapOf(
                    FOLDABLE_UNFOLDED_LAND.displaySize to
                        FullPreviewCropModel(cropHint = newCropRect, cropSizeModel = null)
                )
            )
            executePendingWork(this)

            onSetWallpaperButtonClicked()?.invoke()

            val showDialog = collectLastValue(wallpaperPreviewViewModel.showSetWallpaperDialog)()
            assertThat(showDialog).isTrue()
        }

    @Test
    fun accessibilityEnabled_ignoresGeneric() {
        testScope.runTest {
            val feedbackTypeCaptor = ArgumentCaptor.forClass(Int::class.java)
            val enabled = wallpaperPreviewViewModel.isAccessibilityEnabled(accessibilityManager)
            verify(accessibilityManager)
                .getEnabledAccessibilityServiceList(feedbackTypeCaptor.capture())
            // Verify that we don't include FEEDBACK_GENERIC when asking about A11y services
            assertThat(feedbackTypeCaptor.value and AccessibilityServiceInfo.FEEDBACK_GENERIC)
                .isEqualTo(0)
            assertThat(enabled).isFalse()
        }
    }

    /**
     * Updates all upstream flows of [WallpaperPreviewViewModel.fullWallpaper] except
     * [WallpaperPreviewViewModel.fullPreviewConfigViewModel].
     *
     * Restarting activity and view model to apply the new display.
     */
    private fun updateFullWallpaperFlow(
        model: WallpaperModel,
        whichPreview: WhichPreview,
        internalDisplays: List<FakeDisplaysProvider.FakeDisplayConfig>,
    ) {
        // Restart activity and view model to apply the new display.
        displaysProvider.setDisplays(internalDisplays)
        scenario = ActivityScenario.launch(PreviewTestActivity::class.java)
        scenario.onActivity { setEverything(it) }
        wallpaperPreviewRepository.setWallpaperModel(model)
        wallpaperPreviewViewModel.setWhichPreview(whichPreview)
    }

    /** Launches the test activity to set remaining test objects */
    private fun startActivityForTesting(
        wallpaperInfo: WallpaperInfo? = null,
        isViewAsHome: Boolean? = null,
        isAssetIdPresent: Boolean? = null,
        isNewTask: Boolean? = null,
    ) {
        scenario =
            ActivityScenario.launch(
                startActivityIntent.apply {
                    wallpaperInfo?.let { putExtra(EXTRA_WALLPAPER_INFO, it) }
                    isViewAsHome?.let { putExtra(EXTRA_VIEW_AS_HOME, it) }
                    isAssetIdPresent?.let { putExtra(IS_ASSET_ID_PRESENT, it) }
                    isNewTask?.let { putExtra(IS_NEW_TASK, it) }
                }
            )
        scenario.onActivity { setEverything(it) }
    }

    private fun executePendingWork(testScope: TestScope) {
        // Run suspendCancellableCoroutine in assetDetail's decodeRawDimensions
        testScope.runCurrent()
        // Run handler.post in TestAsset.decodeRawDimensions
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        // Run suspendCancellableCoroutine in assetDetail's decodeBitmap
        testScope.runCurrent()
        // Run handler.post in TestAsset.decodeBitmap
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
