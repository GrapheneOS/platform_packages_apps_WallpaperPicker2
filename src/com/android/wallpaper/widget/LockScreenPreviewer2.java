/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.app.WallpaperColors;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ReplacementSpan;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.wallpaper.R;
import com.android.wallpaper.util.ScreenSizeCalculator;
import com.android.wallpaper.util.TimeUtils;
import com.android.wallpaper.util.TimeUtils.TimeTicker;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

/** A class to load the new custom lockscreen view to the preview screen. */
public class LockScreenPreviewer2 implements LifecycleObserver {

    private static final String DEFAULT_DATE_PATTERN = "EEE, MMM d";

    private final Lifecycle mLifecycle;
    private final Context mContext;
    private final String mDatePattern;
    private final TextView mLockTime;
    private final TextView mLockDate;
    private TimeTicker mTicker;

    public LockScreenPreviewer2(Lifecycle lifecycle, Context context, ViewGroup previewContainer) {
        mLifecycle = lifecycle;
        mContext = context;
        View contentView = LayoutInflater.from(mContext).inflate(
                R.layout.lock_screen_preview2, /* root= */ null);
        mLockTime = contentView.findViewById(R.id.lock_time);
        mLockDate = contentView.findViewById(R.id.lock_date);
        mDatePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), DEFAULT_DATE_PATTERN);

        Display defaultDisplay = mContext.getSystemService(WindowManager.class).getDefaultDisplay();
        Point screenSize = ScreenSizeCalculator.getInstance().getScreenSize(defaultDisplay);

        Configuration config = mContext.getResources().getConfiguration();
        boolean directionLTR = config.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;

        View rootView = previewContainer.getRootView();
        rootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int cardHeight = previewContainer.getMeasuredHeight();
                int cardWidth = previewContainer.getMeasuredWidth();

                // Relayout the content view to match full screen size.
                contentView.measure(
                        makeMeasureSpec(screenSize.x, EXACTLY),
                        makeMeasureSpec(screenSize.y, EXACTLY));
                contentView.layout(0, 0, screenSize.x, screenSize.y);

                // Scale the content view from full screen size to the container(card) size.
                float scale = cardHeight > 0 ? (float) cardHeight / screenSize.y
                        : (float) cardWidth / screenSize.x;
                contentView.setScaleX(scale);
                contentView.setScaleY(scale);
                // The pivot point is centered by default, set to (0, 0).
                contentView.setPivotX(directionLTR ? 0f : contentView.getMeasuredWidth());
                contentView.setPivotY(0f);

                previewContainer.removeAllViews();
                previewContainer.addView(
                        contentView,
                        contentView.getMeasuredWidth(),
                        contentView.getMeasuredHeight());
                rootView.removeOnLayoutChangeListener(this);
            }
        });
        mLifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    @MainThread
    public void onResume() {
        if (mContext != null) {
            Executors.newSingleThreadExecutor().submit(() -> {
                if (mContext != null && mLifecycle.getCurrentState().isAtLeast(
                        Lifecycle.State.RESUMED)) {
                    mTicker = TimeTicker.registerNewReceiver(mContext, this::updateDateTime);
                }
            });

            updateDateTime();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    @MainThread
    public void onPause() {
        if (mContext != null && mTicker != null) {
            mContext.unregisterReceiver(mTicker);
        }
    }

    /**
     * Sets the content's color based on the wallpaper's {@link WallpaperColors}.
     *
     * @param colors the {@link WallpaperColors} of the wallpaper which the lock screen overlay
     *               will attach to, or {@code null} to use light color as default
     */
    public void setColor(@Nullable WallpaperColors colors) {
        boolean useLightTextColor = colors == null
                || (colors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0;
        int color = mContext.getColor(useLightTextColor
                ? R.color.text_color_light : R.color.text_color_dark);
        int textShadowColor = mContext.getColor(useLightTextColor
                ? R.color.smartspace_preview_shadow_color_dark
                : R.color.smartspace_preview_shadow_color_transparent);
        mLockDate.setTextColor(color);
        mLockDate.setShadowLayer(
                mContext.getResources().getDimension(
                        R.dimen.smartspace_preview_key_ambient_shadow_blur),
                /* dx = */ 0,
                /* dy = */ 0,
                textShadowColor);
    }

    public void release() {
        mLifecycle.removeObserver(this);
    }

    private void updateDateTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        mLockDate.setText(DateFormat.format(mDatePattern, calendar));
        SpannableString timeWithMonospaceText = new SpannableString(
                TimeUtils.getDoubleLineFormattedTime(mContext, calendar));
        timeWithMonospaceText.setSpan(new MonospaceSpan(), /* start= */ 0,
                timeWithMonospaceText.length(), /* flag= */ 0);
        mLockTime.setText(timeWithMonospaceText);
    }

    /** Make text monospace without overriding the text fontFamily. */
    private static class MonospaceSpan extends ReplacementSpan {

        @Override
        public int getSize(@NonNull Paint paint, @NonNull CharSequence text, int start, int end,
                @Nullable Paint.FontMetricsInt fontMetricsInt) {
            if (fontMetricsInt != null) {
                paint.getFontMetricsInt(fontMetricsInt);
            }
            int count = end - start;
            if (text.charAt(start) == '\n') {
                count--;
            }
            if (text.charAt(end - 1) == '\n') {
                count--;
            }
            return getMaxCharWidth(paint, text, /* start= */ 0, text.length())
                    * Math.max(count, 0);
        }

        @Override
        public void draw(@NonNull Canvas canvas, @NonNull CharSequence text, int start, int end,
                float x, int top, int y, int bottom, @NonNull Paint paint) {
            float[] widths = new float[end - start];
            paint.getTextWidths(text, start, end, widths);
            int maxCharWidth = getMaxCharWidth(paint, text, /* start= */ 0, text.length());
            for (int i = 0; i < end - start; ++i) {
                canvas.drawText(text, start + i, start + i + 1,
                        x + maxCharWidth * i + (maxCharWidth - widths[i]) / 2, y, paint);
            }
        }

        private int getMaxCharWidth(Paint paint, CharSequence text, int start, int end) {
            float[] widths = new float[end - start];
            paint.getTextWidths(text, start, end, widths);
            float max = 0;
            for (float w : widths) {
                if (max < w) {
                    max = w;
                }
            }
            return Math.round(max);
        }
    }
}
