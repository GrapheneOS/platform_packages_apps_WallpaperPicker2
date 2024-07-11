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

package com.android.wallpaper.picker.preview.ui.viewmodel

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.activity.viewModels
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CategoriesViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var categoriesViewModel: CategoriesViewModel

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject @ApplicationContext lateinit var appContext: Context

    @Inject lateinit var testInjector: TestInjector

    @Before
    fun setUp() {
        hiltRule.inject()

        InjectorProvider.setInjector(testInjector)
        Dispatchers.setMain(testDispatcher)

        val activityInfo =
            ActivityInfo().apply {
                name = PreviewTestActivity::class.java.name
                packageName = appContext.packageName
            }
        Shadows.shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
        scenario = ActivityScenario.launch(PreviewTestActivity::class.java)
        scenario.onActivity { setEverything(it) }
    }

    private fun setEverything(activity: PreviewTestActivity) {
        categoriesViewModel = activity.viewModels<CategoriesViewModel>().value
    }

    @Test
    fun sections_verifyNumberOfSections() = runTest {
        val sections = collectLastValue(categoriesViewModel.sections)()
        assertThat(sections?.size).isEqualTo(EXPECTED_NUMBER_OF_SECTIONS)
    }

    @Test
    fun sections_verifyTilesInCreativeCategory() = runTest {
        val sections = collectLastValue(categoriesViewModel.sections)()
        val creativeSection = sections?.get(EXPECTED_POSITION_CREATIVE_CATEGORY)

        assertThat(creativeSection?.tileViewModels?.size).isEqualTo(EXPECTED_SIZE_CREATIVE_CATEGORY)

        val emojiTile = creativeSection?.tileViewModels?.get(EXPECTED_POSITION_EMOJI_TILE)
        assertThat(emojiTile?.text).isEqualTo(EXPECTED_TITLE_EMOJI_TILE)

        val aiTile = creativeSection?.tileViewModels?.get(EXPECTED_POSITION_AI_TILE)
        assertThat(aiTile?.text).isEqualTo(EXPECTED_TITLE_AI_TILE)
    }

    @Test
    fun sections_verifyTilesInMyPhotosCategory() = runTest {
        val sections = collectLastValue(categoriesViewModel.sections)()
        val myPhotosSection = sections?.get(EXPECTED_POSITION_MY_PHOTOS_CATEGORY)

        assertThat(myPhotosSection?.tileViewModels?.size)
            .isEqualTo(EXPECTED_SIZE_MY_PHOTOS_CATEGORY)

        val photoTile = myPhotosSection?.tileViewModels?.get(EXPECTED_POSITION_PHOTO_TILE)
        assertThat(photoTile?.text).isEqualTo(EXPECTED_TITLE_PHOTO_TILE)
    }

    @Test
    fun sections_verifyIndividualCategory() = runTest {
        val sections = collectLastValue(categoriesViewModel.sections)()
        val individualSections =
            sections?.subList(EXPECTED_POSITION_SINGLE_CATEGORIES, sections.size)

        assertThat(individualSections?.size).isEqualTo(EXPECTED_SIZE_SINGLE_CATEGORIES)

        // each section should only have 1 category
        individualSections?.let {
            it.forEach { sectionViewModel ->
                assertThat(sectionViewModel.tileViewModels.size)
                    .isEqualTo(EXPECTED_SIZE_SINGLE_CATEGORY_TILES)
            }
        }
    }

    @Test
    fun navigationEvents_verifyNavigateToWallpaperCollection() = runTest {
        val sections = collectLastValue(categoriesViewModel.sections)()

        val individualSections =
            sections?.subList(EXPECTED_POSITION_SINGLE_CATEGORIES, sections.size)

        individualSections?.let {
            var sectionViewModel = it[CATEGORY_INDEX_CELESTIAL_DREAMSCAPES]

            // trigger the onClick of the tile and observe that the correct navigation event is
            // emitted
            sectionViewModel.tileViewModels[0].onClicked?.let { onClick ->
                onClick()
                val navigationEvent = collectLastValue(categoriesViewModel.navigationEvents)()
                assertThat(navigationEvent)
                    .isEqualTo(
                        CategoriesViewModel.NavigationEvent.NavigateToWallpaperCollection(
                            CATEGORY_ID_CELESTIAL_DREAMSCAPES
                        )
                    )
            }

            sectionViewModel = it[CATEGORY_INDEX_CYBERPUNK_CITYSCAPE]
            sectionViewModel.tileViewModels[0].onClicked?.let { onClick ->
                onClick()
                val navigationEvent = collectLastValue(categoriesViewModel.navigationEvents)()
                assertThat(navigationEvent)
                    .isEqualTo(
                        CategoriesViewModel.NavigationEvent.NavigateToWallpaperCollection(
                            CATEGORY_ID_CYBERPUNK_CITYSCAPE
                        )
                    )
            }

            sectionViewModel = it[CATEGORY_INDEX_COSMIC_NEBULA]
            sectionViewModel.tileViewModels[0].onClicked?.let { onClick ->
                onClick()
                val navigationEvent = collectLastValue(categoriesViewModel.navigationEvents)()
                assertThat(navigationEvent)
                    .isEqualTo(
                        CategoriesViewModel.NavigationEvent.NavigateToWallpaperCollection(
                            CATEGORY_ID_COSMIC_NEBULA
                        )
                    )
            }
        }
    }

    @Test
    fun navigationEvents_verifyNavigateToMyPhotos() = runTest {
        val sections = collectLastValue(categoriesViewModel.sections)()
        val myPhotosSection = sections?.get(EXPECTED_POSITION_MY_PHOTOS_CATEGORY)

        val photoTile = myPhotosSection?.tileViewModels?.get(EXPECTED_POSITION_PHOTO_TILE)
        photoTile?.onClicked?.let { onClick ->
            onClick()
            val navigationEvent = collectLastValue(categoriesViewModel.navigationEvents)()
            assertThat(navigationEvent)
                .isEqualTo(CategoriesViewModel.NavigationEvent.NavigateToPhotosPicker)
        }
    }

    /**
     * These expected values are from fake interactors and thus would not change with device. Once
     * the corresponding real test repositories and interactors are available, these fakes will be
     * replaced with fakes of the repositories or their data sources.
     */
    companion object {
        const val EXPECTED_NUMBER_OF_SECTIONS = 19

        const val EXPECTED_POSITION_CREATIVE_CATEGORY = 0
        const val EXPECTED_SIZE_CREATIVE_CATEGORY = 2
        const val EXPECTED_POSITION_EMOJI_TILE = 0
        const val EXPECTED_POSITION_AI_TILE = 1
        const val EXPECTED_TITLE_EMOJI_TILE = "Emoji"
        const val EXPECTED_TITLE_AI_TILE = "A.I."

        const val EXPECTED_POSITION_MY_PHOTOS_CATEGORY = 1
        const val EXPECTED_SIZE_MY_PHOTOS_CATEGORY = 1
        const val EXPECTED_POSITION_PHOTO_TILE = 0
        const val EXPECTED_TITLE_PHOTO_TILE = "Celestial Dreamscape"

        const val EXPECTED_POSITION_SINGLE_CATEGORIES = 2
        const val EXPECTED_SIZE_SINGLE_CATEGORIES = 17
        const val EXPECTED_SIZE_SINGLE_CATEGORY_TILES = 1

        const val CATEGORY_ID_CELESTIAL_DREAMSCAPES = "celestial_dreamscapes"
        const val CATEGORY_ID_CYBERPUNK_CITYSCAPE = "cyberpunk_cityscape"
        const val CATEGORY_ID_COSMIC_NEBULA = "cosmic_nebula"

        const val CATEGORY_INDEX_CELESTIAL_DREAMSCAPES = 0
        const val CATEGORY_INDEX_CYBERPUNK_CITYSCAPE = 6
        const val CATEGORY_INDEX_COSMIC_NEBULA = 8
    }
}
