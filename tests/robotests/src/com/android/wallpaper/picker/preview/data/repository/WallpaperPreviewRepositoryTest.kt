/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.wallpaper.picker.preview.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.WallpaperModelUtils.Companion.getStaticWallpaperModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for {@link WallpaperPreviewRepository}.
 *
 * WallpaperPreviewRepository cannot be injected in setUp() because it is annotated with scope
 * ActivityRetainedScoped. We make an instance available via TestActivity, which can inject the SUT
 * and expose it for testing.
 */
@RunWith(RobolectricTestRunner::class)
class WallpaperPreviewRepositoryTest {

    private lateinit var context: Context
    private lateinit var testDispatcher: CoroutineDispatcher
    private lateinit var testScope: TestScope
    private lateinit var underTest: WallpaperPreviewRepository
    private lateinit var prefs: WallpaperPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        prefs = TestWallpaperPreferences()
    }

    @Test
    fun setWallpaperModel() {
        underTest = WallpaperPreviewRepository(preferences = prefs)

        val wallpaperModel =
            getStaticWallpaperModel(
                wallpaperId = "aaa",
                collectionId = "testCollection",
            )
        assertThat(underTest.wallpaperModel.value).isNull()

        underTest.setWallpaperModel(wallpaperModel)

        assertThat(underTest.wallpaperModel.value).isEqualTo(wallpaperModel)
    }

    @Test
    fun dismissSmallTooltip() {
        prefs.setHasSmallPreviewTooltipBeenShown(false)
        prefs.setHasFullPreviewTooltipBeenShown(false)
        underTest = WallpaperPreviewRepository(preferences = prefs)
        assertThat(underTest.hasSmallPreviewTooltipBeenShown.value).isFalse()
        assertThat(underTest.hasFullPreviewTooltipBeenShown.value).isFalse()

        underTest.hideSmallPreviewTooltip()

        assertThat(prefs.getHasSmallPreviewTooltipBeenShown()).isTrue()
        assertThat(underTest.hasSmallPreviewTooltipBeenShown.value).isTrue()
        assertThat(prefs.getHasFullPreviewTooltipBeenShown()).isFalse()
        assertThat(underTest.hasFullPreviewTooltipBeenShown.value).isFalse()
    }

    @Test
    fun dismissFullTooltip() {
        prefs.setHasSmallPreviewTooltipBeenShown(false)
        prefs.setHasFullPreviewTooltipBeenShown(false)
        underTest = WallpaperPreviewRepository(preferences = prefs)
        assertThat(underTest.hasSmallPreviewTooltipBeenShown.value).isFalse()
        assertThat(underTest.hasFullPreviewTooltipBeenShown.value).isFalse()

        underTest.hideFullPreviewTooltip()

        assertThat(prefs.getHasSmallPreviewTooltipBeenShown()).isFalse()
        assertThat(underTest.hasSmallPreviewTooltipBeenShown.value).isFalse()
        assertThat(prefs.getHasFullPreviewTooltipBeenShown()).isTrue()
        assertThat(underTest.hasFullPreviewTooltipBeenShown.value).isTrue()
    }
}
