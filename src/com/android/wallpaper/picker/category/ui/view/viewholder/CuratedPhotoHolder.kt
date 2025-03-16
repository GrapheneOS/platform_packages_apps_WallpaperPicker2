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
import com.android.wallpaper.picker.category.ui.viewmodel.TileViewModel
import com.android.wallpaper.picker.customization.animation.view.LoadingAnimation2
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder

class CuratedPhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var backgroundColorBinding: ColorUpdateBinder.Binding? = null
    var loadingAnimation: LoadingAnimation2? = null
    private val curatedPhotoImage: ImageView = itemView.requireViewById(R.id.carousel_image_view)
    private val curatedPhotoTitle: TextView = itemView.requireViewById(R.id.carousel_text_view)

    fun bind(item: TileViewModel, context: Context, isFirst: Boolean) {
        item.thumbnailAsset?.loadDrawableWithTransition(
            context,
            curatedPhotoImage,
            context.resources.getInteger(android.R.integer.config_mediumAnimTime),
            {
                loadingAnimation?.playRevealAnimation {
                    loadingAnimation = null
                    backgroundColorBinding?.destroy()
                    backgroundColorBinding = null
                }
            },
            context.getColor(R.color.system_surface_bright),
        )
        curatedPhotoImage.layoutParams.height =
            context.resources.getDimension(R.dimen.curated_photo_height).toInt()

        curatedPhotoTitle.text = item.text
        curatedPhotoTitle.visibility =
            if (isFirst && item.showTitle) {
                View.VISIBLE
            } else {
                View.GONE
            }

        itemView.setOnClickListener { _ -> item.onClicked?.invoke() }
    }

    fun cleanUp() {
        backgroundColorBinding?.destroy()
        backgroundColorBinding = null
        loadingAnimation?.cancel()
        loadingAnimation = null
    }
}
