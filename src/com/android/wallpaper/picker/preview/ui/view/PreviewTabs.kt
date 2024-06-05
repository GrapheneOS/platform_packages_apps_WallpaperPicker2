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

package com.android.wallpaper.picker.preview.ui.view

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.android.wallpaper.R
import kotlin.math.pow
import kotlin.math.sqrt

class PreviewTabs(
    context: Context,
    attrs: AttributeSet?,
) :
    FrameLayout(
        context,
        attrs,
    ) {

    private val argbEvaluator = ArgbEvaluator()
    private val selectedTextColor = ContextCompat.getColor(context, R.color.system_on_primary)
    private val unSelectedTextColor = ContextCompat.getColor(context, R.color.system_secondary)

    private val motionLayout: MotionLayout
    private val primaryTabText: TextView
    private val secondaryTabText: TextView

    private var downX = 0f
    private var downY = 0f
    private var onTabSelected: ((index: Int) -> Unit)? = null

    init {
        inflate(context, R.layout.preview_tabs, this)
        motionLayout = requireViewById(R.id.preview_tabs)
        primaryTabText = requireViewById(R.id.primary_tab_text)
        secondaryTabText = requireViewById(R.id.secondary_tab_text)

        setCustomAccessibilityDelegate()

        motionLayout.setTransitionListener(
            object : TransitionListener {
                override fun onTransitionStarted(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int
                ) {
                    // Do nothing intended
                }

                override fun onTransitionChange(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int,
                    progress: Float
                ) {
                    updateTabText(progress)
                }

                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    if (currentId == R.id.primary_tab_selected) {
                        updateTabText(0.0f)
                        primaryTabText.isSelected = true
                        secondaryTabText.isSelected = false
                        onTabSelected?.invoke(0)
                    } else if (currentId == R.id.secondary_tab_selected) {
                        updateTabText(1.0f)
                        primaryTabText.isSelected = false
                        secondaryTabText.isSelected = true
                        onTabSelected?.invoke(1)
                    }
                }

                override fun onTransitionTrigger(
                    motionLayout: MotionLayout?,
                    triggerId: Int,
                    positive: Boolean,
                    progress: Float
                ) {
                    // Do nothing intended
                }
            }
        )
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.rawX
            downY = event.rawY
        }

        // We have to use this method to manually intercept a click event, rather than setting the
        // onClickListener to the individual tabs. This is because, when setting the onClickListener
        // to the individual tabs, the swipe gesture of the tabs will be overridden.
        if (isClick(event, downX, downY)) {
            val primaryTabRect = requireViewById<FrameLayout>(R.id.primary_tab).getViewRect()
            val secondaryTabRect = requireViewById<FrameLayout>(R.id.secondary_tab).getViewRect()
            if (primaryTabRect.contains(downX.toInt(), downY.toInt())) {
                onTabSelected?.invoke(0)
                return true
            } else if (secondaryTabRect.contains(downX.toInt(), downY.toInt())) {
                onTabSelected?.invoke(1)
                return true
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    fun setOnTabSelected(onTabSelected: ((index: Int) -> Unit)) {
        this.onTabSelected = onTabSelected
    }

    /** Transition to tab with [TRANSITION_DURATION] transition duration. */
    fun transitionToTab(index: Int) {
        if (index == 0) {
            if (motionLayout.currentState != R.id.primary_tab_selected) {
                motionLayout.setTransitionDuration(TRANSITION_DURATION)
                motionLayout.transitionToStart()
            }
        } else if (index == 1) {
            if (motionLayout.currentState != R.id.secondary_tab_selected) {
                motionLayout.setTransitionDuration(TRANSITION_DURATION)
                motionLayout.transitionToEnd()
            }
        }
    }

    fun resetTransition(targetIndex: Int) {
        motionLayout.setTransition(R.id.primary_tab_selected, R.id.secondary_tab_selected)
        motionLayout.setProgress(targetIndex.toFloat())
    }

    /** Set tab with 0 transition duration. */
    fun setTab(index: Int) {
        if (index == 0) {
            updateTabText(0.0f)
            if (motionLayout.currentState != R.id.primary_tab_selected) {
                motionLayout.setTransitionDuration(0)
                motionLayout.transitionToStart()
            }
        } else if (index == 1) {
            updateTabText(1.0f)
            if (motionLayout.currentState != R.id.secondary_tab_selected) {
                motionLayout.setTransitionDuration(0)
                motionLayout.transitionToEnd()
            }
        }
    }

    private fun updateTabText(progress: Float) {
        primaryTabText.apply {
            setTextColor(
                argbEvaluator.evaluate(progress, selectedTextColor, unSelectedTextColor) as Int
            )
            background.alpha = (255 * (1 - progress)).toInt()
        }
        secondaryTabText.apply {
            setTextColor(
                argbEvaluator.evaluate(progress, unSelectedTextColor, selectedTextColor) as Int
            )
            background.alpha = (255 * progress).toInt()
        }
    }

    fun setTabsText(primaryText: String, secondaryText: String) {
        primaryTabText.text = primaryText
        secondaryTabText.text = secondaryText
    }

    private fun setCustomAccessibilityDelegate() {
        ViewCompat.setAccessibilityDelegate(
            primaryTabText,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK
                    )
                }

                override fun performAccessibilityAction(
                    host: View,
                    action: Int,
                    args: Bundle?
                ): Boolean {
                    if (
                        action ==
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK.id
                    ) {
                        onTabSelected?.invoke(0)
                        return true
                    }
                    return super.performAccessibilityAction(host, action, args)
                }
            }
        )

        ViewCompat.setAccessibilityDelegate(
            secondaryTabText,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK
                    )
                }

                override fun performAccessibilityAction(
                    host: View,
                    action: Int,
                    args: Bundle?
                ): Boolean {
                    if (
                        action ==
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK.id
                    ) {
                        onTabSelected?.invoke(1)
                        return true
                    }
                    return super.performAccessibilityAction(host, action, args)
                }
            }
        )
    }

    companion object {

        const val TRANSITION_DURATION = 200

        private fun isClick(event: MotionEvent, downX: Float, downY: Float): Boolean {
            return when {
                // It's not a click if the event is not an UP action (though it may become one
                // later, when/if an UP is received).
                event.action != MotionEvent.ACTION_UP -> false
                // It's not a click if too much time has passed between the down and the current
                // event.
                gestureElapsedTime(event) > ViewConfiguration.getTapTimeout() -> false
                // It's not a click if the touch traveled too far.
                distanceMoved(event, downX, downY) > ViewConfiguration.getTouchSlop() -> false
                // Otherwise, this is a click!
                else -> true
            }
        }

        /**
         * Returns the distance that the pointer traveled in the touch gesture the given event is
         * part of.
         */
        private fun distanceMoved(event: MotionEvent, downX: Float, downY: Float): Float {
            val deltaX = event.rawX - downX
            val deltaY = event.rawY - downY
            return sqrt(deltaX.pow(2) + deltaY.pow(2))
        }

        /**
         * Returns the elapsed time since the touch gesture the given event is part of has begun.
         */
        private fun gestureElapsedTime(event: MotionEvent): Long {
            return event.eventTime - event.downTime
        }

        private fun View.getViewRect(): Rect {
            val returnRect = Rect()
            this.getGlobalVisibleRect(returnRect)
            return returnRect
        }
    }
}
