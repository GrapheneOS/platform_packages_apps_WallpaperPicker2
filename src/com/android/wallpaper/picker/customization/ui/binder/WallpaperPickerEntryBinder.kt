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
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.category.ui.view.adapter.CuratedPhotosAdapter
import com.android.wallpaper.picker.customization.ui.view.WallpaperPickerEntry
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel.NavigationEvent.NavigateToPreviewScreen
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel.NavigationEvent.NavigateToWallpaperCollection
import com.android.wallpaper.picker.data.WallpaperModel
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import kotlinx.coroutines.launch

object WallpaperPickerEntryBinder {

    fun bind(
        view: WallpaperPickerEntry,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToWallpaperCategoriesScreen: (screen: Screen) -> Unit,
        navigateToPreviewScreen: ((wallpaperModel: WallpaperModel) -> Unit)?,
    ) {

        bindWallpaperCarousel(
            wallpaperCarousel = view.wallpaperCarousel,
            viewModel = viewModel.customizationOptionsViewModel.wallpaperCarouselViewModel,
            lifecycleOwner = lifecycleOwner,
            navigateToPreviewScreen = navigateToPreviewScreen,
        )

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

        val isOnMainScreen = {
            viewModel.customizationOptionsViewModel.selectedOption.value == null
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
                view.moreWallpapersButton.setTextColor(color)
                view.collapsedButton.setTextColor(color)
            },
            color = colorUpdateViewModel.colorPrimary,
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
            },
            color = colorUpdateViewModel.colorOnPrimaryContainer,
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

    private fun bindWallpaperCarousel(
        wallpaperCarousel: RecyclerView,
        viewModel: WallpaperCarouselViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToPreviewScreen: ((wallpaperModel: WallpaperModel) -> Unit)?,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.wallpaperCarouselItems.collect {
                        wallpaperCarousel.apply {
                            adapter = CuratedPhotosAdapter(it)
                            layoutManager = CarouselLayoutManager()
                        }
                        if (wallpaperCarousel.onFlingListener == null) {
                            CarouselSnapHelper().attachToRecyclerView(wallpaperCarousel)
                        }
                    }
                }

                launch {
                    viewModel.navigationEvents.collect {
                        navigationEvent: WallpaperCarouselViewModel.NavigationEvent ->
                        when (navigationEvent) {
                            is NavigateToWallpaperCollection -> {
                                // TODO (b/398250531): implement navigation to creative
                                // category collection page
                            }
                            is NavigateToPreviewScreen -> {
                                navigateToPreviewScreen?.invoke(navigationEvent.wallpaperModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
