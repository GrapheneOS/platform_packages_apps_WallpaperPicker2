/*
 * Copyright (C) 2025 The Android Open Source Project
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
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isInvisible
import com.android.wallpaper.R
import com.google.android.material.progressindicator.CircularProgressIndicator

class ApplyButton(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    enum class ApplyButtonState {
        APPLY_BUTTON_DISABLED,
        APPLY_BUTTON_ENABLED,
        APPLY_BUTTON_IN_PROGRESS,
    }

    private val applyButtonBackground: FrameLayout
    private val text: TextView
    private val progressIndicator: CircularProgressIndicator

    init {
        inflate(context, R.layout.apply_button, this)
        applyButtonBackground = requireViewById(R.id.apply_button_background)
        text = requireViewById(R.id.apply_button_text)
        progressIndicator = requireViewById(R.id.apply_button_progress_indicator)
    }

    fun setApplyButtonState(state: ApplyButtonState) {
        when (state) {
            ApplyButtonState.APPLY_BUTTON_ENABLED -> {
                isEnabled = true
                applyButtonBackground.isEnabled = true
                applyButtonBackground.background.alpha = 255 // 255 for 100% transparent
                text.isInvisible = false
                progressIndicator.isInvisible = true
            }
            ApplyButtonState.APPLY_BUTTON_DISABLED -> {
                isEnabled = false
                applyButtonBackground.isEnabled = false
                applyButtonBackground.background.alpha = 31 // 31 for 12% transparent
                text.isInvisible = false
                progressIndicator.isInvisible = true
            }
            ApplyButtonState.APPLY_BUTTON_IN_PROGRESS -> {
                isEnabled = false
                applyButtonBackground.isEnabled = true
                applyButtonBackground.background.alpha = 255 // 255 for 100% transparent
                text.isInvisible = true
                progressIndicator.isInvisible = false
            }
        }
    }

    fun setApplyButtonTextColor(color: Int) {
        text.setTextColor(color)
    }

    fun setApplyButtonBackgroundColor(color: Int) {
        DrawableCompat.setTint(DrawableCompat.wrap(applyButtonBackground.background), color)
    }

    fun setIndicatorColor(color: Int) {
        progressIndicator.setIndicatorColor(color)
    }
}
