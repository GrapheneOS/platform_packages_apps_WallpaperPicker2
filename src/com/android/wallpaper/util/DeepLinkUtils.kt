/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Intent

/** Util class for deep link. */
object DeepLinkUtils {
    private const val KEY_COLLECTION_ID = "collection_id"
    private const val SCHEME = "https"
    private const val SCHEME_SPECIFIC_PART_PREFIX = "//g.co/wallpaper"
    const val EXTRA_KEY_COLLECTION_ID = "extra_collection_id"

    /** Checks if it is the deep link case. */
    @JvmStatic
    fun isDeepLink(intent: Intent): Boolean {
        val data = intent.data
        return data != null &&
            SCHEME == data.scheme &&
            data.schemeSpecificPart.startsWith(SCHEME_SPECIFIC_PART_PREFIX)
    }

    /**
     * Gets the wallpaper collection which wants to deep link to.
     *
     * @return the wallpaper collection id
     */
    @JvmStatic
    fun getCollectionId(intent: Intent): String? {
        return if (isDeepLink(intent)) intent.data?.getQueryParameter(KEY_COLLECTION_ID)
        else intent.getStringExtra(EXTRA_KEY_COLLECTION_ID)
    }
}
