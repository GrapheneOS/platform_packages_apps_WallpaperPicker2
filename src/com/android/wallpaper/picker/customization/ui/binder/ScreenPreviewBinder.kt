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

package com.android.wallpaper.picker.customization.ui.binder

import android.app.Activity
import android.app.WallpaperColors
import android.content.Intent
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.view.SurfaceView
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.asset.BitmapCachingAsset
import com.android.wallpaper.asset.CurrentWallpaperAssetVN
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.picker.WorkspaceSurfaceHolderCallback
import com.android.wallpaper.picker.customization.ui.viewmodel.ScreenPreviewViewModel
import com.android.wallpaper.util.ResourceUtils
import com.android.wallpaper.util.WallpaperConnection
import com.android.wallpaper.util.WallpaperSurfaceCallback
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

/**
 * Binds between view and view-model for rendering the preview of the home screen or the lock
 * screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object ScreenPreviewBinder {
    interface Binding {
        fun show()
        fun hide()
        fun sendMessage(
            id: Int,
            args: Bundle = Bundle.EMPTY,
        )
    }

    /**
     * Binds the view to the given [viewModel].
     *
     * Note that if [dimWallpaper] is `true`, the wallpaper will be dimmed (to help highlight
     * something that is changing on top of the wallpaper, for example, the lock screen shortcuts or
     * the clock).
     */
    @JvmStatic
    fun bind(
        activity: Activity,
        previewView: CardView,
        viewModel: ScreenPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
        offsetToStart: Boolean,
        dimWallpaper: Boolean = false,
    ): Binding {
        val workspaceSurface: SurfaceView = previewView.requireViewById(R.id.workspace_surface)
        val wallpaperSurface: SurfaceView = previewView.requireViewById(R.id.wallpaper_surface)
        wallpaperSurface.setZOrderOnTop(false)

        if (dimWallpaper) {
            previewView.requireViewById<View>(R.id.wallpaper_dimming_scrim).isVisible = true
            workspaceSurface.setZOrderOnTop(true)
        }

        previewView.radius =
            previewView.resources.getDimension(R.dimen.wallpaper_picker_entry_card_corner_radius)

        var previewSurfaceCallback: WorkspaceSurfaceHolderCallback? = null
        var wallpaperSurfaceCallback: WallpaperSurfaceCallback? = null
        var wallpaperConnection: WallpaperConnection? = null
        var wallpaperInfo: WallpaperInfo? = null

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                previewSurfaceCallback =
                    WorkspaceSurfaceHolderCallback(
                        workspaceSurface,
                        viewModel.previewUtils,
                        viewModel.getInitialExtras(),
                    )
                workspaceSurface.holder.addCallback(previewSurfaceCallback)

                wallpaperSurfaceCallback =
                    WallpaperSurfaceCallback(
                        previewView.context,
                        previewView,
                        wallpaperSurface,
                        CompletableFuture.completedFuture(
                            WallpaperInfo.ColorInfo(
                                /* wallpaperColors= */ null,
                                ResourceUtils.getColorAttr(
                                    previewView.context,
                                    android.R.attr.colorSecondary,
                                )
                            )
                        ),
                    ) {
                        maybeLoadThumbnail(
                            activity = activity,
                            wallpaperInfo = wallpaperInfo,
                            surfaceCallback = wallpaperSurfaceCallback,
                            offsetToStart = offsetToStart,
                        )
                    }
                wallpaperSurface.holder.addCallback(wallpaperSurfaceCallback)
            }

            // Here when destroyed.
            workspaceSurface.holder.removeCallback(previewSurfaceCallback)
            previewSurfaceCallback?.cleanUp()
            wallpaperSurface.holder.removeCallback(wallpaperSurfaceCallback)
            wallpaperSurfaceCallback?.cleanUp()
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Do nothing.
            }

            // Here when stopped.
            wallpaperConnection?.disconnect()
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                lifecycleOwner.lifecycleScope.launch {
                    wallpaperInfo = viewModel.getWallpaperInfo()
                    (wallpaperInfo as? LiveWallpaperInfo)?.let { liveWallpaperInfo ->
                        if (WallpaperConnection.isPreviewAvailable()) {
                            wallpaperConnection =
                                WallpaperConnection(
                                    Intent(WallpaperService.SERVICE_INTERFACE).apply {
                                        setClassName(
                                            liveWallpaperInfo.wallpaperComponent.packageName,
                                            liveWallpaperInfo.wallpaperComponent.serviceName
                                        )
                                    },
                                    previewView.context,
                                    object : WallpaperConnection.WallpaperConnectionListener {
                                        override fun onWallpaperColorsChanged(
                                            colors: WallpaperColors?,
                                            displayId: Int
                                        ) {
                                            viewModel.onWallpaperColorsChanged(colors)
                                        }
                                    },
                                    wallpaperSurface,
                                    null,
                                )

                            wallpaperConnection?.connect()
                            wallpaperConnection?.setVisibility(true)
                        }
                    }
                    maybeLoadThumbnail(
                        activity = activity,
                        wallpaperInfo = wallpaperInfo,
                        surfaceCallback = wallpaperSurfaceCallback,
                        offsetToStart = offsetToStart,
                    )
                }
            }

            // Here when paused.
            wallpaperConnection?.setVisibility(false)
        }

        return object : Binding {
            override fun show() {
                previewView.isVisible = true
                wallpaperSurface.isVisible = true
                workspaceSurface.isVisible = true
            }

            override fun hide() {
                previewView.isVisible = false
                wallpaperSurface.isVisible = false
                workspaceSurface.isVisible = false
            }

            override fun sendMessage(id: Int, args: Bundle) {
                previewSurfaceCallback?.send(id, args)
            }
        }
    }

    private fun maybeLoadThumbnail(
        activity: Activity,
        wallpaperInfo: WallpaperInfo?,
        surfaceCallback: WallpaperSurfaceCallback?,
        offsetToStart: Boolean,
    ) {
        if (wallpaperInfo == null || surfaceCallback == null) {
            return
        }

        val imageView = surfaceCallback.homeImageWallpaper
        val thumbAsset: Asset = wallpaperInfo.getThumbAsset(activity)
        if (imageView != null && imageView.drawable == null) {
            // Respect offsetToStart only for CurrentWallpaperAssetVN otherwise true.
            BitmapCachingAsset(activity, thumbAsset)
                .loadPreviewImage(
                    activity,
                    imageView,
                    ResourceUtils.getColorAttr(activity, android.R.attr.colorSecondary),
                    /* offsetToStart= */ thumbAsset !is CurrentWallpaperAssetVN || offsetToStart
                )
        }
    }
}
