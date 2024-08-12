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

package com.android.wallpaper.picker.preview.ui.fragment

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout.LayoutParams
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.wallpaper.R
import com.android.wallpaper.picker.TrampolinePickerActivity
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.picker.preview.ui.binder.SetWallpaperDialogBinder
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.LaunchSourceUtils.LAUNCH_SOURCE_LAUNCHER
import com.android.wallpaper.util.LaunchSourceUtils.LAUNCH_SOURCE_SETTINGS_HOMEPAGE
import com.android.wallpaper.util.LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Shows LS/HS previews and confirmation to set as wallpaper for HS, LS or both. */
@AndroidEntryPoint(DialogFragment::class)
class SetWallpaperDialogFragment : Hilt_SetWallpaperDialogFragment() {

    @Inject lateinit var displayUtils: DisplayUtils
    @Inject @MainDispatcher lateinit var mainScope: CoroutineScope
    @Inject lateinit var wallpaperConnectionUtils: WallpaperConnectionUtils

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    override fun onStart() {
        super.onStart()
        // Set dialog size
        val widthDimenId =
            if (displayUtils.hasMultiInternalDisplays()) R.dimen.set_wallpaper_dialog_foldable_width
            else R.dimen.set_wallpaper_dialog_handheld_width
        requireDialog()
            .window
            ?.setLayout(resources.getDimension(widthDimenId).toInt(), LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val layout =
            LayoutInflater.from(requireContext()).inflate(R.layout.set_wallpaper_dialog, null)
        val dialog =
            AlertDialog.Builder(requireContext(), R.style.SetWallpaperPreviewDialogTheme)
                .setView(layout)
                .create()

        /**
         * We need to keep the reference shortly, because the activity will be forced to restart due
         * to the theme color update from the system wallpaper change. The activityReference is used
         * to kill [WallpaperPreviewActivity].
         */
        val activityReference = activity
        SetWallpaperDialogBinder.bind(
            layout,
            wallpaperPreviewViewModel,
            displayUtils.hasMultiInternalDisplays(),
            displayUtils.getRealSize(displayUtils.getWallpaperDisplay()),
            lifecycleOwner = this,
            mainScope,
            checkNotNull(findNavController().currentDestination?.id),
            onFinishActivity = {
                Toast.makeText(
                        context,
                        R.string.wallpaper_set_successfully_message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
                if (activityReference != null) {
                    if (wallpaperPreviewViewModel.isNewTask) {
                        activityReference.window?.exitTransition = Slide(Gravity.END)
                        val intent = Intent(activityReference, TrampolinePickerActivity::class.java)
                        intent.setFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                        intent.putExtra(
                            WALLPAPER_LAUNCH_SOURCE,
                            if (wallpaperPreviewViewModel.isViewAsHome) LAUNCH_SOURCE_LAUNCHER
                            else LAUNCH_SOURCE_SETTINGS_HOMEPAGE
                        )
                        activityReference.startActivity(
                            intent,
                            ActivityOptions.makeSceneTransitionAnimation(activityReference)
                                .toBundle()
                        )
                    } else {
                        activityReference.setResult(Activity.RESULT_OK)
                    }
                    activityReference.finish()
                }
            },
            onDismissDialog = { findNavController().popBackStack() },
            wallpaperConnectionUtils = wallpaperConnectionUtils,
            isFirstBinding = false,
            navigate = null,
        )

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        wallpaperPreviewViewModel.dismissSetWallpaperDialog()
    }
}
