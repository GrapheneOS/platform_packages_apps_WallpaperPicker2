/*
 * Copyright 2023 The Android Open Source Project
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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo

/**
 * CreativeCategoryAdapter subclass for a creative category wallpaper tile in the RecyclerView which
 * internally creates the CreativeCategoryIndividualHolder instance for the tile .
 */
class CreativeCategoryAdapter(
    private val layoutInflater: LayoutInflater,
    private val tileSizePx: Int,
    private val onCategoryClicked: (wallpaper: WallpaperInfo) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var listItems: List<Pair<WallpaperInfo, WallpaperInfo?>> = emptyList()

    fun setItems(items: List<WallpaperInfo>) {
        listItems = items.toPairs()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return createIndividualHolder(parent)
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val wallpaperInfoPair = listItems[position]
        (wallpaperInfoPair.first as? LiveWallpaperInfo)?.isVisibleTitle = true
        (wallpaperInfoPair.second as? LiveWallpaperInfo)?.isVisibleTitle = true
        val creativeCategoryViewHolder = holder as? CreativeCategoryIndividualHolder2 ?: return
        creativeCategoryViewHolder.bind(
            wallpaperInfoPair.first,
            wallpaperInfoPair.second,
            onCategoryClicked,
        )
    }

    private fun List<WallpaperInfo>.toPairs(): List<Pair<WallpaperInfo, WallpaperInfo?>> {
        val result = mutableListOf<Pair<WallpaperInfo, WallpaperInfo?>>()
        for (i in indices step 2) {
            val first = this[i]
            val second = if (i + 1 < size) this[i + 1] else null
            result.add(Pair(first, second))
        }
        return result
    }

    private fun createIndividualHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view: View =
            layoutInflater.inflate(R.layout.creative_category_individual_item_view, parent, false)
        return CreativeCategoryIndividualHolder2(tileSizePx, view)
    }
}
