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

package com.android.wallpaper.picker.customization.ui.viewmodel

import android.content.Context
import com.android.wallpaper.asset.ContentUriAsset
import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CuratedPhotosInteractor
import com.android.wallpaper.picker.category.domain.interactor.OnDeviceWallpapersInteractor
import com.android.wallpaper.picker.category.ui.view.SectionCardinality
import com.android.wallpaper.picker.category.ui.viewmodel.TileViewModel
import com.android.wallpaper.picker.data.WallpaperModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WallpaperCarouselViewModel
@AssistedInject
constructor(
    @ApplicationContext context: Context,
    curatedPhotosInteractor: CuratedPhotosInteractor,
    creativeCategoryInteractor: CreativeCategoryInteractor,
    onDeviceWallpapersInteractor: OnDeviceWallpapersInteractor,
    @Assisted private val viewModelScope: CoroutineScope,
) {

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    val curatedPhotoCarouselItems: Flow<List<TileViewModel>> =
        curatedPhotosInteractor.category.distinctUntilChanged().map { category ->
            category.categoryModel.collectionCategoryData?.wallpaperModels?.map { wallpaperModel ->
                val staticWallpaperModel = wallpaperModel as? WallpaperModel.StaticWallpaperModel
                TileViewModel(
                    defaultDrawable = null,
                    thumbnailAsset =
                        ContentUriAsset(context, staticWallpaperModel?.imageWallpaperData?.uri),
                    text = category.categoryModel.commonCategoryData.title,
                    showTitle = false,
                    maxCategoriesInRow = SectionCardinality.Single,
                ) {
                    navigateToPreviewScreen(wallpaperModel, CategoryType.CuratedPhotos)
                }
            } ?: emptyList()
        }

    /**
     * This [Flow] maps on device [WallpaperModel] to [TileViewModel]. It is consumed by the
     * carousel in the case there is an insufficient number of curated photos
     */
    val defaultWallpapersTileVieModels: Flow<List<TileViewModel>> =
        onDeviceWallpapersInteractor.defaultWallpapers.distinctUntilChanged().map {
            wallpaperModelList ->
            wallpaperModelList.map { wallpaperModel ->
                val staticWallpaperModel = wallpaperModel as? WallpaperModel.StaticWallpaperModel
                TileViewModel(
                    defaultDrawable = null,
                    thumbnailAsset = staticWallpaperModel?.commonWallpaperData?.thumbAsset,
                    text = wallpaperModel.commonWallpaperData.title ?: "",
                    showTitle = false,
                    maxCategoriesInRow = SectionCardinality.Single,
                ) {
                    navigateToPreviewScreen(wallpaperModel, CategoryType.Default)
                }
            } ?: emptyList()
        }

    /**
     * This [Flow] maps creative categories to [TileViewModel]. This flow is consumed by the
     * carousel in the case there is an insufficient number of curated photos
     */
    val creativeSectionViewModel: Flow<List<TileViewModel>> =
        combine(
            creativeCategoryInteractor.categories,
            creativeCategoryInteractor.standaloneCategories,
        ) { categories, standAlone ->
            val combinedList = categories + standAlone
            combinedList.map { category ->
                TileViewModel(
                    defaultDrawable = null,
                    thumbnailAsset = category.collectionCategoryData?.thumbAsset,
                    text = category.commonCategoryData.title,
                    showTitle = true,
                    maxCategoriesInRow = SectionCardinality.Triple,
                ) {
                    if (category.collectionCategoryData?.isSingleWallpaperCategory == true) {
                        navigateToPreviewScreen(
                            category.collectionCategoryData.wallpaperModels[0],
                            CategoryType.CreativeCategories,
                        )
                    } else {
                        navigateToWallpaperCollection(
                            category.commonCategoryData.collectionId,
                            CategoryType.CreativeCategories,
                        )
                    }
                }
            }
        }

    /**
     * This [Flow] emits the desired [TileViewModel] collection based on the number of individual
     * curated photos, on-device wallpapers and creative wallpapers
     */
    val wallpaperCarouselItems: Flow<List<TileViewModel>> =
        combine(
            curatedPhotoCarouselItems,
            defaultWallpapersTileVieModels,
            creativeSectionViewModel,
        ) {
            curatedPhotos: List<TileViewModel>,
            defaultWallpapers: List<TileViewModel>,
            creatives: List<TileViewModel> ->
            // if more than 3 curated photos return only curated photos
            if (curatedPhotos.size > CAROUSEL_ITEMS_THRESHOLD) {
                return@combine curatedPhotos
            } else if (creatives.size >= CAROUSEL_ITEMS_THRESHOLD) {
                // if creatives more or equal to 3 than return only creatives
                return@combine creatives
            } else {
                // otherwise just return on-device wallpapers
                return@combine defaultWallpapers
            }
        }

    private fun navigateToPreviewScreen(
        wallpaperModel: WallpaperModel,
        categoryType: CategoryType,
    ) {
        viewModelScope.launch {
            _navigationEvents.emit(
                NavigationEvent.NavigateToPreviewScreen(wallpaperModel, categoryType)
            )
        }
    }

    private fun navigateToWallpaperCollection(collectionId: String, categoryType: CategoryType) {
        viewModelScope.launch {
            _navigationEvents.emit(
                NavigationEvent.NavigateToWallpaperCollection(collectionId, categoryType)
            )
        }
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): WallpaperCarouselViewModel
    }

    enum class CategoryType {
        CreativeCategories,
        CuratedPhotos,
        Default,
    }

    sealed class NavigationEvent {
        data class NavigateToPreviewScreen(
            val wallpaperModel: WallpaperModel,
            val categoryType: CategoryType,
        ) : NavigationEvent()

        data class NavigateToWallpaperCollection(
            val categoryId: String,
            val categoryType: CategoryType,
        ) : NavigationEvent()
    }

    companion object {
        const val CAROUSEL_ITEMS_THRESHOLD = 3
    }
}
