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

package com.android.wallpaper.picker.preview.ui.util

import android.graphics.Path
import androidx.core.animation.PathInterpolator
import androidx.transition.Fade
import androidx.transition.Transition

object AnimationUtil {
    /** Immediate quick fade out with a bit of ease in. */
    fun getFastFadeOutTransition(): Transition {
        val fastOut =
            Path().apply {
                quadTo(0.1f, 0f, 0.25f, 1f)
                lineTo(1f, 1f)
            }
        return Fade().setInterpolator { input -> PathInterpolator(fastOut).getInterpolation(input) }
    }

    /** Delayed quick fade in with a bit of ease out. */
    fun getFastFadeInTransition(): Transition {
        val fastIn =
            Path().apply {
                lineTo(0.75f, 0f)
                quadTo(0.9f, 1f, 1f, 1f)
            }
        return Fade().setInterpolator { input -> PathInterpolator(fastIn).getInterpolation(input) }
    }
}
