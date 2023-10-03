/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.wallpaper.module.logging

import android.content.Intent
import android.stats.style.StyleEnums
import androidx.annotation.IntDef

/** Interface for logging user events in the wallpaper picker. */
interface UserEventLogger {

    fun logAppLaunched(launchSource: Intent)

    fun logActionClicked(collectionId: String, actionLabelResId: Int)

    /** Log current existing snapshot data. */
    fun logSnapshot()

    /**
     * Logs the behavior when applying wallpaper.
     *
     * @param collectionId wallpaper category.
     * @param wallpaperId wallpaper id.
     * @param effects effects set with wallpaper.
     */
    fun logWallpaperApplied(collectionId: String?, wallpaperId: String?, effects: String?)

    /** Logs the action related to effect. */
    fun logEffectApply(
        effect: String,
        @EffectStatus status: Int,
        timeElapsedMillis: Long,
        resultCode: Int,
    )

    /** Logs the effect probe result. */
    fun logEffectProbe(effect: String, @EffectStatus status: Int)

    /** Logs the effect foreground download event. */
    fun logEffectForegroundDownload(
        effect: String,
        @EffectStatus status: Int,
        timeElapsedMillis: Long,
    )

    /**
     * Possible actions for cinematic effect. These actions would be used for effect apply, effect
     * probe, effect download.
     */
    @IntDef(
        StyleEnums.EFFECT_PREFERENCE_UNSPECIFIED,
        StyleEnums.EFFECT_APPLIED_ON_SUCCESS,
        StyleEnums.EFFECT_APPLIED_ON_FAILED,
        StyleEnums.EFFECT_APPLIED_OFF,
        StyleEnums.EFFECT_APPLIED_ABORTED,
        StyleEnums.EFFECT_APPLIED_STARTED
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class EffectStatus
}
