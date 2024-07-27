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

package com.android.wallpaper.picker.customization.ui.view.animator

import android.animation.ValueAnimator
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView.State
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.android.wallpaper.R
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter.TabViewHolder

class TabItemAnimator : DefaultItemAnimator() {

    override fun canReuseUpdatedViewHolder(viewHolder: ViewHolder, payloads: MutableList<Any>) =
        true

    override fun recordPreLayoutInformation(
        state: State,
        viewHolder: ViewHolder,
        changeFlags: Int,
        payloads: MutableList<Any>
    ): ItemHolderInfo {
        if (changeFlags == FLAG_CHANGED && payloads.isNotEmpty()) {
            return when (payloads[0] as? Int) {
                SELECT_ITEM -> TabItemHolderInfo(true)
                UNSELECT_ITEM -> TabItemHolderInfo(false)
                else -> super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads)
            }
        }
        return super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads)
    }

    override fun animateChange(
        oldHolder: ViewHolder,
        newHolder: ViewHolder,
        preLayoutInfo: ItemHolderInfo,
        postLayoutInfo: ItemHolderInfo,
    ): Boolean {
        if (preLayoutInfo is TabItemHolderInfo) {
            val viewHolder = newHolder as TabViewHolder
            val iconSize =
                viewHolder.itemView.resources.getDimensionPixelSize(
                    R.dimen.floating_tab_toolbar_tab_icon_size
                )
            ValueAnimator.ofFloat(
                    if (preLayoutInfo.selectItem) 0f else 1f,
                    if (preLayoutInfo.selectItem) 1f else 0f,
                )
                .apply {
                    addUpdateListener {
                        val value = it.animatedValue as Float
                        viewHolder.icon.layoutParams =
                            viewHolder.icon.layoutParams.apply {
                                width = (value * iconSize).toInt()
                            }
                        viewHolder.container.background.alpha =
                            (value * BACKGROUND_ALPHA_MAX).toInt()
                    }
                    addListener { doOnEnd { dispatchAnimationFinished(viewHolder) } }
                    duration = ANIMATION_DURATION_MILLIS
                }
                .start()
            return true
        }

        return super.animateChange(oldHolder, newHolder, preLayoutInfo, postLayoutInfo)
    }

    class TabItemHolderInfo(val selectItem: Boolean) : ItemHolderInfo()

    companion object {
        const val SELECT_ITEM = 3024
        const val UNSELECT_ITEM = 1114
        const val BACKGROUND_ALPHA_MAX = 255
        const val ANIMATION_DURATION_MILLIS = 200L
    }
}
