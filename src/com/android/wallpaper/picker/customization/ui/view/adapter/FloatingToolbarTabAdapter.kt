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

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.view.animator.TabItemAnimator.Companion.BACKGROUND_ALPHA_MAX
import com.android.wallpaper.picker.customization.ui.view.animator.TabItemAnimator.Companion.SELECT_ITEM
import com.android.wallpaper.picker.customization.ui.view.animator.TabItemAnimator.Companion.UNSELECT_ITEM
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import java.lang.ref.WeakReference

/** List adapter for the floating toolbar of tabs. */
class FloatingToolbarTabAdapter(
    private val colorUpdateViewModel: WeakReference<ColorUpdateViewModel>,
    private val shouldAnimateColor: () -> Boolean,
) :
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
        val tabViewHolder = TabViewHolder(view)
        return tabViewHolder
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
        // Bind tab color in onBindViewHolder and destroy in onViewRecycled. Bind in this
        // onBindViewHolder instead of the one with payload since this function is generally
        // called when view holders are created or recycled, ensuring each view holder is only
        // bound once, whereas the view holder with payload is called not only in the above cases,
        // but also when the state is changed, which could result in multiple bindings.
        colorUpdateViewModel.get()?.let {
            ColorUpdateBinder.bind(
                setColor = { color ->
                    holder.itemView.background.colorFilter =
                        BlendModeColorFilter(color, BlendMode.SRC_ATOP)
                },
                color = it.colorSecondaryContainer,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = holder,
            )
        }

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

    override fun onViewAttachedToWindow(holder: TabViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onAttachToWindow()
    }

    override fun onViewDetachedFromWindow(holder: TabViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onDetachFromWindow()
    }

    override fun onViewRecycled(holder: TabViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycled()
    }

    /**
     * A [RecyclerView.ViewHolder] for the floating tabs recycler view, that also extends
     * [LifecycleOwner] to enable binding flows and collecting based on lifecycle states. This
     * optimizes the binding so that view holders that are not visible on screen will not be
     * actively collecting and updating from a bound flow. The lifecycle state is created when the
     * ViewHolder is created, then started and stopped in onViewAttachedToWindow and
     * onViewDetachedFromWindow, and destroyed in onViewRecycled, where a new lifecycle is created.
     */
    class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LifecycleOwner {
        val container = itemView.requireViewById<ViewGroup>(R.id.tab_container)
        val icon = itemView.requireViewById<ImageView>(R.id.tab_icon)
        val label = itemView.requireViewById<TextView>(R.id.label_text)

        private lateinit var lifecycleRegistry: LifecycleRegistry
        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        init {
            initializeRegistry()
        }

        private fun initializeRegistry() {
            lifecycleRegistry =
                LifecycleRegistry(this).also { it.handleLifecycleEvent(Lifecycle.Event.ON_CREATE) }
        }

        fun onAttachToWindow() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onDetachFromWindow() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        fun onRecycled() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            initializeRegistry()
        }
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
