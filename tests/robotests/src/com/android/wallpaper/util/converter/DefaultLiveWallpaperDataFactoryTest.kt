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

import android.app.WallpaperColors
import android.app.WallpaperInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import androidx.core.graphics.toColor
import androidx.test.platform.app.InstrumentationRegistry
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.util.testutil.WallpaperInfoFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Ignore
class DefaultLiveWallpaperDataFactoryTest {
    private var context: Context? = null
    private val SAMPLE_AUTHOR = "Sample-Author"
    private val SAMPLE_DESCRIPTION = "Sample-Description"
    private val SAMPLE_CONTENT_DESCRIPTION = "Sample-Content-Description"
    private val SAMPLE_WALLPAPER_ID = "Sample-Wallpaper-Id"
    private val SAMPLE_PACKAGE = "Sample-Package"
    private val SAMPLE_CL = "Sample-CL"

    private var wallpaperModelFactory = DefaultLiveWallpaperDataFactory()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testGetCreativeWallpaperData_FromCreativeWallpaperInfo() {
        val mockWallpaperInfo = Mockito.mock(WallpaperInfo::class.java)
        val creativeWallpaperInfo =
            WallpaperInfoFactory.createCreativeWallpaperInfo(mockWallpaperInfo)

        val componentName = ComponentName(SAMPLE_PACKAGE, SAMPLE_CL)
        val colorInfo =
            com.android.wallpaper.model.WallpaperInfo.ColorInfo(
                WallpaperColors(Color.BLACK.toColor(), Color.BLUE.toColor(), Color.CYAN.toColor())
            )

        val thumbAsset = Mockito.mock(Asset::class.java)
        val spyCreativeWallpaperInfo = Mockito.spy(creativeWallpaperInfo)

        Mockito.doReturn(componentName).`when`(mockWallpaperInfo).component
        Mockito.doReturn(SAMPLE_WALLPAPER_ID).`when`(spyCreativeWallpaperInfo).wallpaperId
        Mockito.doReturn(colorInfo).`when`(spyCreativeWallpaperInfo).colorInfo
        Mockito.doReturn(thumbAsset).`when`(spyCreativeWallpaperInfo).getThumbAsset(context)
        Mockito.doReturn(mockWallpaperInfo).`when`(spyCreativeWallpaperInfo).info

        val creativeWallpaperData =
            wallpaperModelFactory.getCreativeWallpaperData(creativeWallpaperInfo)
        assertThat(creativeWallpaperData).isNotNull()
        if (creativeWallpaperData != null) {
            assertThat(creativeWallpaperData.author).isEqualTo(SAMPLE_AUTHOR)
            assertThat(creativeWallpaperData.description).isEqualTo(SAMPLE_DESCRIPTION)
            assertThat(creativeWallpaperData.contentDescription)
                .isEqualTo(SAMPLE_CONTENT_DESCRIPTION)
        }
    }

    @Test
    fun testGetLiveWallpaperData_FromLiveWallpaperInfo() {
        val liveWallpaperInfo = Mockito.mock(LiveWallpaperInfo::class.java)
        val wallpaperInfo = Mockito.mock(WallpaperInfo::class.java)

        Mockito.doReturn(wallpaperInfo).`when`(liveWallpaperInfo).info
        val liveWallpaperData =
            context?.let { wallpaperModelFactory.getLiveWallpaperData(liveWallpaperInfo, it) }

        assertThat(liveWallpaperData).isNotNull()
        if (liveWallpaperData != null) {
            assertThat(liveWallpaperData.groupName).isEqualTo("")
            assertThat(liveWallpaperData.isTitleVisible).isEqualTo(false)
            assertThat(liveWallpaperData.isApplied).isEqualTo(false)
        }
    }
}
