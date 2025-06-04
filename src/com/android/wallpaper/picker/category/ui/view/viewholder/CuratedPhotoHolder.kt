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
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.category.ui.viewmodel.TileViewModel
import com.android.wallpaper.picker.customization.animation.view.LoadingAnimation2
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.util.CuratedPhotosTimeUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class CuratedPhotoHolder(
    itemView: View,
    private val curatedPhotosTimeUtil: CuratedPhotosTimeUtil,
    private val userEventLogger: UserEventLogger,
) : RecyclerView.ViewHolder(itemView) {

    var backgroundColorBinding: ColorUpdateBinder.Binding? = null
    var loadingAnimation: LoadingAnimation2? = null
    private val curatedPhotoImage: ImageView = itemView.requireViewById(R.id.carousel_image_view)

    private val curatedPhotoTitle: TextView = itemView.requireViewById(R.id.carousel_text_view)

    fun bind(item: TileViewModel, context: Context, isFirst: Boolean) {
        curatedPhotoImage.contentDescription = item.contentDescription
        item.thumbnailAsset?.let { asset ->
            asset.loadDrawableWithTransition(
                context,
                curatedPhotoImage,
                context.resources.getInteger(android.R.integer.config_mediumAnimTime),
                {
                    val startTime = curatedPhotosTimeUtil.getStartTime()
                    val timeMilliseconds = System.currentTimeMillis() - startTime
                    userEventLogger.logCuratedPhotosRendered(timeMilliseconds, true)
                    loadingAnimation?.playRevealAnimation {
                        loadingAnimation = null
                        backgroundColorBinding?.destroy()
                        backgroundColorBinding = null
                    }
                },
                context.getColor(R.color.system_surface_bright),
            )
        }
            ?: run {
                // Glide will render the gif and on completion or failure will dismiss the
                // place-holder animation
                Glide.with(itemView.context)
                    .load(item.defaultDrawable)
                    .addListener(
                        object : RequestListener<Drawable> {
                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>,
                                dataSource: DataSource,
                                isFirstResource: Boolean,
                            ): Boolean {
                                val startTime = curatedPhotosTimeUtil.getStartTime()
                                val timeMilliseconds = System.currentTimeMillis() - startTime
                                userEventLogger.logCuratedPhotosRendered(timeMilliseconds, false)
                                loadingAnimation?.playRevealAnimation {
                                    loadingAnimation = null
                                    backgroundColorBinding?.destroy()
                                    backgroundColorBinding = null
                                }
                                return false
                            }

                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean,
                            ): Boolean {
                                // TODO(b/406560705): define behaviour if gif loading fails
                                loadingAnimation?.playRevealAnimation {
                                    loadingAnimation = null
                                    backgroundColorBinding?.destroy()
                                    backgroundColorBinding = null
                                }
                                return false
                            }
                        }
                    )
                    .into(curatedPhotoImage)
            }
        curatedPhotoImage.layoutParams.height =
            context.resources.getDimension(R.dimen.curated_photo_height).toInt()

        updateTitleVisibility(item, context, isFirst)

        itemView.setOnClickListener { _ -> item.onClicked?.invoke() }
    }

    public fun updateTitleVisibility(item: TileViewModel, context: Context, isFirst: Boolean) {
        curatedPhotoTitle.text = item.text
        curatedPhotoTitle.visibility =
            if (isFirst && item.showTitle) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (isFirst && item.showTitle) {
            val gradientDrawable =
                ContextCompat.getDrawable(context, R.drawable.carousel_item_gradient_overlay)
            curatedPhotoImage.foreground = gradientDrawable
        } else {
            curatedPhotoImage.foreground = null
        }
    }

    fun cleanUp() {
        backgroundColorBinding?.destroy()
        backgroundColorBinding = null
        loadingAnimation?.cancel()
        loadingAnimation = null
    }
}
