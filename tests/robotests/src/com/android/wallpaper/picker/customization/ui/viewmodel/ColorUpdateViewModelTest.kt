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

package com.android.wallpaper.picker.customization.ui.viewmodel

import android.content.Context
import android.util.SparseIntArray
import android.widget.RemoteViews.ColorResources
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.systemui.monet.Style
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.internal.lifecycle.RetainedLifecycleImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@SmallTest
@RunWith(RobolectricTestRunner::class)
class ColorUpdateViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var underTest: ColorUpdateViewModel
    @Inject lateinit var testScope: TestScope

    @Before
    fun setUp() {
        hiltRule.inject()

        context = InstrumentationRegistry.getInstrumentation().targetContext
        underTest = ColorUpdateViewModel(context, RetainedLifecycleImpl())
    }

    private fun overlayColors(context: Context, colorMapping: SparseIntArray) {
        ColorResources.create(context, colorMapping)?.apply(context)
    }

    @Test
    fun updateColors() {
        testScope.runTest {
            val colorPrimary = collectLastValue(underTest.colorPrimary)
            assertThat(colorPrimary()).isNotEqualTo(12345)

            overlayColors(
                context,
                SparseIntArray().apply {
                    put(android.R.color.system_primary_light, 12345)
                    put(android.R.color.system_primary_dark, 12345)
                },
            )
            underTest.updateColors()

            assertThat(colorPrimary()).isEqualTo(12345)
        }
    }

    @Test
    fun previewColors_withPreviewEnabled() {
        testScope.runTest {
            val colorPrimary = collectLastValue(underTest.colorPrimary)
            overlayColors(
                context,
                SparseIntArray().apply {
                    put(android.R.color.system_primary_light, 12345)
                    put(android.R.color.system_primary_dark, 12345)
                },
            )
            underTest.updateColors()

            underTest.setPreviewEnabled(true)
            underTest.previewColors(54321, Style.VIBRANT, isDarkMode = false)

            assertThat(colorPrimary()).isNotEqualTo(12345)
        }
    }

    @Test
    fun previewColors_withPreviewDisabled() {
        testScope.runTest {
            val colorPrimary = collectLastValue(underTest.colorPrimary)
            overlayColors(
                context,
                SparseIntArray().apply {
                    put(android.R.color.system_primary_light, 12345)
                    put(android.R.color.system_primary_dark, 12345)
                },
            )
            underTest.updateColors()

            underTest.setPreviewEnabled(false)
            underTest.previewColors(54321, Style.VIBRANT, isDarkMode = false)

            assertThat(colorPrimary()).isEqualTo(12345)
        }
    }

    @Test
    fun resetPreview() {
        testScope.runTest {
            val colorPrimary = collectLastValue(underTest.colorPrimary)
            overlayColors(
                context,
                SparseIntArray().apply {
                    put(android.R.color.system_primary_light, 12345)
                    put(android.R.color.system_primary_dark, 12345)
                },
            )
            underTest.updateColors()
            assertThat(colorPrimary()).isEqualTo(12345)

            underTest.previewColors(54321, Style.VIBRANT, isDarkMode = false)
            underTest.resetPreview()

            assertThat(colorPrimary()).isEqualTo(12345)
        }
    }
}
