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

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.wallpaper.model.WallpaperAction
import com.android.wallpaper.model.WallpaperInfoContract
import com.android.wallpaper.picker.data.CreativeWallpaperEffectsData
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.preview.shared.model.CreativeEffectsModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@ActivityRetainedScoped
class CreativeEffectsRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) {

    private val _creativeEffectsModel = MutableStateFlow<CreativeEffectsModel?>(null)
    val creativeEffectsModel = _creativeEffectsModel.asStateFlow()

    private var clearActionUri: Uri? = null

    suspend fun initializeEffect(data: CreativeWallpaperEffectsData) {
        withContext(bgDispatcher) {
            clearActionUri = data.clearActionUri
            try {
                data.effectsUri.authority
                    ?.let { context.contentResolver.acquireContentProviderClient(it) }
                    ?.use { it.query(data.effectsUri, null, null, null, null) }
                    ?.use { effectsCursor ->
                        while (effectsCursor.moveToNext()) {
                            val effectsToggleUri =
                                Uri.parse(
                                    effectsCursor.getString(
                                        effectsCursor.getColumnIndex(
                                            WallpaperInfoContract.WALLPAPER_EFFECTS_TOGGLE_URI
                                        )
                                    )
                                )
                            val effectsButtonLabel: String =
                                effectsCursor.getString(
                                    effectsCursor.getColumnIndex(
                                        WallpaperInfoContract.WALLPAPER_EFFECTS_BUTTON_LABEL
                                    )
                                )
                            val effectsId: String =
                                effectsCursor.getString(
                                    effectsCursor.getColumnIndex(
                                        WallpaperInfoContract.WALLPAPER_EFFECTS_TOGGLE_ID
                                    )
                                )
                            _creativeEffectsModel.value =
                                CreativeEffectsModel(
                                    title = data.effectsBottomSheetTitle,
                                    subtitle = data.effectsBottomSheetSubtitle,
                                    actions =
                                        listOf(
                                            WallpaperAction(
                                                label = effectsButtonLabel,
                                                applyActionUri = effectsToggleUri,
                                                effectId = effectsId,
                                                toggled = effectsId == data.currentEffectId,
                                            )
                                        ),
                                )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Read wallpaper effects with exception.", e)
            }
        }
    }

    suspend fun turnOnCreativeEffect(actionPosition: Int) {
        withContext(bgDispatcher) {
            val clearActionUri =
                clearActionUri
                    ?: throw NullPointerException(
                        "clearActionUri should be initialized already if creative wallpaper" +
                            " effects are available."
                    )
            val model = _creativeEffectsModel.value ?: return@withContext
            val updatedActions =
                model.actions.mapIndexed { index, action ->
                    val applyActionUri = action.applyActionUri
                    if (actionPosition == index && applyActionUri != null) {
                        context.contentResolver.update(applyActionUri, ContentValues(), null)
                    }
                    action.copy(toggled = actionPosition == index && applyActionUri != null)
                }
            if (actionPosition < 0) {
                context.contentResolver.update(clearActionUri, ContentValues(), null)
            }
            _creativeEffectsModel.value = model.copy(actions = updatedActions)
        }
    }

    fun destroy() {
        _creativeEffectsModel.value = null
        clearActionUri = null
    }

    companion object {
        private const val TAG = "CreativeEffectsRepository"
    }
}
