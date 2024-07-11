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
 * A toolbar hosting 2 or 3 tabs that enables single selection. The selected tab is highlighted with
 * a tab icon shown. Please call [showTertiaryTab] before setting any text and icon resources.
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
        TERTIARY,
    }

    val primaryIcon: ImageView
    val secondaryIcon: ImageView
    val tertiaryIcon: ImageView

    private val motionLayout: MotionLayout
    private val primaryText: TextView
    private val secondaryText: TextView
    private val tertiaryText: TextView
    private var onPrimaryClick: (() -> Unit)? = null
    private var onSecondaryClick: (() -> Unit)? = null
    private var onTertiaryClick: (() -> Unit)? = null
    // Tertiary tab is hidden by default
    private var showTertiaryTab = false

    /** @param show True means there are 3 tabs. False means there are 2 tabs. */
    fun showTertiaryTab(show: Boolean) {
        showTertiaryTab = show
        val visibility = if (show) VISIBLE else GONE
        val primaryTabSelected = motionLayout.getConstraintSet(R.id.primary_tab_selected)
        primaryTabSelected.setVisibility(R.id.divider2, visibility)
        primaryTabSelected.setVisibility(R.id.tertiary_background, if (show) INVISIBLE else GONE)
        primaryTabSelected.setVisibility(R.id.tertiary_icon, visibility)
        primaryTabSelected.setVisibility(R.id.tertiary_text, visibility)

        val secondaryTabSelected = motionLayout.getConstraintSet(R.id.secondary_tab_selected)
        secondaryTabSelected.setVisibility(R.id.divider2, visibility)
        secondaryTabSelected.setVisibility(R.id.tertiary_background, if (show) INVISIBLE else GONE)
        secondaryTabSelected.setVisibility(R.id.tertiary_icon, visibility)
        secondaryTabSelected.setVisibility(R.id.tertiary_text, visibility)

        val tertiaryTabSelected = motionLayout.getConstraintSet(R.id.tertiary_tab_selected)
        tertiaryTabSelected.setVisibility(R.id.divider2, visibility)
        tertiaryTabSelected.setVisibility(R.id.tertiary_background, if (show) VISIBLE else GONE)
        tertiaryTabSelected.setVisibility(R.id.tertiary_icon, visibility)
        tertiaryTabSelected.setVisibility(R.id.tertiary_text, visibility)
    }

    fun setOnTabClick(tab: Tab, onClick: (() -> Unit)?) {
        when (tab) {
            Tab.PRIMARY -> {
                onPrimaryClick = onClick
            }
            Tab.SECONDARY -> {
                onSecondaryClick = onClick
            }
            Tab.TERTIARY -> {
                if (!showTertiaryTab) {
                    throw IllegalStateException(
                        "showTertiaryTab is false. Please set showTertiaryTab true first."
                    )
                }
                onTertiaryClick = onClick
            }
        }
    }

    fun setTabText(tab: Tab, text: String) {
        when (tab) {
            Tab.PRIMARY -> {
                primaryText.text = text
            }
            Tab.SECONDARY -> {
                secondaryText.text = text
            }
            Tab.TERTIARY -> {
                if (!showTertiaryTab) {
                    throw IllegalStateException(
                        "showTertiaryTab is false. Please set showTertiaryTab true first."
                    )
                }
                tertiaryText.text = text
            }
        }
    }

    fun setTabSelected(tab: Tab) {
        when (tab) {
            Tab.PRIMARY -> {
                motionLayout.transitionToState(R.id.primary_tab_selected, 200)
            }
            Tab.SECONDARY -> {
                motionLayout.transitionToState(R.id.secondary_tab_selected, 200)
            }
            Tab.TERTIARY -> {
                if (!showTertiaryTab) {
                    throw IllegalStateException(
                        "showTertiaryTab is false. Please set showTertiaryTab true first."
                    )
                }
                motionLayout.transitionToState(R.id.tertiary_tab_selected, 200)
            }
        }
    }

    init {
        inflate(context, R.layout.floating_tab_toolbar, this)
        motionLayout = requireViewById(R.id.motion_layout)
        primaryText = requireViewById(R.id.primary_text)
        primaryIcon = requireViewById(R.id.primary_icon)
        secondaryText = requireViewById(R.id.secondary_text)
        secondaryIcon = requireViewById(R.id.secondary_icon)
        tertiaryText = requireViewById(R.id.tertiary_text)
        tertiaryIcon = requireViewById(R.id.tertiary_icon)

        primaryText.setOnClickListener {
            setTabSelected(Tab.PRIMARY)
            onPrimaryClick?.invoke()
        }
        secondaryText.setOnClickListener {
            setTabSelected(Tab.SECONDARY)
            onSecondaryClick?.invoke()
        }
        tertiaryText.setOnClickListener {
            setTabSelected(Tab.TERTIARY)
            onTertiaryClick?.invoke()
        }

        // Tertiary tab is hidden by default
        showTertiaryTab(false)
    }
}
