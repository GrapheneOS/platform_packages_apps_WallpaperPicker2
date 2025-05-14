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
package com.android.wallpaper.module

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Performs daily logging operations when alarm is received. */
class DailyLoggingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val pendingResult: PendingResult? = goAsync() // Marks the BroadcastReceiver to keep alive

        // Create a scope for this specific onReceive call.
        val scope =
            CoroutineScope(Dispatchers.Default + Job()) // Or Dispatchers.IO for blocking I/O

        scope.launch {
            try {
                val appContext = context.applicationContext
                val injector = InjectorProvider.getInjector()
                val logger = injector.getUserEventLogger()
                val preferences = injector.getPreferences(appContext)

                logger.logSnapshot()

                preferences.setLastDailyLogTimestamp(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Error logging daily snapshot", e)
            } finally {
                Log.d(TAG, "Finishing PendingResult.")
                pendingResult?.finish() // IMPORTANT: Always call finish()
            }
        }
        Log.d(TAG, "onReceive returned, background work continues via goAsync.")
    }

    companion object {
        private const val TAG: String = "DailyLoggingAlarmReceiver"
    }
}
