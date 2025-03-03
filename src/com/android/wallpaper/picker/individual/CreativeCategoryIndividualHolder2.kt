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

package com.android.wallpaper.picker.individual

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.android.wallpaper.R
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.util.ResourceUtils

/**
 * A [ViewHolder] for the creative category individual view.
 *
 * This holder displays two wallpapers in a single item view.
 */
class CreativeCategoryIndividualHolder2(tileHeightPx: Int, itemView: View) : ViewHolder(itemView) {

    private val upperCard: View = itemView.requireViewById(R.id.upper_card)
    private val lowerCard: View = itemView.requireViewById(R.id.lower_card)

    init {
        val width = (TILE_WIDTH_SCALE_FACTOR * tileHeightPx).toInt()
        val height = (TILE_HEIGHT_SCALE_FACTOR * tileHeightPx).toInt()
        setCardSize(upperCard, width, height)
        setCardSize(lowerCard, width, height)
    }

    private fun setCardSize(view: View, tileWidthPx: Int, tileHeightPx: Int) {
        val tile: View = view.requireViewById(R.id.tile)
        val wallpaperContainer: View = view.requireViewById(R.id.wallpaper_container)
        tile.layoutParams = tile.layoutParams.apply { width = tileWidthPx }
        wallpaperContainer.layoutParams =
            wallpaperContainer.layoutParams.apply { height = tileHeightPx }
    }

    fun bind(
        upperCardWallpaper: WallpaperInfo,
        lowerCardWallpaper: WallpaperInfo?,
        onCategoryClicked: (wallpaper: WallpaperInfo) -> Unit,
    ) {
        bindWallpaperInfo(upperCard, upperCardWallpaper, onCategoryClicked)
        lowerCardWallpaper?.let { bindWallpaperInfo(lowerCard, it, onCategoryClicked) }
        lowerCard.isInvisible = lowerCardWallpaper == null
    }

    private fun bindWallpaperInfo(
        view: View,
        wallpaper: WallpaperInfo,
        onCategoryClicked: (wallpaper: WallpaperInfo) -> Unit,
    ) {
        val context: Context = view.context
        val titleView: TextView = view.requireViewById(R.id.title)
        val overlayIconView: ImageView = view.requireViewById(R.id.overlay_icon)
        val thumbnailView: ImageView = view.requireViewById(R.id.thumbnail)

        val title: String? = wallpaper.getTitle(context)
        val attributions: List<String> = wallpaper.getAttributions(context)
        val firstAttribution = if (attributions.isNotEmpty()) attributions[0] else null

        if (title != null) {
            titleView.text = title
            titleView.visibility = View.VISIBLE
            view.contentDescription = title
        } else if (firstAttribution != null) {
            val contentDescription: String? = wallpaper.getContentDescription(context)
            view.contentDescription = contentDescription ?: firstAttribution
        }

        view.setOnClickListener { onCategoryClicked.invoke(wallpaper) }

        val overlayIcon = wallpaper.getOverlayIcon(context)
        if (overlayIcon != null) {
            overlayIconView.setImageDrawable(overlayIcon)
        } else {
            wallpaper
                .getThumbAsset(context)
                .loadDrawable(
                    context,
                    thumbnailView,
                    ResourceUtils.getColorAttr(context, android.R.attr.colorSecondary),
                )
        }
    }

    companion object {
        private const val TILE_HEIGHT_SCALE_FACTOR: Float = 1.2f
        private const val TILE_WIDTH_SCALE_FACTOR: Float = 0.95f
    }
}
