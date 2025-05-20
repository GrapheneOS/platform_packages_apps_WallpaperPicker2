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
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.LinearLayout
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
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.Transition
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.module.LargeScreenMultiPanesChecker
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.WallpaperPickerDelegate.VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE
import com.android.wallpaper.picker.category.ui.view.CategoriesFragment
import com.android.wallpaper.picker.category.ui.view.PhotoPickerFragment
import com.android.wallpaper.picker.category.ui.view.providers.IndividualPickerFactory
import com.android.wallpaper.picker.common.preview.data.repository.PersistentWallpaperModelRepository
import com.android.wallpaper.picker.common.preview.ui.binder.BasePreviewBinder
import com.android.wallpaper.picker.common.preview.ui.binder.PreviewAlphaAnimationBinder
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity2.ActivityEnterAnimationCallback
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationPickerBinder2
import com.android.wallpaper.picker.customization.ui.binder.DarkModeUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.PackThemeSuggestedEntryBinder
import com.android.wallpaper.picker.customization.ui.binder.PreviewPagerBinder
import com.android.wallpaper.picker.customization.ui.binder.ToolbarBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.util.EmptyTransitionListener
import com.android.wallpaper.picker.customization.ui.view.ApplyButton
import com.android.wallpaper.picker.customization.ui.view.PackThemeSuggestedChip
import com.android.wallpaper.picker.customization.ui.view.PreviewPagerViews
import com.android.wallpaper.picker.customization.ui.view.WallpaperPickerEntry
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsData
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.picker.preview.ui.view.ClickableMotionLayout
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.CuratedPhotosTimeUtil
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.WallpaperConnection
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint(AppbarFragment::class)
class CustomizationPickerFragment2 :
    Hilt_CustomizationPickerFragment2(), ActivityEnterAnimationCallback {

    @Inject lateinit var customizationOptionUtil: CustomizationOptionUtil
    @Inject lateinit var customizationOptionsBinder: CustomizationOptionsBinder
    @Inject lateinit var packThemeSuggestedEntryBinder: PackThemeSuggestedEntryBinder
    @Inject lateinit var toolbarBinder: ToolbarBinder
    @Inject lateinit var colorUpdateViewModel: ColorUpdateViewModel
    @Inject lateinit var clockViewFactory: ClockViewFactory
    @Inject lateinit var workspaceCallbackBinder: WorkspaceCallbackBinder
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var wallpaperConnectionUtils: WallpaperConnectionUtils
    @Inject lateinit var persistentWallpaperModelRepository: PersistentWallpaperModelRepository
    @Inject @MainDispatcher lateinit var mainScope: CoroutineScope
    @Inject lateinit var multiPanesChecker: MultiPanesChecker
    @Inject lateinit var individualPickerFactory: IndividualPickerFactory
    @Inject lateinit var userEventLogger: UserEventLogger
    @Inject lateinit var curatedPhotosTimeUtil: CuratedPhotosTimeUtil

    private val customizationPickerViewModel: CustomizationPickerViewModel2 by viewModels()

    private val isOnMainScreen = {
        customizationPickerViewModel.customizationOptionsViewModel.selectedOption.value == null
    }

    private var fullyCollapsed = false

    private var onBackPressedCallback: OnBackPressedCallback? = null

    private val startForResult =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    // This boolean is to determine that when onCreateView, if it is a fragment reenter after the
    // last fragment exit.
    private var isReenterAfterExit = false

    private var isInitialCreation = true // Flag to track initial creation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            // Fragment is being restored, not initial creation
            isInitialCreation = false
        } else {
            // If the fragment is initially created, get isFromLauncher from intent and set preview
            // screen to the view model accordingly; otherwise, respect the value in the view model.
            val isFromLauncher =
                activity?.intent?.let { ActivityUtils.isLaunchedFromLauncher(it) } ?: false
            if (isFromLauncher) {
                customizationPickerViewModel.selectPreviewScreen(HOME_SCREEN)
            }
        }

        prepareFragmentExitTransitionAnimation()
        prepareFragmentReenterTransitionAnimation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_customization_picker2, container, false)

        val toolbar: Toolbar = view.requireViewById(R.id.toolbar)
        setupToolbar(
            view.requireViewById(R.id.nav_button),
            toolbar,
            view.requireViewById(R.id.apply_button),
        )

        val showSuggestedChip =
            Settings.Secure.getInt(
                view.context.contentResolver,
                Settings.Secure.SUGGESTED_THEME_FEATURE_ENABLED,
                /* def= */ 0,
            ) == 1
        val packThemeSuggestedChip: PackThemeSuggestedChip? =
            if (BaseFlags.get().isPackThemeEnabled() && showSuggestedChip) {
                val stubView: ViewStub = view.requireViewById(R.id.stub_pack_theme_suggested_chip)
                stubView.inflate() as PackThemeSuggestedChip
            } else null
        packThemeSuggestedChip?.visibility = View.INVISIBLE

        val pickerMotionContainer: MotionLayout = view.requireViewById(R.id.picker_motion_layout)

        val initSelectedOption: CustomizationOption? =
            customizationPickerViewModel.screen.value.second
        val isInitSecondaryScreen = initSelectedOption != null
        if (isInitSecondaryScreen) {
            // If initially on secondary screen, hide the whole motion container until children's
            // dimensions are calculated and ready to render.
            pickerMotionContainer.isInvisible = true
        }

        var isMotionContainerInitialized = false
        val optionContainer: ConstraintLayout =
            view.requireViewById(R.id.customization_option_container)
        val customizationFloatingSheetContainer: FrameLayout =
            view.requireViewById(R.id.customization_option_floating_sheet_container)
        // Listen to the window's bottom nav bar height and the top status bar height and update the
        // layout padding accordingly.
        ViewCompat.setOnApplyWindowInsetsListener(pickerMotionContainer) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            applySystemBarInsets(
                toolbar = toolbar,
                optionContainer = optionContainer,
                customizationFloatingSheetContainer = customizationFloatingSheetContainer,
                statusBarHeight = insets.top,
                navBarHeight = insets.bottom,
            )

            if (isMotionContainerInitialized) {
                // Reconfigure motion container constraints if already initialized, to adjust
                // for new insets (doing it only after it's initialized to avoid jumping if
                // insets first arrive before the first initialization)
                configurePickerMotionConstraints(
                    pickerMotionContainer = pickerMotionContainer,
                    wallpaperPickerEntry = view.requireViewById(R.id.wallpaper_picker_entry),
                    previewLabelHeight = view.requireViewById<View>(R.id.label_placeholder).height,
                    optionContainerHeight = optionContainer.height,
                    packThemeSuggestedChip = packThemeSuggestedChip,
                    bottomInset = insets.bottom,
                )
            }
            WindowInsetsCompat.CONSUMED
        }
        // Inflate the views of customization options only when options data is ready.
        viewLifecycleOwner.lifecycleScope.launch {
            // Wait and collect only the first emission. Note that customizationOptionsData is
            // expected to stay the same across the Fragment lifecycle.
            val customizationOptionsData =
                customizationPickerViewModel.customizationOptionsViewModel.customizationOptionsData
                    .first()

            val lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>> =
                initCustomizationOptionEntries(
                    customizationOptionsData = customizationOptionsData,
                    view = view,
                    screen = LOCK_SCREEN,
                )
            val homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>> =
                initCustomizationOptionEntries(
                    customizationOptionsData = customizationOptionsData,
                    view = view,
                    screen = HOME_SCREEN,
                )
            val customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View> =
                customizationOptionUtil.initFloatingSheet(
                    customizationOptionsData,
                    customizationFloatingSheetContainer,
                    layoutInflater,
                )

            view.post {
                // Post to wait for the essential view dimensions to be obtained, to
                // further calculate the motion scene dimensions.
                configurePickerMotionConstraints(
                    pickerMotionContainer = pickerMotionContainer,
                    wallpaperPickerEntry = view.requireViewById(R.id.wallpaper_picker_entry),
                    previewLabelHeight = view.requireViewById<View>(R.id.label_placeholder).height,
                    optionContainerHeight = optionContainer.height,
                    packThemeSuggestedChip = packThemeSuggestedChip,
                    bottomInset = optionContainer.paddingBottom,
                )

                if (isInitSecondaryScreen && initSelectedOption != null) {
                    // If initially on secondary screen, initiate the secondary screen and bind the
                    // picker content after.
                    initSecondaryScreen(
                        customizationOptionFloatingSheetViewMap,
                        initSelectedOption,
                        customizationFloatingSheetContainer,
                        pickerMotionContainer,
                        onComplete = {
                            pickerMotionContainer.isInvisible = false
                            bindCustomizationPicker(
                                customizationOptionsData,
                                pickerMotionContainer,
                                lockScreenCustomizationOptionEntries,
                                homeScreenCustomizationOptionEntries,
                                customizationOptionFloatingSheetViewMap,
                                customizationFloatingSheetContainer,
                                packThemeSuggestedChip,
                            )
                        },
                    )
                } else {
                    bindCustomizationPicker(
                        customizationOptionsData,
                        pickerMotionContainer,
                        lockScreenCustomizationOptionEntries,
                        homeScreenCustomizationOptionEntries,
                        customizationOptionFloatingSheetViewMap,
                        customizationFloatingSheetContainer,
                        packThemeSuggestedChip,
                    )
                }
                isMotionContainerInitialized = true
            }
        }

        val previewViewModel = customizationPickerViewModel.basePreviewViewModel
        previewViewModel.setWhichPreview(WallpaperConnection.WhichPreview.PREVIEW_CURRENT)
        // TODO (b/348462236): adjust flow so this is always false when previewing current wallpaper
        previewViewModel.setIsWallpaperColorPreviewEnabled(false)
        activity?.let {
            val size = displayUtils.getActiveDisplaySize(it)
            previewViewModel.updateDisplayConfiguration(size)
        }

        val previewPager: ClickableMotionLayout = view.requireViewById(R.id.preview_pager)
        val previewPagerViews: PreviewPagerViews =
            initPreviewPager(rootView = view, previewPager = previewPager)
        bindPreviewPager(
            rootView = view,
            previewPagerViews = previewPagerViews,
            isFirstBinding = savedInstanceState == null,
        )

        if (isInitialCreation || isReenterAfterExit) {
            // 1. If the fragment is created the first time, hide the preview pager. This is to
            // prevent preview surface views from triggering surfaceCreated too early and binding
            // the wallpaper and workspace surface. This can potentially block the initiation of the
            // app start, e.g. Activity's enter animation. We need to hide the preview pager and
            // delay PreviewAlphaAnimationBinder.bind() until the callback
            // onEnterAnimationCompleteAfterActivityCreated().
            //
            // 2. If isReenterAfterExit true, it means that it is a fragment reenter after a
            // fragment exit. To avoid unnecessary flashes, we need to hide the preview pager and
            // delay PreviewAlphaAnimationBinder.bind() until the reenter onTransitionEnd() is
            // called.
            setPreviewPagerVisible(previewPager = previewPager, isVisible = false)
            isReenterAfterExit = false
        } else {
            // In general cases, we bind the animation when onViewCreated()
            PreviewAlphaAnimationBinder.bind(
                previewPager = previewPager,
                viewModel = customizationPickerViewModel,
                lifecycleOwner = viewLifecycleOwner,
            )
        }

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

        (view as ViewGroup).isTransitionGroup = true
        return view
    }

    private fun applySystemBarInsets(
        toolbar: Toolbar,
        optionContainer: ConstraintLayout,
        customizationFloatingSheetContainer: FrameLayout,
        statusBarHeight: Int,
        navBarHeight: Int,
    ) {
        (toolbar.layoutParams as MarginLayoutParams).setMargins(0, statusBarHeight, 0, 0)

        val horizontalPadding =
            resources.getDimensionPixelSize(
                R.dimen.customization_option_container_horizontal_padding
            )
        optionContainer.setPaddingRelative(horizontalPadding, 0, horizontalPadding, navBarHeight)

        customizationFloatingSheetContainer.setPaddingRelative(0, 0, 0, navBarHeight)
    }

    private fun configurePickerMotionConstraints(
        pickerMotionContainer: MotionLayout,
        wallpaperPickerEntry: WallpaperPickerEntry,
        previewLabelHeight: Int,
        optionContainerHeight: Int,
        packThemeSuggestedChip: PackThemeSuggestedChip?,
        bottomInset: Int,
    ) {
        val isLargeScreenSingleDisplayPortrait = displayUtils.isLargeScreenSingleDisplayPortrait()
        val wallpaperPickerEntryExpandedHeight = wallpaperPickerEntry.height
        // Do not collapse the wallpaper entry when isLargeScreenSingleDisplayPortrait
        val wallpaperPickerEntryCollapsedHeight =
            if (isLargeScreenSingleDisplayPortrait) wallpaperPickerEntryExpandedHeight
            else wallpaperPickerEntry.collapsedButton.height
        val minCollapsedPreviewHeight =
            resources.getDimensionPixelSize(
                R.dimen.customization_picker_min_preview_collapsed_height
            )
        wallpaperPickerEntry.configureForAnimation()

        val minCollapsedPagerHeight = minCollapsedPreviewHeight + previewLabelHeight
        val minExpandedPreviewHeight =
            resources.getDimensionPixelSize(
                R.dimen.customization_picker_min_preview_expanded_height
            )
        val maxExpandedPreviewHeight =
            resources.getDimensionPixelSize(
                R.dimen.customization_picker_max_preview_expanded_height
            )
        val minExpandedPagerHeight = minExpandedPreviewHeight + previewLabelHeight
        val maxExpandedPagerHeight = maxExpandedPreviewHeight + previewLabelHeight

        // For collapsed, it needs to show the all option entries, with the collapsed wallpaper
        // entry, which shows as a single button.
        val collapsedHeaderHeight =
            (pickerMotionContainer.height -
                    (optionContainerHeight -
                        (packThemeSuggestedChip?.height ?: 0) -
                        (wallpaperPickerEntryExpandedHeight - wallpaperPickerEntryCollapsedHeight)))
                .coerceAtLeast(minCollapsedPagerHeight)
        pickerMotionContainer
            .getConstraintSet(R.id.collapsed_header_primary)
            ?.constrainHeight(R.id.preview_header, collapsedHeaderHeight)
        // For expanded, it needs to show at least half of the entry view below the wallpaper entry.
        val expandedHeaderHeight =
            (pickerMotionContainer.height -
                    wallpaperPickerEntryExpandedHeight -
                    bottomInset -
                    resources.getDimensionPixelSize(R.dimen.customization_option_entry_height) / 2)
                .coerceAtMost(maxExpandedPagerHeight)
                .coerceAtLeast(minExpandedPagerHeight)
        pickerMotionContainer
            .getConstraintSet(R.id.expanded_header_primary)
            ?.constrainHeight(R.id.preview_header, expandedHeaderHeight)

        // Transition listener handle 2 things
        // 1. Expand and collapse the wallpaper entry
        // 2. Reset the transition and preview when transition back to the primary
        // screen
        pickerMotionContainer.setTransitionListener(
            object : EmptyTransitionListener {

                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    if (currentId == R.id.expanded_header_primary) {
                        // Do not collapse or expand the wallpaper entry when
                        // isLargeScreenSingleDisplayPortrait is true
                        if (!isLargeScreenSingleDisplayPortrait) {
                            wallpaperPickerEntry.animateToExpanded()
                            packThemeSuggestedChip?.animateToExpanded()
                        }
                    } else if (currentId == R.id.collapsed_header_primary) {
                        // Do not collapse or expand the wallpaper entry when
                        // isLargeScreenSingleDisplayPortrait is true
                        if (!isLargeScreenSingleDisplayPortrait) {
                            wallpaperPickerEntry.animateToCollapsed()
                            packThemeSuggestedChip?.animateToCollapsed({})
                        }
                    }

                    if (
                        currentId == R.id.expanded_header_primary ||
                            currentId == R.id.collapsed_header_primary
                    ) {
                        // This is when we complete the transition back to the primary screen. Post
                        // to let this transition fully complete first
                        pickerMotionContainer.post {
                            pickerMotionContainer.setTransition(R.id.transition_primary)
                        }
                        // Reset the preview only after the transition is completed, because the
                        // reset will trigger the animation of the UI components in the floating
                        // sheet content, which can possibly be interrupted by the floating sheet
                        // translating down.
                        customizationPickerViewModel.customizationOptionsViewModel.resetPreview()
                    } else if (currentId == R.id.secondary) {
                        customizationPickerViewModel.customizationOptionsViewModel
                            .onTransitionToSecondaryScreenComplete()
                    }
                }
            }
        )
    }

    private fun initSecondaryScreen(
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>,
        initSelectedOption: CustomizationOption,
        customizationFloatingSheetContainer: FrameLayout,
        pickerMotionContainer: MotionLayout,
        onComplete: () -> Unit,
    ) {
        // If initially on secondary screen, set the correspondent floating layout
        // content, calculate motion layout dimensions and bind the content after.
        customizationOptionFloatingSheetViewMap[initSelectedOption]?.let { floatingSheetView ->
            setCustomizationOptionFloatingSheet(
                floatingSheetViewContent = floatingSheetView,
                floatingSheetContainer = customizationFloatingSheetContainer,
                motionContainer = pickerMotionContainer,
                onComplete = {
                    pickerMotionContainer.transitionToState(R.id.secondary)
                    pickerMotionContainer.progress = 1.0f
                    pickerMotionContainer.post { onComplete.invoke() }
                },
            )
        }
    }

    private fun bindCustomizationPicker(
        customizationOptionsData: CustomizationOptionsData,
        pickerMotionContainer: MotionLayout,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>,
        customizationFloatingSheetContainer: FrameLayout,
        packThemeSuggestedChip: PackThemeSuggestedChip?,
    ) {
        CustomizationPickerBinder2.bind(
            customizationOptionsData = customizationOptionsData,
            view = pickerMotionContainer,
            lockScreenCustomizationOptionEntries = lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries = homeScreenCustomizationOptionEntries,
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
            navigateToSecondary = { option ->
                if (pickerMotionContainer.currentState != R.id.secondary) {
                    customizationOptionFloatingSheetViewMap[option]?.let { floatingSheetView ->
                        setCustomizationOptionFloatingSheet(
                            floatingSheetViewContent = floatingSheetView,
                            floatingSheetContainer = customizationFloatingSheetContainer,
                            motionContainer = pickerMotionContainer,
                            onComplete = {
                                userEventLogger.logEnterScreen(
                                    userEventLogger.transformCustomizationOptionToScreenForLogging(
                                        option
                                    )
                                )
                                // Transition to secondary screen after content is set
                                fullyCollapsed = pickerMotionContainer.progress == 1.0f
                                pickerMotionContainer.transitionToState(R.id.secondary)
                            },
                        )
                    }
                }
            },
            navigateToWallpaperCategoriesScreen = { _ -> switchFragment(CategoriesFragment()) },
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
            navigateToPackThemeActivity = { intent -> context?.startActivity(intent) },
            navigateToWallpaperCollectionScreen = { categoryId, categoryType ->
                switchFragment(
                    individualPickerFactory.getIndividualPickerInstance(categoryId, categoryType)
                )
            },
            navigateToExtendedWallpaperEffects = {
                switchFragment(
                    PhotoPickerFragment.newInstance(shouldNavigateToExtendedWallpaperEffects = true)
                )
            },
            packThemeSuggestedChip = packThemeSuggestedChip,
            packThemeSuggestedEntryBinder = packThemeSuggestedEntryBinder,
            curatedPhotosTimeUtil = curatedPhotosTimeUtil,
            userEventLogger = userEventLogger,
        )

        customizationOptionsBinder.bindDiscardChangesDialog(
            customizationOptionsViewModel =
                customizationPickerViewModel.customizationOptionsViewModel,
            lifecycleOwner = viewLifecycleOwner,
            activity = requireActivity(),
        )
    }

    private fun switchFragment(fragment: Fragment) {
        if (isAdded) {
            parentFragmentManager.commit {
                replace(R.id.fragment_container, fragment)
                addToBackStack(null)
            }
        }
    }

    override fun onEnterAnimationCompleteAfterActivityCreated() {
        if (isInitialCreation) {
            val previewPager: View = view?.findViewById(R.id.preview_pager) ?: return
            // Show the preview pager only after enter animation completes. If the preview pager was
            // invisible, making it visible will trigger the surface view's surfaceCreated callback,
            // as well as the binding of the wallpaper preview and workspace preview.
            setPreviewPagerVisible(previewPager = previewPager, isVisible = true)
            PreviewAlphaAnimationBinder.bind(
                previewPager = previewPager,
                viewModel = customizationPickerViewModel,
                lifecycleOwner = viewLifecycleOwner,
            )
            isInitialCreation = false
        }
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
            mainScope.launch { wallpaperConnectionUtils.disconnectAll() }
        }

        super.onDestroyView()
        onBackPressedCallback?.remove()
    }

    private fun setupToolbar(navButton: FrameLayout, toolbar: Toolbar, applyButton: ApplyButton) {
        toolbar.title = getString(R.string.app_name)
        toolbar.setBackgroundColor(Color.TRANSPARENT)
        DarkModeUpdateBinder.bind(
            onProgressChange = { progress ->
                val shouldUseLightText = progress == 1f
                setUpStatusBar(shouldUseLightText)
            },
            colorUpdateViewModel = colorUpdateViewModel,
            // Status bar text can only be set to light or dark, and cannot be animated
            shouldAnimate = { false },
            lifecycleOwner = viewLifecycleOwner,
        )
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
        rootView: View,
        previewPager: ClickableMotionLayout,
    ): PreviewPagerViews {
        previewPager.addClickableViewId(R.id.preview_card)

        // Inflate the clock and attach to the lock preview and bind clock view
        val lockPreview: View = previewPager.requireViewById(R.id.lock_preview)
        val lockPreviewContainer: ViewGroup =
            lockPreview.requireViewById(R.id.wallpaper_preview_crop)
        val clockHostView =
            customizationOptionUtil.createClockPreviewAndAddToParent(
                lockPreviewContainer,
                layoutInflater,
            )
        // The shade covers the two surface views of the preview and reveals when the preview is
        // ready to show.
        val lockPreviewShade =
            layoutInflater.inflate(R.layout.preview_shade, lockPreviewContainer, false)
        lockPreviewContainer.addView(lockPreviewShade)

        val homePreview: View = previewPager.requireViewById(R.id.home_preview)
        val homePreviewContainer: ViewGroup =
            homePreview.requireViewById(R.id.wallpaper_preview_crop)
        // The shade covers the two surface views of the preview and reveals when the preview is
        // ready to show.
        val homePreviewShade =
            layoutInflater.inflate(R.layout.preview_shade, homePreviewContainer, false)
        homePreviewContainer.addView(homePreviewShade)

        return PreviewPagerViews(
            previewPager = previewPager,
            lockPreviewLabel = previewPager.requireViewById(R.id.lock_preview_label),
            homePreviewLabel = previewPager.requireViewById(R.id.home_preview_label),
            lockPreview = previewPager.requireViewById(R.id.lock_preview),
            homePreview = previewPager.requireViewById(R.id.home_preview),
            lockPreviewShade = lockPreviewShade,
            homePreviewShade = homePreviewShade,
            clockHostView = clockHostView,
            clockFaceClickDelegateView = rootView.requireViewById(R.id.clock_face_click_delegate),
        )
    }

    private fun bindPreviewPager(
        rootView: View,
        previewPagerViews: PreviewPagerViews,
        isFirstBinding: Boolean,
    ) {
        PreviewPagerBinder.bind(previewPagerViews, customizationPickerViewModel, viewLifecycleOwner)

        ColorUpdateBinder.bind(
            setColor = { color -> previewPagerViews.lockPreviewLabel.setTextColor(color) },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = viewLifecycleOwner,
        )

        ColorUpdateBinder.bind(
            setColor = { color -> previewPagerViews.homePreviewLabel.setTextColor(color) },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = viewLifecycleOwner,
        )

        ColorUpdateBinder.bind(
            setColor = { color -> previewPagerViews.lockPreviewShade.setBackgroundColor(color) },
            color = colorUpdateViewModel.colorSurfaceContainer,
            shouldAnimate = { previewPagerViews.lockPreviewShade.alpha != 0F },
            lifecycleOwner = viewLifecycleOwner,
        )

        ColorUpdateBinder.bind(
            setColor = { color -> previewPagerViews.homePreviewShade.setBackgroundColor(color) },
            color = colorUpdateViewModel.colorSurfaceContainer,
            shouldAnimate = { previewPagerViews.homePreviewShade.alpha != 0F },
            lifecycleOwner = viewLifecycleOwner,
        )

        // Bind the clock view if nonnull
        val clockHostView = previewPagerViews.clockHostView
        val clockFaceClickDelegateView = previewPagerViews.clockFaceClickDelegateView
        if (clockHostView != null && clockFaceClickDelegateView != null) {
            customizationOptionsBinder.bindClockPreview(
                context = requireContext(),
                rootView = rootView,
                clockHostView = clockHostView,
                clockFaceClickDelegateView = clockFaceClickDelegateView,
                viewModel = customizationPickerViewModel,
                colorUpdateViewModel = colorUpdateViewModel,
                lifecycleOwner = viewLifecycleOwner,
                clockViewFactory = clockViewFactory,
            )
        }

        bindPreview(
            screen = LOCK_SCREEN,
            previewPager = previewPagerViews.previewPager,
            preview = previewPagerViews.lockPreview,
            isFirstBinding = isFirstBinding,
            previewTextLabel = previewPagerViews.lockPreviewLabel,
        )

        bindPreview(
            screen = HOME_SCREEN,
            previewPager = previewPagerViews.previewPager,
            preview = previewPagerViews.homePreview,
            isFirstBinding = isFirstBinding,
            previewTextLabel = previewPagerViews.homePreviewLabel,
        )
    }

    private fun bindPreview(
        screen: Screen,
        previewPager: ClickableMotionLayout,
        preview: View,
        isFirstBinding: Boolean,
        previewTextLabel: View? = null,
    ) {
        val appContext = context?.applicationContext ?: return
        val activity = activity ?: return
        val previewViewModel = customizationPickerViewModel.basePreviewViewModel

        val previewCard: View = preview.requireViewById(R.id.preview_card)

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
            onPreviewReady = { previewScreen ->
                customizationPickerViewModel.setPreviewReady(previewScreen, true)
            },
            onPreviewSurfaceDestroyed = { previewScreen ->
                customizationPickerViewModel.setPreviewReady(previewScreen, false)
            },
            clockViewFactory = clockViewFactory,
            previewTextLabel = previewTextLabel,
        )
    }

    private fun initCustomizationOptionEntries(
        customizationOptionsData: CustomizationOptionsData,
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
            customizationOptionUtil.getOptionEntries(
                customizationOptionsData = customizationOptionsData,
                screen = screen,
                optionContainer = optionEntriesContainer,
                layoutInflater = layoutInflater,
            )
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
            val translationY = floatingSheetViewContent.height
            floatingSheetContainer.translationY = 0.0f
            floatingSheetContainer.alpha = 0.0f
            // Update the motion container
            motionContainer.getConstraintSet(R.id.expanded_header_primary)?.apply {
                setTranslationY(
                    R.id.customization_option_floating_sheet_container,
                    translationY.toFloat(),
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
                    translationY.toFloat(),
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
            // Wait until motion container's constraints are updated
            motionContainer.post { onComplete() }
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
        private const val WALLPAPER_ENTRY_EARLY_COLLAPSE_PROGRESS_THRESHOLD = 0.25f
        private const val ANIMATION_DURATION = 200
        private const val PACK_THEME_PACKAGE_NAME =
            "com.google.android.apps.pixel.customizationbundle"
        private const val PACK_THEME_SERVICE_NAME =
            "$PACK_THEME_PACKAGE_NAME.tiktok.app.MainActivity"
    }

    private fun prepareFragmentExitTransitionAnimation() {
        val transition = (exitTransition as? Transition) ?: return
        transition.addListener(
            object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition) {
                    val previewPager: View = view?.findViewById(R.id.preview_pager) ?: return
                    setPreviewPagerVisible(previewPager = previewPager, isVisible = false)
                    isReenterAfterExit = true
                }

                override fun onTransitionEnd(transition: Transition) {
                    val previewPager: View = view?.findViewById(R.id.preview_pager) ?: return
                    setPreviewPagerVisible(previewPager = previewPager, isVisible = true)
                }

                override fun onTransitionCancel(transition: Transition) {
                    val previewPager: View = view?.findViewById(R.id.preview_pager) ?: return
                    setPreviewPagerVisible(previewPager = previewPager, isVisible = true)
                    isReenterAfterExit = false
                }

                override fun onTransitionPause(transition: Transition) {}

                override fun onTransitionResume(transition: Transition) {}
            }
        )
    }

    private fun prepareFragmentReenterTransitionAnimation() {
        val transition = (reenterTransition as? Transition) ?: return
        transition.addListener(
            object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition) {}

                override fun onTransitionEnd(transition: Transition) {
                    val rootView = view ?: return
                    val previewPager: View = rootView.requireViewById(R.id.preview_pager)
                    setPreviewPagerVisible(previewPager = previewPager, isVisible = true)
                    PreviewAlphaAnimationBinder.bind(
                        previewPager = previewPager,
                        viewModel = customizationPickerViewModel,
                        lifecycleOwner = viewLifecycleOwner,
                    )
                }

                override fun onTransitionCancel(transition: Transition) {}

                override fun onTransitionPause(transition: Transition) {}

                override fun onTransitionResume(transition: Transition) {}
            }
        )
    }

    /**
     * Specifically set the preview pager visible or invisible. We set the preview pager invisible
     * early before some Fragment transitions. This is because we encounter the preview flashing
     * issue due to the unexpected [SurfaceView] callbacks of onSurfaceCreated and
     * onSurfaceDestroyed, during Fragment transition.
     */
    private fun setPreviewPagerVisible(previewPager: View, isVisible: Boolean) {
        val lockPreviewLabel: View = previewPager.requireViewById(R.id.lock_preview_label)
        val homePreviewLabel: View = previewPager.requireViewById(R.id.home_preview_label)
        val lockPreview: View = previewPager.requireViewById(R.id.lock_preview)
        val homePreview: View = previewPager.requireViewById(R.id.home_preview)
        val lockWallpaperSurface: SurfaceView = lockPreview.requireViewById(R.id.wallpaper_surface)
        val lockWorkspaceSurface: SurfaceView = lockPreview.requireViewById(R.id.workspace_surface)
        val homeWallpaperSurface: SurfaceView = homePreview.requireViewById(R.id.wallpaper_surface)
        val homeWorkspaceSurface: SurfaceView = homePreview.requireViewById(R.id.workspace_surface)
        lockPreviewLabel.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        lockWallpaperSurface.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        lockWorkspaceSurface.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        homePreviewLabel.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        homeWallpaperSurface.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        homeWorkspaceSurface.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }
}
