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

import android.app.wallpaper.WallpaperDescription
import android.graphics.Point
import android.graphics.Rect
import android.os.PersistableBundle
import androidx.core.net.toUri
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.updateMetadata

/**
 * Utilities for [WallpaperDescription], such as manipulating picker-specific metadata in the
 * content bundle.
 */
class WallpaperDescriptionUtils {

    companion object {
        private const val CONTENT_KEY_COLLECTION_ID = "picker_metadata_collection_id"
        private const val CONTENT_KEY_PLACEHOLDER_COLOR = "picker_metadata_placeholder_color"
        private const val CONTENT_KEY_EFFECTS = "picker_metadata_effects"

        /**
         * Updates the content bundle with picker-specific metadata.
         *
         * Changes the bundle in place, and also returns the updated bundle for convenience.
         */
        fun updateMetadata(
            content: PersistableBundle,
            collectionId: String?,
            placeHolderColor: Int?,
            effects: String?,
        ): PersistableBundle {
            return content.apply {
                putString(CONTENT_KEY_COLLECTION_ID, collectionId)
                placeHolderColor?.let { putInt(CONTENT_KEY_PLACEHOLDER_COLOR, it) }
                effects?.let { putString(CONTENT_KEY_EFFECTS, it) }
            }
        }

        fun getCollectionId(content: PersistableBundle): String? {
            return content.getString(CONTENT_KEY_COLLECTION_ID)
        }

        fun getPlaceHolderColor(content: PersistableBundle): Int? {
            return if (content.containsKey(CONTENT_KEY_PLACEHOLDER_COLOR))
                content.getInt(CONTENT_KEY_PLACEHOLDER_COLOR)
            else null
        }

        fun getEffects(content: PersistableBundle): String? {
            return content.getString(CONTENT_KEY_EFFECTS)
        }
    }
}

fun LiveWallpaperModel.toDescription(): WallpaperDescription {
    val content =
        updateMetadata(
            liveWallpaperData.description.content,
            commonWallpaperData.id.collectionId,
            commonWallpaperData.placeholderColorInfo.placeholderColor,
            liveWallpaperData.effectNames,
        )
    val attribs = commonWallpaperData.attributions
    val title = attribs?.getOrNull(0)
    val desc = attribs?.slice(1 until attribs.size).orEmpty()
    return liveWallpaperData.description.let { source ->
        source
            .toBuilder()
            .setTitle(source.title ?: title)
            .setDescription(if (source.description.size > 0) source.description else desc)
            .setContextUri(source.contextUri ?: commonWallpaperData.exploreActionUrl?.toUri())
            .setContent(content)
            .build()
    }
}

fun StaticWallpaperModel.toDescription(
    id: String,
    cropHints: Map<Point, Rect>,
): WallpaperDescription {
    val content =
        updateMetadata(
            PersistableBundle(),
            commonWallpaperData.id.collectionId,
            commonWallpaperData.placeholderColorInfo.placeholderColor,
            null,
        )
    val attribs = commonWallpaperData.attributions
    val title = attribs?.getOrNull(0)
    val desc = attribs?.subList(1, attribs.size).orEmpty()
    return WallpaperDescription.Builder()
        .setId(id)
        .setTitle(title)
        .setDescription(desc)
        .setContextUri(commonWallpaperData.exploreActionUrl?.toUri())
        .setCropHints(cropHints)
        .setContent(content)
        .build()
}
