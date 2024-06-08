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

import android.graphics.Point
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout.Companion.getViewId
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Binds the dialog on small preview confirming and setting wallpaper with destination. */
object SetWallpaperDialogBinder {
    private val PreviewScreenIds =
        mapOf(
            Screen.LOCK_SCREEN to R.id.lock_preview_selector,
            Screen.HOME_SCREEN to R.id.home_preview_selector
        )

    fun bind(
        dialogContent: View,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        isFoldable: Boolean,
        handheldDisplaySize: Point,
        lifecycleOwner: LifecycleOwner,
        mainScope: CoroutineScope,
        currentNavDestId: Int,
        onFinishActivity: () -> Unit,
        onDismissDialog: () -> Unit,
        isFirstBinding: Boolean,
        navigate: ((View) -> Unit)?,
    ) {
        val previewLayout: View =
            if (isFoldable) dialogContent.requireViewById(R.id.foldable_previews)
            else dialogContent.requireViewById(R.id.handheld_previews)
        if (isFoldable)
            bindFoldablePreview(
                previewLayout,
                wallpaperPreviewViewModel,
                lifecycleOwner,
                currentNavDestId,
                isFirstBinding,
                navigate,
            )
        else
            bindHandheldPreview(
                previewLayout,
                wallpaperPreviewViewModel,
                handheldDisplaySize,
                lifecycleOwner,
                currentNavDestId,
                isFirstBinding,
                navigate,
            )

        val cancelButton = dialogContent.requireViewById<Button>(R.id.button_cancel)
        cancelButton.setOnClickListener { onDismissDialog() }

        val confirmButton = dialogContent.requireViewById<Button>(R.id.button_set)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    wallpaperPreviewViewModel.showSetWallpaperDialog.collect { show ->
                        if (!show) {
                            onDismissDialog()
                        }
                    }
                }

                launch {
                    wallpaperPreviewViewModel.setWallpaperDialogOnConfirmButtonClicked.collect {
                        onClicked ->
                        confirmButton.setOnClickListener {
                            mainScope.launch {
                                onClicked()
                                onFinishActivity()
                            }
                        }
                    }
                }

                launch {
                    wallpaperPreviewViewModel.setWallpaperDialogSelectedScreens.collect {
                        selectedScreens ->
                        confirmButton.isEnabled = selectedScreens.isNotEmpty()
                        PreviewScreenIds.forEach { screenId ->
                            bindPreviewSelector(
                                previewLayout.requireViewById(screenId.value),
                                screenId.key,
                                selectedScreens,
                                wallpaperPreviewViewModel,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun bindFoldablePreview(
        previewLayout: View,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
        currentNavDestId: Int,
        isFirstBinding: Boolean,
        navigate: ((View) -> Unit)?,
    ) {
        previewLayout.isVisible = true
        PreviewScreenIds.forEach { screenId ->
            val dualDisplayAspectRatioLayout: DualDisplayAspectRatioLayout =
                previewLayout
                    .requireViewById<FrameLayout>(screenId.value)
                    .requireViewById(R.id.dual_preview)

            dualDisplayAspectRatioLayout.setDisplaySizes(
                mapOf(
                    DeviceDisplayType.FOLDED to wallpaperPreviewViewModel.smallerDisplaySize,
                    DeviceDisplayType.UNFOLDED to
                        wallpaperPreviewViewModel.wallpaperDisplaySize.value,
                )
            )
            DeviceDisplayType.FOLDABLE_DISPLAY_TYPES.forEach { display ->
                val previewDisplaySize = dualDisplayAspectRatioLayout.getPreviewDisplaySize(display)
                val view: View = dualDisplayAspectRatioLayout.requireViewById(display.getViewId())
                previewDisplaySize?.let {
                    SmallPreviewBinder.bind(
                        applicationContext = previewLayout.context.applicationContext,
                        view = view,
                        viewModel = wallpaperPreviewViewModel,
                        viewLifecycleOwner = lifecycleOwner,
                        screen = screenId.key,
                        displaySize = it,
                        deviceDisplayType = display,
                        currentNavDestId = currentNavDestId,
                        isFirstBinding = isFirstBinding,
                        navigate = navigate,
                    )
                }
            }
        }
    }

    private fun bindHandheldPreview(
        previewLayout: View,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        displaySize: Point,
        lifecycleOwner: LifecycleOwner,
        currentNavDestId: Int,
        isFirstBinding: Boolean,
        navigate: ((View) -> Unit)?,
    ) {
        previewLayout.isVisible = true
        PreviewScreenIds.forEach { screenId ->
            val view: View =
                previewLayout
                    .requireViewById<FrameLayout>(screenId.value)
                    .requireViewById(R.id.preview)
            SmallPreviewBinder.bind(
                applicationContext = previewLayout.context.applicationContext,
                view = view,
                viewModel = wallpaperPreviewViewModel,
                screen = screenId.key,
                displaySize = displaySize,
                deviceDisplayType = DeviceDisplayType.SINGLE,
                viewLifecycleOwner = lifecycleOwner,
                currentNavDestId = currentNavDestId,
                isFirstBinding = isFirstBinding,
                navigate = navigate,
            )
        }
    }

    private fun bindPreviewSelector(
        selector: View,
        screen: Screen,
        selectedScreens: Set<Screen>,
        dialogViewModel: WallpaperPreviewViewModel,
    ) {
        selector.isActivated = selectedScreens.contains(screen)
        selector.isSelected = selector.isActivated
        selector.setOnClickListener { dialogViewModel.onSetWallpaperDialogScreenSelected(screen) }
    }
}
