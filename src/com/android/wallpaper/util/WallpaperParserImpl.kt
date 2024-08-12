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

import android.content.Context
import android.content.res.XmlResourceParser
import android.util.Log
import android.util.Xml
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.PartnerWallpaperInfo
import com.android.wallpaper.model.SystemStaticWallpaperInfo
import com.android.wallpaper.model.WallpaperCategory
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.PartnerProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/**
 * Utility class for parsing an XML file containing information about a list of wallpapers. The
 * logic in this class has been extracted into a separate class which uses dependency injection and
 * was earlier present in a single method.
 */
@Singleton
class WallpaperParserImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val partnerProvider: PartnerProvider
) : WallpaperParser {

    /** This method is responsible for parsing the XML file for system categories. */
    override fun parseSystemCategories(parser: XmlResourceParser): List<WallpaperCategory> {
        val categories = mutableListOf<WallpaperCategory>()
        try {
            var priorityTracker = 0
            val depth = parser.depth
            var type: Int
            while (
                (parser.next().also { type = it } != XmlPullParser.END_TAG ||
                    parser.depth > depth) && type != XmlPullParser.END_DOCUMENT
            ) {
                if (type == XmlPullParser.START_TAG && WallpaperCategory.TAG_NAME == parser.name) {
                    val categoryBuilder =
                        WallpaperCategory.Builder(
                            partnerProvider.resources,
                            Xml.asAttributeSet(parser)
                        )
                    categoryBuilder.setPriorityIfEmpty(PRIORITY_SYSTEM + priorityTracker++)
                    var publishedPlaceholder = false
                    val categoryDepth = parser.depth
                    while (
                        (parser.next().also { type = it } != XmlPullParser.END_TAG ||
                            parser.depth > categoryDepth) && type != XmlPullParser.END_DOCUMENT
                    ) {
                        if (type == XmlPullParser.START_TAG) {
                            val wallpaper =
                                if (SystemStaticWallpaperInfo.TAG_NAME == parser.name) {
                                    SystemStaticWallpaperInfo.fromAttributeSet(
                                        partnerProvider.packageName,
                                        categoryBuilder.id,
                                        Xml.asAttributeSet(parser)
                                    )
                                } else if (LiveWallpaperInfo.TAG_NAME == parser.name) {
                                    LiveWallpaperInfo.fromAttributeSet(
                                        context,
                                        categoryBuilder.id,
                                        Xml.asAttributeSet(parser)
                                    )
                                } else {
                                    null
                                }
                            if (wallpaper != null) {
                                categoryBuilder.addWallpaper(wallpaper)
                                if (!publishedPlaceholder) {
                                    publishedPlaceholder = true
                                }
                            }
                        }
                    }
                    val category = categoryBuilder.build()
                    if (!category.getUnmodifiableWallpapers().isEmpty()) {
                        categories.add(category)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is IOException,
                is XmlPullParserException -> {
                    Log.w(TAG, "Failed to parse the XML file of system wallpapers", e)
                    return emptyList()
                }
                else -> throw e
            }
        }
        return categories
    }

    /**
     * This method is responsible for parsing resources for PartnerWallpaperInfo wallpapers and
     * returning a list of such wallpapers.
     */
    override fun parsePartnerWallpaperInfoResources(): List<WallpaperInfo> {
        val wallpaperInfos: MutableList<WallpaperInfo> = ArrayList()

        val partnerRes = partnerProvider.getResources()
        val packageName = partnerProvider.getPackageName()
        if (partnerRes == null) {
            return wallpaperInfos
        }

        val resId =
            partnerRes.getIdentifier(PartnerProvider.LEGACY_WALLPAPER_RES_ID, "array", packageName)
        // Certain partner configurations don't have wallpapers provided, so need to check; return
        // early if they are missing.
        if (resId == 0) {
            return wallpaperInfos
        }

        val extras = partnerRes.getStringArray(resId)
        for (extra in extras) {
            val wpResId = partnerRes.getIdentifier(extra, "drawable", packageName)
            if (wpResId != 0) {
                val thumbRes = partnerRes.getIdentifier(extra + "_small", "drawable", packageName)
                if (thumbRes != 0) {
                    val wallpaperInfo: WallpaperInfo = PartnerWallpaperInfo(thumbRes, wpResId)
                    wallpaperInfos.add(wallpaperInfo)
                }
            } else {
                Log.e(TAG, "Couldn't find wallpaper $extra")
            }
        }

        return wallpaperInfos
    }

    companion object {
        const val PRIORITY_SYSTEM = 100
        private const val TAG = "WallpaperXMLParser"
    }
}
