package com.android.wallpaper.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.wallpaper.R;

import com.google.android.material.tabs.TabLayout;

/** Custom {@link TabLayout} for separated tabs. */
public class SeparatedTabLayout extends TabLayout {

    public SeparatedTabLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    @NonNull
    public Tab newTab() {
        Tab tab = super.newTab();
        tab.view.setBackgroundResource(R.drawable.separated_tabs_ripple_mask);
        return tab;
    }
}
