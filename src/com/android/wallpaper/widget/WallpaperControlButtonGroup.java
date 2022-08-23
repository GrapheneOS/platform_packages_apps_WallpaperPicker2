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

    public static final int INFORMATION = 0;
    public static final int EFFECTS = 1;

    /**
     * Overlay tab
     */
    @IntDef({INFORMATION, EFFECTS})
    public @interface WallpaperControlType {
    }

    ToggleButton mInformationButton;
    ToggleButton mEffectsButton;

    /**
     * Constructor
     */
    public WallpaperControlButtonGroup(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.wallpaper_control_button_group, this, true);
        mInformationButton = findViewById(R.id.information_button);
        mEffectsButton = findViewById(R.id.effects_button);
    }

    /**
     * Show a button by giving a correspondent listener
     */
    public void showButton(@WallpaperControlType int type,
            CompoundButton.OnCheckedChangeListener listener) {
        switch (type) {
            case INFORMATION:
                mInformationButton.setVisibility(VISIBLE);
                mInformationButton.setOnCheckedChangeListener(listener);
                break;
            case EFFECTS:
                mEffectsButton.setVisibility(VISIBLE);
                mEffectsButton.setOnCheckedChangeListener(listener);
                break;
            default:
                break;
        }
    }

    /**
     * Set checked for a button
     */
    public void setChecked(@WallpaperControlType int type, boolean checked) {
        switch (type) {
            case INFORMATION:
                mInformationButton.setChecked(checked);
                break;
            case EFFECTS:
                mEffectsButton.setChecked(checked);
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
        mInformationButton.setForeground(null);
        mEffectsButton.setForeground(null);
        mInformationButton.setForeground(
                AppCompatResources.getDrawable(context, R.drawable.wallpaper_control_button_info));
        mEffectsButton.setForeground(AppCompatResources.getDrawable(context,
                R.drawable.wallpaper_control_button_effect));
    }
}
