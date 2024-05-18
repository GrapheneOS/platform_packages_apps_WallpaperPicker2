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
import com.android.wallpaper.module.CustomizationSections.Screen
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
    private val lockScreenTabText: TextView
    private val homeScreenTabText: TextView

    private var downX = 0f
    private var downY = 0f
    private var onTabSelected: ((tab: Screen) -> Unit)? = null

    init {
        inflate(context, R.layout.preview_tabs, this)
        motionLayout = requireViewById(R.id.preview_tabs)
        lockScreenTabText = requireViewById(R.id.lock_screen_tab_text)
        homeScreenTabText = requireViewById(R.id.home_screen_tab_text)

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
                    if (currentId == R.id.tab_lock_screen_selected) {
                        updateTabText(0.0f)
                        lockScreenTabText.isSelected = true
                        homeScreenTabText.isSelected = false
                        onTabSelected?.invoke(Screen.LOCK_SCREEN)
                    } else if (currentId == R.id.tab_home_screen_selected) {
                        updateTabText(1.0f)
                        lockScreenTabText.isSelected = false
                        homeScreenTabText.isSelected = true
                        onTabSelected?.invoke(Screen.HOME_SCREEN)
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
            val tabLockScreenRect = requireViewById<FrameLayout>(R.id.lock_screen_tab).getViewRect()
            val tabHomeScreenRect = requireViewById<FrameLayout>(R.id.home_screen_tab).getViewRect()
            if (tabLockScreenRect.contains(downX.toInt(), downY.toInt())) {
                onTabSelected?.invoke(Screen.LOCK_SCREEN)
                return true
            } else if (tabHomeScreenRect.contains(downX.toInt(), downY.toInt())) {
                onTabSelected?.invoke(Screen.HOME_SCREEN)
                return true
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    fun setOnTabSelected(onTabSelected: ((tab: Screen) -> Unit)) {
        this.onTabSelected = onTabSelected
    }

    /** Transition to tab with [TRANSITION_DURATION] transition duration. */
    fun transitionToTab(tab: Screen) {
        when (tab) {
            Screen.LOCK_SCREEN ->
                if (motionLayout.currentState != R.id.tab_lock_screen_selected) {
                    motionLayout.setTransitionDuration(TRANSITION_DURATION)
                    motionLayout.transitionToStart()
                }
            Screen.HOME_SCREEN ->
                if (motionLayout.currentState != R.id.tab_home_screen_selected) {
                    motionLayout.setTransitionDuration(TRANSITION_DURATION)
                    motionLayout.transitionToEnd()
                }
        }
    }

    /** Set tab with 0 transition duration. */
    fun setTab(tab: Screen) {
        when (tab) {
            Screen.LOCK_SCREEN -> {
                updateTabText(0.0f)
                if (motionLayout.currentState != R.id.tab_lock_screen_selected) {
                    motionLayout.setTransitionDuration(0)
                    motionLayout.transitionToStart()
                }
            }
            Screen.HOME_SCREEN -> {
                updateTabText(1.0f)
                if (motionLayout.currentState != R.id.tab_home_screen_selected) {
                    motionLayout.setTransitionDuration(0)
                    motionLayout.transitionToEnd()
                }
            }
        }
    }

    private fun updateTabText(progress: Float) {
        lockScreenTabText.apply {
            setTextColor(
                argbEvaluator.evaluate(progress, selectedTextColor, unSelectedTextColor) as Int
            )
            background.alpha = (255 * (1 - progress)).toInt()
        }
        homeScreenTabText.apply {
            setTextColor(
                argbEvaluator.evaluate(progress, unSelectedTextColor, selectedTextColor) as Int
            )
            background.alpha = (255 * progress).toInt()
        }
    }

    private fun setCustomAccessibilityDelegate() {
        ViewCompat.setAccessibilityDelegate(
            lockScreenTabText,
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
                        onTabSelected?.invoke(Screen.LOCK_SCREEN)
                        return true
                    }
                    return super.performAccessibilityAction(host, action, args)
                }
            }
        )

        ViewCompat.setAccessibilityDelegate(
            homeScreenTabText,
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
                        onTabSelected?.invoke(Screen.HOME_SCREEN)
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
