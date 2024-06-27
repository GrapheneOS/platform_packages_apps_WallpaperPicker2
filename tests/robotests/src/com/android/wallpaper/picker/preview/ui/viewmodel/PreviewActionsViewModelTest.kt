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

import android.app.WallpaperInfo
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.FakeEffectsController
import com.android.wallpaper.picker.data.CreativeWallpaperData
import com.android.wallpaper.picker.preview.data.repository.CreativeEffectsRepository
import com.android.wallpaper.picker.preview.data.repository.DownloadableWallpaperRepository
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.domain.interactor.PreviewActionsInteractor
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.picker.preview.ui.util.LiveWallpaperDeleteUtil
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
class PreviewActionsViewModelTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    private lateinit var underTest: PreviewActionsViewModel

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var wallpaperPreferences: TestWallpaperPreferences
    @Inject lateinit var imageEffectsRepository: FakeImageEffectsRepository
    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var liveWallpaperDownloader: FakeLiveWallpaperDownloader
    @Inject lateinit var liveWallpaperDeleteUtil: LiveWallpaperDeleteUtil

    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        wallpaperPreviewRepository = WallpaperPreviewRepository(wallpaperPreferences)
        underTest =
            PreviewActionsViewModel(
                PreviewActionsInteractor(
                    wallpaperPreviewRepository,
                    imageEffectsRepository,
                    CreativeEffectsRepository(appContext, testDispatcher),
                    DownloadableWallpaperRepository(liveWallpaperDownloader),
                ),
                liveWallpaperDeleteUtil,
                appContext,
            )
    }

    @Test
    fun informationClicked_preparesInformationFloatingSheet() = runTest {
        val model = WallpaperModelUtils.getStaticWallpaperModel("testId", "testCollection")
        wallpaperPreviewRepository.setWallpaperModel(model)

        // Simulate click of info button
        collectLastValue(underTest.onInformationClicked)()?.invoke()

        val preview = collectLastValue(underTest.previewFloatingSheetViewModel)()
        assertThat(preview?.informationFloatingSheetViewModel).isNotNull()
    }

    @Test
    fun isInformationVisible_checksIfInformationButtonIsVisible() = runTest {
        val model = WallpaperModelUtils.getStaticWallpaperModel("testId", "testCollection")
        wallpaperPreviewRepository.setWallpaperModel(model)

        val isInformationButtonVisible = collectLastValue(underTest.isInformationVisible)
        assertThat(isInformationButtonVisible()).isTrue()
    }

    @Test
    fun isInformationVisible_invisibleWhenActionUrlNull() = runTest {
        val model = WallpaperModelUtils.getStaticWallpaperModel("testId", "testCollection")
        wallpaperPreviewRepository.setWallpaperModel(model)

        val isInformationButtonVisible = collectLastValue(underTest.isInformationVisible)

        wallpaperPreviewRepository.setWallpaperModel(
            WallpaperModelUtils.getStaticWallpaperModel(
                "testId",
                "testCollection",
                actionUrl = null
            )
        )
        assertThat(isInformationButtonVisible()).isFalse()
    }

    @Test
    fun isInformationChecked_checksIfInformationButtonIsChecked() = runTest {
        val model = WallpaperModelUtils.getStaticWallpaperModel("testId", "testCollection")
        wallpaperPreviewRepository.setWallpaperModel(model)

        val isInformationButtonChecked = collectLastValue(underTest.isInformationChecked)
        assertThat(isInformationButtonChecked()).isFalse()

        collectLastValue(underTest.onInformationClicked)()?.invoke()

        assertThat(isInformationButtonChecked()).isTrue()
    }

    @Test
    fun imageEffectSet_preparesImageEffectFloatingSheet() = runTest {
        val model = WallpaperModelUtils.getStaticWallpaperModel("testId", "testCollection")
        wallpaperPreviewRepository.setWallpaperModel(model)
        val effect =
            Effect(id = 1, title = "test effect", type = FakeEffectsController.Effect.FAKE_EFFECT)
        imageEffectsRepository.wallpaperEffect.value = effect
        val imageEffectsModel = ImageEffectsModel(status = EffectStatus.EFFECT_READY)
        imageEffectsRepository.imageEffectsModel.value = imageEffectsModel

        // Simulate click of effects button
        collectLastValue(underTest.onEffectsClicked)()?.invoke()

        val preview = collectLastValue(underTest.previewFloatingSheetViewModel)()
        assertThat(preview?.imageEffectFloatingSheetViewModel).isNotNull()
    }

    @Test
    fun isDownloadVisible_preparesDownloadableWallpaperData() = runTest {
        val isDownloadVisible = collectLastValue(underTest.isDownloadVisible)

        liveWallpaperDownloader.initiateDownloadableServiceByPass()

        assertThat(isDownloadVisible()).isTrue()
    }

    @Test
    fun isDownloadButtonEnabled_trueWhenDownloading() = runTest {
        val isDownloadButtonEnabled = collectLastValue(underTest.isDownloadButtonEnabled)

        liveWallpaperDownloader.initiateDownloadableServiceByPass()

        assertThat(isDownloadButtonEnabled()).isTrue()
    }

    @Test
    fun isDeleteVisible_whenWallpaperCanBeDeleted() = runTest {
        val resolveInfo =
            ResolveInfo().apply {
                serviceInfo = ServiceInfo()
                serviceInfo.packageName = "com.google.android.apps.wallpaper.nexus"
                serviceInfo.splitName = "wallpaper_cities_ny"
                serviceInfo.name = "NewYorkWallpaper"
                serviceInfo.flags = PackageManager.GET_META_DATA
            }
        val wallpaperInfo = WallpaperInfo(appContext, resolveInfo)
        val liveWallpaperModel =
            WallpaperModelUtils.getLiveWallpaperModel(
                wallpaperId = "testWallpaperId",
                collectionId = "testCollection",
                systemWallpaperInfo = wallpaperInfo,
                isApplied = false,
                creativeWallpaperData =
                    CreativeWallpaperData(
                        configPreviewUri = null,
                        cleanPreviewUri = null,
                        deleteUri = Uri.parse("https://www.deleteme.com"),
                        thumbnailUri = null,
                        shareUri = null,
                        author = "fake",
                        description = "fake",
                        contentDescription = null,
                        isCurrent = false,
                        creativeWallpaperEffectsData = null,
                    ),
            )
        wallpaperPreviewRepository.setWallpaperModel(liveWallpaperModel)

        val isDeleteVisible = collectLastValue(underTest.isDeleteVisible)
        assertThat(isDeleteVisible()).isTrue()
    }
}
