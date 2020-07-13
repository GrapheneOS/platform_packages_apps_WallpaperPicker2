/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.WallpaperColors;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.wallpaper.R;
import com.android.wallpaper.util.TimeTicker;

import java.text.FieldPosition;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * An updater to update the lock screen overlay's content and layout.
 */
public class LockScreenOverlayUpdater implements LifecycleObserver {

    private static final String DEFAULT_DATE_PATTERN = "EEE, MMM d";

    private Context mContext;
    private String mDatePattern;
    private TimeTicker mTicker;
    private ImageView mLockIcon;
    private TextView mLockTime;
    private TextView mLockDate;

    public LockScreenOverlayUpdater(Context context, View lockScreenOverlay, Lifecycle lifecycle) {
        mContext = context;
        mLockIcon = lockScreenOverlay.findViewById(R.id.lock_icon);
        mLockTime = lockScreenOverlay.findViewById(R.id.lock_time);
        mLockDate = lockScreenOverlay.findViewById(R.id.lock_date);
        mDatePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), DEFAULT_DATE_PATTERN);
        lifecycle.addObserver(this);
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    @MainThread
    public void onResume() {
        mTicker = TimeTicker.registerNewReceiver(mContext, this::updateDateTime);
        updateDateTime();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    @MainThread
    public void onPause() {
        if (mContext != null) {
            mContext.unregisterReceiver(mTicker);
        }
    }

    /**
     * Adjusts the overlay's layout according to full screen preview or small preview.
     */
    public void adjustOverlayLayout(boolean isFullScreen) {
        Resources resources = mContext.getResources();
        int lockIconSize = resources.getDimensionPixelSize(isFullScreen
                ? R.dimen.lock_screen_full_preview_lock_icon_size
                : R.dimen.lock_screen_preview_lock_icon_size);
        setLockIconSize(lockIconSize, lockIconSize);
        setClockFontSize(resources.getDimensionPixelSize(isFullScreen
                ? R.dimen.lock_screen_full_preview_time_text_size
                : R.dimen.lock_screen_preview_time_text_size));
        setDateFontSize(resources.getDimensionPixelSize(isFullScreen
                ? R.dimen.lock_screen_full_preview_date_text_size
                : R.dimen.lock_screen_preview_date_text_size));
    }

    /**
     * Sets the content's color based on the wallpaper's {@link WallpaperColors}.
     *
     * @param colors the {@link WallpaperColors} of the wallpaper which the lock screen overlay
     *               will attach to
     */
    public void setColor(WallpaperColors colors) {
        int color = mContext.getColor(
                (colors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0
                        ? R.color.text_color_light
                        : R.color.text_color_dark);
        mLockIcon.setImageTintList(ColorStateList.valueOf(color));
        mLockDate.setTextColor(color);
        mLockTime.setTextColor(color);
    }

    /**
     * Sets the size of the lock icon.
     *
     * @param width  the width of the lock icon
     * @param height the height of the lock icon
     */
    public void setLockIconSize(int width, int height) {
        mLockIcon.getLayoutParams().width = width;
        mLockIcon.getLayoutParams().height = height;
    }

    /**
     * Sets the font size of the clock.
     *
     * @param fontSize the font size of the clock
     */
    public void setClockFontSize(int fontSize) {
        mLockTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
    }

    /**
     * Sets the font size of the date.
     *
     * @param fontSize the font size of the date
     */
    public void setDateFontSize(int fontSize) {
        mLockDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
    }

    private void updateDateTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        StringBuffer time = new StringBuffer();
        FieldPosition amPmPosition = new FieldPosition(java.text.DateFormat.Field.AM_PM);
        DateFormat.getTimeFormat(mContext).format(calendar.getTime(), time, amPmPosition);
        if (amPmPosition.getBeginIndex() > 0) {
            time.delete(amPmPosition.getBeginIndex(), amPmPosition.getEndIndex());
        }
        CharSequence date = DateFormat.format(mDatePattern, calendar);
        mLockTime.setText(time);
        mLockDate.setText(date);
    }
}
