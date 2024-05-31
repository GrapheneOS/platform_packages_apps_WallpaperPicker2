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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Top level [ViewModel] for the categories screen */
@HiltViewModel
class CategoriesViewModel
@Inject
constructor(
// TODO: inject interacters here
) : ViewModel() {

    // this is a stub flow to mimic category data until the interactor is ready to consume
    val sections: Flow<List<SectionViewModel>> = flow {
        val tiles = generateTiles()

        val sectionsList =
            listOf(
                SectionViewModel(tiles.subList(0, 2), 3),
                SectionViewModel(tiles.subList(2, 3), 3),
                SectionViewModel(listOf(tiles[4]), 1),
                SectionViewModel(listOf(tiles[5]), 1),
                SectionViewModel(listOf(tiles[6]), 1),
                SectionViewModel(listOf(tiles[7]), 1),
                SectionViewModel(listOf(tiles[8]), 1),
                SectionViewModel(listOf(tiles[9]), 1),
            )
        // Emit the list of sections
        emit(sectionsList)
    }

    // stub data source for testing until interacter is ready to cosnume
    fun generateTiles(): List<TileViewModel> {
        return (1..10).map { TileViewModel(null, "Tile $it") }
    }
}
