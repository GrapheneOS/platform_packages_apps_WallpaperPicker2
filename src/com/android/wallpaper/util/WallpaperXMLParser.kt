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
import android.util.Xml
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.SystemStaticWallpaperInfo
import com.android.wallpaper.model.WallpaperCategory
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.PartnerProvider
import javax.inject.Inject
import javax.inject.Singleton
import org.xmlpull.v1.XmlPullParser

/**
 * Utility class for parsing an XML file containing information about a list of wallpapers. The
 * logic in this class has been extracted into a separate class which uses dependency injection and
 * was earlier present in a single method.
 */
@Singleton
class WallpaperXMLParser
@Inject
constructor(private val context: Context, private val partnerProvider: PartnerProvider) :
    WallpaperXMLParserInterface {

    override fun parseCategory(parser: XmlResourceParser): WallpaperCategory? {
        val categoryBuilder =
            WallpaperCategory.Builder(partnerProvider.resources, Xml.asAttributeSet(parser))
        categoryBuilder.setPriorityIfEmpty(PRIORITY_SYSTEM)
        var publishedPlaceholder = false
        val pair = parseXML(parser, parser.depth, categoryBuilder, false)
        publishedPlaceholder = pair.first
        val category = categoryBuilder.build()
        return if (category.unmodifiableWallpapers.isNotEmpty()) category else null
    }

    private fun parseXML(
        parser: XmlResourceParser,
        categoryDepth: Int,
        categoryBuilder: WallpaperCategory.Builder,
        publishedPlaceholder: Boolean
    ): Pair<Boolean, Int> {
        var type1 = parser.eventType
        var publishedPlaceholder1 = publishedPlaceholder
        while (
            parser.next().also { type1 = it } != XmlPullParser.END_TAG ||
                parser.depth > categoryDepth
        ) {
            if (type1 == XmlPullParser.START_TAG) {
                var wallpaper: WallpaperInfo? = null
                if (SystemStaticWallpaperInfo.TAG_NAME == parser.name) {
                    wallpaper =
                        SystemStaticWallpaperInfo.fromAttributeSet(
                            partnerProvider.packageName,
                            categoryBuilder.id,
                            Xml.asAttributeSet(parser)
                        )
                } else if (LiveWallpaperInfo.TAG_NAME == parser.name) {
                    wallpaper =
                        LiveWallpaperInfo.fromAttributeSet(
                            context,
                            categoryBuilder.id,
                            Xml.asAttributeSet(parser)
                        )
                }
                if (wallpaper != null) {
                    categoryBuilder.addWallpaper(wallpaper)
                    if (!publishedPlaceholder1) {
                        publishedPlaceholder1 = true
                    }
                }
            }
        }
        return Pair(publishedPlaceholder1, type1)
    }

    companion object {
        private const val PRIORITY_SYSTEM = 100
    }
}
