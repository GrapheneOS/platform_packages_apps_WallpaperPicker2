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
import com.android.wallpaper.R
import com.android.wallpaper.asset.ContentUriAsset
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CuratedPhotosInteractor
import com.android.wallpaper.picker.category.domain.interactor.OnDeviceWallpapersInteractor
import com.android.wallpaper.picker.category.ui.view.SectionCardinality
import com.android.wallpaper.picker.category.ui.viewmodel.TileViewModel
import com.android.wallpaper.picker.customization.shared.model.CategoryType
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
            category.categoryModel.collectionCategoryData?.wallpaperModels?.withIndex()?.map {
                wallpaperModelWithIndex ->
                val staticWallpaperModel =
                    wallpaperModelWithIndex.value as? WallpaperModel.StaticWallpaperModel
                val total = category.categoryModel.collectionCategoryData.wallpaperModels.size

                TileViewModel(
                    defaultDrawable = null,
                    thumbnailAsset =
                        ContentUriAsset(context, staticWallpaperModel?.imageWallpaperData?.uri),
                    text = category.categoryModel.commonCategoryData.title,
                    showTitle = false,
                    maxCategoriesInRow = SectionCardinality.Single,
                    contentDescription =
                        context.getString(
                            R.string.carousel_content_description_photos,
                            wallpaperModelWithIndex.index + 1,
                            total,
                        ),
                ) {
                    navigateToPreviewScreen(
                        wallpaperModelWithIndex.value,
                        CategoryType.CuratedPhotos,
                    )
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
            val totalWallpaperModels = wallpaperModelList.size
            wallpaperModelList.withIndex()?.map { wallpaperModel ->
                val defaultWallpaperContentDesscription =
                    if (wallpaperModel.value.commonWallpaperData.title == null) {
                        context.getString(
                            R.string.carousel_content_description_default_wallpapers,
                            wallpaperModel.index + 1,
                            totalWallpaperModels,
                        )
                    } else {
                        wallpaperModel.value.commonWallpaperData.title
                    }
                val staticWallpaperModel =
                    wallpaperModel.value as? WallpaperModel.StaticWallpaperModel
                TileViewModel(
                    defaultDrawable = null,
                    thumbnailAsset = staticWallpaperModel?.commonWallpaperData?.thumbAsset,
                    text = wallpaperModel.value.commonWallpaperData.title ?: "",
                    showTitle = false,
                    contentDescription = defaultWallpaperContentDesscription,
                    maxCategoriesInRow = SectionCardinality.Single,
                ) {
                    navigateToPreviewScreen(wallpaperModel.value, CategoryType.Default)
                }
            } ?: emptyList()
        }

    /**
     * This [Flow] maps creative categories to [TileViewModel]. This flow is consumed by the
     * carousel in the case there is an insufficient number of curated photos
     */
    val creativeSectionViewModel: Flow<List<TileViewModel>> =
        creativeCategoryInteractor.categories.map { categories ->
            categories.map { category ->
                TileViewModel(
                    defaultDrawable = category.commonCategoryData?.thumbnailDrawable,
                    thumbnailAsset = category.collectionCategoryData?.thumbAsset,
                    text = category.commonCategoryData.title,
                    showTitle = true,
                    maxCategoriesInRow = SectionCardinality.Triple,
                    contentDescription = category.commonCategoryData.title,
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

    /** This [Flow] maps standalone creative categories to [TileViewModel]. */
    val standaloneSectionViewModel: Flow<List<TileViewModel>> =
        creativeCategoryInteractor.standaloneCategories.map { categories ->
            categories.map { category ->
                TileViewModel(
                    defaultDrawable = category.commonCategoryData?.thumbnailDrawable,
                    thumbnailAsset = category.collectionCategoryData?.thumbAsset,
                    text = category.commonCategoryData.title,
                    showTitle = true,
                    maxCategoriesInRow = SectionCardinality.Triple,
                ) {
                    navigateToExtendedWallpaperEffects()
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
            standaloneSectionViewModel,
        ) {
            curatedPhotos: List<TileViewModel>,
            defaultWallpapers: List<TileViewModel>,
            creatives: List<TileViewModel>,
            standAlone: List<TileViewModel> ->
            val creativeCategories =
                if (BaseFlags.get().isMagicPortraitEntryPointsEnabled()) {
                    standAlone + creatives
                } else {
                    creatives
                }
            // if more than 3 curated photos return only curated photos
            if (curatedPhotos.size > CAROUSEL_ITEMS_THRESHOLD) {
                return@combine curatedPhotos
            } else if (creativeCategories.size >= CAROUSEL_ITEMS_THRESHOLD) {
                // if creatives more or equal to 3 than return only creatives
                return@combine creativeCategories
            } else if (defaultWallpapers.size >= CAROUSEL_ITEMS_THRESHOLD) {
                // otherwise just return on-device wallpapers
                return@combine defaultWallpapers
            } else {
                return@combine emptyList()
            }
        }

    /**
     * This [Flow] emits a [Boolean] which signifies whether the suggested photos label should be
     * visible
     */
    val shouldShowSuggestedPhotosLabel: Flow<Boolean> =
        curatedPhotoCarouselItems.map { curatedPhotos: List<TileViewModel> ->
            return@map curatedPhotos.size > CAROUSEL_ITEMS_THRESHOLD
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

    private fun navigateToExtendedWallpaperEffects() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToExtendedWallpaperEffects(null))
        }
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): WallpaperCarouselViewModel
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

        data class NavigateToExtendedWallpaperEffects(val wallpaperModel: WallpaperModel?) :
            NavigationEvent()
    }

    companion object {
        const val CAROUSEL_ITEMS_THRESHOLD = 3
    }
}
