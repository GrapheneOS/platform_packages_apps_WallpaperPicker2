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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeEffectsController @Inject constructor() : EffectsController() {

    var fakeAreEffectsAvailable: Boolean = true
    var fakeIsEffectTriggered: Boolean = true
    var fakeEffectTitle: String = ""
    var fakeEffectFailedTitle: String = ""
    var fakeEffectSubTitle: String = ""
    var fakeRetryInstruction: String = ""
    var fakeNoEffectInstruction: String = ""

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
                        LIVE_WALLPAPER_COMPONENT_PKG_NAME,
                        LIVE_WALLPAPER_COMPONENT_CLASS_NAME,
                    )
                )
                putInt(EffectContract.ASSET_ID, 1)
            },
            RESULT_SUCCESS,
            RESULT_ORIGINAL_UNKNOWN,
            null, /* errorMessage */
        )
    }

    override fun areEffectsAvailable(): Boolean = fakeAreEffectsAvailable

    override fun triggerEffect(context: Context?) {
        effectsServiceListener?.onEffectFinished(
            Effect.FAKE_EFFECT,
            Bundle(),
            RESULT_PROBE_SUCCESS,
            RESULT_ORIGINAL_UNKNOWN,
            null, /* errorMessage */
        )
    }

    override fun setListener(listener: EffectsServiceListener?) {
        effectsServiceListener = listener
    }

    override fun getTargetEffect(): EffectEnumInterface = Effect.FAKE_EFFECT

    override fun isEffectTriggered(): Boolean = fakeIsEffectTriggered

    override fun getEffectTitle(): String = fakeEffectTitle

    override fun getEffectFailedTitle(): String = fakeEffectFailedTitle

    override fun getEffectSubTitle(): String = fakeEffectSubTitle

    override fun getRetryInstruction(): String = fakeRetryInstruction

    override fun getNoEffectInstruction(): String = fakeNoEffectInstruction

    override fun getContentUri(): Uri = CONTENT_URI

    override fun startForegroundDownload(effect: com.android.wallpaper.effects.Effect?) {
        effectsServiceListener?.onEffectFinished(
            Effect.FAKE_EFFECT,
            Bundle(),
            RESULT_FOREGROUND_DOWNLOAD_SUCCEEDED,
            RESULT_ORIGINAL_UNKNOWN,
            null, /* errorMessage */
        )
    }

    companion object {
        const val LIVE_WALLPAPER_COMPONENT_PKG_NAME = "com.test.effects"
        const val LIVE_WALLPAPER_COMPONENT_CLASS_NAME = "test.Effects"
        const val AUTHORITY = "com.test.effects"
        private const val EFFECT_PATH = "/effects"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY$EFFECT_PATH")
    }
}
