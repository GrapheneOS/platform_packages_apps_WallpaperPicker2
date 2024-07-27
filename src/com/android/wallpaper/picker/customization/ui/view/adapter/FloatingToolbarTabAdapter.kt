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

package com.android.wallpaper.picker.customization.ui.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.view.animator.TabItemAnimator.Companion.BACKGROUND_ALPHA_MAX
import com.android.wallpaper.picker.customization.ui.view.animator.TabItemAnimator.Companion.SELECT_ITEM
import com.android.wallpaper.picker.customization.ui.view.animator.TabItemAnimator.Companion.UNSELECT_ITEM
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel

/** List adapter for the floating toolbar of tabs. */
class FloatingToolbarTabAdapter :
    ListAdapter<FloatingToolbarTabViewModel, FloatingToolbarTabAdapter.TabViewHolder>(
        ProductDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.floating_toolbar_tab,
                    parent,
                    false,
                )
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TabViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val payload = if (payloads.isNotEmpty()) payloads[0] as? Int else null
        val item = getItem(position)
        when (payload) {
            SELECT_ITEM -> {
                // When transition from unselected to selected, initial state should be unselected
                bindViewHolder(holder, item.icon, item.text, false, item.onClick)
            }
            UNSELECT_ITEM -> {
                // When transition from selected to unselected, initial state should be selected
                bindViewHolder(holder, item.icon, item.text, true, item.onClick)
            }
            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val item = getItem(position)
        bindViewHolder(holder, item.icon, item.text, item.isSelected, item.onClick)
    }

    private fun bindViewHolder(
        holder: TabViewHolder,
        icon: Icon,
        text: String,
        isSelected: Boolean,
        onClick: (() -> Unit)?,
    ) {
        IconViewBinder.bind(holder.icon, icon)
        holder.label.text = text
        val iconSize =
            holder.itemView.resources.getDimensionPixelSize(
                R.dimen.floating_tab_toolbar_tab_icon_size
            )
        holder.icon.layoutParams =
            holder.icon.layoutParams.apply { width = if (isSelected) iconSize else 0 }
        holder.container.background.alpha = if (isSelected) BACKGROUND_ALPHA_MAX else 0
        holder.itemView.setOnClickListener { onClick?.invoke() }
    }

    class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container = itemView.requireViewById<ViewGroup>(R.id.tab_container)
        val icon = itemView.requireViewById<ImageView>(R.id.tab_icon)
        val label = itemView.requireViewById<TextView>(R.id.label_text)
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<FloatingToolbarTabViewModel>() {

        override fun areItemsTheSame(
            oldItem: FloatingToolbarTabViewModel,
            newItem: FloatingToolbarTabViewModel
        ): Boolean {
            return oldItem.text == newItem.text
        }

        override fun areContentsTheSame(
            oldItem: FloatingToolbarTabViewModel,
            newItem: FloatingToolbarTabViewModel
        ): Boolean {
            return oldItem.text == newItem.text &&
                oldItem.isSelected == newItem.isSelected &&
                oldItem.icon == newItem.icon
        }

        override fun getChangePayload(
            oldItem: FloatingToolbarTabViewModel,
            newItem: FloatingToolbarTabViewModel
        ): Any? {
            return when {
                !oldItem.isSelected && newItem.isSelected -> SELECT_ITEM
                oldItem.isSelected && !newItem.isSelected -> UNSELECT_ITEM
                else -> null
            }
        }
    }
}
