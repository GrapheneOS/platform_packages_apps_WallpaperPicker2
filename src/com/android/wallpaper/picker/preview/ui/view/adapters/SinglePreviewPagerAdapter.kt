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
package com.android.wallpaper.picker.preview.ui.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags

/**
 * This adapter provides preview views for the small preview fragment
 *
 * TODO(b/361583350): Use [PreviewPagerAdapter], remove this class once new picker UI is released
 */
class SinglePreviewPagerAdapter(
    private val onBindViewHolder: (ViewHolder, Int) -> Unit,
) : RecyclerView.Adapter<SinglePreviewPagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layout =
            if (BaseFlags.get().isNewPickerUi()) R.layout.small_preview_handheld_card_view2
            else R.layout.small_preview_handheld_card_view

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)

        view.setPadding(
            0,
            parent.resources.getDimension(R.dimen.small_preview_preview_top_padding).toInt(),
            0,
            parent.resources.getDimension(R.dimen.small_preview_preview_bottom_padding).toInt(),
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder.invoke(holder, position)
    }

    override fun getItemCount(): Int = PREVIEW_PAGER_ITEM_COUNT

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        const val PREVIEW_PAGER_ITEM_COUNT = 2
    }
}
