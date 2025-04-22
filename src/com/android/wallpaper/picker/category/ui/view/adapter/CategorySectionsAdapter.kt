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

package com.android.wallpaper.picker.category.ui.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.ui.binder.BannerProvider
import com.android.wallpaper.picker.category.ui.view.viewholder.CategorySectionViewHolder
import com.android.wallpaper.picker.category.ui.view.viewholder.CuratedPhotoSectionViewHolder
import com.android.wallpaper.picker.category.ui.viewmodel.PhotosViewModel
import com.android.wallpaper.picker.category.ui.viewmodel.SectionViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel

class CategorySectionsAdapter(
    var items: List<SectionViewModel>,
    private val windowWidth: Int,
    private val colorUpdateViewModel: ColorUpdateViewModel,
    private val shouldAnimateColor: () -> Boolean,
    private val lifecycleOwner: LifecycleOwner,
    private val onSignInBannerDismissed: (dismissed: Boolean) -> Unit,
    private var bannerProvider: BannerProvider,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return createIndividualHolder(parent, viewType)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PhotosViewModel -> SectionViewType.PHOTOS.value
            is SectionViewModel -> SectionViewType.CATEGORY.value
            else ->
                throw IllegalStateException(
                    "Unsupported view model type found at position $position" +
                        "Adapter only handles PhotosViewModel and SectionViewModel."
                )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val section: SectionViewModel = items[position]
        when (holder) {
            is CuratedPhotoSectionViewHolder ->
                holder.bind(
                    item = (section as PhotosViewModel),
                    colorUpdateViewModel = colorUpdateViewModel,
                    shouldAnimateColor = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                    bannerProvider = bannerProvider,
                    isSignInBannerVisible = (section as PhotosViewModel).isDismissed,
                    onSignInBannerDismissed = onSignInBannerDismissed,
                )
            is CategorySectionViewHolder ->
                holder.bind(
                    item = section,
                    colorUpdateViewModel = colorUpdateViewModel,
                    shouldAnimateColor = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
        }
    }

    private fun createIndividualHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View = layoutInflater.inflate(R.layout.category_section_view, parent, false)
        return when (viewType) {
            SectionViewType.PHOTOS.value -> {
                CuratedPhotoSectionViewHolder(view, windowWidth)
            }
            SectionViewType.CATEGORY.value -> {
                CategorySectionViewHolder(view, windowWidth)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    enum class SectionViewType(val value: Int) {
        CATEGORY(1),
        PHOTOS(2),
    }

    companion object {
        private const val TAG = "CategorySectionsAdapter"
    }
}
