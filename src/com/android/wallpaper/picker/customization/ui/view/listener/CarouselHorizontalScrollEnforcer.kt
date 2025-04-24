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

package com.android.wallpaper.picker.customization.ui.view.listener

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.absoluteValue

/**
 * Used to disallow a RecyclerView's parent's scroll when the RV is scrolling horizontally. Set an
 * instance of this class as both {@see RecyclerView#onScrollListener} and {@see
 * ReciclerView#onItemTouchListener} to make sure that horizontal scrolling doesn't trigger nested
 * vertical scrolling.
 */
class CarouselHorizontalScrollEnforcer(context: Context) :
    RecyclerView.OnScrollListener(), RecyclerView.OnItemTouchListener {
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    // Keep track if WE requested the parent to stop intercepting
    // So the OnScrollListener knows when to release it.
    private var isChildHandlingScroll = false

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        // Don't intercept if RecyclerView can't scroll horizontally
        if (!rv.canScrollHorizontally(1) && !rv.canScrollHorizontally(-1)) {
            return false
        }

        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = e.x
                initialY = e.y
                // If the RV is currently idle, allow parent to intercept initially.
                // If it's already flinging, keep the disallow flag potentially set.
                if (rv.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    requestParentDisallowIntercept(rv, false)
                    // Reset our flag only if RV is idle AND a new touch starts
                    isChildHandlingScroll = false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isChildHandlingScroll) { // Only check if we haven't already disallowed
                    val dx = e.x - initialX
                    val dy = e.y - initialY

                    // Check if horizontal movement is dominant and exceeds touch slop
                    val isHorizontalMove =
                        dx.absoluteValue > touchSlop && dx.absoluteValue > dy.absoluteValue
                    if (isHorizontalMove) {
                        // We've detected a horizontal drag starting, tell parent to stop
                        // intercepting
                        requestParentDisallowIntercept(rv, true)
                        isChildHandlingScroll = true
                    }
                }
                // If already handling scroll, the parent should remain disallowed.
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // *** DO NOT reset requestDisallowInterceptTouchEvent here ***
                // Let the OnScrollListener handle it when the scrolling actually stops.
                // If it wasn't a scroll/fling (just a tap), isChildHandlingScroll might still be
                // false, and the parent's state wasn't changed, or it will be reset by
                // OnScrollListener if a tiny scroll happened.
            }
        }

        // We return false because we don't want THIS listener to *consume* the event,
        // just to manage the parent's interception state. RV's internal scrolling needs the event.
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        // No-op - We only care about interception logic
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // No-op - Child views of RecyclerView might call this, but we don't need to react
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            // Only reset if our blocker was the one that set it
            if (isChildHandlingScroll) {
                // Allow parent to intercept future events again
                requestParentDisallowIntercept(recyclerView, false)

                isChildHandlingScroll = false // Reset the flag
            } else {
                // If the scroll ended but wasn't initiated by our horizontal blocker
                // (e.g., programmatically scrolled), ensure parent intercept is allowed anyway.
                requestParentDisallowIntercept(recyclerView, false)
            }
        } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
            // Flinging is happening. Parent should already be disallowed if drag started
            // horizontally.
            if (isChildHandlingScroll) {
                requestParentDisallowIntercept(recyclerView, true)
            }
        }
    }

    private fun requestParentDisallowIntercept(
        recyclerView: RecyclerView,
        disallowIntercept: Boolean,
    ) {
        recyclerView.parent.parent.requestDisallowInterceptTouchEvent(disallowIntercept)
        recyclerView.parent.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
}
