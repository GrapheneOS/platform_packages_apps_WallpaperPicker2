/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.wallpaper.testing;

import android.content.Intent;

import com.android.wallpaper.module.UserEventLogger;

/**
 * Test implementation of {@link UserEventLogger}.
 */
public class TestUserEventLogger implements UserEventLogger {

    private int mNumWallpaperSetEvents;
    private int mNumWallpaperSetResultEvents;
    @WallpaperSetResult
    private int mLastWallpaperSetResult;

    public TestUserEventLogger() {
        // No-op
    }

    @Override
    public void logAppLaunched(Intent launchSource) {
        // No-op
    }

    @Override
    public void logActionClicked(String collectionId, int actionLabelResId) {
        // No-op
    }

    @Override
    public void logIndividualWallpaperSelected(String collectionId) {
        // No-op
    }

    @Override
    public void logCategorySelected(String collectionId) {
        // No-op
    }

    @Override
    public void logSnapshot() {
        // No-op
    }

    @Override
    public void logWallpaperSet(String collectionId, String wallpaperId, String effects) {
        mNumWallpaperSetEvents++;
    }

    @Override
    public void logWallpaperSetResult(@WallpaperSetResult int result) {
        mNumWallpaperSetResultEvents++;
        mLastWallpaperSetResult = result;
    }

    @Override
    public void logWallpaperSetFailureReason(@WallpaperSetFailureReason int reason) {
        // No-op
    }


    @Override
    public void logNumDailyWallpaperRotationsInLastWeek() {
        // No-op
    }

    @Override
    public void logNumDailyWallpaperRotationsPreviousDay() {
        // No-op
    }

    @Override
    public void logRefreshDailyWallpaperButtonClicked() {
        // No-op
    }

    @Override
    public void logDailyWallpaperRotationStatus(int status) {
        // No-ops
    }

    @Override
    public void logDailyWallpaperSetNextWallpaperResult(@DailyWallpaperUpdateResult int result) {
        // No-op
    }

    @Override
    public void logDailyWallpaperSetNextWallpaperCrash(@DailyWallpaperUpdateCrash int crash) {
        // No-op
    }

    @Override
    public void logNumDaysDailyRotationFailed(int days) {
        // No-op
    }

    @Override
    public void logDailyWallpaperMetadataRequestFailure(
            @DailyWallpaperMetadataFailureReason int reason) {
        // No-op
    }

    @Override
    public void logNumDaysDailyRotationNotAttempted(int days) {
        // No-op
    }

    @Override
    public void logStandalonePreviewLaunched() {
        // No-op
    }

    @Override
    public void logStandalonePreviewImageUriHasReadPermission(boolean isReadPermissionGranted) {
        // No-op
    }

    @Override
    public void logStandalonePreviewStorageDialogApproved(boolean isApproved) {
        // No-op
    }

    @Override
    public void logWallpaperPresentationMode() {
        // No-op
    }

    @Override
    public void logRestored() {
        // No-op
    }

    @Override
    public void logEffectApply(String effect, int status, long timeElapsedMillis, int resultCode) {
        // No-op
    }

    @Override
    public void logEffectProbe(String effect, @EffectStatus int status) {
        // No-op
    }

    @Override
    public void logEffectForegroundDownload(String effect, int status, long timeElapsedMillis) {
        // No-op
    }

    public int getNumWallpaperSetEvents() {
        return mNumWallpaperSetEvents;
    }

    public int getNumWallpaperSetResultEvents() {
        return mNumWallpaperSetResultEvents;
    }

    @WallpaperSetResult
    public int getLastWallpaperSetResult() {
        return mLastWallpaperSetResult;
    }
}
