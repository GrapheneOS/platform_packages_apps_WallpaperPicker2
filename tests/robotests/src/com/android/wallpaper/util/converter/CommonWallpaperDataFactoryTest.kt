/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.test.platform.app.InstrumentationRegistry
import com.android.wallpaper.model.wallpaper.Destination
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.util.testutil.WallpaperInfoFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Ignore
class CommonWallpaperDataFactoryTest {

    private var context: Context? = null
    private val SAMPLE_COLLECTION_ID = "Sample-Collection-ID"
    private val STATIC_WALLPAPER_PACKAGE = "StaticWallpaperPackage"
    private val STATIC_WALLPAPER_CLASS = "StaticWallpaperClass"

    @Before
    fun setUp() {
        InjectorProvider.setInjector(/* injector = */ TestInjector())
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testGetCommonWallpaperData_fromCurrentWallpaperInfo() {
        // Create a CurrentWallpaperInfo for system wallpaper
        val currentWallpaperInfo = WallpaperInfoFactory.createCurrentWallpaperInfo()

        val commonWallpaperData =
            context?.let {
                CommonWallpaperDataFactory.getCommonWallpaperData(currentWallpaperInfo, it)
            }

        assertThat(commonWallpaperData).isNotNull()
        if (commonWallpaperData != null) {
            assertThat(commonWallpaperData.collectionId).isEqualTo(SAMPLE_COLLECTION_ID)
            assertThat(commonWallpaperData.destination).isEqualTo(Destination.APPLIED_TO_SYSTEM)
            assertThat(commonWallpaperData.id.componentName)
                .isEqualTo(ComponentName(STATIC_WALLPAPER_PACKAGE, STATIC_WALLPAPER_CLASS))
        }
    }
}
