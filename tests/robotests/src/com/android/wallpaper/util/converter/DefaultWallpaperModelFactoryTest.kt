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
import android.net.Uri
import androidx.core.graphics.toColor
import androidx.test.platform.app.InstrumentationRegistry
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.model.ImageWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo.ColorInfo
import com.android.wallpaper.util.testutil.WallpaperInfoFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultWallpaperModelFactoryTest {

    private var context: Context? = null
    private val SAMPLE_WALLPAPER_ID = "Sample-Wallpaper-ID"
    private val SAMPLE_COLLECTION_ID = "Sample-Collection-ID"
    private val SAMPLE_PACKAGE = "Sample-Package"
    private val SAMPLE_CL = "Sample-CL"
    private var wallpaperModelFactory = DefaultWallpaperModelFactory()

    private val SAMPLE_URI = Uri.parse("content://sample/share_uri")

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testGetWallpaperModel_FromLiveWallpaperInfo() {
        val mockWallpaperInfo = Mockito.mock(WallpaperInfo::class.java)
        val creativeWallpaperInfo =
            WallpaperInfoFactory.createCreativeWallpaperInfo(mockWallpaperInfo)

        val componentName = ComponentName(SAMPLE_PACKAGE, SAMPLE_CL)
        val colorInfo =
            ColorInfo(
                WallpaperColors(Color.BLACK.toColor(), Color.BLUE.toColor(), Color.CYAN.toColor())
            )

        val thumbAsset = Mockito.mock(Asset::class.java)
        val spyCreativeWallpaperInfo = Mockito.spy(creativeWallpaperInfo)

        Mockito.doReturn(componentName).`when`(mockWallpaperInfo).component
        Mockito.doReturn(SAMPLE_WALLPAPER_ID).`when`(spyCreativeWallpaperInfo).wallpaperId
        Mockito.doReturn(colorInfo).`when`(spyCreativeWallpaperInfo).colorInfo
        Mockito.doReturn(thumbAsset).`when`(spyCreativeWallpaperInfo).getThumbAsset(context)
        Mockito.doReturn(mockWallpaperInfo).`when`(spyCreativeWallpaperInfo).info

        val liveWallpaperDataModel =
            context?.let { wallpaperModelFactory.getWallpaperModel(it, spyCreativeWallpaperInfo) }
        assertThat(liveWallpaperDataModel).isNotNull()
        if (liveWallpaperDataModel != null) {
            assertThat(liveWallpaperDataModel.commonWallpaperData.id.uniqueId)
                .isEqualTo(SAMPLE_WALLPAPER_ID)
        }
    }

    @Test
    fun testGetWallpaperModel_FromImageWallpaperInfo() {
        val imageWallpaperInfo = Mockito.mock(ImageWallpaperInfo::class.java)
        val mockWallpaperInfo = Mockito.mock(WallpaperInfo::class.java)
        val componentName = ComponentName(SAMPLE_PACKAGE, SAMPLE_CL)
        val colorInfo =
            ColorInfo(
                WallpaperColors(Color.BLACK.toColor(), Color.BLUE.toColor(), Color.CYAN.toColor())
            )
        val thumbAsset = Mockito.mock(Asset::class.java)
        val asset = Mockito.mock(Asset::class.java)

        Mockito.doReturn(mockWallpaperInfo).`when`(imageWallpaperInfo).wallpaperComponent
        Mockito.doReturn(componentName).`when`(mockWallpaperInfo).component
        Mockito.doReturn(SAMPLE_WALLPAPER_ID).`when`(imageWallpaperInfo).wallpaperId
        Mockito.doReturn(SAMPLE_COLLECTION_ID).`when`(imageWallpaperInfo).getCollectionId(context)
        Mockito.doReturn(colorInfo).`when`(imageWallpaperInfo).colorInfo
        Mockito.doReturn(thumbAsset).`when`(imageWallpaperInfo).getThumbAsset(context)
        Mockito.doReturn(asset).`when`(imageWallpaperInfo).getAsset(context)
        Mockito.doReturn(SAMPLE_URI).`when`(imageWallpaperInfo).uri

        val imageWallpaperDataModel =
            context?.let { wallpaperModelFactory.getWallpaperModel(it, imageWallpaperInfo) }
        assertThat(imageWallpaperDataModel).isNotNull()
        if (imageWallpaperDataModel != null) {
            assertThat(imageWallpaperDataModel.commonWallpaperData.id.uniqueId)
                .isEqualTo(SAMPLE_WALLPAPER_ID)
            assertThat(imageWallpaperDataModel.networkWallpaperData).isNull()
        }
    }
}
