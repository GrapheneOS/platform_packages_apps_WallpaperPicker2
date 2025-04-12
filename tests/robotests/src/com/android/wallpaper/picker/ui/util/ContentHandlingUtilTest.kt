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

package com.android.wallpaper.picker.ui.util

import android.app.wallpaper.WallpaperDescription
import android.content.ComponentName
import android.content.Context
import com.android.wallpaper.picker.preview.ui.util.ContentHandlingUtil
import com.android.wallpaper.picker.preview.ui.util.ContentHandlingUtil.toLiveWallpaperModel
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.WallpaperInfoUtils.Companion.createWallpaperInfo
import com.android.wallpaper.testing.WallpaperModelUtils
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
class ContentHandlingUtilTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun updatePreview_fillsInMissingComponent() {
        val wallpaperId = "id"
        val component = ComponentName("package", "class")
        val wallpaperInfo = createWallpaperInfo(context, component)
        val description = WallpaperDescription.Builder().setId(wallpaperId).build()
        val startingModel =
            WallpaperModelUtils.getLiveWallpaperModel(
                wallpaperId = wallpaperId,
                collectionId = "",
                systemWallpaperInfo = wallpaperInfo,
            )

        ContentHandlingUtil.updatePreview(context, startingModel, description) { updatedLiveModel ->
            assertThat(updatedLiveModel.liveWallpaperData.description.component)
                .isEqualTo(component)
        }
    }

    @Test
    fun updatePreview_liveWallpaper_succeeds() {
        val component = ComponentName("package", "class")
        val wallpaperInfo = createWallpaperInfo(context, component)
        val description = WallpaperDescription.Builder().setId("id").setComponent(component).build()
        val model =
            WallpaperModelUtils.getLiveWallpaperModel(
                wallpaperId = "id",
                collectionId = "",
                systemWallpaperInfo = wallpaperInfo,
            )

        ContentHandlingUtil.updatePreview(context, model, description) { updatedModel ->
            assertThat(updatedModel.liveWallpaperData.description).isEqualTo(description)
        }
    }

    @Test
    fun updatePreview_staticWallpaper_validComponent_succeeds() {
        val component = ComponentName("package", "class")
        createWallpaperInfo(context, component)
        val description = WallpaperDescription.Builder().setId("id").setComponent(component).build()
        val model =
            WallpaperModelUtils.getStaticWallpaperModel(wallpaperId = "id", collectionId = "")

        ContentHandlingUtil.updatePreview(context, model, description) { updatedModel ->
            assertThat(updatedModel.liveWallpaperData.description).isEqualTo(description)
        }
    }

    @Test
    fun updatePreview_staticWallpaper_invalidComponent_doesNothing() {
        val component = ComponentName("package", "class")
        val description = WallpaperDescription.Builder().setId("id").setComponent(component).build()
        val bogusModel =
            WallpaperModelUtils.getStaticWallpaperModel(wallpaperId = "id", collectionId = "")
        var updated = false

        ContentHandlingUtil.updatePreview(context, bogusModel, description) { updated = true }

        assertThat(updated).isFalse()
    }

    @Test
    fun queryLiveWallpaperInfo_succeeds() {
        val component = ComponentName("package", "class")
        createWallpaperInfo(context, component)

        val info = ContentHandlingUtil.queryLiveWallpaperInfo(context, component)

        assertThat(info).isNotNull()
        assertThat(info!!.packageName).isEqualTo(component.packageName)
        assertThat(info.serviceName).isEqualTo(component.className)
    }

    @Test
    fun toLiveWallpaperModel_succeeds() {
        val component = ComponentName("package", "class")
        createWallpaperInfo(context, component)
        val wallpaperId = "wallpaperId"
        val collectionId = "collectionId"
        val assetId = "asset_id"
        val isEffectWallpaper = true
        val effectNames = "some_effect"
        val description =
            WallpaperDescription.Builder().setComponent(component).setId(assetId).build()
        val staticModel =
            WallpaperModelUtils.getStaticWallpaperModel(
                wallpaperId = wallpaperId,
                collectionId = collectionId,
            )

        val newModel =
            staticModel.toLiveWallpaperModel(
                context,
                component,
                assetId,
                isEffectWallpaper,
                effectNames,
                description,
            )

        assertThat(newModel).isNotNull()
        val commonData = newModel!!.commonWallpaperData
        assertThat(commonData.id.componentName).isEqualTo(component)
        assertThat(commonData.id.uniqueId).isEqualTo("${component.className}_$assetId")
        assertThat(commonData.id.collectionId).isEqualTo(collectionId)

        val liveData = newModel.liveWallpaperData
        assertThat(liveData.systemWallpaperInfo.component).isEqualTo(component)
        assertThat(liveData.isEffectWallpaper).isEqualTo(isEffectWallpaper)
        assertThat(liveData.effectNames).isEqualTo(effectNames)
        assertThat(liveData.description).isEqualTo(description)
        assertThat(liveData.supportsMultipleEngines).isTrue()
    }
}
