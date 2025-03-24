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

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.service.wallpaper.WallpaperService
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.R
import com.android.wallpaper.asset.ContentUriAsset
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.module.PackageStatusNotifier
import com.android.wallpaper.module.PackageStatusNotifier.PackageStatus
import com.android.wallpaper.picker.category.domain.interactor.CategoriesLoadingStatusInteractor
import com.android.wallpaper.picker.category.domain.interactor.CategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CuratedPhotosInteractor
import com.android.wallpaper.picker.category.domain.interactor.MyPhotosInteractor
import com.android.wallpaper.picker.category.domain.interactor.ThirdPartyCategoryInteractor
import com.android.wallpaper.picker.category.ui.view.SectionCardinality
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.network.domain.NetworkStatusInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch

/** Top level [ViewModel] for the categories screen */
@HiltViewModel
class CategoriesViewModel
@Inject
constructor(
    private val singleCategoryInteractor: CategoryInteractor,
    private val creativeCategoryInteractor: CreativeCategoryInteractor,
    private val curatedPhotosInteractor: CuratedPhotosInteractor,
    private val myPhotosInteractor: MyPhotosInteractor,
    private val thirdPartyCategoryInteractor: ThirdPartyCategoryInteractor,
    private val loadindStatusInteractor: CategoriesLoadingStatusInteractor,
    private val networkStatusInteractor: NetworkStatusInteractor,
    private val packageStatusNotifier: PackageStatusNotifier,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    // Calling this method ensures that we are able to trigger loading of categories when user
    // enters the picker main page.
    fun initialize() {
        Log.i(TAG, "Initializing Categories!")
    }

    init {
        registerLiveWallpaperReceiver()
        registerThirdPartyWallpaperCategories()
    }

    // TODO: b/379138560: Add tests for this method and method below
    private fun registerLiveWallpaperReceiver() {
        packageStatusNotifier.addListener(
            { packageName, status ->
                if (packageName != null) {
                    updateLiveWallpapersCategories(packageName, status)
                }
            },
            WallpaperService.SERVICE_INTERFACE,
        )
    }

    private fun registerThirdPartyWallpaperCategories() {
        packageStatusNotifier.addListener(
            { packageName, status ->
                if (packageName != null) {
                    updateThirdPartyAppCategories(packageName, status)
                }
            },
            Intent.ACTION_SET_WALLPAPER,
        )
    }

    private fun updateLiveWallpapersCategories(packageName: String, @PackageStatus status: Int) {
        refreshThirdPartyLiveWallpaperCategories()
    }

    private fun updateThirdPartyAppCategories(packageName: String, @PackageStatus status: Int) {
        refreshThirdPartyCategories()
    }

    private fun refreshThirdPartyLiveWallpaperCategories() {
        singleCategoryInteractor.refreshThirdPartyLiveWallpaperCategories()
    }

    private fun refreshThirdPartyCategories() {
        thirdPartyCategoryInteractor.refreshThirdPartyAppCategories()
    }

    private fun navigateToWallpaperCollection(collectionId: String, categoryType: CategoryType) {
        viewModelScope.launch {
            _navigationEvents.emit(
                NavigationEvent.NavigateToWallpaperCollection(collectionId, categoryType)
            )
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

    private fun navigateToPhotosPicker(wallpaperModel: WallpaperModel?) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToPhotosPicker(wallpaperModel))
        }
    }

    private fun navigateToThirdPartyApp(resolveInfo: ResolveInfo) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToThirdParty(resolveInfo))
        }
    }

    val categoryModelListDifferentiator =
        { oldList: List<CategoryModel>, newList: List<CategoryModel> ->
            if (oldList.size != newList.size) {
                false
            } else {
                !oldList.containsAll(newList)
            }
        }

    /**
     * This section is only for third party category apps, and not third party live wallpaper
     * category apps which are handled as part of default category sections.
     */
    private val thirdPartyCategorySections: Flow<List<SectionViewModel>> =
        thirdPartyCategoryInteractor.categories
            .distinctUntilChanged { old, new -> categoryModelListDifferentiator(old, new) }
            .map { categories ->
                return@map categories.map { category ->
                    SectionViewModel(
                        tileViewModels =
                            listOf(
                                TileViewModel(
                                    /* defaultDrawable = */ category.thirdPartyCategoryData
                                        ?.defaultDrawable,
                                    /* thumbnailAsset = */ null,
                                    /* text = */ category.commonCategoryData.title,
                                ) {
                                    category.thirdPartyCategoryData?.resolveInfo?.let {
                                        navigateToThirdPartyApp(it)
                                    }
                                }
                            ),
                        columnCount = 1,
                        sectionTitle = null,
                    )
                }
            }

    private val defaultCategorySections: Flow<List<SectionViewModel>> =
        singleCategoryInteractor.categories
            .distinctUntilChanged { old, new -> categoryModelListDifferentiator(old, new) }
            .map { categories ->
                return@map categories.map { category ->
                    SectionViewModel(
                        tileViewModels =
                            listOf(
                                TileViewModel(
                                    defaultDrawable = null,
                                    thumbnailAsset = category.collectionCategoryData?.thumbAsset,
                                    text = category.commonCategoryData.title,
                                ) {
                                    if (
                                        category.collectionCategoryData
                                            ?.isSingleWallpaperCategory == true
                                    ) {
                                        navigateToPreviewScreen(
                                            category.collectionCategoryData.wallpaperModels[0],
                                            CategoryType.DefaultCategories,
                                        )
                                    } else {
                                        navigateToWallpaperCollection(
                                            category.commonCategoryData.collectionId,
                                            CategoryType.DefaultCategories,
                                        )
                                    }
                                }
                            ),
                        columnCount = 1,
                        sectionTitle = null,
                    )
                }
            }

    private val individualSectionViewModels: Flow<List<SectionViewModel>> =
        combine(defaultCategorySections, thirdPartyCategorySections) { list1, list2 ->
            list1 + list2
        }

    private val standaloneCreativeSectionViewModel: Flow<SectionViewModel?> =
        creativeCategoryInteractor.standaloneCategories
            .distinctUntilChanged { old, new -> categoryModelListDifferentiator(old, new) }
            .map { categories ->
                val tiles =
                    categories.map { category ->
                        TileViewModel(
                            defaultDrawable = null,
                            thumbnailAsset = category.collectionCategoryData?.thumbAsset,
                            text = category.commonCategoryData.title,
                            maxCategoriesInRow = SectionCardinality.Single,
                        ) {
                            // TODO: implement navigation for standalone creative category
                        }
                    }

                if (tiles.isEmpty()) {
                    return@map null
                }
                return@map SectionViewModel(
                    tileViewModels = tiles,
                    columnCount = 3,
                    sectionTitle = "",
                )
            }

    private val creativeSectionViewModel: Flow<SectionViewModel?> =
        creativeCategoryInteractor.categories
            .distinctUntilChanged { old, new -> categoryModelListDifferentiator(old, new) }
            .map { categories ->
                val tiles =
                    categories.map { category ->
                        TileViewModel(
                            defaultDrawable = null,
                            thumbnailAsset = category.collectionCategoryData?.thumbAsset,
                            text = category.commonCategoryData.title,
                            maxCategoriesInRow = SectionCardinality.Triple,
                        ) {
                            if (
                                category.collectionCategoryData?.isSingleWallpaperCategory == true
                            ) {
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

                if (tiles.isEmpty()) {
                    return@map null
                }
                return@map SectionViewModel(
                    tileViewModels = tiles,
                    columnCount = 3,
                    sectionTitle = context.getString(R.string.creative_wallpaper_title),
                )
            }

    // Handles the MyPhotos block case. In case there is nothing returned from the PhotosApp,
    // we emit an empty value so it can be filtered out from the categories screen.
    // TODO: Handle the case when user isn't logged into GooglePhotos
    private val myPhotosSectionViewModel: Flow<SectionViewModel> =
        if (BaseFlags.get().isNewPickerUi()) {
                curatedPhotosInteractor.category.distinctUntilChanged().map { category ->
                    PhotosViewModel(
                        tileViewModels =
                            category.categoryModel.collectionCategoryData?.wallpaperModels?.map {
                                wallpaperModel ->
                                val staticWallpaperModel =
                                    wallpaperModel as? WallpaperModel.StaticWallpaperModel
                                TileViewModel(
                                    defaultDrawable = null,
                                    thumbnailAsset =
                                        ContentUriAsset(
                                            context,
                                            staticWallpaperModel?.imageWallpaperData?.uri,
                                        ),
                                    text = category.categoryModel.commonCategoryData.title,
                                    maxCategoriesInRow = SectionCardinality.Single,
                                ) {
                                    navigateToPreviewScreen(
                                        wallpaperModel,
                                        CategoryType.MyPhotosCategories,
                                    )
                                }
                            } ?: emptyList(),
                        columnCount = 3,
                        sectionTitle =
                            context.getString(R.string.choose_a_curated_photo_section_title),
                        displayType = DisplayType.Carousel,
                        status = category.status,
                        isDismissed = curatedPhotosInteractor.dismissBanner.value,
                        pendingIntent = category.pendingIntent,
                    ) {
                        navigateToPhotosPicker(null)
                    }
                }
            } else {
                myPhotosInteractor.category.distinctUntilChanged().map { category ->
                    SectionViewModel(
                        tileViewModels =
                            listOf(
                                TileViewModel(
                                    defaultDrawable = category.imageCategoryData?.defaultDrawable,
                                    thumbnailAsset = category.imageCategoryData?.thumbnailAsset,
                                    text = category.commonCategoryData.title,
                                    maxCategoriesInRow = SectionCardinality.Single,
                                ) {
                                    // TODO(b/352081782): trigger the effect with effect controller
                                    navigateToPhotosPicker(null)
                                }
                            ),
                        columnCount = 3,
                        sectionTitle = context.getString(R.string.choose_a_wallpaper_section_title),
                    )
                }
            }
            .onEmpty {
                emit(
                    SectionViewModel(
                        tileViewModels = emptyList(),
                        columnCount = 0,
                        sectionTitle = "No Photos Available",
                    )
                )
            }

    // The ordering of addition of viewModels here decides the final ordering how sections would
    // appear in the categories page.
    val sections: Flow<List<SectionViewModel>> =
        combine(
            individualSectionViewModels,
            creativeSectionViewModel,
            myPhotosSectionViewModel,
            standaloneCreativeSectionViewModel,
        ) { individualViewModels, creativeViewModel, myPhotosViewModel, standaloneCreativeViewModel
            ->
            buildList {
                if (BaseFlags.get().isNewPickerUi()) {
                    creativeViewModel?.let { add(it) }
                    add(myPhotosViewModel)
                    if (false) {
                        standaloneCreativeViewModel?.let { add(it) }
                    }
                } else {
                    creativeViewModel?.let { add(it) }
                    add(myPhotosViewModel)
                }
                addAll(individualViewModels)
            }
        }

    val isLoading: Flow<Boolean> = loadindStatusInteractor.isLoading

    /** A [Flow] to indicate when the network status has been made enabled */
    val isConnectionObtained: Flow<Boolean> =
        networkStatusInteractor.isConnectionObtained.map { status ->
            return@map status
        }

    /**
     * A [Flow] whether the categories should be updated
     * 1. when network gets connected and network categories are cached, there should be no refresh
     *    when first entering the categories page
     * 2. when network gets connected and network categories are not cached, there should be a
     *    refresh when entering the categories page
     * 3. when network gets disconnected and network categories are cached, there should be a
     *    refresh (purge)
     * 4. when network gets disconnected and network categories are not cached, then no-op when
     *    entering the categories page
     */
    val shouldRefreshCategories: Flow<Boolean> =
        combine(
            networkStatusInteractor.isConnectionObtained,
            singleCategoryInteractor.isNetworkCategoriesNotEmpty,
        ) { isConnected, isCategoriesCached ->
            // if there is connection and empty cache, then refetch
            if (isConnected) {
                if (!isCategoriesCached) {
                    // if categories are not cached then we load them
                    return@combine true
                } else {
                    // if categories are already cached, then no need to refresh
                    return@combine false
                }
            } else { // not connected
                if (isCategoriesCached) {
                    // if no connection and categories are cached we need to purge them
                    return@combine true
                } else {
                    // if no connection and no categories in cache, then no-op
                    return@combine false
                }
            }
        }

    /** This method sets whether the banner is dismissed by the user. */
    fun setBannerDismissed(dismissed: Boolean) {
        curatedPhotosInteractor.setBannerDismissed(dismissed)
    }

    /** This method updates network categories */
    fun refreshNetworkCategories() {
        singleCategoryInteractor.refreshNetworkCategories()
    }

    /** This method updates the photos category */
    fun updateMyPhotosCategory() {
        myPhotosInteractor.updateMyPhotos()
    }

    /** This method updates the specified category */
    fun refreshCategory() {
        // update creative categories at this time only
        creativeCategoryInteractor.updateCreativeCategories()
    }

    enum class CategoryType {
        ThirdPartyCategories,
        DefaultCategories,
        CreativeCategories,
        MyPhotosCategories,
        Default,
    }

    enum class DisplayType {
        Carousel,
        Default,
    }

    sealed class NavigationEvent {
        data class NavigateToWallpaperCollection(
            val categoryId: String,
            val categoryType: CategoryType,
        ) : NavigationEvent()

        data class NavigateToPreviewScreen(
            val wallpaperModel: WallpaperModel,
            val categoryType: CategoryType,
        ) : NavigationEvent()

        data class NavigateToPhotosPicker(val wallpaperModel: WallpaperModel?) : NavigationEvent()

        data class NavigateToThirdParty(val resolveInfo: ResolveInfo) : NavigationEvent()
    }

    companion object {
        private const val TAG = "CategoriesViewModel"
    }
}
