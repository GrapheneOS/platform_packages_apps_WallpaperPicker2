/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.wallpaper.adaptive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.android.internal.logging.nano.MetricsProto.MetricsEvent.NOTIFICATION_ID
import com.android.wallpaper.R
import com.android.wallpaper.model.AdaptiveType
import com.android.wallpaper.module.AdaptiveTaskScheduler
import com.android.wallpaper.module.Injector
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.picker.CustomizationPickerActivity
import com.android.wallpaper.util.AdaptiveWallpaperUtils
import com.android.wallpaper.util.PermissionUtils

/**
 * A [JobService] that rotates the adaptive wallpaper.
 */
class AdaptiveJobService : JobService() {

    companion object {
        private const val TAG = "AdaptiveJobService"
        private const val ADAPTIVE_CHANNEL_ID = "Adaptive_Channel_Id"
        const val FROM_BROADCAST_RECEIVER = "FROM_BROADCAST_RECEIVER"
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val context: Context = applicationContext
        val injector = InjectorProvider.getInjector()
        val wallpaperPreferences = injector.getPreferences(context)
        if (wallpaperPreferences.wallpaperPresentationMode
            != WallpaperPreferences.PRESENTATION_MODE_ADAPTIVE
        ) {
            // The PresentationMode is not adaptive so don't do anything.
            Log.w(TAG, "Presentation mode is not PRESENTATION_MODE_ADAPTIVE, skip rotating")
            return false
        }

        if (PermissionUtils.isAccessCoarseLocationPermissionGranted(context)) {
            startForegroundService()
        }

        val location = AdaptiveWallpaperUtils.getLocation(context)
        var appliedAdaptiveType = wallpaperPreferences.appliedAdaptiveType

        // From broadcast receiver, user may change time or timezone, so we need to calculate
        // the next adaptive type.
        val nextAdaptiveType =
            if (params.extras.getBoolean(FROM_BROADCAST_RECEIVER, false))
                AdaptiveWallpaperUtils.getCurrentAdaptiveType(System.currentTimeMillis(), location)
            else appliedAdaptiveType.getNextType()

        if (nextAdaptiveType == appliedAdaptiveType) {
            // The next adaptive type is same as current type now, so we only need to
            // schedule next job.
            scheduleNextJob(location, appliedAdaptiveType.getNextType(), context)
            return false
        }

        val rotateSuccess =
            AdaptiveWallpaperUtils.rotateNextAdaptiveWallpaper(context, nextAdaptiveType)
        if (!rotateSuccess) {
            // Some error happened so clear adaptive-related data and not schedule next job.
            // TODO(b/197815029): Notify user adaptive wallpaper rotation is failed.
            Log.e(TAG, "Unable to rotate next adaptive wallpaper")
            wallpaperPreferences.clearAdaptiveData()
            return false
        }
        scheduleNextJob(location, nextAdaptiveType.getNextType(), context)
        return false
    }

    private fun scheduleNextJob(
        location: Location?, nextAdaptiveType: AdaptiveType, context: Context
    ) {
        val nextSwitchTimestamp =
            AdaptiveWallpaperUtils.getNextRotateAdaptiveWallpaperTimeByAdaptiveType(
                location,
                nextAdaptiveType
            )
        AdaptiveTaskScheduler.scheduleOneOffTask(
            context,
            nextSwitchTimestamp - System.currentTimeMillis()
        )
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // We return false here as to not reschedule this JobService for later execution because we
        // aren't cancelling the adaptive wallpaper update that was initialized in #onStartJob.
        return false
    }

    private fun startForegroundService() {
        val manager = getSystemService(NotificationManager::class.java)
        val notificationChannel =
            NotificationChannel(
                ADAPTIVE_CHANNEL_ID, getString(R.string.channel_day_and_night),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        manager.createNotificationChannel(notificationChannel)
        val notificationIntent = Intent(this, CustomizationPickerActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, ADAPTIVE_CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }
}
