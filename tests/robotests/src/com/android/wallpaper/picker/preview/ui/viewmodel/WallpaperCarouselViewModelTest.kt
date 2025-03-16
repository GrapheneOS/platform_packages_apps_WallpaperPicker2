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

package com.android.wallpaper.picker.preview.ui.viewmodel

import android.content.Context
import com.android.wallpaper.picker.category.ui.viewmodel.TileViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperCarouselViewModel
import com.android.wallpaper.testing.FakeCreativeWallpaperInteractor
import com.android.wallpaper.testing.FakeCuratedPhotosInteractorImpl
import com.android.wallpaper.testing.FakeOnDeviceWallpapersInteractor
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WallpaperCarouselViewModelTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var onDeviceWallpapersInteractor: FakeOnDeviceWallpapersInteractor
    @Inject lateinit var curatedPhotosInteractor: FakeCuratedPhotosInteractorImpl
    @Inject lateinit var creativeCategoryInteractor: FakeCreativeWallpaperInteractor
    private lateinit var underTest: WallpaperCarouselViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)

        underTest =
            WallpaperCarouselViewModel(
                context = context,
                curatedPhotosInteractor = curatedPhotosInteractor,
                creativeCategoryInteractor = creativeCategoryInteractor,
                onDeviceWallpapersInteractor = onDeviceWallpapersInteractor,
                viewModelScope = testScope,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun wallpaperCarousel_checkNotEmpty() = runTest {
        val tileViewModels =
            collectLastValue<List<TileViewModel>>(underTest.defaultWallpapersTileVieModels).invoke()
        assertThat(tileViewModels?.size)
            .isEqualTo(FakeOnDeviceWallpapersInteractor.fakeOnDeviceWallpapers.size)
    }

    @Test
    fun wallpaperCarousel_showCuratedPhotosWhenMoreThanMinimum() = runTest {
        curatedPhotosInteractor.setCategory(FakeCuratedPhotosInteractorImpl.threeCuratedPhotos)
        val tileViewModels =
            collectLastValue<List<TileViewModel>>(underTest.wallpaperCarouselItems).invoke()
        assertThat(tileViewModels?.get(0)?.text)
            .isEqualTo(FakeCuratedPhotosInteractorImpl.curatedPhotosTitle)
    }

    @Test
    fun wallpaperCarousel_showCreativesWhenMinimumAndCuratedLessThanMinimum() = runTest {
        creativeCategoryInteractor.setCreativeCategories(
            FakeCreativeWallpaperInteractor.dataListMinumumOrMore
        )
        curatedPhotosInteractor.setCategory(FakeCuratedPhotosInteractorImpl.twoCuratedPhotos)
        val tileViewModels =
            collectLastValue<List<TileViewModel>>(underTest.wallpaperCarouselItems).invoke()
        assertThat(tileViewModels?.get(0)?.text)
            .isEqualTo(
                FakeCreativeWallpaperInteractor.dataListMinumumOrMore
                    .get(0)
                    .commonCategoryData
                    .title
            )
    }

    @Test
    fun wallpaperCarousel_showDefaultsWhenCuratedAndCreativesLessThanMinimum() = runTest {
        curatedPhotosInteractor.setCategory(FakeCuratedPhotosInteractorImpl.twoCuratedPhotos)
        creativeCategoryInteractor.setCreativeCategories(
            FakeCreativeWallpaperInteractor.dataListLessThanMinimum
        )
        val tileViewModels =
            collectLastValue<List<TileViewModel>>(underTest.wallpaperCarouselItems).invoke()
        assertThat(tileViewModels?.get(0)?.text)
            .isEqualTo(
                FakeOnDeviceWallpapersInteractor.fakeOnDeviceWallpapers
                    .get(0)
                    .commonWallpaperData
                    .title
            )
    }
}
