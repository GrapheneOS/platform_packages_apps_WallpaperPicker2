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
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.categorypicker.viewmodel.TileViewModel
import com.android.wallpaper.util.ResourceUtils
import com.bumptech.glide.Glide

/** Caches and binds [TileViewHolder] to a [WallpaperTileView] */
class TileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var title: TextView
    private var categorySubtitle: TextView
    private var wallpaperCategoryImage: ImageView

    init {
        title = itemView.requireViewById(R.id.tile_title)
        categorySubtitle = itemView.requireViewById(R.id.category_title)
        wallpaperCategoryImage = itemView.requireViewById(R.id.image)
    }

    fun bind(item: TileViewModel, context: Context, displayDensity: Float) {
        // TODO: the tiles binding has a lot more logic which will be handled in a dedicated binder
        // TODO: size the tiles appropriately
        title.visibility = View.GONE

        // Size the overlay icon according to the category.
        val thumbnailDimenDp: Int = 75 // TODO: calculate the correct size of the thumbnail
        val thumbnailDimenPx = (thumbnailDimenDp * displayDensity).toInt()
        wallpaperCategoryImage.getLayoutParams().width = thumbnailDimenPx
        wallpaperCategoryImage.getLayoutParams().height = thumbnailDimenPx
        if (item.thumbAsset == null) {
            val placeHolderColor =
                ResourceUtils.getColorAttr(context, android.R.attr.colorSecondary)
            item.thumbAsset?.loadDrawable(context, wallpaperCategoryImage, placeHolderColor)
        } else {
            // defaulting to solid color if assets are null
            wallpaperCategoryImage.setBackgroundColor(
                itemView.context.getResources().getColor(R.color.myphoto_background_color)
            )
            val nullObj: Any? = null
            Glide.with(itemView.context).asDrawable().load(nullObj).into(wallpaperCategoryImage)
        }
        categorySubtitle.text = item.text
    }
}
