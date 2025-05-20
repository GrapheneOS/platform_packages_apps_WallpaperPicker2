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

package com.android.wallpaper.picker.category.ui.view

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.MyPhotosStarter
import com.android.wallpaper.picker.MyPhotosStarter.PermissionChangedListener
import com.android.wallpaper.picker.WallpaperPickerDelegate
import com.android.wallpaper.picker.category.ui.view.CategoriesFragment.Companion.READ_IMAGE_PERMISSION
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles all the operations related to photo picker and MyPhotos tile in the category
 * page.
 */
@Singleton
class MyPhotosStarterImpl @Inject constructor() : MyPhotosStarter {

    private val permissionChangedListeners: MutableList<PermissionChangedListener> = mutableListOf()
    private val isNewPickerUi = BaseFlags.get().isNewPickerUi()

    override fun requestCustomPhotoPicker(
        listener: PermissionChangedListener,
        activity: Activity,
        photoPickerLauncher: ActivityResultLauncher<Intent>,
    ) {
        // TODO (b/282073506): Figure out a better way to have better photos experience
        if (!isReadExternalStoragePermissionGranted(activity) && !isNewPickerUi) {
            val wrappedListener: PermissionChangedListener =
                object : PermissionChangedListener {
                    override fun onPermissionsGranted() {
                        listener.onPermissionsGranted()
                        showCustomPhotoPicker(photoPickerLauncher)
                    }

                    override fun onPermissionsDenied(dontAskAgain: Boolean) {
                        listener.onPermissionsDenied(dontAskAgain)
                    }
                }
            requestExternalStoragePermission(wrappedListener, activity)
            return
        }

        showCustomPhotoPicker(photoPickerLauncher)
    }

    private fun isReadExternalStoragePermissionGranted(activity: Activity): Boolean {
        return activity.packageManager.checkPermission(
            Manifest.permission.READ_MEDIA_IMAGES,
            activity.packageName,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestExternalStoragePermission(
        listener: PermissionChangedListener?,
        activity: Activity,
    ) {
        if (listener != null) {
            permissionChangedListeners.add(listener)
        }
        activity.requestPermissions(
            arrayOf<String>(READ_IMAGE_PERMISSION),
            WallpaperPickerDelegate.READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE,
        )
    }

    private fun showCustomPhotoPicker(photoPickerLauncher: ActivityResultLauncher<Intent>) {
        val injector = InjectorProvider.getInjector()
        try {
            val intent: Intent = injector.getMyPhotosIntentProvider().getMyPhotosIntent()
            photoPickerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            val fallback: Intent? = injector.getMyPhotosIntentProvider().fallbackIntent
            if (fallback != null) {
                Log.i(TAG, "Couldn't launch photo picker with main intent, trying with fallback")
                photoPickerLauncher.launch(fallback)
            } else {
                Log.e(
                    TAG,
                    "Couldn't launch photo picker with main intent and no fallback is " +
                        "available",
                )
                throw e
            }
        }
    }

    /**
     * This method is not implemented on purpose since the other method that allows specifying a
     * custom activity is already implemented which achieves the main purpose of requesting the
     * photo picker. This method is only added for backward compatibility purposes so we can
     * continue to use the same interface as earlier.
     */
    override fun requestCustomPhotoPicker(listener: PermissionChangedListener?) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "WallpaperPickerDelegate2"
    }
}
