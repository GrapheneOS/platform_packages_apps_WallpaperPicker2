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

package com.android.wallpaper.picker.customization.data.content

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Looper
import android.util.Log
import com.android.wallpaper.asset.BitmapUtils
import com.android.wallpaper.model.StaticWallpaperMetadata
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.model.wallpaper.ScreenOrientation
import com.android.wallpaper.model.wallpaper.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.module.CurrentWallpaperInfoFactory
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.UserEventLogger.SetWallpaperEntryPoint
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.customization.shared.model.WallpaperModel
import java.io.IOException
import java.util.EnumMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class WallpaperClientImpl(
    private val context: Context,
    private val infoFactory: CurrentWallpaperInfoFactory,
    private val wallpaperManager: WallpaperManager,
    private val wallpaperPreferences: WallpaperPreferences,
) : WallpaperClient {

    private var recentsContentProviderAvailable: Boolean? = null
    private val cachedRecents: MutableMap<WallpaperDestination, List<WallpaperModel>> =
        EnumMap(WallpaperDestination::class.java)

    init {
        if (areRecentsAvailable()) {
            context.contentResolver.registerContentObserver(
                LIST_RECENTS_URI,
                /* notifyForDescendants= */ true,
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        cachedRecents.clear()
                    }
                },
            )
        }
    }

    override fun recentWallpapers(
        destination: WallpaperDestination,
        limit: Int,
    ): Flow<List<WallpaperModel>> {
        return callbackFlow {
            // TODO(b/280891780) Remove this check
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw IllegalStateException("Do not call method recentWallpapers() on main thread")
            }
            suspend fun queryAndSend(limit: Int) {
                send(queryRecentWallpapers(destination = destination, limit = limit))
            }

            val contentObserver =
                if (areRecentsAvailable()) {
                        object : ContentObserver(null) {
                            override fun onChange(selfChange: Boolean) {
                                launch { queryAndSend(limit = limit) }
                            }
                        }
                    } else {
                        null
                    }
                    ?.also {
                        context.contentResolver.registerContentObserver(
                            LIST_RECENTS_URI,
                            /* notifyForDescendants= */ true,
                            it,
                        )
                    }
            queryAndSend(limit = limit)

            awaitClose {
                if (contentObserver != null) {
                    context.contentResolver.unregisterContentObserver(contentObserver)
                }
            }
        }
    }

    override suspend fun setStaticWallpaper(
        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperModel: StaticWallpaperModel,
        bitmap: Bitmap,
        cropHints: Map<ScreenOrientation, Rect>,
        onDone: () -> Unit
    ) {
        // TODO (b/309138446): Use the new multi-crop API from WallpaperManager
        val wallpaperManagerId =
            wallpaperManager.setBitmap(
                bitmap,
                cropHints[ScreenOrientation.PORTRAIT],
                true,
                destination.toFlags()
            )
        // Save wallpaper metadata in the preference for two purposes
        // 1. Quickly reconstruct the currently-selected wallpaper when opening the app
        // 2. Snapshot logging
        val bitmapHash = BitmapUtils.generateHashCode(bitmap)
        val metadata =
            StaticWallpaperMetadata(
                wallpaperModel.commonWallpaperData.attributions,
                wallpaperModel.commonWallpaperData.exploreActionUrl,
                wallpaperModel.commonWallpaperData.id.collectionId,
                bitmapHash,
                wallpaperManagerId,
                wallpaperModel.commonWallpaperData.id.uniqueId,
                // TODO (b/309139122): Introduce crop hints to StaticWallpaperMetadata
                cropHints = null,
            )
        if (destination == WallpaperDestination.HOME || destination == WallpaperDestination.BOTH) {
            wallpaperPreferences.clearHomeWallpaperMetadata()
            wallpaperPreferences.setHomeStaticImageWallpaperMetadata(metadata)
        }
        if (destination == WallpaperDestination.LOCK || destination == WallpaperDestination.BOTH) {
            wallpaperPreferences.clearLockWallpaperMetadata()
            wallpaperPreferences.setLockStaticImageWallpaperMetadata(metadata)
        }
        // Save the static wallpaper to recent wallpapers
        wallpaperPreferences.addStaticWallpaperToRecentWallpapers(
            destination,
            wallpaperModel,
            bitmap,
            // TODO (b/309139122): Introduce crop hints to recent wallpapers
            emptyMap(),
        )
    }

    override suspend fun setRecentWallpaper(
        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperId: String,
        onDone: () -> Unit,
    ) {
        val updateValues = ContentValues()
        updateValues.put(KEY_ID, wallpaperId)
        updateValues.put(KEY_SCREEN, destination.asString())
        updateValues.put(KEY_SET_WALLPAPER_ENTRY_POINT, setWallpaperEntryPoint)
        val updatedRowCount = context.contentResolver.update(SET_WALLPAPER_URI, updateValues, null)
        if (updatedRowCount == 0) {
            Log.e(TAG, "Error setting wallpaper: $wallpaperId")
        }
        onDone.invoke()
    }

    private suspend fun queryRecentWallpapers(
        destination: WallpaperDestination,
        limit: Int,
    ): List<WallpaperModel> {
        val recentWallpapers =
            cachedRecents[destination]
                ?: if (!areRecentsAvailable()) {
                    listOf(getCurrentWallpaperFromFactory(destination))
                } else {
                    queryAllRecentWallpapers(destination)
                }

        cachedRecents[destination] = recentWallpapers
        return recentWallpapers.take(limit)
    }

    private suspend fun queryAllRecentWallpapers(
        destination: WallpaperDestination
    ): List<WallpaperModel> {
        context.contentResolver
            .query(
                LIST_RECENTS_URI.buildUpon().appendPath(destination.asString()).build(),
                arrayOf(KEY_ID, KEY_PLACEHOLDER_COLOR, KEY_LAST_UPDATED),
                null,
                null,
            )
            .use { cursor ->
                if (cursor == null || cursor.count == 0) {
                    return emptyList()
                }

                return buildList {
                    val idColumnIndex = cursor.getColumnIndex(KEY_ID)
                    val placeholderColorColumnIndex = cursor.getColumnIndex(KEY_PLACEHOLDER_COLOR)
                    val lastUpdatedColumnIndex = cursor.getColumnIndex(KEY_LAST_UPDATED)
                    while (cursor.moveToNext()) {
                        val wallpaperId = cursor.getString(idColumnIndex)
                        val placeholderColor = cursor.getInt(placeholderColorColumnIndex)
                        val lastUpdated = cursor.getLong(lastUpdatedColumnIndex)
                        add(
                            WallpaperModel(
                                wallpaperId = wallpaperId,
                                placeholderColor = placeholderColor,
                                lastUpdated = lastUpdated
                            )
                        )
                    }
                }
            }
    }

    private suspend fun getCurrentWallpaperFromFactory(
        destination: WallpaperDestination
    ): WallpaperModel {
        val currentWallpapers = getCurrentWallpapers()
        val wallpaper: WallpaperInfo =
            if (destination == WallpaperDestination.LOCK) {
                currentWallpapers.second ?: currentWallpapers.first
            } else {
                currentWallpapers.first
            }
        val colors = wallpaperManager.getWallpaperColors(destination.toFlags())

        return WallpaperModel(
            wallpaper.wallpaperId,
            colors?.primaryColor?.toArgb() ?: Color.TRANSPARENT
        )
    }

    private suspend fun getCurrentWallpapers(): Pair<WallpaperInfo, WallpaperInfo?> =
        suspendCancellableCoroutine { continuation ->
            infoFactory.createCurrentWallpaperInfos(
                context,
                false,
            ) { homeWallpaper, lockWallpaper, _ ->
                continuation.resume(Pair(homeWallpaper, lockWallpaper), null)
            }
        }

    override suspend fun loadThumbnail(
        wallpaperId: String,
        destination: WallpaperDestination
    ): Bitmap? {
        if (areRecentsAvailable()) {
            try {
                // We're already using this in a suspend function, so we're okay.
                @Suppress("BlockingMethodInNonBlockingContext")
                context.contentResolver
                    .openFile(
                        GET_THUMBNAIL_BASE_URI.buildUpon()
                            .appendPath(wallpaperId)
                            .appendQueryParameter(KEY_DESTINATION, destination.asString())
                            .build(),
                        "r",
                        null,
                    )
                    .use { file ->
                        if (file == null) {
                            Log.e(TAG, "Error getting wallpaper preview: $wallpaperId")
                        } else {
                            return BitmapFactory.decodeFileDescriptor(file.fileDescriptor)
                        }
                    }
            } catch (e: IOException) {
                Log.e(TAG, "Error getting wallpaper preview: $wallpaperId", e)
            }
        } else {
            val currentWallpapers = getCurrentWallpapers()
            val wallpaper =
                if (currentWallpapers.first.wallpaperId == wallpaperId) {
                    currentWallpapers.first
                } else if (currentWallpapers.second?.wallpaperId == wallpaperId) {
                    currentWallpapers.second
                } else null
            return wallpaper?.getThumbAsset(context)?.getLowResBitmap(context)
        }

        return null
    }

    override fun areRecentsAvailable(): Boolean {
        if (recentsContentProviderAvailable == null) {
            recentsContentProviderAvailable =
                try {
                    context.packageManager.resolveContentProvider(
                        AUTHORITY,
                        0,
                    ) != null
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Exception trying to resolve recents content provider, skipping it",
                        e
                    )
                    false
                }
        }
        return recentsContentProviderAvailable == true
    }

    fun WallpaperDestination.asString(): String {
        return when (this) {
            WallpaperDestination.BOTH -> SCREEN_ALL
            WallpaperDestination.HOME -> SCREEN_HOME
            WallpaperDestination.LOCK -> SCREEN_LOCK
        }
    }

    private fun WallpaperDestination.toFlags(): Int {
        return when (this) {
            WallpaperDestination.BOTH -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
            WallpaperDestination.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperDestination.LOCK -> WallpaperManager.FLAG_LOCK
        }
    }

    companion object {
        private const val TAG = "WallpaperClientImpl"
        private const val AUTHORITY = "com.google.android.apps.wallpaper.recents"

        /** Path for making a content provider request to set the wallpaper. */
        private const val PATH_SET_WALLPAPER = "set_recent_wallpaper"
        /** Path for making a content provider request to query for the recent wallpapers. */
        private const val PATH_LIST_RECENTS = "list_recent"
        /** Path for making a content provider request to query for the thumbnail of a wallpaper. */
        private const val PATH_GET_THUMBNAIL = "thumb"

        private val BASE_URI =
            Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).build()
        /** [Uri] for making a content provider request to set the wallpaper. */
        private val SET_WALLPAPER_URI = BASE_URI.buildUpon().appendPath(PATH_SET_WALLPAPER).build()
        /** [Uri] for making a content provider request to query for the recent wallpapers. */
        private val LIST_RECENTS_URI = BASE_URI.buildUpon().appendPath(PATH_LIST_RECENTS).build()
        /**
         * [Uri] for making a content provider request to query for the thumbnail of a wallpaper.
         */
        private val GET_THUMBNAIL_BASE_URI =
            BASE_URI.buildUpon().appendPath(PATH_GET_THUMBNAIL).build()

        /** Key for a parameter used to pass the wallpaper ID to/from the content provider. */
        private const val KEY_ID = "id"
        /** Key for a parameter used to pass the screen to/from the content provider. */
        private const val KEY_SCREEN = "screen"
        /** Key for a parameter used to pass the wallpaper destination to/from content provider. */
        private const val KEY_DESTINATION = "destination"
        /** Key for a parameter used to pass the screen to/from the content provider. */
        private const val KEY_SET_WALLPAPER_ENTRY_POINT = "set_wallpaper_entry_point"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val SCREEN_ALL = "all_screens"
        private const val SCREEN_HOME = "home_screen"
        private const val SCREEN_LOCK = "lock_screen"
        /**
         * Key for a parameter used to get the placeholder color for a wallpaper from the content
         * provider.
         */
        private const val KEY_PLACEHOLDER_COLOR = "placeholder_color"
    }
}
