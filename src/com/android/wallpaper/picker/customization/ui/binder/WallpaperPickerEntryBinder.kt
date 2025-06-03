/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.picker.customization.ui.binder

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.Screen
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.category.ui.view.adapter.CuratedPhotosAdapter
import com.android.wallpaper.picker.category.ui.view.adapter.LoadingAnimationAdapter
import com.android.wallpaper.picker.customization.shared.model.CategoryType
import com.android.wallpaper.picker.customization.ui.view.WallpaperPickerEntry
import com.android.wallpaper.picker.customization.ui.view.listener.CarouselHorizontalScrollEnforcer
import com.android.wallpaper.picker.customization.ui.view.listener.WallpaperTitleScrollListener
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel.NavigationEvent.NavigateToExtendedWallpaperEffects
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel.NavigationEvent.NavigateToPreviewScreen
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel.NavigationEvent.NavigateToWallpaperCollection
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.util.CuratedPhotosTimeUtil
import com.android.wallpaper.widget.GridPaddingDecoration
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object WallpaperPickerEntryBinder {
    private const val DESKTOP_CAROUSEL_ITEMS_COUNT = 5

    private class CustomScrollableCarouselLayoutManager : CarouselLayoutManager() {
        private var isScrollable = false

        override fun canScrollHorizontally(): Boolean {
            return if (isScrollable) super.canScrollHorizontally() else false
        }

        override fun canScrollVertically(): Boolean {
            return false
        }

        fun setIsScrollable(isScrollable: Boolean) {
            this.isScrollable = isScrollable
        }
    }

    fun bind(
        view: WallpaperPickerEntry,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToWallpaperCategoriesScreen: (screen: Screen) -> Unit,
        navigateToPreviewScreen: ((wallpaperModel: WallpaperModel) -> Unit)?,
        navigateToWallpaperCollectionScreen:
            ((collectionId: String, categoryType: CategoryType) -> Unit)?,
        navigateToExtendedWallpaperEffects: (() -> Unit)?,
        curatedPhotosTimeUtil: CuratedPhotosTimeUtil,
        userEventLogger: UserEventLogger,
    ) {
        val isOnMainScreen = {
            viewModel.customizationOptionsViewModel.selectedOption.value == null
        }
        val shouldShowDesktopUi = BaseFlags.get().shouldShowDesktopUi(view.context)
        bindWallpaperCarousel(
            wallpaperPickerEntryView = view,
            viewModel = viewModel.customizationOptionsViewModel.wallpaperCarouselViewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            shouldAnimateColor = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
            navigateToPreviewScreen = navigateToPreviewScreen,
            navigateToWallpaperCollectionScreen = navigateToWallpaperCollectionScreen,
            navigateToExtendedWallpaperEffects = navigateToExtendedWallpaperEffects,
            curatedPhotosTimeUtil = curatedPhotosTimeUtil,
            userEventLogger = userEventLogger,
            shouldShowDesktopUi = shouldShowDesktopUi,
        )
        val container =
            view.requireViewById<ConstraintLayout>(R.id.wallpaper_picker_entry_expanded_container)
        if (!shouldShowDesktopUi) {
            bindWallpaperPickerEntryLabels(
                container = container,
                suggestedPhotosLabel = view.suggestedPhotosText,
                viewModel = viewModel.customizationOptionsViewModel.wallpaperCarouselViewModel,
                lifecycleOwner = lifecycleOwner,
            )
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedPreviewScreen.collect { previewScreen ->
                    view.collapsedButton.setOnClickListener {
                        navigateToWallpaperCategoriesScreen.invoke(previewScreen)
                    }
                    view.moreWallpapersButton.setOnClickListener {
                        navigateToWallpaperCategoriesScreen.invoke(previewScreen)
                    }
                }
            }
        }

        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(DrawableCompat.wrap(view.background), color)
            },
            color = colorUpdateViewModel.colorSurfaceBright,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )

        ColorUpdateBinder.bind(
            setColor = { color ->
                TextViewCompat.setCompoundDrawableTintList(
                    view.moreWallpapersButton,
                    ColorStateList.valueOf(color),
                )
                TextViewCompat.setCompoundDrawableTintList(
                    view.collapsedButton,
                    ColorStateList.valueOf(color),
                )
                view.moreWallpapersButton.setTextColor(color)
                view.collapsedButton.setTextColor(color)
            },
            color = colorUpdateViewModel.colorPrimary,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )

        ColorUpdateBinder.bind(
            setColor = { color -> view.suggestedPhotosText.setTextColor(color) },
            color = colorUpdateViewModel.colorSecondary,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )
    }

    /**
     * Sets up the RecyclerView's LayoutManager and attaches mode-specific listeners based on
     * whether the application is in desktop mode.
     */
    private fun setupWallpaperCarouselRecyclerView(
        recyclerView: RecyclerView,
        shouldShowDesktopUi: Boolean,
    ) {
        if (shouldShowDesktopUi) {
            // Desktop mode: Use GridLayoutManager for a static list-like appearance. The carousel
            // is not scrollable.
            recyclerView.layoutManager =
                GridLayoutManager(recyclerView.context, DESKTOP_CAROUSEL_ITEMS_COUNT)
            val itemDecoration =
                GridPaddingDecoration(
                    recyclerView.context.resources
                        .getDimension(R.dimen.curated_photo_horizontal_margin)
                        .toInt(),
                    0,
                )
            recyclerView.addItemDecoration(itemDecoration)
            recyclerView.onFlingListener = null
        } else {
            // Mobile/Tablet mode: Use CustomScrollableCarouselLayoutManager to enable carousel
            // experience.
            recyclerView.layoutManager = CustomScrollableCarouselLayoutManager()
            if (recyclerView.onFlingListener == null) {
                CarouselSnapHelper().attachToRecyclerView(recyclerView)
            }
            val horizontalScrollEnforcer = CarouselHorizontalScrollEnforcer(recyclerView.context)
            recyclerView.addOnScrollListener(horizontalScrollEnforcer)
            recyclerView.addOnItemTouchListener(horizontalScrollEnforcer)
            recyclerView.isNestedScrollingEnabled = false
        }
    }

    private fun bindWallpaperCarousel(
        wallpaperPickerEntryView: WallpaperPickerEntry,
        viewModel: WallpaperCarouselViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
        navigateToPreviewScreen: ((wallpaperModel: WallpaperModel) -> Unit)?,
        navigateToWallpaperCollectionScreen:
            ((collectionId: String, categoryType: CategoryType) -> Unit)?,
        navigateToExtendedWallpaperEffects: (() -> Unit)?,
        curatedPhotosTimeUtil: CuratedPhotosTimeUtil,
        userEventLogger: UserEventLogger,
        shouldShowDesktopUi: Boolean,
    ) {
        val wallpaperCarousel: RecyclerView = wallpaperPickerEntryView.wallpaperCarousel
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val loadingAnimationAdapter =
                        LoadingAnimationAdapter(
                            size = 3,
                            colorUpdateViewModel = colorUpdateViewModel,
                            shouldAnimateColor = shouldAnimateColor,
                            lifecycleOwner = lifecycleOwner,
                            curatedPhotosTimeUtil = curatedPhotosTimeUtil,
                            userEventLogger = userEventLogger,
                        )
                    wallpaperCarousel.adapter = loadingAnimationAdapter

                    setupWallpaperCarouselRecyclerView(wallpaperCarousel, shouldShowDesktopUi)

                    viewModel.wallpaperCarouselItems
                        .map { it ->
                            // Only display a fixed number of items within wallpaperCarousel for
                            // desktop mode.
                            if (shouldShowDesktopUi && it.size >= DESKTOP_CAROUSEL_ITEMS_COUNT) {
                                it.take(DESKTOP_CAROUSEL_ITEMS_COUNT)
                            } else {
                                it
                            }
                        }
                        .collect {
                            wallpaperPickerEntryView.post {
                                if (it.isEmpty()) {
                                    wallpaperPickerEntryView.animateToCollapsed()
                                }
                            }

                            wallpaperCarousel.swapAdapter(
                                CuratedPhotosAdapter(
                                    it,
                                    curatedPhotosTimeUtil,
                                    userEventLogger,
                                    shouldShowDesktopUi,
                                ),
                                /** removeAndRecycleExistingViews= */
                                false,
                            )

                            // Enable scrolling for the carousel only for mobile/tablet mode.
                            if (!shouldShowDesktopUi) {
                                if (!it.isEmpty() && it.get(0).showTitle) {
                                    wallpaperCarousel.addOnScrollListener(
                                        WallpaperTitleScrollListener()
                                    )
                                }
                                (wallpaperCarousel.layoutManager
                                        as? CustomScrollableCarouselLayoutManager)
                                    ?.setIsScrollable(true)
                            }
                        }
                }

                launch {
                    viewModel.navigationEvents.collect {
                        navigationEvent: WallpaperCarouselViewModel.NavigationEvent ->
                        when (navigationEvent) {
                            is NavigateToWallpaperCollection -> {
                                navigateToWallpaperCollectionScreen?.invoke(
                                    navigationEvent.categoryId,
                                    navigationEvent.categoryType,
                                )
                            }
                            is NavigateToPreviewScreen -> {
                                navigateToPreviewScreen?.invoke(navigationEvent.wallpaperModel)
                            }
                            is NavigateToExtendedWallpaperEffects -> {
                                navigateToExtendedWallpaperEffects?.invoke()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindWallpaperPickerEntryLabels(
        container: ConstraintLayout,
        suggestedPhotosLabel: TextView,
        viewModel: WallpaperCarouselViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.shouldShowSuggestedPhotosLabel.collect {
                        suggestedPhotosLabel.isVisible = it
                        if (it) {
                            applyEndAlignedConstraints(container)
                        } else {
                            applyCenteredConstraints(container)
                        }
                    }
                }
            }
        }
    }

    private fun applyEndAlignedConstraints(
        wallpaperPickerEntryExpandedContainer: ConstraintLayout
    ) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(wallpaperPickerEntryExpandedContainer)

        constraintSet.clear(R.id.more_wallpapers_button, ConstraintSet.START)
        constraintSet.connect(
            R.id.more_wallpapers_button,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
        )

        constraintSet.applyTo(wallpaperPickerEntryExpandedContainer)
    }

    private fun applyCenteredConstraints(wallpaperPickerEntryExpandedContainer: ConstraintLayout) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(wallpaperPickerEntryExpandedContainer)

        constraintSet.clear(R.id.more_wallpapers_button, ConstraintSet.END)
        constraintSet.connect(
            R.id.more_wallpapers_button,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
        )
        constraintSet.connect(
            R.id.more_wallpapers_button,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
        )
        constraintSet.setHorizontalBias(R.id.more_wallpapers_button, 0.5f)

        constraintSet.applyTo(wallpaperPickerEntryExpandedContainer)
    }
}
