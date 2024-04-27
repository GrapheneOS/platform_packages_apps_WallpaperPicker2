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
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.testing.FakeDisplaysProvider
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.FOLDABLE_FOLDED
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.FOLDABLE_UNFOLDED_LAND
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
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

    lateinit var appContext: Context

    @Inject lateinit var displaysProvider: FakeDisplaysProvider
    @Inject lateinit var displayUtils: DisplayUtils

    @Before
    fun setUp() {
        hiltRule.inject()

        appContext = ApplicationProvider.getApplicationContext<HiltTestApplication>()
    }

    @Test
    // TODO (b/335647826): remove this basic test and add more thorough tests for DisplayUtils
    fun test() {
        val expected = listOf(FOLDABLE_FOLDED, FOLDABLE_UNFOLDED_LAND)
        displaysProvider.setDisplays(expected)

        assertThat(displaysProvider.getInternalDisplays()[1])
            .isEqualTo(displayUtils.getWallpaperDisplay())
        assertThat(FOLDABLE_FOLDED).isEqualTo(displayUtils.getInternalDisplaySizes()[0])
        assertThat(FOLDABLE_UNFOLDED_LAND).isEqualTo(displayUtils.getInternalDisplaySizes()[1])
    }
}
