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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.wallpaper.R;
import com.android.wallpaper.util.SystemColors;

/**
 * Custom layout for duo tabs.
 */
public final class DuoTabs extends FrameLayout {

    public static final int TAB_PRIMARY = 0;
    public static final int TAB_SECONDARY = 1;

    /**
     * Overlay tab
     */
    @IntDef({TAB_PRIMARY, TAB_SECONDARY})
    public @interface Tab {
    }

    /**
     * Overlay tab selected listener
     */
    public interface OnTabSelectedListener {

        /**
         * On tab selected
         */
        void onTabSelected(@Tab int tab);
    }

    private OnTabSelectedListener mOnTabSelectedListener;
    private final TextView mPrimaryTab;
    private final FrameLayout mPrimaryTabContainer;
    private final TextView mSecondaryTab;
    private final FrameLayout mSecondaryTabContainer;

    @Tab private int mCurrentOverlayTab;

    private int mSelectedTabColor;

    private int mNonSelectedTabColor;

    private int mSelectedTabTextColor;

    private int mNonSelectedTabTextColor;

    /**
     * Constructor
     */
    public DuoTabs(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DuoTabs, 0, 0);
        boolean shouldUseShortTabs = a.getBoolean(R.styleable.DuoTabs_should_use_short_tabs, false);

        mSelectedTabColor = a.getColor(R.styleable.DuoTabs_selected_tab_color,
                getResources().getColor(R.color.color_accent_primary));
        mNonSelectedTabColor = a.getColor(R.styleable.DuoTabs_non_selected_tab_color,
                getResources().getColor(R.color.color_surface));
        mSelectedTabTextColor = a.getColor(R.styleable.DuoTabs_selected_tab_text_color,
                getResources().getColor(R.color.text_color_on_accent));
        mNonSelectedTabTextColor = a.getColor(R.styleable.DuoTabs_non_selected_tab_text_color,
                SystemColors.getColor(getContext(), android.R.attr.textColorPrimary));

        a.recycle();
        LayoutInflater.from(context).inflate(
                shouldUseShortTabs ? R.layout.duo_tabs_short : R.layout.duo_tabs,
                this,
                true);

        mPrimaryTab = findViewById(R.id.tab_primary);
        mSecondaryTab = findViewById(R.id.tab_secondary);
        mPrimaryTabContainer = findViewById(R.id.tab_primary_container);
        mSecondaryTabContainer = findViewById(R.id.tab_secondary_container);

        if (mPrimaryTabContainer != null && mSecondaryTabContainer != null) {
            mPrimaryTabContainer.setOnClickListener(v -> selectTab(TAB_PRIMARY));
            mSecondaryTabContainer.setOnClickListener(v -> selectTab(TAB_SECONDARY));
        } else {
            mPrimaryTab.setOnClickListener(v -> selectTab(TAB_PRIMARY));
            mSecondaryTab.setOnClickListener(v -> selectTab(TAB_SECONDARY));
        }
    }

    /**
     * Set tab text
     */
    public void setTabText(String primaryTabText, String secondaryTabText) {
        mPrimaryTab.setText(primaryTabText);
        mSecondaryTab.setText(secondaryTabText);
    }

    /**
     * Select a tab
     */
    public void selectTab(@Tab int tab) {
        updateTabIndicator(tab);
        if (mOnTabSelectedListener != null) {
            mOnTabSelectedListener.onTabSelected(tab);
        }
        mCurrentOverlayTab = tab;
    }

    /**
     * Set listener
     */
    public void setOnTabSelectedListener(
            OnTabSelectedListener onTabSelectedListener) {
        mOnTabSelectedListener = onTabSelectedListener;
    }

    /**
     * Update the background color in case the context theme has changed.
     */
    public void updateBackgroundColor() {
        mPrimaryTab.setBackground(null);
        mSecondaryTab.setBackground(null);
        updateTabIndicator(mCurrentOverlayTab);
    }

    private void updateTabIndicator(@Tab int tab) {
        Context c = getContext();
        mPrimaryTab.setBackgroundResource(
                tab == TAB_PRIMARY
                        ? R.drawable.duo_tabs_button_indicator_background
                        : R.drawable.duo_tabs_button_background);
        mPrimaryTab.setBackgroundTintList(
                tab == TAB_PRIMARY
                        ? ColorStateList.valueOf(mSelectedTabColor)
                        : ColorStateList.valueOf(mNonSelectedTabColor));
        mPrimaryTab.setTextColor(
                tab == TAB_PRIMARY
                        ? mSelectedTabTextColor
                        : mNonSelectedTabTextColor);
        mSecondaryTab.setBackgroundResource(
                tab == TAB_SECONDARY
                        ? R.drawable.duo_tabs_button_indicator_background
                        : R.drawable.duo_tabs_button_background);
        mSecondaryTab.setBackgroundTintList(
                tab == TAB_SECONDARY
                        ? ColorStateList.valueOf(mSelectedTabColor)
                        : ColorStateList.valueOf(mNonSelectedTabColor));
        mSecondaryTab.setTextColor(
                tab == TAB_SECONDARY
                        ? mSelectedTabTextColor
                        : mNonSelectedTabTextColor);
    }

    public @Tab int getSelectedTab() {
        return mCurrentOverlayTab;
    }
}
