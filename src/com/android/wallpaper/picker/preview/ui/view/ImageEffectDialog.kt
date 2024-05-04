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

package com.android.wallpaper.picker.preview.ui.view

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog

/** The dialog is used for image effect preview, with a continue button and a cancel button. */
class ImageEffectDialog(
    activity: Activity,
    title: String,
    description: String,
    cancelButtonString: String,
    continueButtonString: String,
) {

    var onDismiss: (() -> Unit)? = null
    var onContinue: (() -> Unit)? = null

    private val dialog: Dialog

    init {
        dialog =
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(description)
                .setOnDismissListener { onDismiss?.invoke() }
                .setNegativeButton(cancelButtonString, null)
                .setPositiveButton(continueButtonString) { _, _ -> onContinue?.invoke() }
                .create()
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
