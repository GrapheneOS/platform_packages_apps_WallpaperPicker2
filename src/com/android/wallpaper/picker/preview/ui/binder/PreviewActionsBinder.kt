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

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.preview.ui.util.ImageEffectDialogUtil
import com.android.wallpaper.picker.preview.ui.view.ImageEffectDialog
import com.android.wallpaper.picker.preview.ui.view.PreviewActionFloatingSheet
import com.android.wallpaper.picker.preview.ui.view.PreviewActionGroup
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.CUSTOMIZE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DOWNLOAD
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EDIT
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EFFECTS
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.SHARE
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperActionsToggleAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import kotlinx.coroutines.launch

/** Binds the action buttons and bottom sheet to [PreviewActionsViewModel] */
object PreviewActionsBinder {

    fun bind(
        actionGroup: PreviewActionGroup,
        floatingSheet: PreviewActionFloatingSheet,
        previewViewModel: WallpaperPreviewViewModel,
        actionsViewModel: PreviewActionsViewModel,
        deviceDisplayType: DeviceDisplayType,
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        logger: UserEventLogger,
        imageEffectDialogUtil: ImageEffectDialogUtil,
        onNavigateToEditScreen: (intent: Intent) -> Unit,
        onStartShareActivity: (intent: Intent) -> Unit,
    ) {
        var deleteDialog: AlertDialog? = null
        var onDelete: (() -> Unit)?
        var imageEffectConfirmDownloadDialog: ImageEffectDialog? = null
        var imageEffectConfirmExitDialog: ImageEffectDialog? = null
        var onBackPressedCallback: OnBackPressedCallback? = null

        val floatingSheetCallback =
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(view: View, newState: Int) {
                    // We set visibility to invisible, instead of gone because we listen to the
                    // state change of the BottomSheet and the state change callbacks are only fired
                    // when the view is not gone.
                    if (newState == STATE_HIDDEN) {
                        actionsViewModel.onFloatingSheetCollapsed()
                        floatingSheet.isInvisible = true
                    } else {
                        floatingSheet.isInvisible = false
                    }
                }

                override fun onSlide(p0: View, p1: Float) {}
            }
        floatingSheet.isInvisible = !actionsViewModel.isAnyActionChecked()
        floatingSheet.addFloatingSheetCallback(floatingSheetCallback)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                floatingSheet.addFloatingSheetCallback(floatingSheetCallback)
            }
            floatingSheet.removeFloatingSheetCallback(floatingSheetCallback)
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                /** [INFORMATION] */
                launch {
                    actionsViewModel.isInformationVisible.collect {
                        actionGroup.setIsVisible(INFORMATION, it)
                    }
                }

                launch {
                    actionsViewModel.isInformationChecked.collect {
                        actionGroup.setIsChecked(INFORMATION, it)
                    }
                }

                launch {
                    actionsViewModel.onInformationClicked.collect {
                        actionGroup.setClickListener(INFORMATION, it)
                    }
                }

                /** [DOWNLOAD] */
                launch {
                    actionsViewModel.isDownloadVisible.collect {
                        actionGroup.setIsVisible(DOWNLOAD, it)
                    }
                }

                launch {
                    actionsViewModel.isDownloading.collect { actionGroup.setIsDownloading(it) }
                }

                launch {
                    actionsViewModel.isDownloadButtonEnabled.collect {
                        actionGroup.setClickListener(
                            DOWNLOAD,
                            if (it) {
                                { actionsViewModel.downloadWallpaper() }
                            } else null,
                        )
                    }
                }

                /** [DELETE] */
                launch {
                    actionsViewModel.isDeleteVisible.collect {
                        actionGroup.setIsVisible(DELETE, it)
                    }
                }

                launch {
                    actionsViewModel.isDeleteChecked.collect {
                        actionGroup.setIsChecked(DELETE, it)
                    }
                }

                launch {
                    actionsViewModel.onDeleteClicked.collect {
                        actionGroup.setClickListener(DELETE, it)
                    }
                }

                launch {
                    actionsViewModel.deleteConfirmationDialogViewModel.collect { viewModel ->
                        val appContext = activity.applicationContext
                        if (viewModel != null) {
                            onDelete = {
                                if (viewModel.creativeWallpaperDeleteUri != null) {
                                    appContext.contentResolver.delete(
                                        viewModel.creativeWallpaperDeleteUri,
                                        null,
                                        null
                                    )
                                } else if (viewModel.liveWallpaperDeleteIntent != null) {
                                    appContext.startService(viewModel.liveWallpaperDeleteIntent)
                                }
                                activity.finish()
                            }
                            val dialog =
                                deleteDialog
                                    ?: AlertDialog.Builder(activity)
                                        .setMessage(R.string.delete_wallpaper_confirmation)
                                        .setOnDismissListener { viewModel.onDismiss.invoke() }
                                        .setPositiveButton(R.string.delete_live_wallpaper) { _, _ ->
                                            onDelete?.invoke()
                                        }
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .create()
                                        .also { deleteDialog = it }
                            dialog.show()
                        } else {
                            deleteDialog?.dismiss()
                        }
                    }
                }

                /** [EDIT] */
                launch {
                    actionsViewModel.isEditVisible.collect { actionGroup.setIsVisible(EDIT, it) }
                }

                launch {
                    actionsViewModel.isEditChecked.collect { actionGroup.setIsChecked(EDIT, it) }
                }

                launch {
                    actionsViewModel.editIntent.collect {
                        actionGroup.setClickListener(
                            EDIT,
                            if (it != null) {
                                {
                                    // We need to set default wallpaper preview config view model
                                    // before entering full screen with edit activity overlay.
                                    previewViewModel.setDefaultFullPreviewConfigViewModel(
                                        deviceDisplayType
                                    )
                                    onNavigateToEditScreen.invoke(it)
                                }
                            } else null
                        )
                    }
                }

                /** [CUSTOMIZE] */
                launch {
                    actionsViewModel.isCustomizeVisible.collect {
                        actionGroup.setIsVisible(CUSTOMIZE, it)
                    }
                }

                launch {
                    actionsViewModel.isCustomizeChecked.collect {
                        actionGroup.setIsChecked(CUSTOMIZE, it)
                    }
                }

                launch {
                    actionsViewModel.onCustomizeClicked.collect {
                        actionGroup.setClickListener(CUSTOMIZE, it)
                    }
                }

                /** [EFFECTS] */
                launch {
                    actionsViewModel.isEffectsVisible.collect {
                        actionGroup.setIsVisible(EFFECTS, it)
                    }
                }

                launch {
                    actionsViewModel.isEffectsChecked.collect {
                        actionGroup.setIsChecked(EFFECTS, it)
                    }
                }

                launch {
                    actionsViewModel.onEffectsClicked.collect {
                        actionGroup.setClickListener(EFFECTS, it)
                    }
                }

                launch {
                    actionsViewModel.effectDownloadFailureToastText.collect {
                        Toast.makeText(floatingSheet.context, it, Toast.LENGTH_LONG).show()
                    }
                }

                launch {
                    actionsViewModel.imageEffectConfirmDownloadDialogViewModel.collect { viewModel
                        ->
                        if (viewModel != null) {
                            val dialog =
                                imageEffectConfirmDownloadDialog
                                    ?: imageEffectDialogUtil
                                        .createConfirmDownloadDialog(activity)
                                        .also { imageEffectConfirmDownloadDialog = it }
                            dialog.onDismiss = viewModel.onDismiss
                            dialog.onContinue = viewModel.onContinue
                            dialog.show()
                        } else {
                            imageEffectConfirmDownloadDialog?.dismiss()
                        }
                    }
                }

                launch {
                    actionsViewModel.imageEffectConfirmExitDialogViewModel.collect { viewModel ->
                        if (viewModel != null) {
                            val dialog =
                                imageEffectConfirmExitDialog
                                    ?: imageEffectDialogUtil
                                        .createConfirmExitDialog(activity)
                                        .also { imageEffectConfirmExitDialog = it }
                            dialog.onDismiss = viewModel.onDismiss
                            dialog.onContinue = {
                                viewModel.onContinue()
                                activity.onBackPressedDispatcher.onBackPressed()
                            }
                            dialog.show()
                        } else {
                            imageEffectConfirmExitDialog?.dismiss()
                        }
                    }
                }

                launch {
                    actionsViewModel.handleOnBackPressed.collect { handleOnBackPressed ->
                        // Reset the callback
                        onBackPressedCallback?.remove()
                        onBackPressedCallback = null
                        if (handleOnBackPressed != null) {
                            // If handleOnBackPressed is not null, set it to the activity
                            val callback =
                                object : OnBackPressedCallback(true) {
                                        override fun handleOnBackPressed() {
                                            val handled = handleOnBackPressed()
                                            if (!handled) {
                                                onBackPressedCallback?.remove()
                                                onBackPressedCallback = null
                                                activity.onBackPressedDispatcher.onBackPressed()
                                            }
                                        }
                                    }
                                    .also { onBackPressedCallback = it }
                            activity.onBackPressedDispatcher.addCallback(lifecycleOwner, callback)
                        }
                    }
                }

                /** [SHARE] */
                launch {
                    actionsViewModel.isShareVisible.collect { actionGroup.setIsVisible(SHARE, it) }
                }

                launch {
                    actionsViewModel.shareIntent.collect {
                        actionGroup.setClickListener(
                            SHARE,
                            if (it != null) {
                                { onStartShareActivity.invoke(it) }
                            } else null
                        )
                    }
                }

                /** Floating sheet behavior */
                launch {
                    actionsViewModel.previewFloatingSheetViewModel.collect { floatingSheetViewModel
                        ->
                        if (floatingSheetViewModel != null) {
                            val (
                                informationViewModel,
                                imageEffectViewModel,
                                creativeEffectViewModel,
                                customizeViewModel,
                            ) = floatingSheetViewModel
                            when {
                                informationViewModel != null -> {
                                    floatingSheet.setInformationContent(
                                        informationViewModel.attributions,
                                        informationViewModel.actionUrl?.let { url ->
                                            {
                                                logger.logWallpaperExploreButtonClicked()
                                                floatingSheet.context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                )
                                            }
                                        },
                                        informationViewModel.actionButtonTitle,
                                    )
                                }
                                imageEffectViewModel != null ->
                                    floatingSheet.setImageEffectContent(
                                        imageEffectViewModel.effectType,
                                        imageEffectViewModel.myPhotosClickListener,
                                        imageEffectViewModel.collapseFloatingSheetListener,
                                        imageEffectViewModel.effectSwitchListener,
                                        imageEffectViewModel.effectDownloadClickListener,
                                        imageEffectViewModel.status,
                                        imageEffectViewModel.resultCode,
                                        imageEffectViewModel.errorMessage,
                                        imageEffectViewModel.title,
                                        imageEffectViewModel.effectTextRes,
                                    )
                                creativeEffectViewModel != null ->
                                    floatingSheet.setCreativeEffectContent(
                                        creativeEffectViewModel.title,
                                        creativeEffectViewModel.subtitle,
                                        creativeEffectViewModel.wallpaperActions,
                                        object :
                                            WallpaperActionsToggleAdapter.WallpaperEffectSwitchListener {
                                            override fun onEffectSwitchChanged(checkedItem: Int) {
                                                launch {
                                                    creativeEffectViewModel
                                                        .wallpaperEffectSwitchListener(checkedItem)
                                                }
                                            }
                                        },
                                    )
                                customizeViewModel != null ->
                                    floatingSheet.setCustomizeContent(
                                        customizeViewModel.customizeSliceUri
                                    )
                            }
                            floatingSheet.expand()
                        } else {
                            floatingSheet.collapse()
                        }
                    }
                }
            }
        }
    }
}
