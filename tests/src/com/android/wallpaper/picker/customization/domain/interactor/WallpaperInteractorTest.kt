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
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.undo.shared.model.RestorableSnapshot
import com.android.wallpaper.testing.FakeSnapshotStore
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
            snapshotRestorer.setUpSnapshotRestorer(FakeSnapshotStore())
        }
    }

    @Test
    fun `previews - limits to maximum results`() =
        testScope.runTest {
            val limited =
                collectLastValue(
                    underTest.previews(
                        destination = WallpaperDestination.HOME,
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
            val homePreviews =
                collectLastValue(
                    underTest.previews(
                        destination = WallpaperDestination.HOME,
                        maxResults = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.size
                    )
                )
            val lockPreviews =
                collectLastValue(
                    underTest.previews(
                        destination = WallpaperDestination.LOCK,
                        maxResults = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.size
                    )
                )
            val selectedHomeWallpaperId =
                collectLastValue(underTest.selectedWallpaperId(WallpaperDestination.HOME))
            val selectedLockWallpaperId =
                collectLastValue(underTest.selectedWallpaperId(WallpaperDestination.LOCK))
            val selectingHomeWallpaperId =
                collectLastValue(underTest.selectingWallpaperId(WallpaperDestination.HOME))
            val selectingLockWallpaperId =
                collectLastValue(underTest.selectingWallpaperId(WallpaperDestination.LOCK))
            assertThat(homePreviews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(lockPreviews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedLockWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectedLockWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectingHomeWallpaperId()).isNull()
            assertThat(selectingLockWallpaperId()).isNull()
            val homeWallpaperId = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId
            val lockWallpaperId = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2].wallpaperId

            // Pause the client so we can examine the interim state.
            client.pause()
            underTest.setWallpaper(WallpaperDestination.HOME, homeWallpaperId)
            underTest.setWallpaper(WallpaperDestination.LOCK, lockWallpaperId)
            assertThat(homePreviews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(lockPreviews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedHomeWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectedLockWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectingHomeWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId)
            assertThat(selectingLockWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2].wallpaperId)

            // Unpause the client so we can examine the final state.
            client.unpause()
            assertThat(homePreviews())
                .isEqualTo(
                    listOf(
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2],
                    )
                )
            assertThat(lockPreviews())
                .isEqualTo(
                    listOf(
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1],
                    )
                )
            assertThat(selectedHomeWallpaperId()).isEqualTo(homeWallpaperId)
            assertThat(selectedLockWallpaperId()).isEqualTo(lockWallpaperId)
            assertThat(selectingHomeWallpaperId()).isNull()
            assertThat(selectingLockWallpaperId()).isNull()
        }

    @Test
    fun restore() =
        testScope.runTest {
            val homePreviews =
                collectLastValue(
                    underTest.previews(
                        destination = WallpaperDestination.HOME,
                        maxResults = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.size
                    )
                )
            val lockPreviews =
                collectLastValue(
                    underTest.previews(
                        destination = WallpaperDestination.LOCK,
                        maxResults = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS.size
                    )
                )
            val selectedHomeWallpaperId =
                collectLastValue(underTest.selectedWallpaperId(WallpaperDestination.HOME))
            val selectedLockWallpaperId =
                collectLastValue(underTest.selectedWallpaperId(WallpaperDestination.LOCK))
            assertThat(homePreviews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(lockPreviews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(selectedHomeWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectedLockWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            val homeWallpaperId = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1].wallpaperId
            val lockWallpaperId = FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2].wallpaperId
            underTest.setWallpaper(WallpaperDestination.HOME, homeWallpaperId)
            underTest.setWallpaper(WallpaperDestination.LOCK, lockWallpaperId)

            // Pause the client so we can examine the interim state.
            client.pause()
            snapshotRestorer.restoreToSnapshot(initialSnapshot)
            assertThat(homePreviews())
                .isEqualTo(
                    listOf(
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2],
                    )
                )
            assertThat(lockPreviews())
                .isEqualTo(
                    listOf(
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1],
                    )
                )
            assertThat(selectedHomeWallpaperId()).isEqualTo(homeWallpaperId)
            assertThat(selectedLockWallpaperId()).isEqualTo(lockWallpaperId)

            // Unpause the client so we can examine the final state.
            client.unpause()
            assertThat(homePreviews()).isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS)
            assertThat(lockPreviews())
                .isEqualTo(
                    listOf(
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[2],
                        FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[1],
                    )
                )
            assertThat(selectedHomeWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
            assertThat(selectedLockWallpaperId())
                .isEqualTo(FakeWallpaperClient.INITIAL_RECENT_WALLPAPERS[0].wallpaperId)
        }
}
