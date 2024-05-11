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
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.EffectTextRes
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException

@ActivityRetainedScoped
class ImageEffectsRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val effectsController: EffectsController,
    private val logger: UserEventLogger,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) : ImageEffectsRepository {
    private val _imageEffectsModel =
        MutableStateFlow(ImageEffectsModel(EffectStatus.EFFECT_DISABLE))
    override val imageEffectsModel = _imageEffectsModel.asStateFlow()
    private val _wallpaperEffect = MutableStateFlow<Effect?>(null)
    override val wallpaperEffect = _wallpaperEffect.asStateFlow()
    // This StaticWallpaperModel is set when initializing the repository and used for
    // 1. Providing essential data to construct LiveWallpaperData when effect is enabled
    // 2. Reverting back to the original static image wallpaper when effect is disabled
    private lateinit var staticWallpaperModel: StaticWallpaperModel
    private lateinit var onWallpaperUpdated: (wallpaper: WallpaperModel) -> Unit

    private val timeOutHandler: Handler = Handler(Looper.getMainLooper())
    private var startGeneratingTime = 0L
    private var startDownloadTime = 0L

    /** Returns whether effects are available at all on the device */
    override fun areEffectsAvailable(): Boolean {
        return effectsController.areEffectsAvailable()
    }

    override suspend fun initializeEffect(
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
                    errorMessage ->
                    timeOutHandler.removeCallbacksAndMessages(null)
                    when (resultCode) {
                        EffectsController.RESULT_PROBE_SUCCESS -> {
                            _imageEffectsModel.value =
                                ImageEffectsModel(EffectStatus.EFFECT_READY, resultCode)
                        }
                        EffectsController.RESULT_PROBE_ERROR -> {
                            // Do nothing intended
                        }
                        EffectsController.RESULT_ERROR_PROBE_SUPPORT_FOREGROUND -> {
                            if (BaseFlags.get().isWallpaperEffectModelDownloadEnabled()) {
                                _imageEffectsModel.value =
                                    ImageEffectsModel(
                                        EffectStatus.EFFECT_DOWNLOAD_READY,
                                        resultCode,
                                    )
                            }
                        }
                        EffectsController.RESULT_PROBE_FOREGROUND_DOWNLOADING -> {
                            _imageEffectsModel.value =
                                ImageEffectsModel(
                                    EffectStatus.EFFECT_DOWNLOAD_IN_PROGRESS,
                                    resultCode,
                                )
                        }
                        EffectsController.RESULT_FOREGROUND_DOWNLOAD_SUCCEEDED -> {
                            _imageEffectsModel.value =
                                ImageEffectsModel(EffectStatus.EFFECT_READY, resultCode)
                            logger.logEffectForegroundDownload(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_SUCCESS,
                                System.currentTimeMillis() - startDownloadTime,
                            )
                        }
                        EffectsController.RESULT_FOREGROUND_DOWNLOAD_FAILED -> {
                            _imageEffectsModel.value =
                                ImageEffectsModel(
                                    EffectStatus.EFFECT_DOWNLOAD_FAILED,
                                    resultCode,
                                    errorMessage,
                                )
                            logger.logEffectForegroundDownload(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_FAILED,
                                System.currentTimeMillis() - startDownloadTime,
                            )
                        }
                        EffectsController.RESULT_SUCCESS,
                        EffectsController.RESULT_SUCCESS_WITH_GENERATION_ERROR -> {
                            _imageEffectsModel.value =
                                ImageEffectsModel(
                                    EffectStatus.EFFECT_APPLIED,
                                    resultCode,
                                    errorMessage,
                                )
                            bundle.getCinematicWallpaperModel(effect)?.let {
                                onWallpaperUpdated.invoke(it)
                            }
                            logger.logEffectApply(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_SUCCESS,
                                /* timeElapsedMillis= */ System.currentTimeMillis() -
                                    startGeneratingTime,
                                /* resultCode= */ originalStatusCode,
                            )
                        }
                        EffectsController.RESULT_SUCCESS_REUSED -> {
                            _imageEffectsModel.value =
                                ImageEffectsModel(EffectStatus.EFFECT_APPLIED, resultCode)
                            bundle.getCinematicWallpaperModel(effect)?.let {
                                onWallpaperUpdated.invoke(it)
                            }
                        }
                        EffectsController.RESULT_ERROR_TRY_AGAIN_LATER -> {
                            _imageEffectsModel.value =
                                ImageEffectsModel(
                                    EffectStatus.EFFECT_APPLY_FAILED,
                                    resultCode,
                                    errorMessage,
                                )
                            logger.logEffectApply(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_FAILED,
                                /* timeElapsedMillis= */ System.currentTimeMillis() -
                                    startGeneratingTime,
                                /* resultCode= */ originalStatusCode
                            )
                        }
                        else -> {
                            _imageEffectsModel.value =
                                ImageEffectsModel(
                                    EffectStatus.EFFECT_APPLY_FAILED,
                                    resultCode,
                                    errorMessage,
                                )
                            logger.logEffectApply(
                                getEffectNameForLogging(),
                                StyleEnums.EFFECT_APPLIED_ON_FAILED,
                                /* timeElapsedMillis= */ System.currentTimeMillis() -
                                    startGeneratingTime,
                                /* resultCode= */ originalStatusCode,
                            )
                        }
                    }
                }
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
                                    effectsController.targetEffect,
                                )
                        }
                    }
            }

            if (effectsController.isEffectTriggered) {
                _imageEffectsModel.value = ImageEffectsModel(EffectStatus.EFFECT_READY)
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

    override fun enableImageEffect(effect: EffectEnumInterface) {
        startGeneratingTime = System.currentTimeMillis()
        _imageEffectsModel.value = ImageEffectsModel(EffectStatus.EFFECT_APPLY_IN_PROGRESS)
        val uri = staticWallpaperModel.imageWallpaperData?.uri ?: return
        effectsController.generateEffect(effect, uri)
        timeOutHandler.postDelayed(
            {
                wallpaperEffect.value?.let { effectsController.interruptGenerate(it) }
                _imageEffectsModel.value =
                    ImageEffectsModel(
                        EffectStatus.EFFECT_APPLY_FAILED,
                        null,
                        effectsController.retryInstruction,
                    )
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

    override fun disableImageEffect() {
        _imageEffectsModel.value = ImageEffectsModel(EffectStatus.EFFECT_READY)
        logger.logEffectApply(
            wallpaperEffect.value?.type.toString(),
            StyleEnums.EFFECT_APPLIED_OFF,
            0L,
            0,
        )
        onWallpaperUpdated.invoke(staticWallpaperModel)
    }

    override fun destroy() {
        timeOutHandler.removeCallbacksAndMessages(null)
        effectsController.removeListener()
        // We need to call interruptGenerate() and destroy() to make sure there is no cached effect
        // wallpaper overriding the currently-selected effect wallpaper preview.
        wallpaperEffect.value?.let { effectsController.interruptGenerate(it) }
        effectsController.destroy()
        _wallpaperEffect.value = null
    }

    override fun isTargetEffect(effect: EffectEnumInterface): Boolean {
        return effectsController.isTargetEffect(effect)
    }

    override fun getEffectTextRes(): EffectTextRes {
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
    override fun startEffectsModelDownload(effect: Effect) {
        _imageEffectsModel.value = ImageEffectsModel(EffectStatus.EFFECT_DOWNLOAD_IN_PROGRESS)
        effectsController.startForegroundDownload(effect)
        startDownloadTime = System.currentTimeMillis()
        logger.logEffectForegroundDownload(
            getEffectNameForLogging(),
            StyleEnums.EFFECT_APPLIED_STARTED,
            0,
        )
    }

    override fun interruptEffectsModelDownload(effect: Effect) {
        _imageEffectsModel.value = ImageEffectsModel(EffectStatus.EFFECT_DOWNLOAD_READY)
        effectsController.interruptForegroundDownload(effect)
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
