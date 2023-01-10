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

import com.android.wallpaper.picker.undo.domain.interactor.SnapshotRestorer
import com.android.wallpaper.picker.undo.shared.model.RestorableSnapshot

/** Stores and restores undo snapshots for wallpaper state. */
class WallpaperSnapshotRestorer(
    private val interactor: WallpaperInteractor,
) : SnapshotRestorer {

    private lateinit var updater: (RestorableSnapshot) -> Unit

    fun storeSnapshot(
        selectedWallpaperId: String,
    ) {
        updater(snapshot(selectedWallpaperId))
    }

    override suspend fun setUpSnapshotRestorer(
        updater: (RestorableSnapshot) -> Unit,
    ): RestorableSnapshot {
        this.updater = updater
        return snapshot(interactor.selectedWallpaperId.value)
    }

    override suspend fun restoreToSnapshot(
        snapshot: RestorableSnapshot,
    ) {
        val wallpaperId = snapshot.args[SELECTED_WALLPAPER_ID]
        if (!wallpaperId.isNullOrEmpty()) {
            interactor.setWallpaper(wallpaperId = wallpaperId)
        }
    }

    private fun snapshot(selectedWallpaperId: String): RestorableSnapshot {
        return RestorableSnapshot(
            args = buildMap { put(SELECTED_WALLPAPER_ID, selectedWallpaperId) }
        )
    }

    companion object {
        private const val SELECTED_WALLPAPER_ID = "selected_wallpaper_id"
    }
}
