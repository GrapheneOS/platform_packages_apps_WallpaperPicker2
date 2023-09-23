/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.preview.ui.binder.SmallPreviewBinder
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.CoroutineScope

/** This adapter provides preview views for the small preview fragment */
class SinglePreviewPagerAdapter(
    val applicationContext: Context,
    val isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
    val lifecycleOwner: LifecycleOwner,
    val isRtl: Boolean,
    val mainScope: CoroutineScope,
    // TODO: provide appropriate view models
    val wallpaperPreviewViewModels: List<WallpaperPreviewViewModel>
) : RecyclerView.Adapter<SinglePreviewPagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.preview_handheld_card_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = wallpaperPreviewViewModels.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val previewView: CardView = itemView.requireViewById(R.id.preview)

        fun bind(position: Int) {
            SmallPreviewBinder.bind(
                applicationContext = applicationContext,
                view = previewView,
                viewModel = wallpaperPreviewViewModels[position],
                mainScope = mainScope,
                lifecycleOwner = lifecycleOwner,
                isSingleDisplayOrUnfoldedHorizontalHinge = isSingleDisplayOrUnfoldedHorizontalHinge,
                isRtl = isRtl,
            )
        }
    }
}
