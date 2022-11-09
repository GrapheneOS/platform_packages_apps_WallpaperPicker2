/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.wallpaper.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.android.wallpaper.R
import com.android.wallpaper.util.SizeCalculator
import com.android.wallpaper.widget.floatingsheetcontent.FloatingSheetContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback

/** A `ViewGroup` which provides the specific actions for the user to interact with. */
class FloatingSheet(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val floatingSheetView: ViewGroup
    private val floatingSheetBehavior: BottomSheetBehavior<ViewGroup>
    private var content: FloatingSheetContent<*>? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.floating_sheet, this, true)
        floatingSheetView = requireViewById(R.id.floating_sheet_content)
        SizeCalculator.adjustBackgroundCornerRadius(floatingSheetView)
        setColor(context)
        val floatingSheetContainer: ViewGroup = requireViewById(R.id.floating_sheet_container)
        floatingSheetBehavior = BottomSheetBehavior.from(floatingSheetContainer)
        floatingSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /**
     * Binds the `floatingSheetContent` with the current floating sheet
     *
     * @param floatingSheetContent the content object with view being added to the floating sheet
     */
    fun bindFloatingSheetContent(floatingSheetContent: FloatingSheetContent<*>) {
        floatingSheetView.addView(floatingSheetContent.contentView)
        content = floatingSheetContent
    }

    /** Dynamic update color with `Context`. */
    fun setColor(context: Context) {
        // Set floating sheet background.
        floatingSheetView.background = context.getDrawable(R.drawable.floating_sheet_background)
        if (content != null) {
            floatingSheetView.removeAllViews()
            content!!.recreateView(context)
            floatingSheetView.addView(content!!.contentView)
        }
    }

    /** Expands [FloatingSheet]. */
    fun expand() {
        floatingSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /** Collapses [FloatingSheet]. */
    fun collapse() {
        floatingSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /** Shows [FloatingSheet]. */
    fun show() {
        visibility = VISIBLE
    }

    /** Hides [FloatingSheet]. */
    fun hide() {
        visibility = GONE
    }

    /** Adds Floating Sheet Callback to connected [BottomSheetBehavior]. */
    fun addFloatingSheetCallback(callback: BottomSheetCallback?) {
        floatingSheetBehavior.addBottomSheetCallback(callback!!)
    }
}
