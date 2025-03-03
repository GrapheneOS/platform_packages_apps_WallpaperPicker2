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

package com.android.wallpaper.picker.customization.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.fragment.app.viewModels
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.module.LargeScreenMultiPanesChecker
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.picker.WallpaperPickerDelegate.VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE
import com.android.wallpaper.picker.category.ui.view.CategoriesFragment
import com.android.wallpaper.picker.common.preview.data.repository.PersistentWallpaperModelRepository
import com.android.wallpaper.picker.common.preview.ui.binder.BasePreviewBinder
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationPickerBinder2
import com.android.wallpaper.picker.customization.ui.binder.PagerTouchInterceptorBinder
import com.android.wallpaper.picker.customization.ui.binder.PreviewLabelBinder
import com.android.wallpaper.picker.customization.ui.binder.ToolbarBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.util.EmptyTransitionListener
import com.android.wallpaper.picker.customization.ui.view.WallpaperPickerEntry
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.picker.preview.ui.view.ClickableMotionLayout
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.WallpaperConnection
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint(Fragment::class)
class CustomizationPickerFragment2 : Hilt_CustomizationPickerFragment2() {

    @Inject lateinit var customizationOptionUtil: CustomizationOptionUtil
    @Inject lateinit var customizationOptionsBinder: CustomizationOptionsBinder
    @Inject lateinit var toolbarBinder: ToolbarBinder
    @Inject lateinit var colorUpdateViewModel: ColorUpdateViewModel
    @Inject lateinit var clockViewFactory: ClockViewFactory
    @Inject lateinit var workspaceCallbackBinder: WorkspaceCallbackBinder
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var wallpaperConnectionUtils: WallpaperConnectionUtils
    @Inject lateinit var persistentWallpaperModelRepository: PersistentWallpaperModelRepository
    @Inject @MainDispatcher lateinit var mainScope: CoroutineScope
    @Inject lateinit var multiPanesChecker: MultiPanesChecker

    private val customizationPickerViewModel: CustomizationPickerViewModel2 by viewModels()

    private val isOnMainScreen = {
        customizationPickerViewModel.customizationOptionsViewModel.selectedOption.value == null
    }

    private var fullyCollapsed = false
    private var navBarHeight: Int = 0

    private var onBackPressedCallback: OnBackPressedCallback? = null

    private val startForResult =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val isFromLauncher =
            activity?.intent?.let { ActivityUtils.isLaunchedFromLauncher(it) } ?: false
        if (isFromLauncher) {
            customizationPickerViewModel.selectPreviewScreen(HOME_SCREEN)
        }

        val view = inflater.inflate(R.layout.fragment_customization_picker2, container, false)

        setupToolbar(
            view.requireViewById(R.id.nav_button),
            view.requireViewById(R.id.toolbar),
            view.requireViewById(R.id.apply_button),
        )

        val pickerMotionContainer: MotionLayout = view.requireViewById(R.id.picker_motion_layout)
        ViewCompat.setOnApplyWindowInsetsListener(pickerMotionContainer) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            navBarHeight = insets.bottom
            view
                .requireViewById<FrameLayout>(R.id.customization_option_floating_sheet_container)
                .setPaddingRelative(0, 0, 0, navBarHeight)
            val statusBarHeight = insets.top
            val params =
                view.requireViewById<Toolbar>(R.id.toolbar).layoutParams as MarginLayoutParams
            params.setMargins(0, statusBarHeight, 0, 0)
            WindowInsetsCompat.CONSUMED
        }

        val customizationOptionFloatingSheetViewMap =
            customizationOptionUtil.initFloatingSheet(
                pickerMotionContainer.requireViewById(
                    R.id.customization_option_floating_sheet_container
                ),
                layoutInflater,
            )

        val previewViewModel = customizationPickerViewModel.basePreviewViewModel
        previewViewModel.setWhichPreview(WallpaperConnection.WhichPreview.PREVIEW_CURRENT)
        // TODO (b/348462236): adjust flow so this is always false when previewing current wallpaper
        previewViewModel.setIsWallpaperColorPreviewEnabled(false)

        initPreviewPager(
            pagerTouchInterceptor = view.requireViewById(R.id.pager_touch_interceptor),
            previewPager = view.requireViewById(R.id.preview_pager),
            isFirstBinding = savedInstanceState == null,
            initialScreen = if (isFromLauncher) HOME_SCREEN else LOCK_SCREEN,
        )

        val optionContainer: ConstraintLayout =
            view.requireViewById(R.id.customization_option_container)
        val wallpaperPickerEntry: WallpaperPickerEntry =
            view.requireViewById(R.id.wallpaper_picker_entry)
        view.post {
            val wallpaperPickerEntryExpandedHeight = wallpaperPickerEntry.height
            val wallpaperPickerEntryCollapsedHeight = wallpaperPickerEntry.collapsedButton.height
            // The expanded / collapsed header height should be updated when optionContainer
            // height is known.
            // For expanded, it needs to show at least half of the entry view below the wallpaper
            // entry.
            val expandedHeaderHeight =
                pickerMotionContainer.height -
                    wallpaperPickerEntryExpandedHeight -
                    resources.getDimensionPixelSize(R.dimen.customization_option_entry_height) / 2
            pickerMotionContainer
                .getConstraintSet(R.id.expanded_header_primary)
                ?.constrainHeight(R.id.preview_header, expandedHeaderHeight)
            // For collapsed, it needs to show the all option entries, with the collapsed wallpaper
            // entry, which shows as a single button.
            val collapsedHeaderHeight =
                pickerMotionContainer.height -
                    (optionContainer.height -
                        (wallpaperPickerEntryExpandedHeight -
                            wallpaperPickerEntryCollapsedHeight)) -
                    navBarHeight
            pickerMotionContainer
                .getConstraintSet(R.id.collapsed_header_primary)
                ?.constrainHeight(R.id.preview_header, collapsedHeaderHeight)

            // Transition listener handle 2 things
            // 1. Expand and collapse the wallpaper entry
            // 2. Reset the transition and preview when transition back to the primary screen
            pickerMotionContainer.setTransitionListener(
                object : EmptyTransitionListener {

                    override fun onTransitionChange(
                        motionLayout: MotionLayout?,
                        startId: Int,
                        endId: Int,
                        progress: Float,
                    ) {
                        if (
                            startId == R.id.expanded_header_primary &&
                                endId == R.id.collapsed_header_primary
                        ) {
                            wallpaperPickerEntry.setProgress(progress)
                        }
                    }

                    override fun onTransitionCompleted(
                        motionLayout: MotionLayout?,
                        currentId: Int,
                    ) {
                        if (currentId == R.id.expanded_header_primary) {
                            wallpaperPickerEntry.setProgress(0f)
                        } else if (currentId == R.id.collapsed_header_primary) {
                            wallpaperPickerEntry.setProgress(1f)
                        }

                        if (
                            currentId == R.id.expanded_header_primary ||
                                currentId == R.id.collapsed_header_primary
                        ) {
                            // This is when we complete the transition back to the primary screen
                            pickerMotionContainer.setTransition(R.id.transition_primary)
                            // Reset the preview only after the transition is completed, because the
                            // reset will trigger the animation of the UI components in the floating
                            // sheet content, which can possibly be interrupted by the floating
                            // sheet translating down.
                            customizationPickerViewModel.customizationOptionsViewModel
                                .resetPreview()
                        }
                    }
                }
            )
        }

        CustomizationPickerBinder2.bind(
            view = pickerMotionContainer,
            lockScreenCustomizationOptionEntries =
                initCustomizationOptionEntries(view, LOCK_SCREEN),
            homeScreenCustomizationOptionEntries =
                initCustomizationOptionEntries(view, HOME_SCREEN),
            customizationOptionFloatingSheetViewMap = customizationOptionFloatingSheetViewMap,
            viewModel = customizationPickerViewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            customizationOptionsBinder = customizationOptionsBinder,
            lifecycleOwner = viewLifecycleOwner,
            navigateToPrimary = {
                if (pickerMotionContainer.currentState == R.id.secondary) {
                    pickerMotionContainer.transitionToState(
                        if (fullyCollapsed) R.id.collapsed_header_primary
                        else R.id.expanded_header_primary
                    )
                }
            },
            navigateToSecondary = { screen ->
                if (pickerMotionContainer.currentState != R.id.secondary) {
                    customizationOptionFloatingSheetViewMap[screen]?.let { floatingSheetView ->
                        setCustomizationOptionFloatingSheet(
                            floatingSheetViewContent = floatingSheetView,
                            floatingSheetContainer =
                                view.requireViewById(
                                    R.id.customization_option_floating_sheet_container
                                ),
                            motionContainer = pickerMotionContainer,
                            onComplete = {
                                // Transition to secondary screen after content is set
                                fullyCollapsed = pickerMotionContainer.progress == 1.0f
                                pickerMotionContainer.transitionToState(R.id.secondary)
                            },
                        )
                    }
                }
            },
            navigateToWallpaperCategoriesScreen = { _ ->
                if (isAdded) {
                    parentFragmentManager.commit {
                        replace<CategoriesFragment>(R.id.fragment_container)
                        addToBackStack(null)
                    }
                }
            },
            navigateToMoreLockScreenSettingsActivity = {
                activity?.startActivity(Intent(Settings.ACTION_LOCKSCREEN_SETTINGS))
            },
            navigateToColorContrastSettingsActivity = {
                activity?.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_COLOR_CONTRAST_SETTINGS)
                )
            },
            navigateToLockScreenNotificationsSettingsActivity = {
                activity?.startActivity(Intent(Settings.ACTION_LOCKSCREEN_NOTIFICATIONS_SETTINGS))
            },
            navigateToPreviewScreen = { wallpaperModel ->
                // navigate to standard preview screen
                startWallpaperPreviewActivity(wallpaperModel, false)
            },
        )

        customizationOptionsBinder.bindDiscardChangesDialog(
            customizationOptionsViewModel =
                customizationPickerViewModel.customizationOptionsViewModel,
            lifecycleOwner = viewLifecycleOwner,
            activity = requireActivity(),
        )

        activity?.onBackPressedDispatcher?.let {
            it.addCallback {
                    isEnabled =
                        customizationPickerViewModel.customizationOptionsViewModel
                            .handleBackPressed()
                    if (!isEnabled) it.onBackPressed()
                }
                .also { callback -> onBackPressedCallback = callback }
        }

        return view
    }

    override fun onDestroyView() {
        context?.applicationContext?.let { appContext ->
            // TODO(b/333879532): Only disconnect when leaving the Activity without introducing
            // black
            //  preview. If onDestroy is caused by an orientation change, we should keep the
            // connection
            //  to avoid initiating the engines again.
            // TODO(b/328302105): MainScope ensures the job gets done non-blocking even if the
            //   activity has been destroyed already. Consider making this part of
            //   WallpaperConnectionUtils.
            mainScope.launch { wallpaperConnectionUtils.disconnectAll(appContext) }
        }

        super.onDestroyView()
        onBackPressedCallback?.remove()
    }

    private fun setupToolbar(navButton: FrameLayout, toolbar: Toolbar, applyButton: Button) {
        toolbar.title = getString(R.string.app_name)
        toolbar.setBackgroundColor(Color.TRANSPARENT)
        toolbarBinder.bind(
            navButton,
            toolbar,
            applyButton,
            customizationPickerViewModel.customizationOptionsViewModel,
            colorUpdateViewModel,
            viewLifecycleOwner,
        ) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun initPreviewPager(
        pagerTouchInterceptor: View,
        previewPager: ClickableMotionLayout,
        isFirstBinding: Boolean,
        initialScreen: Screen,
    ) {
        PagerTouchInterceptorBinder.bind(
            pagerTouchInterceptor,
            customizationPickerViewModel,
            viewLifecycleOwner,
        )
        previewPager.addClickableViewId(R.id.preview_card)
        when (initialScreen) {
            LOCK_SCREEN -> {
                previewPager.setTransitionDuration(0)
                previewPager.transitionToState(R.id.lock_preview_selected)
            }

            HOME_SCREEN -> {
                previewPager.setTransitionDuration(0)
                previewPager.transitionToState(R.id.home_preview_selected)
            }
        }

        val lockPreviewLabel: TextView = previewPager.requireViewById(R.id.lock_preview_label)
        PreviewLabelBinder.bind(
            previewLabel = lockPreviewLabel,
            screen = LOCK_SCREEN,
            viewModel = customizationPickerViewModel,
            lifecycleOwner = viewLifecycleOwner,
        )
        ColorUpdateBinder.bind(
            setColor = { color -> lockPreviewLabel.setTextColor(color) },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = viewLifecycleOwner,
        )
        val homePreviewLabel: TextView = previewPager.requireViewById(R.id.home_preview_label)
        PreviewLabelBinder.bind(
            previewLabel = homePreviewLabel,
            screen = HOME_SCREEN,
            viewModel = customizationPickerViewModel,
            lifecycleOwner = viewLifecycleOwner,
        )
        ColorUpdateBinder.bind(
            setColor = { color -> homePreviewLabel.setTextColor(color) },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = viewLifecycleOwner,
        )

        bindPreview(
            screen = LOCK_SCREEN,
            previewPager = previewPager,
            preview = previewPager.requireViewById(R.id.lock_preview),
            isFirstBinding = isFirstBinding,
        )

        bindPreview(
            screen = HOME_SCREEN,
            previewPager = previewPager,
            preview = previewPager.requireViewById(R.id.home_preview),
            isFirstBinding = isFirstBinding,
        )
    }

    private fun bindPreview(
        screen: Screen,
        previewPager: ClickableMotionLayout,
        preview: View,
        isFirstBinding: Boolean,
    ) {
        val appContext = context?.applicationContext ?: return
        val activity = activity ?: return
        val previewViewModel = customizationPickerViewModel.basePreviewViewModel

        val previewCard: View = preview.requireViewById(R.id.preview_card)

        if (screen == LOCK_SCREEN) {
            val clockHostView =
                (previewCard.parent as? ViewGroup)?.let {
                    customizationOptionUtil.createClockPreviewAndAddToParent(it, layoutInflater)
                }
            if (clockHostView != null) {
                customizationOptionsBinder.bindClockPreview(
                    context = requireContext(),
                    clockHostView = clockHostView,
                    viewModel = customizationPickerViewModel,
                    colorUpdateViewModel = colorUpdateViewModel,
                    lifecycleOwner = viewLifecycleOwner,
                    clockViewFactory = clockViewFactory,
                )
            }
        }

        BasePreviewBinder.bind(
            applicationContext = appContext,
            view = previewCard,
            viewModel = customizationPickerViewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            workspaceCallbackBinder = workspaceCallbackBinder,
            screen = screen,
            deviceDisplayType = displayUtils.getCurrentDisplayType(activity),
            displaySize =
                if (displayUtils.isOnWallpaperDisplay(activity))
                    previewViewModel.wallpaperDisplaySize.value
                else previewViewModel.smallerDisplaySize,
            mainScope = mainScope,
            lifecycleOwner = viewLifecycleOwner,
            wallpaperConnectionUtils = wallpaperConnectionUtils,
            isFirstBindingDeferred = CompletableDeferred(isFirstBinding),
            onLaunchPreview = { wallpaperModel ->
                persistentWallpaperModelRepository.setWallpaperModel(wallpaperModel)
                val multiPanesChecker = LargeScreenMultiPanesChecker()
                val isMultiPanel = multiPanesChecker.isMultiPanesEnabled(appContext)
                startForResult.launch(
                    WallpaperPreviewActivity.newIntent(
                        context = appContext,
                        isAssetIdPresent = false,
                        isViewAsHome = screen == HOME_SCREEN,
                        isNewTask = isMultiPanel,
                    )
                )
            },
            onTransitionToScreen = {
                when (it) {
                    LOCK_SCREEN ->
                        previewPager.transitionToState(
                            R.id.lock_preview_selected,
                            ANIMATION_DURATION,
                        )
                    HOME_SCREEN ->
                        previewPager.transitionToState(
                            R.id.home_preview_selected,
                            ANIMATION_DURATION,
                        )
                }
            },
            clockViewFactory = clockViewFactory,
        )
    }

    private fun initCustomizationOptionEntries(
        view: View,
        screen: Screen,
    ): List<Pair<CustomizationOption, View>> {
        val optionEntriesContainer =
            view.requireViewById<LinearLayout>(
                when (screen) {
                    LOCK_SCREEN -> R.id.lock_customization_option_container
                    HOME_SCREEN -> R.id.home_customization_option_container
                }
            )
        val optionEntries =
            customizationOptionUtil.getOptionEntries(screen, optionEntriesContainer, layoutInflater)
        optionEntries.onEachIndexed { index, (_, view) ->
            val isFirst = index == 0
            val isLast = index == optionEntries.size - 1
            view.setBackgroundResource(
                if (isFirst) R.drawable.customization_option_entry_top_background
                else if (isLast) R.drawable.customization_option_entry_bottom_background
                else R.drawable.customization_option_entry_background
            )
            optionEntriesContainer.addView(view)
        }
        return optionEntries
    }

    /**
     * Set customization option floating sheet content to the floating sheet container and get the
     * new container's height for repositioning the preview's guideline.
     */
    private fun setCustomizationOptionFloatingSheet(
        floatingSheetViewContent: View,
        floatingSheetContainer: FrameLayout,
        motionContainer: MotionLayout,
        onComplete: () -> Unit,
    ) {
        floatingSheetContainer.removeAllViews()
        floatingSheetContainer.addView(floatingSheetViewContent)

        floatingSheetViewContent.doOnPreDraw {
            val height = floatingSheetViewContent.height + navBarHeight
            floatingSheetContainer.translationY = 0.0f
            floatingSheetContainer.alpha = 0.0f
            // Update the motion container
            motionContainer.getConstraintSet(R.id.expanded_header_primary)?.apply {
                setTranslationY(
                    R.id.customization_option_floating_sheet_container,
                    height.toFloat(),
                )
                setAlpha(R.id.customization_option_floating_sheet_container, 0.0f)
                connect(
                    R.id.customization_option_floating_sheet_container,
                    ConstraintSet.BOTTOM,
                    R.id.picker_motion_layout,
                    ConstraintSet.BOTTOM,
                )
                constrainHeight(
                    R.id.customization_option_floating_sheet_container,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                )
            }
            motionContainer.getConstraintSet(R.id.collapsed_header_primary)?.apply {
                setTranslationY(
                    R.id.customization_option_floating_sheet_container,
                    height.toFloat(),
                )
                setAlpha(R.id.customization_option_floating_sheet_container, 0.0f)
                connect(
                    R.id.customization_option_floating_sheet_container,
                    ConstraintSet.BOTTOM,
                    R.id.picker_motion_layout,
                    ConstraintSet.BOTTOM,
                )
                constrainHeight(
                    R.id.customization_option_floating_sheet_container,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                )
            }
            motionContainer.getConstraintSet(R.id.secondary)?.apply {
                setTranslationY(R.id.customization_option_floating_sheet_container, 0.0f)
                setAlpha(R.id.customization_option_floating_sheet_container, 1.0f)
                constrainHeight(
                    R.id.customization_option_floating_sheet_container,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                )
            }
            onComplete()
        }
    }

    private fun startWallpaperPreviewActivity(
        wallpaperModel: WallpaperModel,
        isCreativeCategories: Boolean,
    ) {
        val appContext = requireContext()
        val activity = requireActivity()
        persistentWallpaperModelRepository.setWallpaperModel(wallpaperModel)
        val isMultiPanel = multiPanesChecker.isMultiPanesEnabled(appContext)
        val previewIntent =
            WallpaperPreviewActivity.newIntent(
                context = appContext,
                isAssetIdPresent = true,
                isViewAsHome = true,
                isNewTask = isMultiPanel,
                shouldCategoryRefresh = isCreativeCategories,
            )
        ActivityUtils.startActivityForResultSafely(
            activity,
            previewIntent,
            VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE,
        )
    }

    companion object {
        private const val ANIMATION_DURATION = 200
    }
}
