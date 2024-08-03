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

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.wallpaper.R
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.preview.ui.binder.FullWallpaperPreviewBinder
import com.android.wallpaper.picker.preview.ui.fragment.SmallPreviewFragment.Companion.ARG_EDIT_INTENT
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred

/** Shows full preview with an edit activity overlay. */
@AndroidEntryPoint(AppbarFragment::class)
class CreativeEditPreviewFragment : Hilt_CreativeEditPreviewFragment() {

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var displayUtils: DisplayUtils

    private lateinit var currentView: View

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        currentView = inflater.inflate(R.layout.fragment_full_preview, container, false)
        setUpToolbar(currentView, true, true)

        wallpaperPreviewViewModel.setDefaultFullPreviewConfigViewModel(
            deviceDisplayType = displayUtils.getCurrentDisplayType(requireActivity()),
        )

        currentView.requireViewById<Toolbar>(R.id.toolbar).isVisible = false
        currentView.requireViewById<SurfaceView>(R.id.workspace_surface).isVisible = false
        currentView.requireViewById<Button>(R.id.crop_wallpaper_button).isVisible = false

        val intent =
            arguments?.getParcelable(ARG_EDIT_INTENT, Intent::class.java)
                ?: throw IllegalArgumentException(
                    "To render the first screen in the create new creative wallpaper flow, the intent for rendering the edit activity overlay can not be null."
                )
        val isCreateNew =
            intent.getBooleanExtra(PreviewActionsViewModel.EXTRA_KEY_IS_CREATE_NEW, false)
        val creativeWallpaperEditActivityResult =
            if (isCreateNew) {
                requireActivity().activityResultRegistry.register(
                    CREATIVE_RESULT_REGISTRY,
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    // Reset full preview view model to disable full to small preview transition
                    wallpaperPreviewViewModel.resetFullPreviewConfigViewModel()
                    wallpaperPreviewViewModel.isCurrentlyEditingCreativeWallpaper = false
                    // Callback when the overlaying edit activity is finished. Result code of
                    // RESULT_OK means the user clicked on the check button; RESULT_CANCELED
                    // otherwise.
                    if (it.resultCode == RESULT_OK) {
                        // When clicking on the check button, navigate to the small preview
                        // fragment.
                        findNavController()
                            .navigate(
                                R.id.action_creativeEditPreviewFragment_to_smallPreviewFragment
                            )
                    } else {
                        activity?.finish()
                    }
                }
            } else {
                requireActivity().activityResultRegistry.register(
                    CREATIVE_RESULT_REGISTRY,
                    object : ActivityResultContract<Intent, Int>() {
                        override fun createIntent(context: Context, input: Intent): Intent {
                            return input
                        }

                        override fun parseResult(resultCode: Int, intent: Intent?): Int {
                            wallpaperPreviewViewModel.isCurrentlyEditingCreativeWallpaper = false
                            return resultCode
                        }
                    },
                ) {
                    // Reset full preview view model to disable full to small preview transition
                    wallpaperPreviewViewModel.resetFullPreviewConfigViewModel()
                    findNavController().popBackStack()
                }
            }

        if (!wallpaperPreviewViewModel.isCurrentlyEditingCreativeWallpaper) {
            wallpaperPreviewViewModel.isCurrentlyEditingCreativeWallpaper = true
            creativeWallpaperEditActivityResult.launch(intent)
        }

        return currentView
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        FullWallpaperPreviewBinder.bind(
            applicationContext = appContext,
            view = currentView,
            viewModel = wallpaperPreviewViewModel,
            transition = null,
            displayUtils = displayUtils,
            lifecycleOwner = viewLifecycleOwner,
            savedInstanceState = savedInstanceState,
            isFirstBindingDeferred = CompletableDeferred(savedInstanceState == null)
        )
    }

    companion object {
        private const val CREATIVE_RESULT_REGISTRY = "creative_result_registry"
    }
}
