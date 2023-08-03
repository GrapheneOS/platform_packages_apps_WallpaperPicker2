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
package com.android.wallpaper.picker.wallpaper

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.BasePreviewActivity
import com.android.wallpaper.picker.ImagePreviewFragment
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestStaticWallpaperInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WallpaperPreviewSectionActivityTest {
    private val testStaticWallpaper = TestStaticWallpaperInfo(TestStaticWallpaperInfo.COLOR_DEFAULT)

    @Before
    fun setUp() {
        InjectorProvider.setInjector(TestInjector())

        val attributions: MutableList<String> = ArrayList()
        attributions.add("Title")
        attributions.add("Subtitle 1")
        attributions.add("Subtitle 2")
        testStaticWallpaper.setAttributions(attributions)
        testStaticWallpaper.setCollectionId("collectionStatic")
        testStaticWallpaper.setWallpaperId("wallpaperStatic")
        testStaticWallpaper.setActionUrl("http://google.com")
    }

    @Test
    fun showsPreviewFragment() {
        val intent =
            Intent(
                InstrumentationRegistry.getInstrumentation().targetContext,
                WallpaperPreviewSectionActivity::class.java
            )
        intent.putExtra(BasePreviewActivity.EXTRA_WALLPAPER_INFO, testStaticWallpaper)

        val scenario: ActivityScenario<WallpaperPreviewSectionActivity> =
            ActivityScenario.launch(intent)

        scenario.onActivity { activity ->
            val previews =
                activity.supportFragmentManager.fragments.filterIsInstance<ImagePreviewFragment>()
            assertThat(previews).hasSize(1)
        }
    }
}
