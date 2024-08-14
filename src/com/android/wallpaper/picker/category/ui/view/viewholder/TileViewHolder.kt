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

package com.android.wallpaper.picker.category.ui.view.viewholder

import android.content.Context
import android.graphics.Point
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.ui.viewmodel.TileViewModel
import com.android.wallpaper.util.ResourceUtils
import com.android.wallpaper.util.SizeCalculator

/** Caches and binds [TileViewHolder] to a [WallpaperTileView] */
class TileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var title: TextView
    private var categorySubtitle: TextView
    private var wallpaperCategoryImage: ImageView
    private var categoryCardView: CardView

    init {
        title = itemView.requireViewById(R.id.tile_title)
        categorySubtitle = itemView.requireViewById(R.id.category_title)
        wallpaperCategoryImage = itemView.requireViewById(R.id.image)
        categoryCardView = itemView.requireViewById(R.id.category)
    }

    fun bind(
        item: TileViewModel,
        context: Context,
        columnCount: Int,
        tileCount: Int,
        windowWidth: Int
    ) {
        title.visibility = View.GONE

        var tileSize: Point
        var tileRadius: Int
        // calculate the height
        if (columnCount == 1 && tileCount == 1) {
            // sections that take 1 column and have 1 tile
            tileSize = SizeCalculator.getCategoryTileSize(itemView.context, windowWidth)
            tileRadius = context.resources.getDimension(R.dimen.grid_item_all_radius_small).toInt()
        } else if (columnCount > 1 && tileCount == 1) {
            // sections with more than 1 column and 1 tile
            tileSize = SizeCalculator.getFeaturedCategoryTileSize(itemView.context, windowWidth)
            tileRadius = tileSize.y
        } else {
            // sections witch take more than 1 column and have more than 1 tile
            tileSize = SizeCalculator.getFeaturedCategoryTileSize(itemView.context, windowWidth)
            tileSize.y /= 2
            tileRadius = context.resources.getDimension(R.dimen.grid_item_all_radius).toInt()
        }

        wallpaperCategoryImage.getLayoutParams().height = tileSize.y
        categoryCardView.radius = tileRadius.toFloat()

        if (item.thumbnailAsset != null) {
            val placeHolderColor =
                ResourceUtils.getColorAttr(context, android.R.attr.colorSecondary)
            item.thumbnailAsset.loadDrawable(context, wallpaperCategoryImage, placeHolderColor)
        } else {
            wallpaperCategoryImage.setImageDrawable(item.defaultDrawable)
            wallpaperCategoryImage.setBackgroundColor(
                context.resources.getColor(R.color.myphoto_background_color)
            )
        }
        categorySubtitle.text = item.text

        // bind the tile action to the button
        itemView.setOnClickListener { _ -> item.onClicked?.invoke() }
    }
}
