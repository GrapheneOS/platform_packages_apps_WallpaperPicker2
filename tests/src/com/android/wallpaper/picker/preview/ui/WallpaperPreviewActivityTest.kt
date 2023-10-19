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
package com.android.wallpaper.picker.preview.ui

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestStaticWallpaperInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WallpaperPreviewActivityTest {
    private val testStaticWallpaper =
        TestStaticWallpaperInfo(TestStaticWallpaperInfo.COLOR_DEFAULT).setWallpaperAttributions()
    private val activityStartIntent =
        WallpaperPreviewActivity.newIntent(
            context = ApplicationProvider.getApplicationContext(),
            wallpaperInfo = testStaticWallpaper,
            isNewTask = false,
        )

    @Before
    fun setUp() {
        InjectorProvider.setInjector(TestInjector())
    }

    @Test
    fun showsNavHostFragment() {
        val scenario: ActivityScenario<WallpaperPreviewActivity> =
            ActivityScenario.launch(activityStartIntent)

        scenario.onActivity { activity ->
            val previews =
                activity.supportFragmentManager.fragments.filterIsInstance<NavHostFragment>()
            assertThat(previews).hasSize(1)
        }
    }

    @Test
    fun launchActivity_setsWallpaperInfo() {
        val scenario: ActivityScenario<WallpaperPreviewActivity> =
            ActivityScenario.launch(activityStartIntent)

        scenario.onActivity { activity ->
            val provider = ViewModelProvider(activity)
            val viewModel = provider[WallpaperPreviewViewModel::class.java]

            assertThat(viewModel.editingWallpaper).isEqualTo(testStaticWallpaper)
        }
    }

    private fun TestStaticWallpaperInfo.setWallpaperAttributions(): WallpaperInfo {
        setAttributions(listOf("Title", "Subtitle 1", "Subtitle 2"))
        setCollectionId("collectionStatic")
        setWallpaperId("wallpaperStatic")
        setActionUrl("http://google.com")
        return this
    }
}
