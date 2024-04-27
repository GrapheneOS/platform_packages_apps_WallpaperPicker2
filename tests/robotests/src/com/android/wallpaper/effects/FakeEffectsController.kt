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

package com.android.wallpaper.effects

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle

class FakeEffectsController(
    private val areEffectsAvailable: Boolean,
    private val isEffectTriggered: Boolean,
    private val effectTitle: String,
    private val effectFailedTitle: String,
    private val effectSubTitle: String,
    private val retryInstruction: String,
    private val noEffectInstruction: String,
) : EffectsController() {

    enum class Effect : EffectEnumInterface {
        NONE,
        FAKE_EFFECT,
    }

    private var effectsServiceListener: EffectsServiceListener? = null

    override fun generateEffect(effect: EffectEnumInterface?, image: Uri?) {
        effectsServiceListener?.onEffectFinished(
            Effect.FAKE_EFFECT,
            Bundle().apply {
                putParcelable(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(
                        "com.google.android.wallpaper.effects",
                        "com.google.android.wallpaper.effects.cinematic.CinematicWallpaperService",
                    )
                )
                putInt(EffectContract.ASSET_ID, 1)
            },
            RESULT_SUCCESS,
            RESULT_ORIGINAL_UNKNOWN,
            null /* errorMessage */,
        )
    }

    override fun areEffectsAvailable(): Boolean = areEffectsAvailable

    override fun triggerEffect(context: Context?) {
        effectsServiceListener?.onEffectFinished(
            Effect.FAKE_EFFECT,
            Bundle(),
            RESULT_PROBE_SUCCESS,
            RESULT_ORIGINAL_UNKNOWN,
            null /* errorMessage */,
        )
    }

    override fun setListener(listener: EffectsServiceListener?) {
        effectsServiceListener = listener
    }

    override fun getTargetEffect(): EffectEnumInterface = Effect.FAKE_EFFECT

    override fun isEffectTriggered(): Boolean = isEffectTriggered

    override fun getEffectTitle(): String = effectTitle

    override fun getEffectFailedTitle(): String = effectFailedTitle

    override fun getEffectSubTitle(): String = effectSubTitle

    override fun getRetryInstruction(): String = retryInstruction

    override fun getNoEffectInstruction(): String = noEffectInstruction

    override fun getContentUri(): Uri = CONTENT_URI

    override fun startForegroundDownload(effect: com.android.wallpaper.effects.Effect?) {
        effectsServiceListener?.onEffectFinished(
            Effect.FAKE_EFFECT,
            Bundle(),
            RESULT_FOREGROUND_DOWNLOAD_SUCCEEDED,
            RESULT_ORIGINAL_UNKNOWN,
            null /* errorMessage */,
        )
    }

    companion object {
        const val AUTHORITY = "com.google.android.wallpaper.effects.effectprovider"
        private const val EFFECT_PATH = "/effects"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY$EFFECT_PATH")
    }
}
