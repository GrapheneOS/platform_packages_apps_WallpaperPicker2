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
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.Transition
import com.android.wallpaper.R
import com.android.wallpaper.R.id.preview_tabs_container
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.preview.ui.binder.DualPreviewSelectorBinder
import com.android.wallpaper.picker.preview.ui.binder.PreviewActionsBinder
import com.android.wallpaper.picker.preview.ui.binder.PreviewSelectorBinder
import com.android.wallpaper.picker.preview.ui.binder.SetWallpaperButtonBinder
import com.android.wallpaper.picker.preview.ui.binder.SetWallpaperProgressDialogBinder
import com.android.wallpaper.picker.preview.ui.util.AnimationUtil
import com.android.wallpaper.picker.preview.ui.util.ImageEffectDialogUtil
import com.android.wallpaper.picker.preview.ui.view.DualPreviewViewPager
import com.android.wallpaper.picker.preview.ui.view.PreviewActionFloatingSheet
import com.android.wallpaper.picker.preview.ui.view.PreviewActionGroup
import com.android.wallpaper.picker.preview.ui.view.PreviewTabs
import com.android.wallpaper.picker.preview.ui.viewmodel.Action
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * This fragment displays the preview of the selected wallpaper on all available workspaces and
 * device displays.
 */
@AndroidEntryPoint(AppbarFragment::class)
class SmallPreviewFragment : Hilt_SmallPreviewFragment() {

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var logger: UserEventLogger
    @Inject lateinit var imageEffectDialogUtil: ImageEffectDialogUtil
    @Inject lateinit var wallpaperConnectionUtils: WallpaperConnectionUtils

    private lateinit var currentView: View
    private lateinit var shareActivityResult: ActivityResultLauncher<Intent>

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()
    private val isFirstBindingDeferred = CompletableDeferred<Boolean>()

    /**
     * True if the view of this fragment is destroyed from the current or previous lifecycle.
     *
     * Null if it's the first life cycle, and false if the view has not been destroyed.
     *
     * Read-only during the first half of the lifecycle (when starting a fragment).
     */
    private var isViewDestroyed: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = AnimationUtil.getFastFadeOutTransition()
        reenterTransition = AnimationUtil.getFastFadeInTransition()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val isFoldable = displayUtils.hasMultiInternalDisplays()
        postponeEnterTransition()
        currentView =
            inflater.inflate(
                if (BaseFlags.get().isNewPickerUi()) {
                    if (isFoldable) R.layout.fragment_small_preview_foldable2
                    else R.layout.fragment_small_preview_handheld2
                } else {
                    if (isFoldable) R.layout.fragment_small_preview_foldable
                    else R.layout.fragment_small_preview_handheld
                },
                container,
                /* attachToRoot= */ false,
            )
        val motionLayout =
            if (BaseFlags.get().isNewPickerUi())
                currentView.findViewById<MotionLayout>(R.id.small_preview_motion_layout)
            else null

        setUpToolbar(currentView, /* upArrow= */ true, /* transparentToolbar= */ true)
        bindScreenPreview(currentView, motionLayout, isFirstBindingDeferred)
        bindPreviewActions(currentView, motionLayout)

        SetWallpaperButtonBinder.bind(
            button = currentView.requireViewById(R.id.button_set_wallpaper),
            viewModel = wallpaperPreviewViewModel,
            lifecycleOwner = viewLifecycleOwner,
        ) {
            findNavController().navigate(R.id.setWallpaperDialog)
        }

        SetWallpaperProgressDialogBinder.bind(
            viewModel = wallpaperPreviewViewModel,
            lifecycleOwner = viewLifecycleOwner,
        ) { visible ->
            activity?.let {
                createSetWallpaperProgressDialog(it).apply { if (visible) show() else hide() }
            }
        }

        currentView.doOnPreDraw {
            // FullPreviewConfigViewModel not being null indicates that we are navigated to small
            // preview from the full preview, and therefore should play the shared element re-enter
            // animation. Reset it after views are finished binding.
            wallpaperPreviewViewModel.resetFullPreviewConfigViewModel()
            startPostponedEnterTransition()
        }

        shareActivityResult =
            registerForActivityResult(
                object : ActivityResultContract<Intent, Int>() {
                    override fun createIntent(context: Context, input: Intent): Intent {
                        return input
                    }

                    override fun parseResult(resultCode: Int, intent: Intent?): Int {
                        return resultCode
                    }
                }
            ) {
                currentView
                    .findViewById<PreviewActionGroup>(R.id.action_button_group)
                    ?.setIsChecked(Action.SHARE, false)
            }

        return currentView
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        isFirstBindingDeferred.complete(savedInstanceState == null)
    }

    override fun onStart() {
        super.onStart()
        // Reinitialize the preview tab motion. If navigating up back to this fragment happened
        // before the transition finished, the lifecycle begins at onStart without recreating the
        // preview tabs,
        isViewDestroyed?.let {
            if (!it) {
                currentView
                    .findViewById<PreviewTabs>(preview_tabs_container)
                    ?.resetTransition(wallpaperPreviewViewModel.getSmallPreviewTabIndex())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // onStop won't destroy view
        isViewDestroyed = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isViewDestroyed = true
    }

    override fun getDefaultTitle(): CharSequence {
        return getString(R.string.preview)
    }

    override fun getToolbarTextColor(): Int {
        return ContextCompat.getColor(requireContext(), R.color.system_on_surface)
    }

    private fun createSetWallpaperProgressDialog(activity: Activity): AlertDialog {
        val dialogView =
            activity.layoutInflater.inflate(R.layout.set_wallpaper_progress_dialog_view, null)
        return AlertDialog.Builder(activity).setView(dialogView).create()
    }

    private fun bindScreenPreview(
        view: View,
        motionLayout: MotionLayout?,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
    ) {
        val currentNavDestId = checkNotNull(findNavController().currentDestination?.id)
        val tabs = view.findViewById<PreviewTabs>(preview_tabs_container)
        if (displayUtils.hasMultiInternalDisplays()) {
            val dualPreviewView: DualPreviewViewPager = view.requireViewById(R.id.pager_previews)

            DualPreviewSelectorBinder.bind(
                tabs,
                dualPreviewView,
                motionLayout,
                wallpaperPreviewViewModel,
                appContext,
                viewLifecycleOwner,
                currentNavDestId,
                (reenterTransition as Transition?),
                wallpaperPreviewViewModel.fullPreviewConfigViewModel.value,
                wallpaperConnectionUtils,
                isFirstBindingDeferred,
            ) { sharedElement ->
                val extras =
                    FragmentNavigatorExtras(sharedElement to FULL_PREVIEW_SHARED_ELEMENT_ID)
                // Set to false on small-to-full preview transition to remove surfaceView jank.
                (view as ViewGroup).isTransitionGroup = false
                findNavController()
                    .navigate(
                        resId = R.id.action_smallPreviewFragment_to_fullPreviewFragment,
                        args = null,
                        navOptions = null,
                        navigatorExtras = extras,
                    )
            }
        } else {
            PreviewSelectorBinder.bind(
                tabs,
                view.requireViewById(R.id.pager_previews),
                motionLayout,
                displayUtils.getRealSize(displayUtils.getWallpaperDisplay()),
                wallpaperPreviewViewModel,
                appContext,
                viewLifecycleOwner,
                currentNavDestId,
                (reenterTransition as Transition?),
                wallpaperPreviewViewModel.fullPreviewConfigViewModel.value,
                wallpaperConnectionUtils,
                isFirstBindingDeferred,
            ) { sharedElement ->
                val extras =
                    FragmentNavigatorExtras(sharedElement to FULL_PREVIEW_SHARED_ELEMENT_ID)
                // Set to false on small-to-full preview transition to remove surfaceView jank.
                (view as ViewGroup).isTransitionGroup = false
                findNavController()
                    .navigate(
                        resId = R.id.action_smallPreviewFragment_to_fullPreviewFragment,
                        args = null,
                        navOptions = null,
                        navigatorExtras = extras,
                    )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Always reset isTransitionGroup value on start for the edge case that the
                // navigation is cancelled and the fragment resumes.
                (view as ViewGroup).isTransitionGroup = true
            }
        }
    }

    private fun bindPreviewActions(view: View, motionLayout: MotionLayout?) {
        val actionButtonGroup = view.findViewById<PreviewActionGroup>(R.id.action_button_group)
        val floatingSheet = view.findViewById<PreviewActionFloatingSheet>(R.id.floating_sheet)
        if (actionButtonGroup == null || floatingSheet == null) {
            return
        }

        val motionLayout =
            if (BaseFlags.get().isNewPickerUi())
                view.findViewById<MotionLayout>(R.id.small_preview_motion_layout)
            else null

        PreviewActionsBinder.bind(
            actionGroup = actionButtonGroup,
            floatingSheet = floatingSheet,
            motionLayout = motionLayout,
            previewViewModel = wallpaperPreviewViewModel,
            actionsViewModel = wallpaperPreviewViewModel.previewActionsViewModel,
            deviceDisplayType = displayUtils.getCurrentDisplayType(requireActivity()),
            activity = requireActivity(),
            lifecycleOwner = viewLifecycleOwner,
            logger = logger,
            imageEffectDialogUtil = imageEffectDialogUtil,
            onNavigateToEditScreen = { navigateToEditScreen(it) },
            onStartShareActivity = { shareActivityResult.launch(it) },
        )
    }

    private fun navigateToEditScreen(intent: Intent) {
        findNavController()
            .navigate(
                resId = R.id.action_smallPreviewFragment_to_creativeEditPreviewFragment,
                args = Bundle().apply { putParcelable(ARG_EDIT_INTENT, intent) },
                navOptions = null,
                navigatorExtras = null,
            )
    }

    companion object {
        const val SMALL_PREVIEW_HOME_SHARED_ELEMENT_ID = "small_preview_home"
        const val SMALL_PREVIEW_LOCK_SHARED_ELEMENT_ID = "small_preview_lock"
        const val SMALL_PREVIEW_HOME_FOLDED_SHARED_ELEMENT_ID = "small_preview_home_folded"
        const val SMALL_PREVIEW_HOME_UNFOLDED_SHARED_ELEMENT_ID = "small_preview_home_unfolded"
        const val SMALL_PREVIEW_LOCK_FOLDED_SHARED_ELEMENT_ID = "small_preview_lock_folded"
        const val SMALL_PREVIEW_LOCK_UNFOLDED_SHARED_ELEMENT_ID = "small_preview_lock_unfolded"
        const val FULL_PREVIEW_SHARED_ELEMENT_ID = "full_preview"
        const val ARG_EDIT_INTENT = "arg_edit_intent"
    }
}
