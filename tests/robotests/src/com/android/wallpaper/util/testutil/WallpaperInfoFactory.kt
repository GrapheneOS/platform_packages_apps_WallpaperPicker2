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

package com.android.wallpaper.util.testutil

import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.net.Uri
import com.android.wallpaper.model.CreativeWallpaperInfo
import com.android.wallpaper.model.CurrentWallpaperInfo

/**
 * This class is used for creating different types of WallpaperInfo objects that are used in tests.
 */
class WallpaperInfoFactory {

    companion object {
        private val SAMPLE_GROUP_NAME = "Sample-Group-Name"
        private val SAMPLE_AUTHOR = "Sample-Author"
        private val SAMPLE_TITLE = "Sample-Title"
        private val SAMPLE_DESCRIPTION = "Sample-Description"
        private val SAMPLE_CONTENT_DESCRIPTION = "Sample-Content-Description"

        private val SAMPLE_CONFIG_PREVIEW_URI = Uri.parse("Sample-Config-Preview-URI")
        private val CLEAN_PREVIEW_URI = Uri.parse("content://sample/clean_preview_uri")
        private val DELETE_URI = Uri.parse("content://sample/delete_uri")
        private val THUMBNAIL_URI = Uri.parse("content://sample/thumbnail_uri")
        private val SHARE_URI = Uri.parse("content://sample/share_uri")
        private val SAMPLE_COLLECTION_ID = "Sample-Collection-ID"
        private val SAMPLE_ACTION_URL = "Sample-Action-URL"

        // Utility method to create a sample CreativeWallpaperInfo object
        fun createCreativeWallpaperInfo(
            mockWallpaperInfo: WallpaperInfo,
        ): CreativeWallpaperInfo {
            return CreativeWallpaperInfo(
                mockWallpaperInfo,
                SAMPLE_TITLE,
                SAMPLE_AUTHOR,
                SAMPLE_DESCRIPTION,
                SAMPLE_CONTENT_DESCRIPTION,
                SAMPLE_CONFIG_PREVIEW_URI,
                CLEAN_PREVIEW_URI,
                DELETE_URI,
                THUMBNAIL_URI,
                SHARE_URI,
                SAMPLE_GROUP_NAME,
                true
            )
        }

        // Utility method to create a sample CurrentWallpaperInfo object
        fun createCurrentWallpaperInfo(): CurrentWallpaperInfo {
            val attributions: MutableList<String> = ArrayList()
            attributions.add(SAMPLE_TITLE)
            val currentWallpaperInfo =
                CurrentWallpaperInfo(
                    attributions,
                    SAMPLE_ACTION_URL,
                    1,
                    1,
                    SAMPLE_COLLECTION_ID,
                    WallpaperManager.FLAG_SYSTEM
                )
            return currentWallpaperInfo
        }
    }
}
