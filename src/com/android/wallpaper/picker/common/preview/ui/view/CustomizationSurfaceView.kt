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

package com.android.wallpaper.picker.common.preview.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView

/**
 * [SurfaceView] that keeps the surface at a fixed size, and resizes it according to view size
 * changes using the Hardware Scaler, rather than resizing the surface itself. It enables better
 * efficiency in cases where resizing is frequently needed. It sets the surface at a fixed size
 * based on the size it is initialized at.
 */
class CustomizationSurfaceView(context: Context, attrs: AttributeSet? = null) :
    SurfaceView(context, attrs) {
    private var isTransitioning = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // TODO (b/348462236): investigate effect on scale transition and touch forwarding layout
        if (oldw == 0 && oldh == 0) {
            // If the view doesn't have a fixed width and height, after the transition the oldw and
            // oldh will be 0, don't set new size in this case as it will interfere with the
            // transition. Set the flag back to false once the transition is completed.
            if (isTransitioning) {
                isTransitioning = false
            } else {
                holder.setFixedSize(w, h)
            }
        }
    }

    /**
     * Indicates the view is transitioning.
     *
     * Needed when using WRAP_CONTENT or 0dp for height or weight together with [MotionLayout]
     */
    fun setTransitioning() {
        this.isTransitioning = true
    }
}
