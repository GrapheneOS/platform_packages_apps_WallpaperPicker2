/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.wallpaper.picker.preview.ui.binder

import android.content.Context
import android.graphics.Point
import android.view.SurfaceView
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.preview.ui.fragment.SmallPreviewFragment
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch

object SmallPreviewBinder {

    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: WallpaperPreviewViewModel,
        screen: Screen,
        displaySize: Point,
        deviceDisplayType: DeviceDisplayType,
        viewLifecycleOwner: LifecycleOwner,
        currentNavDestId: Int,
        navigate: ((View) -> Unit)? = null,
        transition: Transition? = null,
        transitionConfig: FullPreviewConfigViewModel? = null,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
    ) {

        val previewCard: CardView = view.requireViewById(R.id.preview_card)
        val cardRadius = previewCard.radius
        val foldedStateDescription =
            when (deviceDisplayType) {
                DeviceDisplayType.FOLDED ->
                    view.context.getString(R.string.folded_device_state_description)
                DeviceDisplayType.UNFOLDED ->
                    view.context.getString(R.string.unfolded_device_state_description)
                else -> ""
            }
        previewCard.contentDescription =
            view.context.getString(
                R.string.wallpaper_preview_card_content_description_editable,
                foldedStateDescription
            )
        val wallpaperSurface: SurfaceView = view.requireViewById(R.id.wallpaper_surface)
        val workspaceSurface: SurfaceView = view.requireViewById(R.id.workspace_surface)

        // Set transition names to enable the small to full preview enter and return shared
        // element transitions.
        val transitionName =
            when (screen) {
                Screen.LOCK_SCREEN ->
                    when (deviceDisplayType) {
                        DeviceDisplayType.SINGLE ->
                            SmallPreviewFragment.SMALL_PREVIEW_LOCK_SHARED_ELEMENT_ID
                        DeviceDisplayType.FOLDED ->
                            SmallPreviewFragment.SMALL_PREVIEW_LOCK_FOLDED_SHARED_ELEMENT_ID
                        DeviceDisplayType.UNFOLDED ->
                            SmallPreviewFragment.SMALL_PREVIEW_LOCK_UNFOLDED_SHARED_ELEMENT_ID
                    }
                Screen.HOME_SCREEN ->
                    when (deviceDisplayType) {
                        DeviceDisplayType.SINGLE ->
                            SmallPreviewFragment.SMALL_PREVIEW_HOME_SHARED_ELEMENT_ID
                        DeviceDisplayType.FOLDED ->
                            SmallPreviewFragment.SMALL_PREVIEW_HOME_FOLDED_SHARED_ELEMENT_ID
                        DeviceDisplayType.UNFOLDED ->
                            SmallPreviewFragment.SMALL_PREVIEW_HOME_UNFOLDED_SHARED_ELEMENT_ID
                    }
            }
        ViewCompat.setTransitionName(previewCard, transitionName)

        var transitionDisposableHandle: DisposableHandle? = null
        val transitionListener =
            if (transition == null || transitionConfig == null) null
            else
                object : TransitionListenerAdapter() {
                    // All surface views are initially visible in the XML to enable smoother
                    // transitions. Only hide the surface views not used in the shared element
                    // transition until the transition ends to avoid issues with multiple surface
                    // views
                    // overlapping.
                    override fun onTransitionStart(transition: Transition) {
                        super.onTransitionStart(transition)
                        if (
                            transitionConfig.screen == screen &&
                                transitionConfig.deviceDisplayType == deviceDisplayType
                        ) {
                            // Put the surface on top for full transition, the card view is behind
                            // the surface view so we need to apply radius on surface view instead
                            wallpaperSurface.cornerRadius = cardRadius
                            wallpaperSurface.setZOrderOnTop(true)
                            workspaceSurface.setZOrderOnTop(true)
                        } else {
                            // If transitioning to another small preview, keep child surfaces hidden
                            // until transition ends.
                            wallpaperSurface.isVisible = false
                            workspaceSurface.isVisible = false
                        }
                    }

                    override fun onTransitionEnd(transition: Transition) {
                        super.onTransitionEnd(transition)
                        if (
                            transitionConfig.screen == screen &&
                                transitionConfig.deviceDisplayType == deviceDisplayType
                        ) {
                            wallpaperSurface.setZOrderMediaOverlay(true)
                            workspaceSurface.setZOrderMediaOverlay(true)
                        } else {
                            wallpaperSurface.isVisible = true
                            workspaceSurface.isVisible = true
                            wallpaperSurface.alpha = 0f
                            workspaceSurface.alpha = 0f

                            val mediumAnimTimeMs =
                                view.resources
                                    .getInteger(android.R.integer.config_mediumAnimTime)
                                    .toLong()
                            wallpaperSurface.startFadeInAnimation(mediumAnimTimeMs)
                            workspaceSurface.startFadeInAnimation(mediumAnimTimeMs)
                        }

                        transition.removeListener(this)
                        transitionDisposableHandle = null
                    }
                }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                transitionListener?.let {
                    // If transitionListener is not null so do transition and transitionConfig
                    transition!!.addListener(it)
                    transitionDisposableHandle = DisposableHandle { transition.removeListener(it) }
                }

                if (R.id.smallPreviewFragment == currentNavDestId) {
                    viewModel
                        .onSmallPreviewClicked(screen, deviceDisplayType) {
                            navigate?.invoke(previewCard)
                        }
                        .collect { onClick ->
                            if (onClick != null) {
                                view.setOnClickListener { onClick() }
                            } else {
                                view.setOnClickListener(null)
                            }
                        }
                } else if (R.id.setWallpaperDialog == currentNavDestId) {
                    previewCard.radius =
                        previewCard.resources.getDimension(
                            R.dimen.set_wallpaper_dialog_preview_corner_radius
                        )
                }
            }
            // Remove transition listeners on destroy
            transitionDisposableHandle?.dispose()
            transitionDisposableHandle = null
            // Remove on click listener when on destroyed
            view.setOnClickListener(null)
        }

        val config = viewModel.getWorkspacePreviewConfig(screen, deviceDisplayType)
        WorkspacePreviewBinder.bind(
            workspaceSurface,
            config,
            viewModel,
            viewLifecycleOwner,
        )

        SmallWallpaperPreviewBinder.bind(
            surface = wallpaperSurface,
            viewModel = viewModel,
            displaySize = displaySize,
            applicationContext = applicationContext,
            viewLifecycleOwner = viewLifecycleOwner,
            deviceDisplayType = deviceDisplayType,
            wallpaperConnectionUtils = wallpaperConnectionUtils,
            isFirstBindingDeferred = isFirstBindingDeferred,
        )
    }

    private fun SurfaceView.startFadeInAnimation(duration: Long) {
        animate().alpha(1f).setDuration(duration).start()
    }
}
