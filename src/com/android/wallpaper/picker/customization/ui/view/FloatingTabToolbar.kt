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

package com.android.wallpaper.picker.customization.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import com.android.wallpaper.R

/**
 * A toolbar hosting 2 tabs that enables single selection. The selected tab is highlighted with a
 * tab icon shown.
 */
class FloatingTabToolbar(
    context: Context,
    attrs: AttributeSet?,
) :
    FrameLayout(
        context,
        attrs,
    ) {

    enum class Tab {
        PRIMARY,
        SECONDARY,
    }

    val primaryIcon: ImageView
    val secondaryIcon: ImageView

    private val motionLayout: MotionLayout
    private val primaryText: TextView
    private val secondaryText: TextView
    private var onPrimaryClick: (() -> Unit)? = null
    private var onSecondaryClick: (() -> Unit)? = null

    fun setOnTabClick(tab: Tab, onClick: (() -> Unit)?) {
        if (tab == Tab.PRIMARY) {
            onPrimaryClick = onClick
        } else if (tab == Tab.SECONDARY) {
            onSecondaryClick = onClick
        }
    }

    fun setTabText(tab: Tab, text: String) {
        if (tab == Tab.PRIMARY) {
            primaryText.text = text
        } else if (tab == Tab.SECONDARY) {
            secondaryText.text = text
        }
    }

    fun setTabSelected(tab: Tab) {
        if (tab == Tab.PRIMARY) {
            motionLayout.transitionToStart()
        } else if (tab == Tab.SECONDARY) {
            motionLayout.transitionToEnd()
        }
    }

    init {
        inflate(context, R.layout.floating_tab_toolbar, this)
        motionLayout = requireViewById(R.id.motion_layout)
        primaryText = requireViewById(R.id.primary_text)
        primaryIcon = requireViewById(R.id.primary_icon)
        secondaryText = requireViewById(R.id.secondary_text)
        secondaryIcon = requireViewById(R.id.secondary_icon)

        primaryText.setOnClickListener {
            motionLayout.transitionToStart()
            onPrimaryClick?.invoke()
        }
        secondaryText.setOnClickListener {
            motionLayout.transitionToEnd()
            onSecondaryClick?.invoke()
        }
    }

    fun selectTab(isPrimary: Boolean) {
        if (isPrimary) {
            motionLayout.transitionToStart()
        } else {
            motionLayout.transitionToEnd()
        }
    }
}
