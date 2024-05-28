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
package com.android.wallpaper.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.testing.FakeDisplaysProvider
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.FOLDABLE_FOLDED
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.FOLDABLE_UNFOLDED_LAND
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.FOLDABLE_UNFOLDED_PORT
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.HANDHELD
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.TABLET_LAND
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.TABLET_PORT
import com.android.wallpaper.testing.TestInjector
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowDisplayManager::class])
class DisplayUtilsTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var displaysProvider: FakeDisplaysProvider
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var testInjector: TestInjector

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)
    }

    @Test
    fun hasMultiInternalDisplays_oneDisplay_returnsFalse() {
        val displays = listOf(HANDHELD)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.hasMultiInternalDisplays()).isFalse()
    }

    @Test
    fun hasMultiInternalDisplays_twoDisplays_returnsTrue() {
        val displays = listOf(FOLDABLE_FOLDED, FOLDABLE_UNFOLDED_LAND)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.hasMultiInternalDisplays()).isTrue()
    }

    @Test
    fun getWallpaperDisplay_oneDisplay_returnsDefaultDisplay() {
        val displays = listOf(HANDHELD)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.getWallpaperDisplay())
            .isEqualTo(displaysProvider.getInternalDisplays()[0])
    }

    @Test
    fun getWallpaperDisplay_twoDisplays_returnsLargestDisplay() {
        val displays = listOf(FOLDABLE_FOLDED, FOLDABLE_UNFOLDED_LAND)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.getWallpaperDisplay())
            .isEqualTo(displaysProvider.getInternalDisplays()[1])
    }

    @Test
    fun isUnfoldedHorizontalHinge_handheld_returnsFalse() {
        val displays = listOf(HANDHELD)
        displaysProvider.setDisplays(displays)
        val displayContext =
            appContext.createDisplayContext(displaysProvider.getInternalDisplays()[0])

        assertThat(displayUtils.isUnfoldedHorizontalHinge(displayContext)).isFalse()
    }

    @Test
    fun isUnfoldedHorizontalHinge_foldableFolded_returnsFalse() {
        val displays = listOf(FOLDABLE_FOLDED, FOLDABLE_UNFOLDED_PORT)
        displaysProvider.setDisplays(displays)
        val displayContext =
            appContext.createDisplayContext(displaysProvider.getInternalDisplays()[0])

        assertThat(displayUtils.isUnfoldedHorizontalHinge(displayContext)).isFalse()
    }

    @Test
    fun isUnfoldedHorizontalHinge_foldableUnfoldedLandscape_returnsFalse() {
        val displays = listOf(FOLDABLE_UNFOLDED_LAND, FOLDABLE_FOLDED)
        displaysProvider.setDisplays(displays)
        val displayContext =
            appContext.createDisplayContext(displaysProvider.getInternalDisplays()[0])

        assertThat(displayUtils.isUnfoldedHorizontalHinge(displayContext)).isFalse()
    }

    @Test
    fun isUnfoldedHorizontalHinge_foldableUnfoldedPortrait_returnsTrue() {
        val displays = listOf(FOLDABLE_UNFOLDED_PORT, FOLDABLE_FOLDED)
        displaysProvider.setDisplays(displays)
        val displayContext =
            appContext.createDisplayContext(displaysProvider.getInternalDisplays()[0])

        assertThat(displayUtils.isUnfoldedHorizontalHinge(displayContext)).isTrue()
    }

    @Test
    fun isUnfoldedHorizontalHinge_tabletPortrait_returnsFalse() {
        val displays = listOf(TABLET_PORT)
        displaysProvider.setDisplays(displays)
        val displayContext =
            appContext.createDisplayContext(displaysProvider.getInternalDisplays()[0])

        assertThat(displayUtils.isUnfoldedHorizontalHinge(displayContext)).isFalse()
    }

    @Test
    fun getMaxDisplaysDimension_oneDisplay_returnsDisplayDimensions() {
        val displayConfig1 = FakeDisplaysProvider.FakeDisplayConfig(Point(250, 500))
        val displays = listOf(displayConfig1)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.getMaxDisplaysDimension()).isEqualTo(Point(250, 500))
    }

    @Test
    fun getMaxDisplaysDimension_noRotation_returnsMaxOfEachDimension() {
        val displayConfig1 = FakeDisplaysProvider.FakeDisplayConfig(Point(250, 500))
        val displayConfig2 = FakeDisplaysProvider.FakeDisplayConfig(Point(300, 400))
        val displays = listOf(displayConfig1, displayConfig2)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.getMaxDisplaysDimension()).isEqualTo(Point(300, 500))
    }

    @Test
    fun getMaxDisplaysDimension_withRotation_returnsMaxPostRotation() {
        val displayConfig1 = FakeDisplaysProvider.FakeDisplayConfig(Point(250, 500))
        // Display size of config 2 becomes Point(400, 300) with rotation.
        val displayConfig2 =
            FakeDisplaysProvider.FakeDisplayConfig(
                displaySize = Point(300, 400),
                orientation = Configuration.ORIENTATION_LANDSCAPE,
            )
        val displays = listOf(displayConfig1, displayConfig2)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.getMaxDisplaysDimension()).isEqualTo(Point(400, 500))
    }

    @Test
    fun getMaxDisplaysDimension_foldable_returnsMaxOfEachDimension() {
        val displayConfig1 = FakeDisplaysProvider.FakeDisplayConfig(Point(250, 500))
        val displayConfig2 = FakeDisplaysProvider.FakeDisplayConfig(Point(300, 400))
        val displayConfig3 = FakeDisplaysProvider.FakeDisplayConfig(Point(600, 300))
        val displays = listOf(displayConfig1, displayConfig2, displayConfig3)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.getMaxDisplaysDimension()).isEqualTo(Point(600, 500))
    }

    @Test
    fun isLargeScreenDevice_handheld_returnsFalse() {
        val displays = listOf(HANDHELD)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.isLargeScreenDevice()).isFalse()
    }

    @Test
    fun isLargeScreenDevice_foldedFoldable_returnsTrue() {
        val displays = listOf(FOLDABLE_FOLDED, FOLDABLE_UNFOLDED_LAND)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.isLargeScreenDevice()).isTrue()
    }

    @Test
    fun isLargeScreenDevice_unfoldedFoldable_returnsTrue() {
        val displays = listOf(FOLDABLE_UNFOLDED_LAND, FOLDABLE_FOLDED)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.isLargeScreenDevice()).isTrue()
    }

    @Test
    fun isLargeScreenDevice_tablet_returnsTrue() {
        val displays = listOf(TABLET_LAND)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.isLargeScreenDevice()).isTrue()
    }

    @Test
    fun getSmallerDisplay_oneDisplay_returnsDefaultDisplay() {
        val displays = listOf(HANDHELD)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.getSmallerDisplay())
            .isEqualTo(displaysProvider.getInternalDisplays()[0])
    }

    @Test
    fun getSmallerDisplay_twoDisplays_returnsSmallestDisplay() {
        val displays = listOf(FOLDABLE_UNFOLDED_LAND, FOLDABLE_FOLDED)
        displaysProvider.setDisplays(displays)

        assertThat(displayUtils.getSmallerDisplay())
            .isEqualTo(displaysProvider.getInternalDisplays()[1])
    }

    @Test
    fun getInternalDisplaySizes_noRotation_returnsSizesAsIs() {
        val displayConfig1 = FakeDisplaysProvider.FakeDisplayConfig(Point(250, 500))
        val displayConfig2 = FakeDisplaysProvider.FakeDisplayConfig(Point(300, 400))
        val displays = listOf(displayConfig1, displayConfig2)
        displaysProvider.setDisplays(displays)

        val displaySizes = displayUtils.getInternalDisplaySizes()
        assertThat(displaySizes.size).isEqualTo(displays.size)
        assertThat(displaySizes).contains(Point(250, 500))
        assertThat(displaySizes).contains(Point(300, 400))
    }

    @Test
    fun getInternalDisplaySizes_withRotation_returnsSizesPostRotation() {
        val displayConfig1 = FakeDisplaysProvider.FakeDisplayConfig(Point(250, 500))
        // Display size of config 2 becomes Point(400, 300) with rotation.
        val displayConfig2 =
            FakeDisplaysProvider.FakeDisplayConfig(
                displaySize = Point(300, 400),
                orientation = Configuration.ORIENTATION_LANDSCAPE,
            )
        val displays = listOf(displayConfig1, displayConfig2)
        displaysProvider.setDisplays(displays)

        val displaySizes = displayUtils.getInternalDisplaySizes()
        assertThat(displaySizes.size).isEqualTo(displays.size)
        assertThat(displaySizes).contains(Point(250, 500))
        assertThat(displaySizes).contains(Point(400, 300))
    }

    @Test
    fun getInternalDisplaySizes_allDimensions_returnsSizesAndRotatedSizes() {
        val displayConfig1 = FakeDisplaysProvider.FakeDisplayConfig(Point(250, 500))
        val displayConfig2 = FakeDisplaysProvider.FakeDisplayConfig(Point(300, 400))
        val displays = listOf(displayConfig1, displayConfig2)
        displaysProvider.setDisplays(displays)

        val displaySizes = displayUtils.getInternalDisplaySizes(true)
        assertThat(displaySizes.size).isEqualTo(displays.size * 2)
        assertThat(displaySizes).contains(Point(250, 500))
        assertThat(displaySizes).contains(Point(300, 400))
        assertThat(displaySizes).contains(Point(500, 250))
        assertThat(displaySizes).contains(Point(400, 300))
    }
}
