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

package com.android.wallpaper.picker.customization.domain.interactor

import androidx.test.filters.SmallTest
import com.android.wallpaper.picker.customization.data.content.FakeWallpaperClient
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.undo.shared.model.RestorableSnapshot
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
class WallpaperInteractorTest {

    private lateinit var underTest: WallpaperInteractor

    private lateinit var client: FakeWallpaperClient
    private lateinit var testScope: TestScope
    private lateinit var snapshotRestorer: WallpaperSnapshotRestorer
    private lateinit var initialSnapshot: RestorableSnapshot

    @Before
    fun setUp() {
        client = FakeWallpaperClient()

        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        underTest =
            WallpaperInteractor(
                repository =
                    WallpaperRepository(
                        scope = testScope.backgroundScope,
                        client = client,
                        backgroundDispatcher = testDispatcher,
                    ),
                snapshotRestorer = { snapshotRestorer },
            )
        snapshotRestorer =
            WallpaperSnapshotRestorer(
                interactor = underTest,
            )
        initialSnapshot = runBlocking {
            snapshotRestorer.setUpSnapshotRestorer {
                // Do nothing.
            }
        }
    }

    @Test
    fun `previews - limits to maximum results`() =
        testScope.runTest {
            val limited =
                collectLastValue(
                    underTest.previews(
                        maxResults = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.size - 1
                    )
                )

            assertThat(limited())
                .isEqualTo(
                    FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.subList(
                        0,
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.size - 1,
                    )
                )
        }

    @Test
    fun setWallpaper() =
        testScope.runTest {
            val previews =
                collectLastValue(
                    underTest.previews(
                        maxResults = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.size
                    )
                )
            val selectedWallpaperId = collectLastValue(underTest.selectedWallpaperId)
            val selectingWallpaperId = collectLastValue(underTest.selectingWallpaperId)
            assertThat(previews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectingWallpaperId()).isNull()
            val wallpaperId = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId

            // Pause the client so we can examine the interim state.
            client.pause()
            underTest.setWallpaper(wallpaperId)
            assertThat(previews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectingWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId)

            // Unpause the client so we can examine the final state.
            client.unpause()
            assertThat(previews())
                .isEqualTo(
                    listOf(
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2],
                    )
                )
            assertThat(selectedWallpaperId()).isEqualTo(wallpaperId)
            assertThat(selectingWallpaperId()).isNull()
        }

    @Test
    fun restore() =
        testScope.runTest {
            val previews =
                collectLastValue(
                    underTest.previews(
                        maxResults = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.size
                    )
                )
            val selectedWallpaperId = collectLastValue(underTest.selectedWallpaperId)
            val selectingWallpaperId = collectLastValue(underTest.selectingWallpaperId)
            assertThat(previews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectingWallpaperId()).isNull()
            val wallpaperId = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId
            underTest.setWallpaper(wallpaperId)

            // Pause the client so we can examine the interim state.
            client.pause()
            snapshotRestorer.restoreToSnapshot(initialSnapshot)
            assertThat(previews())
                .isEqualTo(
                    listOf(
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2],
                    )
                )
            assertThat(selectedWallpaperId()).isEqualTo(wallpaperId)
            assertThat(selectingWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)

            // Unpause the client so we can examine the final state.
            client.unpause()
            assertThat(previews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectingWallpaperId()).isNull()
        }
}
