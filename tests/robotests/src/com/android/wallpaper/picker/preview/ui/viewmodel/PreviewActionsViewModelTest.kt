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

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.FakeEffectsController
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.domain.interactor.PreviewActionsInteractor
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.picker.preview.ui.util.LiveWallpaperDeleteUtil
import com.android.wallpaper.testing.FakeImageEffectsRepository
import com.android.wallpaper.testing.TestInjector
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PreviewActionsViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var viewModel: PreviewActionsViewModel
    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var imageEffectsRepository: FakeImageEffectsRepository
    @Inject lateinit var liveWallpaperDeleteUtil: LiveWallpaperDeleteUtil

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
        fun previewActionsInteractor(): PreviewActionsInteractor
        fun wallpaperPreviewRepository(): WallpaperPreviewRepository
    }

    private fun setEverything(activity: PreviewTestActivity) {
        val activityScopeEntryPoint =
            EntryPointAccessors.fromActivity(activity, ActivityScopeEntryPoint::class.java)
        viewModel =
            PreviewActionsViewModel(
                activityScopeEntryPoint.previewActionsInteractor(),
                liveWallpaperDeleteUtil,
                appContext
            )
        wallpaperPreviewRepository = activityScopeEntryPoint.wallpaperPreviewRepository()
    }

    @Test
    fun informationClicked_preparesInformationFloatingSheet() = runTest {
        val model = WallpaperModelUtils.getStaticWallpaperModel("testId", "testCollection")
        wallpaperPreviewRepository.setWallpaperModel(model)

        // Simulate click of info button
        collectLastValue(viewModel.onInformationClicked)()?.invoke()

        val preview = collectLastValue(viewModel.previewFloatingSheetViewModel)()
        assertThat(preview?.informationFloatingSheetViewModel).isNotNull()
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
        collectLastValue(viewModel.onEffectsClicked)()?.invoke()

        val preview = collectLastValue(viewModel.previewFloatingSheetViewModel)()
        assertThat(preview?.imageEffectFloatingSheetViewModel).isNotNull()
    }
}
