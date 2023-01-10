/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 */

package com.android.wallpaper.picker.customization.data.repository

import androidx.test.filters.SmallTest
import com.android.wallpaper.picker.customization.data.content.FakeWallpaperClient
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
class WallpaperRepositoryTest {

    private lateinit var underTest: WallpaperRepository

    private lateinit var client: FakeWallpaperClient
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        client = FakeWallpaperClient()

        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        underTest =
            WallpaperRepository(
                scope = testScope.backgroundScope,
                client = client,
                backgroundDispatcher = testDispatcher,
            )
    }

    @Test
    fun setWallpaper() =
        testScope.runTest {
            val recentWallpapers = collectLastValue(underTest.recentWallpapers(limit = 5))
            val selectedWallpaperId = collectLastValue(underTest.selectedWallpaperId)
            val selectingWallpaperId = collectLastValue(underTest.selectingWallpaperId)
            assertThat(recentWallpapers()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.first().wallpaperId)
            assertThat(selectingWallpaperId()).isNull()

            // Pause the client so we can examine the interim state.
            client.pause()
            underTest.setWallpaper(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId)
            assertThat(recentWallpapers()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.first().wallpaperId)
            assertThat(selectingWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId)

            // Unpause the client so we can examine the final state.
            client.unpause()
            assertThat(recentWallpapers())
                .isEqualTo(
                    listOf(
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2],
                    )
                )
            assertThat(selectedWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId)
            assertThat(selectingWallpaperId()).isNull()
        }
}
