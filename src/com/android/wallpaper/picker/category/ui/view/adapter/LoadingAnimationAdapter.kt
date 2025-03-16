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

package com.android.wallpaper.picker.category.ui.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.systemui.monet.ColorScheme
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.ui.view.viewholder.CuratedPhotoHolder
import com.android.wallpaper.picker.customization.animation.view.LoadingAnimation2
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.util.ResourceUtils

/** Custom adaptor for curated photos carousel in the categories page. */
class LoadingAnimationAdapter(
    private val size: Int,
    private val colorUpdateViewModel: ColorUpdateViewModel,
    private val shouldAnimateColor: () -> Boolean,
    private val lifecycleOwner: LifecycleOwner,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return createIndividualHolder(parent)
    }

    override fun getItemCount(): Int {
        return size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as CuratedPhotoHolder).apply {
            val imageView: ImageView =
                itemView.requireViewById<ImageView>(R.id.carousel_image_view).apply {
                    layoutParams.height =
                        context.resources.getDimension(R.dimen.curated_photo_height).toInt()
                }
            backgroundColorBinding?.destroy()
            backgroundColorBinding =
                ColorUpdateBinder.bind(
                    setColor = { color -> imageView.setBackgroundColor(color) },
                    color = colorUpdateViewModel.colorSurfaceBright,
                    shouldAnimate = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
            loadingAnimation?.cancel()
            loadingAnimation =
                LoadingAnimation2(imageView).apply {
                    updateColor(
                        ColorScheme(
                            ResourceUtils.getColorAttr(
                                holder.itemView.context,
                                android.R.attr.colorAccent,
                            ),
                            imageView.resources.configuration.isNightModeActive,
                        )
                    )
                    playLoadingAnimation()
                }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as CuratedPhotoHolder).cleanUp()
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        (holder as CuratedPhotoHolder).cleanUp()
        return super.onFailedToRecycleView(holder)
    }

    private fun createIndividualHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View = layoutInflater.inflate(R.layout.curated_photo_tile, parent, false)
        return CuratedPhotoHolder(view)
    }
}
