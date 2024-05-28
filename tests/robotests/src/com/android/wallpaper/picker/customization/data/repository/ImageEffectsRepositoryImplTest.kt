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

package com.android.wallpaper.picker.customization.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import android.service.wallpaper.WallpaperService
import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.EffectsController
import com.android.wallpaper.effects.EffectsController.RESULT_PROBE_SUCCESS
import com.android.wallpaper.effects.FakeEffectsController
import com.android.wallpaper.effects.FakeEffectsController.Companion.LIVE_WALLPAPER_COMPONENT_CLASS_NAME
import com.android.wallpaper.effects.FakeEffectsController.Companion.LIVE_WALLPAPER_COMPONENT_PKG_NAME
import com.android.wallpaper.module.logging.TestUserEventLogger
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepositoryImpl
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.testing.FakeContentProvider
import com.android.wallpaper.testing.FakeContentProvider.Companion.FAKE_EFFECT_ID
import com.android.wallpaper.testing.FakeContentProvider.Companion.FAKE_EFFECT_TITLE
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.WallpaperModelUtils.Companion.getStaticWallpaperModel
import com.android.wallpaper.testing.collectLastValue
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@HiltAndroidTest
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class ImageEffectsRepositoryImplTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var contentProvider: FakeContentProvider
    @Inject lateinit var effectsController: FakeEffectsController
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope

    private val staticWallpaperModel =
        getStaticWallpaperModel(
            wallpaperId = "testWallpaperId",
            collectionId = "testCollection",
            imageWallpaperUri = Uri.parse("content://com.test/image")
        )

    @Before
    fun setUp() {
        hiltRule.inject()
        // Register a fake the content provider
        ShadowContentResolver.registerProviderInternal(
            FakeEffectsController.AUTHORITY,
            contentProvider,
        )
        // Make a shadow of package manager
        val pm = shadowOf(context.packageManager)
        val packageName = LIVE_WALLPAPER_COMPONENT_PKG_NAME
        val className = LIVE_WALLPAPER_COMPONENT_CLASS_NAME
        val resolveInfo =
            ResolveInfo().apply {
                serviceInfo = ServiceInfo()
                serviceInfo.packageName = packageName
                serviceInfo.splitName = "effectsWallpaper"
                serviceInfo.name = className
                serviceInfo.flags = PackageManager.GET_META_DATA
            }
        val intent = Intent(WallpaperService.SERVICE_INTERFACE).setClassName(packageName, className)
        pm.addResolveInfoForIntent(intent, resolveInfo)
    }

    @Test
    fun areEffectsAvailableTrue() {
        val underTest =
            getImageEffectsRepositoryForTesting(
                areEffectsAvailable = true,
            )

        assertThat(underTest.areEffectsAvailable()).isTrue()
    }

    @Test
    fun areEffectsAvailableFalse() {
        val underTest =
            getImageEffectsRepositoryForTesting(
                areEffectsAvailable = false,
            )

        assertThat(underTest.areEffectsAvailable()).isFalse()
    }

    @Test
    fun initializeEffect() =
        testScope.runTest {
            val underTest = getImageEffectsRepositoryForTesting()
            val wallpaperEffect = collectLastValue(underTest.wallpaperEffect)

            underTest.initializeEffect(
                staticWallpaperModel = staticWallpaperModel,
                onWallpaperModelUpdated = { _ -> },
            )

            assertThat(wallpaperEffect())
                .isEqualTo(
                    Effect(
                        FAKE_EFFECT_ID,
                        FAKE_EFFECT_TITLE,
                        FakeEffectsController.Effect.FAKE_EFFECT,
                    )
                )
        }

    @Test
    fun initializeEffect_isEffectTriggeredTrue() =
        testScope.runTest {
            val underTest =
                getImageEffectsRepositoryForTesting(
                    isEffectTriggered = true,
                )
            val imageEffectsModel = collectLastValue(underTest.imageEffectsModel)

            underTest.initializeEffect(
                staticWallpaperModel = staticWallpaperModel,
                onWallpaperModelUpdated = { _ -> },
            )

            assertThat(imageEffectsModel())
                .isEqualTo(ImageEffectsModel(ImageEffectsRepository.EffectStatus.EFFECT_READY))
        }

    @Test
    fun initializeEffect_isEffectTriggeredFalse() =
        testScope.runTest {
            val underTest =
                getImageEffectsRepositoryForTesting(
                    isEffectTriggered = false,
                )
            val imageEffectsModel = collectLastValue(underTest.imageEffectsModel)

            underTest.initializeEffect(
                staticWallpaperModel = staticWallpaperModel,
                onWallpaperModelUpdated = { _ -> },
            )

            assertThat(imageEffectsModel())
                .isEqualTo(
                    ImageEffectsModel(
                        ImageEffectsRepository.EffectStatus.EFFECT_READY,
                        RESULT_PROBE_SUCCESS
                    )
                )
        }

    @Test
    fun enableImageEffect() =
        testScope.runTest {
            val underTest = getImageEffectsRepositoryForTesting()
            val imageEffectsModel = collectLastValue(underTest.imageEffectsModel)
            var onWallpaperModelUpdatedCalled = false

            underTest.initializeEffect(
                staticWallpaperModel = staticWallpaperModel,
                onWallpaperModelUpdated = { _ -> onWallpaperModelUpdatedCalled = true },
            )
            underTest.enableImageEffect(FakeEffectsController.Effect.FAKE_EFFECT)

            assertThat(imageEffectsModel())
                .isEqualTo(
                    ImageEffectsModel(
                        ImageEffectsRepository.EffectStatus.EFFECT_APPLIED,
                        EffectsController.RESULT_SUCCESS,
                    )
                )
            assertThat(onWallpaperModelUpdatedCalled).isTrue()
        }

    @Test
    fun disableImageEffect() =
        testScope.runTest {
            val underTest = getImageEffectsRepositoryForTesting()
            val imageEffectsModel = collectLastValue(underTest.imageEffectsModel)
            var onWallpaperModelUpdatedCalled = false

            underTest.initializeEffect(
                staticWallpaperModel = staticWallpaperModel,
                onWallpaperModelUpdated = { _ -> onWallpaperModelUpdatedCalled = true },
            )
            underTest.enableImageEffect(FakeEffectsController.Effect.FAKE_EFFECT)
            underTest.disableImageEffect()

            assertThat(imageEffectsModel())
                .isEqualTo(ImageEffectsModel(ImageEffectsRepository.EffectStatus.EFFECT_READY))
            assertThat(onWallpaperModelUpdatedCalled).isTrue()
        }

    @Test
    fun destroy() =
        testScope.runTest {
            val underTest = getImageEffectsRepositoryForTesting()
            val wallpaperEffect = collectLastValue(underTest.wallpaperEffect)

            underTest.initializeEffect(
                staticWallpaperModel = staticWallpaperModel,
                onWallpaperModelUpdated = { _ -> },
            )
            underTest.destroy()

            assertThat(wallpaperEffect()).isNull()
        }

    @Test
    fun isTargetEffect() {
        val underTest = getImageEffectsRepositoryForTesting()

        assertThat(underTest.isTargetEffect(FakeEffectsController.Effect.FAKE_EFFECT)).isTrue()
        assertThat(underTest.isTargetEffect(FakeEffectsController.Effect.NONE)).isFalse()
    }

    @Test
    fun getEffectTextRes() {
        val underTest =
            getImageEffectsRepositoryForTesting(
                effectTitle = "Fake effect title",
                effectFailedTitle = "Fake effect failed title",
                effectSubTitle = "Fake effect subtitle",
                retryInstruction = "Fake effect retry instruction",
                noEffectInstruction = "Fake no effect instruction",
            )

        assertThat(underTest.getEffectTextRes())
            .isEqualTo(
                WallpaperEffectsView2.EffectTextRes(
                    "Fake effect title",
                    "Fake effect failed title",
                    "Fake effect subtitle",
                    "Fake effect retry instruction",
                    "Fake no effect instruction",
                )
            )
    }

    @Test
    fun startEffectsModelDownload() =
        testScope.runTest {
            val underTest = getImageEffectsRepositoryForTesting()
            val imageEffectsModel = collectLastValue(underTest.imageEffectsModel)

            underTest.initializeEffect(
                staticWallpaperModel = staticWallpaperModel,
                onWallpaperModelUpdated = { _ -> },
            )
            underTest.startEffectsModelDownload(
                Effect(
                    FAKE_EFFECT_ID,
                    FAKE_EFFECT_TITLE,
                    FakeEffectsController.Effect.FAKE_EFFECT,
                )
            )

            assertThat(imageEffectsModel())
                .isEqualTo(
                    ImageEffectsModel(
                        ImageEffectsRepository.EffectStatus.EFFECT_READY,
                        EffectsController.RESULT_FOREGROUND_DOWNLOAD_SUCCEEDED
                    )
                )
        }

    private fun getImageEffectsRepositoryForTesting(
        areEffectsAvailable: Boolean = true,
        isEffectTriggered: Boolean = true,
        effectTitle: String = "",
        effectFailedTitle: String = "",
        effectSubTitle: String = "",
        retryInstruction: String = "",
        noEffectInstruction: String = "",
    ): ImageEffectsRepository {
        effectsController.apply {
            fakeAreEffectsAvailable = areEffectsAvailable
            fakeIsEffectTriggered = isEffectTriggered
            fakeEffectTitle = effectTitle
            fakeEffectFailedTitle = effectFailedTitle
            fakeEffectSubTitle = effectSubTitle
            fakeRetryInstruction = retryInstruction
            fakeNoEffectInstruction = noEffectInstruction
        }
        return ImageEffectsRepositoryImpl(
            context = context,
            effectsController = effectsController,
            logger = TestUserEventLogger(),
            bgDispatcher = testDispatcher,
        )
    }
}
