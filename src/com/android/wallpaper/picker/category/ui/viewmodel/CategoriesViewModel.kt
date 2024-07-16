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

package com.android.wallpaper.picker.category.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.picker.category.domain.interactor.CategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.MyPhotosInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Top level [ViewModel] for the categories screen */
@HiltViewModel
class CategoriesViewModel
@Inject
constructor(
    private val singleCategoryInteractor: CategoryInteractor,
    private val creativeWallpaperInteractor: CreativeCategoryInteractor,
    private val myPhotosInteractor: MyPhotosInteractor,
) : ViewModel() {

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    private fun navigateToWallpaperCollection(collectionId: String) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToWallpaperCollection(collectionId))
        }
    }

    private fun navigateToPreviewScreen(collectionId: String) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToPreviewScreen(collectionId))
        }
    }

    private fun navigateToPhotosPicker() {
        viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToPhotosPicker) }
    }

    private fun navigateToThirdPartyApp(componentId: String) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToThirdParty(componentId))
        }
    }

    private val individualSectionViewModels: Flow<List<SectionViewModel>> =
        singleCategoryInteractor.categories.map { categories ->
            return@map categories.map { category ->
                SectionViewModel(
                    tileViewModels =
                        listOf(
                            TileViewModel(null, category.commonCategoryData.title) {
                                //  TODO(b/352081782): check if there is a single wallpaper
                                navigateToWallpaperCollection(
                                    category.commonCategoryData.collectionId
                                )
                            }
                        ),
                    columnCount = 1
                )
            }
        }

    private val creativeSectionViewModel: Flow<SectionViewModel> =
        creativeWallpaperInteractor.categories.map { categories ->
            val tiles =
                categories.map { category ->
                    TileViewModel(null, category.commonCategoryData.title)
                }
            return@map SectionViewModel(tileViewModels = tiles, columnCount = 3)
        }

    private val myPhotosSectionViewModel: Flow<SectionViewModel> =
        myPhotosInteractor.category.map { category ->
            SectionViewModel(
                tileViewModels =
                    listOf(
                        TileViewModel(null, category.commonCategoryData.title) {
                            // TODO(b/352081782): trigger the effect with effect controller
                            navigateToPhotosPicker()
                        }
                    ),
                columnCount = 3
            )
        }

    val sections: Flow<List<SectionViewModel>> =
        combine(individualSectionViewModels, creativeSectionViewModel, myPhotosSectionViewModel) {
            individualViewModels,
            creativeViewModel,
            myPhotosViewModel ->
            buildList {
                add(creativeViewModel)
                add(myPhotosViewModel)
                addAll(individualViewModels)
            }
        }

    /** This method updates the photos category */
    fun updateMyPhotosCategory() {
        myPhotosInteractor.updateMyPhotos()
    }

    sealed class NavigationEvent {
        data class NavigateToWallpaperCollection(val categoryId: String) : NavigationEvent()

        data class NavigateToPreviewScreen(val wallpaperId: String) : NavigationEvent()

        object NavigateToPhotosPicker : NavigationEvent()

        data class NavigateToThirdParty(val component: String) : NavigationEvent()
    }
}
