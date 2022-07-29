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
package com.android.wallpaper.module

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import com.android.wallpaper.adaptive.AdaptiveJobService
import java.util.concurrent.TimeUnit

/**
 * A class for schedule one-off adaptive wallpaper rotation tasks.
 */
object AdaptiveTaskScheduler {
    private const val TAG = "AdaptiveTaskScheduler"

    /**
     * Schedules a one-off task to rotate the users' adaptive wallpaper.
     *
     * @param context the context to use.
     * @param offsetMilliseconds how many milliseconds to wait before executing the one-off task.
     * @param fromBroadcastReceiver whether the task is schedule from broadcast receiver.
     */
    @JvmStatic
    fun scheduleOneOffTask(
        context: Context, offsetMilliseconds: Long,
        fromBroadcastReceiver: Boolean = false
    ) {
        cancelPendingTasks(context)
        val bundle = PersistableBundle()
        bundle.putBoolean(AdaptiveJobService.FROM_BROADCAST_RECEIVER, fromBroadcastReceiver)
        val newJob = JobInfo.Builder(
            JobSchedulerJobIds.JOB_ID_ADAPTIVE_ONEOFF,
            ComponentName(context, AdaptiveJobService::class.java)
        )
            .setMinimumLatency(offsetMilliseconds)
            .setOverrideDeadline(
                offsetMilliseconds + TimeUnit.MILLISECONDS.convert(/* sourceDuration= */ 1,
                    TimeUnit.MINUTES
                )
            )
            .setExtras(bundle)
            .setPersisted(true)
            .build()
        val status = context.getSystemService(JobScheduler::class.java).schedule(newJob)
        if (status == JobScheduler.RESULT_FAILURE) {
            Log.e(TAG, "Unable to schedule JobScheduler one-off job: $newJob")
        }
    }

    /**
     * Cancels pending periodic and one-off tasks scheduled on the BackdropTaskScheduler instance.
     */
    private fun cancelPendingTasks(context: Context) {
        context.getSystemService(JobScheduler::class.java).cancel(
            JobSchedulerJobIds.JOB_ID_ADAPTIVE_ONEOFF
        )
    }
}
