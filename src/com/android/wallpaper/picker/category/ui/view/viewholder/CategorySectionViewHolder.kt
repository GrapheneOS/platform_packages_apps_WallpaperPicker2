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

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.ui.view.adapter.CategoryAdapter
import com.android.wallpaper.picker.category.ui.viewmodel.SectionViewModel
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

/** This view holder caches reference to pertinent views in a [CategorySectionView] */
class CategorySectionViewHolder(itemView: View, val windowWidth: Int) :
    RecyclerView.ViewHolder(itemView) {

    // recycler view for the tiles
    private var sectionTiles: RecyclerView

    // title for the section
    private var sectionTitle: TextView
    init {
        sectionTiles = itemView.requireViewById(R.id.category_wallpaper_tiles)
        sectionTitle = itemView.requireViewById(R.id.section_title)
    }

    fun bind(item: SectionViewModel) {
        // TODO: this probably is not necessary but if in the case the sections get updated we
        //  should just update the adapter instead of instantiating a new instance
        sectionTiles.adapter = CategoryAdapter(item.items, item.columnCount, windowWidth)

        val layoutManager = FlexboxLayoutManager(itemView.context)

        // Horizontal orientation
        layoutManager.flexDirection = FlexDirection.ROW

        // disable wrapping to make sure everything fits on a single row
        layoutManager.flexWrap = FlexWrap.NOWRAP

        // Stretch items to fill the horizontal axis
        layoutManager.alignItems = AlignItems.STRETCH

        // Distribute items evenly on the horizontal axis
        layoutManager.justifyContent = JustifyContent.SPACE_AROUND

        sectionTiles.layoutManager = layoutManager as RecyclerView.LayoutManager?

        if (item.items.size > 1) {
            sectionTitle.text = "Section title" // TODO: update view model to include section title
            sectionTitle.visibility = View.VISIBLE
        } else {
            sectionTitle.visibility = View.GONE
        }
    }
}
