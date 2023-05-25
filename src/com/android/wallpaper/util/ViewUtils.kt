/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.wallpaper.util

import android.annotation.DimenRes
import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View

object ViewUtils {

    /**
     * Sets a TouchDelegate on the View to meet minimum A11Y touch target requirements of 48dp x
     * 48dp. This will increase the child View's touch target area within the parent, without
     * changing the visible view size.
     *
     * @param parentView The parent View containing the View whose touch area will be expanded.
     * @param heightRes Resource for the amount of touch padding to add to the top and bottom.
     * @param widthRes Resource for the amount of touch padding to add to the left and right.
     */
    @JvmStatic
    fun View.setupTouchDelegate(
        parentView: View,
        @DimenRes heightRes: Int? = null,
        @DimenRes widthRes: Int? = null
    ) {
        if (heightRes == null && widthRes == null) return
        parentView.post {
            val rect = Rect()
            getHitRect(rect)
            heightRes?.let {
                val heightPadding = context.getResources().getDimensionPixelSize(heightRes)
                rect.top -= heightPadding
                rect.bottom += heightPadding
            }
            widthRes?.let {
                val widthPadding = context.getResources().getDimensionPixelSize(widthRes)
                rect.left -= widthPadding
                rect.right += widthPadding
            }
            parentView.touchDelegate = TouchDelegate(rect, this)
        }
    }
}
