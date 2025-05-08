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

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.wallpaper.R

/**
 * The Suggested chip on the main screen. This view needs to be in a [ConstraintLayout].
 *
 * This view is used to display the suggested theme. It has a collapsed state and an expanded state.
 * The collapsed state hides the chip. The expanded state shows the chip with title.
 */
class PackThemeSuggestedChip
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    enum class State {
        COLLAPSING,
        COLLAPSED,
        EXPANDING,
        EXPANDED,
    }

    val suggestedChipText: TextView
    val suggestedChip: View
    val cancelButton: ImageButton
    val icon: ImageView

    private val expandedContainer: View

    private var expandedWidth = 0
    private var expandedHeight = 0
    private var progress = 1f
    private var animator: ValueAnimator? = null
    private var state: State = State.EXPANDED
    private var hideSuggestedChip = false

    init {
        inflate(context, R.layout.pack_theme_suggested_chip, this)
        suggestedChip = requireViewById(R.id.theme_pack_entry)
        expandedContainer = requireViewById(R.id.suggested_chip_expanded_container)
        suggestedChipText = requireViewById(R.id.suggested_chip_text)
        icon = requireViewById(R.id.suggested_chip_icon)
        cancelButton = requireViewById(R.id.suggested_chip_cancel_button)

        post {
            expandedWidth = width
            expandedHeight = height
        }

        cancelButton.setOnClickListener {
            animateToCollapsed(
                animationEndCallback = {
                    hideSuggestedChip = true
                    visibility = View.GONE
                }
            )
            Settings.Secure.putInt(
                context.contentResolver,
                Settings.Secure.SUGGESTED_THEME_FEATURE_ENABLED,
                0,
            )
        }
    }

    /**
     * Set progress of the [PackThemeSuggestedChip]
     *
     * @param progress 1.0 means fully expanded and 0.0 means fully collapsed
     */
    private fun setProgress(progress: Float) {
        this.progress = progress
        expandedContainer.alpha = progress
        val params = layoutParams as ConstraintLayout.LayoutParams
        params.width = (expandedWidth * progress).toInt()
        params.height = (expandedHeight * progress).toInt()

        visibility = if (params.height == 0 || hideSuggestedChip) View.GONE else View.VISIBLE

        layoutParams = params
    }

    fun animateToExpanded() {
        if (state == State.EXPANDED || state == State.EXPANDING || hideSuggestedChip) {
            return
        }
        animator?.cancel()
        state = State.EXPANDING
        animator =
            ValueAnimator.ofFloat(progress, PROGRESS_EXPANDED).apply {
                duration = 500
                addUpdateListener { animation -> setProgress(animation.animatedValue as Float) }
                addListener(
                    object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {
                            state = State.EXPANDING
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            state = State.EXPANDED
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            state = State.EXPANDED
                        }

                        override fun onAnimationRepeat(animation: Animator) {
                            // Do nothing intended
                        }
                    }
                )
            }
        animator?.start()
    }

    fun animateToCollapsed(animationEndCallback: () -> Unit) {
        if (state == State.COLLAPSED || state == State.COLLAPSING || hideSuggestedChip) {
            return
        }
        animator?.cancel()
        state = State.COLLAPSING
        animator =
            ValueAnimator.ofFloat(progress, PROGRESS_COLLAPSED).apply {
                duration = ANIMATION_DURATION
                addUpdateListener { animation -> setProgress(animation.animatedValue as Float) }
                addListener(
                    object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {
                            state = State.COLLAPSING
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            state = State.COLLAPSED
                            animationEndCallback()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            state = State.COLLAPSED
                        }

                        override fun onAnimationRepeat(animation: Animator) {
                            // Do nothing intended
                        }
                    }
                )
            }
        animator?.start()
    }

    companion object {
        private const val PROGRESS_COLLAPSED = 0F
        private const val PROGRESS_EXPANDED = 1F
        private const val ANIMATION_DURATION = 300L
    }
}
