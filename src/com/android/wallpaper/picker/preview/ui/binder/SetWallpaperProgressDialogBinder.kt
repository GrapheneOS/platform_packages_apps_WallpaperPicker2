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

import android.app.Activity
import android.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.launch

/** Binds the set wallpaper progress dialog. */
object SetWallpaperProgressDialogBinder {

    fun bind(
        viewModel: WallpaperPreviewViewModel,
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
    ) {
        var setWallpaperProgressDialog: AlertDialog? = null

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSetWallpaperProgressBarVisible.collect { visible ->
                        if (visible) {
                            val dialog =
                                setWallpaperProgressDialog
                                    ?: createSetWallpaperProgressDialog(activity).also {
                                        setWallpaperProgressDialog = it
                                    }
                            dialog.show()
                        } else {
                            setWallpaperProgressDialog?.hide()
                        }
                    }
                }
            }
        }
    }

    private fun createSetWallpaperProgressDialog(
        activity: Activity,
    ): AlertDialog {
        val dialogView =
            activity.layoutInflater.inflate(R.layout.set_wallpaper_progress_dialog_view, null)
        return AlertDialog.Builder(activity).setView(dialogView).create()
    }
}
