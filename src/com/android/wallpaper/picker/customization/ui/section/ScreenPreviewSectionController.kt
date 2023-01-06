/*
 * Copyright (C) 2022 The Android Open Source Project
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
 *
 */

package com.android.wallpaper.picker.customization.ui.section

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperColors
import android.content.Context
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.android.wallpaper.R
import com.android.wallpaper.model.CustomizationSectionController
import com.android.wallpaper.model.WallpaperColorsViewModel
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.CurrentWallpaperInfoFactory
import com.android.wallpaper.module.CustomizationSections
import com.android.wallpaper.picker.customization.ui.binder.ScreenPreviewBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ScreenPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.PreviewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Controls the screen preview section. */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenPreviewSectionController(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val initialScreen: CustomizationSections.Screen,
    private val wallpaperInfoFactory: CurrentWallpaperInfoFactory,
    private val colorViewModel: WallpaperColorsViewModel,
    private val displayUtils: DisplayUtils,
) : CustomizationSectionController<ScreenPreviewView> {

    private lateinit var lockScreenBinding: ScreenPreviewBinder.Binding
    private lateinit var homeScreenBinding: ScreenPreviewBinder.Binding

    override fun isAvailable(context: Context?): Boolean {
        // Assumption is that, if this section controller is included, we are using the revamped UI
        // so it should always be shown.
        return true
    }

    @SuppressLint("InflateParams")
    override fun createView(context: Context): ScreenPreviewView {
        val view =
            LayoutInflater.from(context)
                .inflate(
                    R.layout.screen_preview_section,
                    /* parent= */ null,
                ) as ScreenPreviewView
        val lockScreenView: CardView = view.requireViewById(R.id.lock_preview)
        val homeScreenView: CardView = view.requireViewById(R.id.home_preview)

        lockScreenBinding =
            ScreenPreviewBinder.bind(
                activity = activity,
                previewView = lockScreenView,
                viewModel =
                    ScreenPreviewViewModel(
                        previewUtils =
                            PreviewUtils(
                                context = context,
                                authority =
                                    context.getString(
                                        R.string.lock_screen_preview_provider_authority,
                                    ),
                            ),
                        wallpaperInfoProvider = {
                            suspendCancellableCoroutine { continuation ->
                                wallpaperInfoFactory.createCurrentWallpaperInfos(
                                    { homeWallpaper, lockWallpaper, _ ->
                                        val wallpaper = lockWallpaper ?: homeWallpaper
                                        loadInitialColors(
                                            context = context,
                                            wallpaper = wallpaper,
                                            liveData = colorViewModel.lockWallpaperColors,
                                        )
                                        continuation.resume(wallpaper, null)
                                    },
                                    /* forceRefresh= */ true,
                                )
                            }
                        },
                        onWallpaperColorChanged = { colors ->
                            colorViewModel.lockWallpaperColors.value = colors
                        },
                    ),
                lifecycleOwner = lifecycleOwner,
                offsetToStart = displayUtils.isOnWallpaperDisplay(activity),
            )
        homeScreenBinding =
            ScreenPreviewBinder.bind(
                activity = activity,
                previewView = homeScreenView,
                viewModel =
                    ScreenPreviewViewModel(
                        previewUtils =
                            PreviewUtils(
                                context = context,
                                authorityMetadataKey =
                                    context.getString(
                                        R.string.grid_control_metadata_name,
                                    ),
                            ),
                        wallpaperInfoProvider = {
                            suspendCancellableCoroutine { continuation ->
                                wallpaperInfoFactory.createCurrentWallpaperInfos(
                                    { homeWallpaper, lockWallpaper, _ ->
                                        val wallpaper = homeWallpaper ?: lockWallpaper
                                        loadInitialColors(
                                            context = context,
                                            wallpaper = wallpaper,
                                            liveData = colorViewModel.homeWallpaperColors,
                                        )
                                        continuation.resume(wallpaper, null)
                                    },
                                    /* forceRefresh= */ true,
                                )
                            }
                        },
                        onWallpaperColorChanged = { colors ->
                            colorViewModel.lockWallpaperColors.value = colors
                        },
                    ),
                lifecycleOwner = lifecycleOwner,
                offsetToStart = displayUtils.isOnWallpaperDisplay(activity),
            )

        onScreenSwitched(isOnLockScreen = initialScreen == CustomizationSections.Screen.LOCK_SCREEN)

        return view
    }

    override fun onScreenSwitched(isOnLockScreen: Boolean) {
        if (isOnLockScreen) {
            lockScreenBinding.show()
            homeScreenBinding.hide()
        } else {
            lockScreenBinding.hide()
            homeScreenBinding.show()
        }
    }

    private fun loadInitialColors(
        context: Context,
        wallpaper: WallpaperInfo?,
        liveData: MutableLiveData<WallpaperColors>,
    ) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val colors = wallpaper?.computeColorInfo(context)?.get()?.wallpaperColors
            withContext(Dispatchers.Main) { liveData.value = colors }
        }
    }
}
