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
package com.android.wallpaper.picker.customization.animation.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.RenderEffect
import android.view.View
import androidx.core.graphics.ColorUtils
import com.android.systemui.monet.ColorScheme
import com.android.systemui.surfaceeffects.turbulencenoise.TurbulenceNoiseShader
import com.android.wallpaper.picker.customization.animation.Interpolators
import com.android.wallpaper.picker.customization.animation.shader.CompositeLoadingShader

/**
 * Renders a surface loading animation using turbulence noise, with no sparkle effect and with fade
 * reveal, suitable for light and dark theme.
 */
class LoadingAnimation2(
    /** The view used to play the loading and reveal animation */
    private val revealOverlay: View
) {

    private val pixelDensity = revealOverlay.resources.displayMetrics.density

    private val loadingShader =
        CompositeLoadingShader(type = CompositeLoadingShader.Companion.Type.SURFACE_TURBULENCE)
    private val colorTurbulenceNoiseShader =
        TurbulenceNoiseShader(TurbulenceNoiseShader.Companion.Type.SIMPLEX_NOISE_SIMPLE).apply {
            setPixelDensity(pixelDensity)
            setGridCount(NOISE_SIZE)
            setOpacity(1f)
            setScreenColor(Color.TRANSPARENT)
        }

    private var elapsedTime = 0L
    private var transitionProgress = 0f
    // Responsible for fade in and blur on start of the loading.
    private var fadeAnimator: ValueAnimator? = null
    private var timeAnimator: TimeAnimator? = null

    private var animationState = AnimationState.IDLE

    fun playLoadingAnimation() {
        if (
            animationState == AnimationState.FADE_IN_PLAYING ||
                animationState == AnimationState.FADE_IN_PLAYED
        )
            return

        animationState = AnimationState.FADE_IN_PLAYING

        revealOverlay.visibility = View.VISIBLE

        val randomSeed = (0L..10000L).random()
        elapsedTime = randomSeed

        fadeAnimator?.removeAllListeners()
        fadeAnimator?.removeAllUpdateListeners()
        fadeAnimator?.cancel()
        timeAnimator?.cancel()

        fadeAnimator =
            ValueAnimator.ofFloat(transitionProgress, 1f).apply {
                duration = FADE_IN_DURATION_MS
                interpolator = Interpolators.STANDARD_DECELERATE
                addUpdateListener {
                    transitionProgress = it.animatedValue as Float
                    loadingShader.setAlpha(transitionProgress)
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            animationState = AnimationState.FADE_IN_PLAYED
                        }
                    }
                )
                start()
            }

        // Keep clouds moving until we finish loading
        timeAnimator =
            TimeAnimator().apply {
                setTimeListener { _, _, deltaTime -> flushUniforms(deltaTime) }
                start()
            }
    }

    fun playRevealAnimation(onAnimationEndCallback: (() -> Unit)? = null) {
        if (
            animationState == AnimationState.FADE_OUT_PLAYING ||
                animationState == AnimationState.FADE_OUT_PLAYED
        )
            return

        if (animationState == AnimationState.FADE_IN_PLAYING) {
            fadeAnimator?.removeAllListeners()
            fadeAnimator?.removeAllUpdateListeners()
            fadeAnimator?.cancel()
        }

        animationState = AnimationState.FADE_OUT_PLAYING

        revealOverlay.visibility = View.VISIBLE

        fadeAnimator =
            ValueAnimator.ofFloat(transitionProgress, 0f).apply {
                duration = FADE_OUT_DURATION_MS
                interpolator = Interpolators.STANDARD_DECELERATE
                addUpdateListener {
                    transitionProgress = it.animatedValue as Float
                    loadingShader.setAlpha(transitionProgress)
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            resetRevealAnimation()
                            onAnimationEndCallback?.invoke()
                        }
                    }
                )
                start()
            }
    }

    private fun resetRevealAnimation() {
        animationState = AnimationState.FADE_OUT_PLAYED

        revealOverlay.setRenderEffect(null)

        // Stop turbulence and reset everything.
        timeAnimator?.cancel()
        transitionProgress = 0f
    }

    fun updateColor(colorScheme: ColorScheme) {
        colorTurbulenceNoiseShader.setColor(colorScheme.accent1.s600)
        // Set screen color to the same color but transparent since the color and screen color
        // are blended to create the turbulence noise.
        colorTurbulenceNoiseShader.setScreenColor(
            ColorUtils.setAlphaComponent(colorScheme.accent1.s600, 0)
        )
    }

    private fun flushUniforms(deltaTime: Long) {
        elapsedTime += deltaTime
        val time = elapsedTime / 1000f
        val viewWidth = revealOverlay.width.toFloat()
        val viewHeight = revealOverlay.height.toFloat()

        colorTurbulenceNoiseShader.apply {
            setSize(viewWidth, viewHeight)
            setNoiseMove(time * NOISE_SPEED, 0f, time * NOISE_SPEED)
        }

        loadingShader.setColorTurbulenceMask(colorTurbulenceNoiseShader)

        val renderEffect = RenderEffect.createRuntimeShaderEffect(loadingShader, "in_background")

        revealOverlay.setRenderEffect(renderEffect)
    }

    /** Cancels the animation. Unlike end() , cancel() causes the animation to stop in its tracks */
    fun cancel() {
        fadeAnimator?.removeAllListeners()
        fadeAnimator?.removeAllUpdateListeners()
        fadeAnimator?.cancel()
        timeAnimator?.cancel()
    }

    /** Ends the animation, and causes the animation to skip to the end state */
    fun end() {
        fadeAnimator?.removeAllListeners()
        fadeAnimator?.removeAllUpdateListeners()
        fadeAnimator?.end()
        timeAnimator?.end()
        resetRevealAnimation()
    }

    companion object {
        private const val NOISE_SPEED = 0.2f
        private const val NOISE_SIZE = 1.2f
        private const val FADE_IN_DURATION_MS = 1100L
        private const val FADE_OUT_DURATION_MS = 1500L
    }

    enum class AnimationState {
        IDLE,
        FADE_IN_PLAYING,
        FADE_IN_PLAYED,
        FADE_OUT_PLAYING,
        FADE_OUT_PLAYED,
    }
}
