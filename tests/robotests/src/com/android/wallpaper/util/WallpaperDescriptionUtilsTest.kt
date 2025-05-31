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

package com.android.wallpaper.util

import android.app.WallpaperManager
import android.app.wallpaper.WallpaperDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.PersistableBundle
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.WallpaperInfoUtils
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.getCollectionId
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.getEffects
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.getPlaceHolderColor
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.updateMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class WallpaperDescriptionUtilsTest {
    lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun updateMetadata_addsFields() {
        val content = PersistableBundle()
        val collectionId = "collectionId"
        val placeHolderColor = 12345678
        val effects = "effects"
        assertThat(getCollectionId(content)).isNull()
        assertThat(getPlaceHolderColor(content)).isNull()
        assertThat(getEffects(content)).isNull()

        updateMetadata(content, collectionId, placeHolderColor, effects)

        assertThat(getCollectionId(content)).isEqualTo(collectionId)
        assertThat(getPlaceHolderColor(content)).isEqualTo(placeHolderColor)
        assertThat(getEffects(content)).isEqualTo(effects)
    }

    @Test
    fun updateMetadata_changesFields() {
        val content =
            updateMetadata(
                content = PersistableBundle(),
                collectionId = "bogus",
                placeHolderColor = -1,
                effects = "bogus",
            )
        val collectionId = "collectionId"
        val placeHolderColor = 12345678
        val effects = "effects"
        assertThat(getCollectionId(content)).isNotNull()
        assertThat(getPlaceHolderColor(content)).isNotNull()
        assertThat(getEffects(content)).isNotNull()

        updateMetadata(content, collectionId, placeHolderColor, effects)

        assertThat(getCollectionId(content)).isEqualTo(collectionId)
        assertThat(getPlaceHolderColor(content)).isEqualTo(placeHolderColor)
        assertThat(getEffects(content)).isEqualTo(effects)
    }

    @Test
    fun liveWallpaper_toDescription_succeedsWithAllFields() {
        // Make sure that existing WallpaperDescription fields are preferred over LiveWallpaperModel
        // equivalents
        val componentName = ComponentName("package", "class")
        val contextUri = Uri.parse("uri://context")
        val content = PersistableBundle().apply { putString("key1", "value1") }
        val sourceDescription =
            WallpaperDescription.Builder()
                .setComponent(componentName)
                .setId("id")
                .setTitle("title")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(contextUri)
                .setContent(content)
                .build()
        val wallpaperInfo = WallpaperInfoUtils.createWallpaperInfo(context, componentName)
        val liveWallpaperModel =
            WallpaperModelUtils.getLiveWallpaperModel(
                wallpaperId = "unused",
                collectionId = "collectionId",
                systemWallpaperInfo = wallpaperInfo,
                description = sourceDescription,
                placeholderColor = 123,
                effectNames = "effects",
                attribution = listOf("ignored1", "ignored2"),
                actionUrl = "ignored",
            )

        val description = liveWallpaperModel.toDescription()

        assertThat(description.component).isEqualTo(componentName)
        assertThat(description.id).isEqualTo("id")
        assertThat(getCollectionId(description.content)).isEqualTo("collectionId")
        assertThat(getPlaceHolderColor(description.content)).isEqualTo(123)
        assertThat(getEffects(description.content)).isEqualTo("effects")
        assertThat(description.title).isEqualTo("title")
        assertThat(description.description).containsExactly("line1", "line2").inOrder()
        assertThat(description.contextUri).isEqualTo(contextUri)
        assertThat(description.content.containsKey("key1")).isTrue()
        assertThat(description.content.getString("key1")).isEqualTo("value1")
    }

    @Test
    fun liveWallpaper_toDescription_succeedsWithMinimumFields() {
        // Make sure that existing LiveWallpaperModel values are used when not available from
        // WallpaperDescription
        val componentName = ComponentName("package", "class")
        val contextUri = Uri.parse("uri://context")
        val sourceDescription =
            WallpaperDescription.Builder().setComponent(componentName).setId("id").build()
        val wallpaperInfo = WallpaperInfoUtils.createWallpaperInfo(context, componentName)
        val liveWallpaperModel =
            WallpaperModelUtils.getLiveWallpaperModel(
                wallpaperId = "unused",
                collectionId = "collectionId",
                systemWallpaperInfo = wallpaperInfo,
                description = sourceDescription,
                placeholderColor = 123,
                effectNames = "effects",
                attribution = listOf("title", "line1", "line2"),
                actionUrl = contextUri.toString(),
            )

        val description = liveWallpaperModel.toDescription()

        assertThat(description.component).isEqualTo(componentName)
        assertThat(description.id).isEqualTo("id")
        assertThat(getCollectionId(description.content)).isEqualTo("collectionId")
        assertThat(getPlaceHolderColor(description.content)).isEqualTo(123)
        assertThat(getEffects(description.content)).isEqualTo("effects")
        assertThat(description.title).isEqualTo("title")
        assertThat(description.description).containsExactly("line1", "line2").inOrder()
        assertThat(description.contextUri).isEqualTo(contextUri)
    }

    @Test
    fun staticWallpaper_toDescription_succeeds() {
        val contextUri = Uri.parse("uri://context")
        val cropRect = Rect(1, 2, 3, 4)
        val staticWallpaperModel =
            WallpaperModelUtils.getStaticWallpaperModel(
                wallpaperId = "unused",
                collectionId = "collectionId",
                title = "ignored",
                placeholderColor = 123,
                attribution = listOf("title", "line1", "line2"),
                actionUrl = contextUri.toString(),
            )

        val description =
            staticWallpaperModel.toDescription("id", mapOf(Point(100, 200) to cropRect))

        assertThat(description.id).isEqualTo("id")
        assertThat(description.component).isEqualTo(null)
        assertThat(getCollectionId(description.content)).isEqualTo("collectionId")
        assertThat(getPlaceHolderColor(description.content)).isEqualTo(123)
        assertThat(description.title).isEqualTo("title")
        assertThat(description.description).containsExactly("line1", "line2").inOrder()
        assertThat(description.contextUri).isEqualTo(contextUri)
        assertThat(description.cropHints.contains(WallpaperManager.ORIENTATION_PORTRAIT)).isTrue()
        assertThat(description.cropHints.get(WallpaperManager.ORIENTATION_PORTRAIT))
            .isEqualTo(cropRect)
    }
}
