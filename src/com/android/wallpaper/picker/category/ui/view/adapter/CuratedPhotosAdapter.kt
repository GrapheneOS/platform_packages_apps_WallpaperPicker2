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
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.ui.view.viewholder.CuratedPhotoHolder
import com.android.wallpaper.picker.category.ui.viewmodel.TileViewModel

/** Custom adaptor for curated photos carousel in the categories page. */
class CuratedPhotosAdapter(val items: List<TileViewModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var visiblePosition = -1 // Track the position of the visible TextView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return createIndividualHolder(parent)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val tile: TileViewModel = items[position]
        (holder as CuratedPhotoHolder?)?.bind(
            tile,
            holder.itemView.context,
            this.visiblePosition == position,
        )
    }

    private fun createIndividualHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View = layoutInflater.inflate(R.layout.curated_photo_tile, parent, false)
        return CuratedPhotoHolder(view)
    }

    fun setVisiblePosition(position: Int) {
        val previousPosition = this.visiblePosition
        this.visiblePosition = position
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
        notifyItemChanged(position)
    }
}
