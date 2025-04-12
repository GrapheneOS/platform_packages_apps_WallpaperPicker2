/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.util.converter

import android.content.ComponentName
import android.content.Context
import androidx.core.os.bundleOf
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestLiveWallpaperInfo
import com.android.wallpaper.testing.WallpaperInfoUtils.Companion.createWallpaperInfo
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

@HiltAndroidTest
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class DefaultWallpaperModelFactoryTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context

    private var wallpaperModelFactory = DefaultWallpaperModelFactory()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testGetLiveWallpaperData_FromLiveWallpaperInfo() {
        val wallpaperId = "wallpaperId"
        val component = ComponentName("package", "class")
        val wallpaperInfo = createWallpaperInfo(context, component)
        val liveWallpaperInfo = TestLiveWallpaperInfo(0, wallpaperInfo, wallpaperId)
        liveWallpaperInfo.setTitle("test wallpaper")

        val wallpaperModel = wallpaperModelFactory.getWallpaperModel(context, liveWallpaperInfo)

        val liveWallpaperData =
            (wallpaperModel as WallpaperModel.LiveWallpaperModel).liveWallpaperData
        assertThat(liveWallpaperData).isNotNull()
        assertThat(liveWallpaperData.supportsMultipleEngines).isFalse()
    }

    @Test
    fun testGetLiveWallpaperData_supportsMultipleEngines_isTrue() {
        val wallpaperId = "wallpaperId"
        val component = ComponentName("package", "class")
        // Explicitly hardcoding the string because changing it could change wallpapers
        // expected behavior, so this will ensure we notice if we change it.
        val metadata = bundleOf("com.android.wallpaper.supports_multiple_engines" to true)
        val wallpaperInfo =
            createWallpaperInfo(context = context, componentName = component, metaData = metadata)
        val liveWallpaperInfo = TestLiveWallpaperInfo(0, wallpaperInfo, wallpaperId)
        liveWallpaperInfo.setTitle("test wallpaper")

        val wallpaperModel = wallpaperModelFactory.getWallpaperModel(context, liveWallpaperInfo)

        val liveWallpaperData =
            (wallpaperModel as WallpaperModel.LiveWallpaperModel).liveWallpaperData
        assertThat(liveWallpaperData).isNotNull()
        assertThat(liveWallpaperData.supportsMultipleEngines).isTrue()
    }
}
