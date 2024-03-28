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

package com.android.wallpaper.picker.preview.data.repository

import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.stats.style.StyleEnums
import android.util.Log
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.EffectContract
import com.android.wallpaper.effects.EffectsController
import com.android.wallpaper.effects.EffectsController.EffectEnumInterface
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.data.LiveWallpaperData
import com.android.wallpaper.picker.data.WallpaperId
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.EffectTextRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException

@Singleton
class ImageEffectsRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val effectsController: EffectsController,
    private val logger: UserEventLogger,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) {
    enum class EffectStatus {
        EFFECT_DISABLE,
        EFFECT_READY,
        EFFECT_DOWNLOAD_READY,
        EFFECT_DOWNLOAD_IN_PROGRESS,
        EFFECT_APPLY_IN_PROGRESS,
        EFFECT_APPLIED,
        EFFECT_DOWNLOAD_FAILED,
    }

    private val _effectStatus = MutableStateFlow(EffectStatus.EFFECT_DISABLE)
    val effectStatus = _effectStatus.asStateFlow()
    private val _wallpaperEffect = MutableStateFlow<Effect?>(null)
    val wallpaperEffect = _wallpaperEffect.asStateFlow()
    // This StaticWallpaperModel is set when initializing the repository and used for
    // 1. Providing essential data to construct LiveWallpaperData when effect is enabled
    // 2. Reverting back to the original static image wallpaper when effect is disabled
    private lateinit var staticWallpaperModel: StaticWallpaperModel
    private lateinit var onWallpaperUpdated: (wallpaper: WallpaperModel) -> Unit

    private val timeOutHandler: Handler = Handler(Looper.getMainLooper())
    private var startGeneratingTime = 0L
    private var startDownloadTime = 0L

    /** Returns whether effects are available at all on the device */
    fun areEffectsAvailable(): Boolean {
        return effectsController.areEffectsAvailable()
    }

    suspend fun initializeEffect(
        staticWallpaperModel: StaticWallpaperModel,
        onWallpaperModelUpdated: (wallpaper: WallpaperModel) -> Unit
    ) {
        this.staticWallpaperModel = staticWallpaperModel
        onWallpaperUpdated = onWallpaperModelUpdated

        withContext(bgDispatcher) {
            val listener =
                EffectsController.EffectsServiceListener {
                    effect,
                    bundle,
                    resultCode,
                    originalStatusCode,
                    _ ->
                    when (resultCode) {
                        EffectsController.RESULT_PROBE_SUCCESS -> {
                            _effectStatus.value = EffectStatus.EFFECT_READY
                        }
                        EffectsController.RESULT_PROBE_ERROR -> {
                            // Do nothing intended
                        }
                        EffectsController.RESULT_ERROR_PROBE_SUPPORT_FOREGROUND -> {
                            if (BaseFlags.get().isWallpaperEffectModelDownloadEnabled()) {
                                _effectStatus.value = EffectStatus.EFFECT_DOWNLOAD_READY
                            }
                        }
                        EffectsController.RESULT_PROBE_FOREGROUND_DOWNLOADING -> {
                            _effectStatus.value = EffectStatus.EFFECT_DOWNLOAD_IN_PROGRESS
                        }
                        EffectsController.RESULT_FOREGROUND_DOWNLOAD_SUCCEEDED -> {
                            logger.logEffectForegroundDownload(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_SUCCESS,
                                System.currentTimeMillis() - startDownloadTime,
                            )
                            _effectStatus.value = EffectStatus.EFFECT_READY
                        }
                        EffectsController.RESULT_FOREGROUND_DOWNLOAD_FAILED,
                        EffectsController.RESULT_ERROR_TRY_AGAIN_LATER -> {
                            logger.logEffectForegroundDownload(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_FAILED,
                                System.currentTimeMillis() - startDownloadTime,
                            )
                            _effectStatus.value = EffectStatus.EFFECT_DOWNLOAD_FAILED
                        }
                        EffectsController.RESULT_SUCCESS,
                        EffectsController.RESULT_SUCCESS_WITH_GENERATION_ERROR -> {
                            _effectStatus.value = EffectStatus.EFFECT_APPLIED
                            logger.logEffectApply(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_SUCCESS,
                                /* timeElapsedMillis= */ System.currentTimeMillis() -
                                    startGeneratingTime,
                                /* resultCode= */ originalStatusCode
                            )
                            bundle.getCinematicWallpaperModel(effect)?.let {
                                onWallpaperUpdated.invoke(it)
                            }
                        }
                        EffectsController.RESULT_SUCCESS_REUSED -> {
                            _effectStatus.value = EffectStatus.EFFECT_APPLIED
                            bundle.getCinematicWallpaperModel(effect)?.let {
                                onWallpaperUpdated.invoke(it)
                            }
                        }
                        else -> {
                            // TODO onImageEffectFailed
                            _effectStatus.value = EffectStatus.EFFECT_DOWNLOAD_FAILED
                            logger.logEffectApply(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_FAILED,
                                /* timeElapsedMillis= */ System.currentTimeMillis() -
                                    startGeneratingTime,
                                /* resultCode= */ originalStatusCode
                            )
                        }
                    }
                }
            // TODO remove listener when destroy
            effectsController.setListener(listener)

            effectsController.contentUri.let { uri ->
                if (Uri.EMPTY.equals(uri)) {
                    return@withContext
                }

                // Query effect provider
                context.contentResolver
                    .query(
                        uri,
                        /* projection= */ null,
                        /* selection= */ null,
                        /* selectionArgs= */ null,
                        /* sortOrder= */ null
                    )
                    ?.use {
                        while (it.moveToNext()) {
                            val titleRow: Int = it.getColumnIndex(EffectContract.KEY_EFFECT_TITLE)
                            val idRow: Int = it.getColumnIndex(EffectContract.KEY_EFFECT_ID)
                            _wallpaperEffect.value =
                                Effect(
                                    it.getInt(idRow),
                                    it.getString(titleRow),
                                    effectsController.targetEffect
                                )
                        }
                    }
            }

            if (effectsController.isEffectTriggered) {
                _effectStatus.value = EffectStatus.EFFECT_READY
            } else {
                effectsController.triggerEffect(context)
            }
        }
    }

    private fun Bundle.getCinematicWallpaperModel(
        effect: EffectEnumInterface
    ): LiveWallpaperModel? {
        val componentName =
            if (containsKey(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT)) {
                getParcelable<ComponentName>(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT)
            } else {
                null
            }
                ?: return null

        val assetId =
            if (containsKey(EffectContract.ASSET_ID)) {
                getInt(EffectContract.ASSET_ID).toString()
            } else null

        val resolveInfos =
            Intent(WallpaperService.SERVICE_INTERFACE)
                .apply { setClassName(componentName.packageName, componentName.className) }
                .let {
                    context.packageManager.queryIntentServices(it, PackageManager.GET_META_DATA)
                }
        if (resolveInfos.isEmpty()) {
            Log.w(TAG, "Couldn't find live wallpaper for " + componentName.className)
            return null
        }

        try {
            val wallpaperInfo = WallpaperInfo(context, resolveInfos[0])
            val commonWallpaperData =
                staticWallpaperModel.commonWallpaperData.copy(
                    id =
                        WallpaperId(
                            componentName = componentName,
                            uniqueId =
                                if (assetId != null) "${wallpaperInfo.serviceName}_$assetId"
                                else wallpaperInfo.serviceName,
                            collectionId = staticWallpaperModel.commonWallpaperData.id.collectionId,
                        ),
                )
            val liveWallpaperData =
                LiveWallpaperData(
                    groupName = "",
                    systemWallpaperInfo = wallpaperInfo,
                    isTitleVisible = false,
                    isApplied = false,
                    isEffectWallpaper = effectsController.isEffectsWallpaper(wallpaperInfo),
                    effectNames = effect.toString(),
                )
            return LiveWallpaperModel(
                commonWallpaperData = commonWallpaperData,
                liveWallpaperData = liveWallpaperData,
                creativeWallpaperData = null,
                internalLiveWallpaperData = null,
            )
        } catch (e: XmlPullParserException) {
            Log.w(TAG, "Skipping wallpaper " + resolveInfos[0].serviceInfo, e)
            return null
        } catch (e: IOException) {
            Log.w(TAG, "Skipping wallpaper " + resolveInfos[0].serviceInfo, e)
            return null
        }
    }

    fun enableImageEffect(effect: EffectEnumInterface) {
        startGeneratingTime = System.currentTimeMillis()
        _effectStatus.value = EffectStatus.EFFECT_APPLY_IN_PROGRESS
        // TODO: Maybe we should call reconnect wallpaper if we have created a LiveWallpaperModel
        //       if (mLiveWallpaperInfo != null) {
        //           mCinematicViewModel.reconnectWallpaper()
        //           return
        //       }
        val uri = staticWallpaperModel.imageWallpaperData?.uri ?: return
        effectsController.generateEffect(effect, uri)
        timeOutHandler.postDelayed(
            {
                wallpaperEffect.value?.let { effectsController.interruptGenerate(it) }
                _effectStatus.value = EffectStatus.EFFECT_READY
                logger.logEffectApply(
                    getEffectNameForLogging(),
                    StyleEnums.EFFECT_APPLIED_ON_FAILED,
                    System.currentTimeMillis() - startGeneratingTime,
                    EffectsController.ERROR_ORIGINAL_TIME_OUT,
                )
            },
            TIME_OUT_TIME_IN_MS
        )
    }

    fun disableImageEffect() {
        // TODO implement disabling effect
        _effectStatus.value = EffectStatus.EFFECT_READY
        logger.logEffectApply(
            wallpaperEffect.value?.type.toString(),
            StyleEnums.EFFECT_APPLIED_OFF,
            0L,
            0,
        )
        onWallpaperUpdated.invoke(staticWallpaperModel)
    }

    fun destroy() {
        effectsController.removeListener()
        _wallpaperEffect.value = null
    }

    fun isTargetEffect(effect: EffectEnumInterface): Boolean {
        return effectsController.isTargetEffect(effect)
    }

    fun getEffectTextRes(): EffectTextRes {
        return EffectTextRes(
            effectsController.effectTitle,
            effectsController.effectFailedTitle,
            effectsController.effectSubTitle,
            effectsController.retryInstruction,
            effectsController.noEffectInstruction,
        )
    }

    /**
     * This function triggers the downloading of the machine learning models. The downloading occurs
     * in the foreground off the main thread so it's safe to trigger it from the main thread.
     */
    fun startEffectsModelDownload(effect: Effect) {
        effectsController.startForegroundDownload(effect)
        _effectStatus.value = EffectStatus.EFFECT_DOWNLOAD_IN_PROGRESS
        startDownloadTime = System.currentTimeMillis()
        logger.logEffectForegroundDownload(
            getEffectNameForLogging(),
            StyleEnums.EFFECT_APPLIED_STARTED,
            0,
        )
    }

    private fun getEffectNameForLogging(): String {
        val effect = wallpaperEffect.value
        return effect?.type?.toString() ?: EffectsController.Effect.UNKNOWN.toString()
    }

    companion object {
        private const val TAG = "EffectsRepository"
        private const val TIME_OUT_TIME_IN_MS = 90000L
    }
}
