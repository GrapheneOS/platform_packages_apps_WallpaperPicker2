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

package com.android.wallpaper.picker.category.client

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.service.wallpaper.WallpaperService
import android.util.Log
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.InjectorProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.text.Collator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Defines methods related to handling of live wallpapers.
 */
@Singleton
class LiveWallpapersClientImpl @Inject constructor(@ApplicationContext val context: Context):
    LiveWallpapersClient {

    override fun getAll(
        excludedPackageNames: Set<String?>?
    ): List<WallpaperInfo> {
        val resolveInfos = getAllOnDevice()
        val wallpaperInfos: MutableList<WallpaperInfo> = mutableListOf()
        val factory =
            InjectorProvider.getInjector().getLiveWallpaperInfoFactory(context)

        resolveInfos.forEach { resolveInfo ->
            val wallpaperInfo: android.app.WallpaperInfo
            try {
                wallpaperInfo = android.app.WallpaperInfo(context, resolveInfo)
            } catch (e: XmlPullParserException) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e)
                return@forEach
            } catch (e: IOException) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e)
                return@forEach
            }
            if (excludedPackageNames != null
                && excludedPackageNames.contains(wallpaperInfo.packageName)) {
                return@forEach
            }
            wallpaperInfos.add(factory.getLiveWallpaperInfo(wallpaperInfo))
        }

        return wallpaperInfos
    }

    /**
     * Returns ResolveInfo objects for all live wallpaper services installed on the device. System
     * wallpapers are listed first, unsorted, with other installed wallpapers following sorted
     * in alphabetical order.
     */
    fun getAllOnDevice(): List<ResolveInfo> {
        val pm = context.packageManager
        val packageName = context.packageName

        val resolveInfos = pm.queryIntentServices(
            Intent(WallpaperService.SERVICE_INTERFACE),
            PackageManager.GET_META_DATA
        )

        val wallpaperInfos: MutableList<ResolveInfo> = mutableListOf()

        // Remove the "Rotating Image Wallpaper" live wallpaper, which is owned by this package,
        // and separate system wallpapers to sort only non-system ones.
        val iter = resolveInfos.iterator()
        while (iter.hasNext()) {
            val resolveInfo = iter.next()
            if (packageName == resolveInfo.serviceInfo.packageName) {
                iter.remove()
            } else if (isSystemApp(resolveInfo.serviceInfo.applicationInfo)) {
                wallpaperInfos.add(resolveInfo)
                iter.remove()
            }
        }

        if (resolveInfos.isEmpty()) {
            return wallpaperInfos
        }

        // Sort non-system wallpapers alphabetically and append them to system ones
        val collator = Collator.getInstance()
        resolveInfos.sortWith(compareBy(collator) { it.loadLabel(pm).toString() })

        wallpaperInfos.addAll(resolveInfos)

        return wallpaperInfos
    }

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM
                or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 }

    companion object {
        private const val TAG = "LiveWallpapersClient"
    }
}