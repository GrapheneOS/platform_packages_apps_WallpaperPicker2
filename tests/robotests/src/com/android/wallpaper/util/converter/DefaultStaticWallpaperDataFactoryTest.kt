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

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.model.CreativeWallpaperInfo
import com.android.wallpaper.model.CurrentWallpaperInfo
import com.android.wallpaper.model.ImageWallpaperInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultStaticWallpaperDataFactoryTest {
    private var context: Context? = null
    private var wallpaperModelFactory = DefaultStaticWallpaperDataFactory()
    private val SAMPLE_URI = "Sample-URI"
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testGetStaticWallpaperData_FromCurrentWallpaperInfo() {
        val currentWallpaperInfo = Mockito.mock(CurrentWallpaperInfo::class.java)
        Mockito.doReturn(Mockito.mock(Asset::class.java))
            .`when`(currentWallpaperInfo)
            .getAsset(context)

        val staticWallpaperData =
            context?.let { wallpaperModelFactory.getStaticWallpaperData(currentWallpaperInfo, it) }
        assertThat(staticWallpaperData).isNotNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetStaticWallpaperData_FromCreativeWallpaperInfo_ThrowsException() {
        val creativeWallpaperInfo = Mockito.mock(CreativeWallpaperInfo::class.java)
        context?.let { wallpaperModelFactory.getStaticWallpaperData(creativeWallpaperInfo, it) }
    }

    @Test
    fun testGetImageWallpaperData_FromImageWallpaperInfo() {
        val imageWallpaperInfo = Mockito.mock(ImageWallpaperInfo::class.java)
        Mockito.doReturn(Uri.parse(SAMPLE_URI)).`when`(imageWallpaperInfo).uri
        val imageWallpaperData =
            context?.let { wallpaperModelFactory.getImageWallpaperData(imageWallpaperInfo) }
        assertThat(imageWallpaperData).isNotNull()
        if (imageWallpaperData != null) {
            assertThat(imageWallpaperData.mUri).isEqualTo(Uri.parse(SAMPLE_URI))
        }
    }
}
