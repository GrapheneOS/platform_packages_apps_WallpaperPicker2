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
package com.android.wallpaper.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.android.wallpaper.R;

/**
 * Custom layout for tabs to select in between home screen overlay or lock screen overlay.
 */
public final class WallpaperControlButtonGroup extends FrameLayout {

    public static final int DOWNLOAD = 0;
    public static final int DELETE = 1;
    public static final int CUSTOMIZE = 2;
    public static final int EFFECTS = 3;
    public static final int INFORMATION = 4;

    /**
     * Overlay tab
     */
    @IntDef({DOWNLOAD, DELETE, CUSTOMIZE, EFFECTS, INFORMATION})
    public @interface WallpaperControlType {
    }

    View mDownloadButtonContainer;
    ToggleButton mDownloadButton;
    View mDownloadActionProgressBar;
    ToggleButton mDeleteButton;
    ToggleButton mCustomizeButton;
    ToggleButton mEffectsButton;
    ToggleButton mInformationButton;

    /**
     * Constructor
     */
    public WallpaperControlButtonGroup(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.wallpaper_control_button_group, this, true);
        // Download button
        mDownloadButtonContainer = findViewById(R.id.download_button_container);
        mDownloadButton = findViewById(R.id.download_button);
        mDownloadActionProgressBar = findViewById(R.id.action_download_progress);

        mDeleteButton = findViewById(R.id.delete_button);
        mCustomizeButton = findViewById(R.id.customize_button);
        mEffectsButton = findViewById(R.id.effects_button);
        mInformationButton = findViewById(R.id.information_button);
    }

    /**
     * Show a button by giving a correspondent listener
     */
    public void showButton(@WallpaperControlType int type,
            CompoundButton.OnCheckedChangeListener listener) {
        switch (type) {
            case DOWNLOAD:
                mDownloadButtonContainer.setVisibility(VISIBLE);
                mDownloadButton.setOnCheckedChangeListener(listener);
                break;
            case DELETE:
                mDeleteButton.setVisibility(VISIBLE);
                mDeleteButton.setOnCheckedChangeListener(listener);
                break;
            case CUSTOMIZE:
                mCustomizeButton.setVisibility(VISIBLE);
                mCustomizeButton.setOnCheckedChangeListener(listener);
                break;
            case EFFECTS:
                mEffectsButton.setVisibility(VISIBLE);
                mEffectsButton.setOnCheckedChangeListener(listener);
                break;
            case INFORMATION:
                mInformationButton.setVisibility(VISIBLE);
                mInformationButton.setOnCheckedChangeListener(listener);
                break;
            default:
                break;
        }
    }

    /**
     * Hide a button
     */
    public void hideButton(@WallpaperControlType int type) {
        switch (type) {
            case DOWNLOAD:
                mDownloadButtonContainer.setVisibility(GONE);
                break;
            case DELETE:
                mDeleteButton.setVisibility(GONE);
                break;
            case CUSTOMIZE:
                mCustomizeButton.setVisibility(GONE);
                break;
            case EFFECTS:
                mEffectsButton.setVisibility(GONE);
                break;
            case INFORMATION:
                mInformationButton.setVisibility(GONE);
                break;
            default:
                break;
        }
    }

    /**
     * Show the progress bar for the download button
     */
    public void showDownloadActionProgress() {
        mDownloadButton.setVisibility(GONE);
        mDownloadActionProgressBar.setVisibility(VISIBLE);
    }

    /**
     * Hide the progress bar for the download button
     */
    public void hideDownloadActionProgress() {
        mDownloadButton.setVisibility(VISIBLE);
        mDownloadActionProgressBar.setVisibility(GONE);
    }

    /**
     * Set checked for a button
     */
    public void setChecked(@WallpaperControlType int type, boolean checked) {
        switch (type) {
            case DOWNLOAD:
                mDownloadButton.setChecked(checked);
                break;
            case DELETE:
                mDeleteButton.setChecked(checked);
                break;
            case CUSTOMIZE:
                mCustomizeButton.setChecked(checked);
                break;
            case EFFECTS:
                mEffectsButton.setChecked(checked);
                break;
            case INFORMATION:
                mInformationButton.setChecked(checked);
                break;
            default:
                break;

        }
    }

    /**
     * Update the background color in case the context theme has changed.
     */
    public void updateBackgroundColor() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        mDownloadButton.setForeground(null);
        mDeleteButton.setForeground(null);
        mCustomizeButton.setForeground(null);
        mEffectsButton.setForeground(null);
        mInformationButton.setForeground(null);
        mDownloadButton.setForeground(AppCompatResources.getDrawable(context,
                R.drawable.wallpaper_control_button_download));
        mDeleteButton.setForeground(AppCompatResources.getDrawable(context,
                R.drawable.wallpaper_control_button_delete));
        mCustomizeButton.setForeground(AppCompatResources.getDrawable(context,
                R.drawable.wallpaper_control_button_customize));
        mEffectsButton.setForeground(AppCompatResources.getDrawable(context,
                R.drawable.wallpaper_control_button_effect));
        mInformationButton.setForeground(
                AppCompatResources.getDrawable(context, R.drawable.wallpaper_control_button_info));
    }
}
