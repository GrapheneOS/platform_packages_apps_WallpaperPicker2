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

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.service.wallpaper.WallpaperService
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.EffectContract
import com.android.wallpaper.effects.EffectsController
import com.android.wallpaper.effects.EffectsController.RESULT_PROBE_SUCCESS
import com.android.wallpaper.effects.FakeEffectsController
import com.android.wallpaper.module.logging.TestUserEventLogger
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository
import com.android.wallpaper.picker.preview.data.util.ShadowWallpaperInfo
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.testing.WallpaperModelUtils.Companion.getStaticWallpaperModel
import com.android.wallpaper.testing.collectLastValue
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@OptIn(ExperimentalCoroutinesApi::class)
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class ImageEffectsRepositoryTest {

    private val context: HiltTestApplication = ApplicationProvider.getApplicationContext()
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val staticWallpaperModel =
        getStaticWallpaperModel(
            wallpaperId = "testWallpaperId",
            collectionId = "testCollection",
            imageWallpaperUri =
                Uri.parse(
                    "content://com.google.android.apps.photos.contentprovider/-1/1/content%3A%2F%2Fmedia%2Fexternal%2Fimages%2Fmedia%2F29/ORIGINAL/NONE/image%2Fjpeg/1309681125"
                )
        )

    @Before
    fun setUp() {
        // Register a fake the content provider
        val contentProvider = FakeContentProvider()
        ShadowContentResolver.registerProviderInternal(
            FakeEffectsController.AUTHORITY,
            contentProvider,
        )
        // Make a shadow of package manager
        val pm = shadowOf(context.packageManager)
        val packageName = "com.google.android.wallpaper.effects"
        val className = "com.google.android.wallpaper.effects.cinematic.CinematicWallpaperService"
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
        return ImageEffectsRepository(
            context = context,
            effectsController =
                FakeEffectsController(
                    areEffectsAvailable,
                    isEffectTriggered,
                    effectTitle,
                    effectFailedTitle,
                    effectSubTitle,
                    retryInstruction,
                    noEffectInstruction,
                ),
            logger = TestUserEventLogger(),
            bgDispatcher = testDispatcher,
        )
    }

    private class FakeContentProvider : ContentProvider() {
        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val cursor =
                MatrixCursor(
                    arrayOf(
                        EffectContract.ASSET_ID,
                        EffectContract.KEY_EFFECT_ID,
                        EffectContract.KEY_EFFECT_TITLE,
                    ),
                )
            if (uri.authority.equals(FakeEffectsController.AUTHORITY)) {
                // Return to-be-installed component names (flatten String)
                cursor
                    .newRow()
                    .add(EffectContract.ASSET_ID, FAKE_ASSET_ID)
                    .add(EffectContract.KEY_EFFECT_ID, FAKE_EFFECT_ID)
                    .add(EffectContract.KEY_EFFECT_TITLE, FAKE_EFFECT_TITLE)
            }
            return cursor
        }

        override fun getType(uri: Uri): String? {
            TODO("Not yet implemented")
        }

        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            TODO("Not yet implemented")
        }

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            TODO("Not yet implemented")
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int {
            TODO("Not yet implemented")
        }

        override fun onCreate(): Boolean {
            TODO("Not yet implemented")
        }
    }

    companion object {
        const val FAKE_ASSET_ID = 1
        const val FAKE_EFFECT_ID = 1
        const val FAKE_EFFECT_TITLE = "Fake Effect Title"
    }
}
