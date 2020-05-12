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

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.wallpaper.R;
import com.android.wallpaper.util.SizeCalculator;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A {@code ViewGroup} which provides the specific actions for the user to interact with. */
public class BottomActionBar extends FrameLayout {

    /**
     * Interface to be implemented by an Activity hosting a {@link BottomActionBar}
     */
    public interface BottomActionBarHost {
        /** Gets {@link BottomActionBar}. */
        BottomActionBar getBottomActionBar();
    }

    /**
     * The listener for {@link BottomActionBar} visibility change notification.
     */
    public interface VisibilityChangeListener {
        /**
         * Called when {@link BottomActionBar} visibility changes.
         *
         * @param isVisible {@code true} if it's visible; {@code false} otherwise.
         */
        void onVisibilityChange(boolean isVisible);
    }

    // TODO(b/154299462): Separate downloadable related actions from WallpaperPicker.
    /** The action items in the bottom action bar. */
    public enum BottomAction {
        ROTATION, DELETE, INFORMATION, EDIT, CUSTOMIZE, DOWNLOAD, PROGRESS, APPLY,
    }

    private final Map<BottomAction, View> mActionMap = new EnumMap<>(BottomAction.class);
    private final Map<BottomAction, View> mContentViewMap = new EnumMap<>(BottomAction.class);

    private final ViewGroup mBottomSheetView;
    private final BottomSheetBehavior<ViewGroup> mBottomSheetBehavior;
    private final Set<VisibilityChangeListener> mVisibilityChangeListeners = new HashSet<>();

    /**
     * For updating the selected state of expanding bottom sheet, the corresponding action button
     * will be set to selected state.
     */
    private BottomAction mSelectedAction;

    public BottomActionBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.bottom_actions_layout, this, true);

        mActionMap.put(BottomAction.ROTATION, findViewById(R.id.action_rotation));
        mActionMap.put(BottomAction.DELETE, findViewById(R.id.action_delete));
        mActionMap.put(BottomAction.INFORMATION, findViewById(R.id.action_information));
        mActionMap.put(BottomAction.EDIT, findViewById(R.id.action_edit));
        mActionMap.put(BottomAction.CUSTOMIZE, findViewById(R.id.action_customize));
        mActionMap.put(BottomAction.DOWNLOAD, findViewById(R.id.action_download));
        mActionMap.put(BottomAction.PROGRESS, findViewById(R.id.action_progress));
        mActionMap.put(BottomAction.APPLY, findViewById(R.id.action_apply));

        mBottomSheetView = findViewById(R.id.action_bottom_sheet);
        SizeCalculator.adjustBackgroundCornerRadius(mBottomSheetView);

        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetView);
        mBottomSheetBehavior.setState(STATE_COLLAPSED);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (mSelectedAction == null) {
                    return;
                }

                if (newState == STATE_COLLAPSED) {
                    updateSelectedState(mSelectedAction, /* selected= */ false);
                    mSelectedAction = null;
                } else if (newState == STATE_EXPANDED) {
                    updateSelectedState(mSelectedAction, /* selected= */ true);
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (!isVisible) {
            mBottomSheetBehavior.setState(STATE_COLLAPSED);
        }
        mVisibilityChangeListeners.forEach(listener -> listener.onVisibilityChange(isVisible));
    }

    /**
     * Inflates a content view to the bottom sheet, and binds with a {@code BottomAction} to
     * expand/collapse the bottom sheet.
     *
     * @param contentLayoutId the layout res to be inflected on the bottom sheet
     * @param contentViewId the view id of the inflated content view
     * @param action the action button to be bound to expand/collapse the bottom sheet
     * @return the view of {@param contentViewId}
     */
    public View inflateViewToBottomSheetAndBindAction(
            @LayoutRes int contentLayoutId, @IdRes int contentViewId, BottomAction action) {
        View contentView = LayoutInflater
                .from(getContext())
                .inflate(contentLayoutId, mBottomSheetView)
                .findViewById(contentViewId);
        contentView.setVisibility(GONE);
        mContentViewMap.put(action, contentView);

        setActionClickListener(action, unused -> {
            mContentViewMap.forEach((a, v) -> v.setVisibility(a.equals(action) ? VISIBLE : GONE));
            updateSelectedAction(action);
        });

        return contentView;
    }

    private void updateSelectedAction(BottomAction action) {
        BottomAction previousSelectedButton = mSelectedAction;
        mSelectedAction = action;
        // If the bottom sheet is expanding with a highlight button, then clicking another
        // action button to show bottom sheet will only update the content for expanding bottom
        // sheet, and update the highlight button.
        if (previousSelectedButton != null && !action.equals(previousSelectedButton)) {
            updateSelectedState(previousSelectedButton, /* selected= */ false);
            updateSelectedState(mSelectedAction, /* selected= */ true);
            return;
        }
        mBottomSheetBehavior.setState(mBottomSheetBehavior.getState() == STATE_COLLAPSED
                ? STATE_EXPANDED
                : STATE_COLLAPSED);
    }

    /**
     * Adds content view to the bottom sheet and binds with a {@code BottomAction} to
     * expand / collapse the bottom sheet.
     *
     * @param contentView the view with content to be added on the bottom sheet
     * @param action the action to be bound to expand / collapse the bottom sheet
     */
    public void attachViewToBottomSheetAndBindAction(View contentView, BottomAction action) {
        mContentViewMap.put(action, contentView);
        mBottomSheetView.addView(contentView);
        setActionClickListener(action, unused -> {
            mContentViewMap.forEach((a, v) -> v.setVisibility(a.equals(action) ? VISIBLE : GONE));
            updateSelectedAction(action);
        });
    }

    /**
     * Sets the click listener to the specific action.
     *
     * @param bottomAction the specific action
     * @param actionClickListener the click listener for the action
     */
    public void setActionClickListener(
            BottomAction bottomAction, OnClickListener actionClickListener) {
        View buttonView = mActionMap.get(bottomAction);
        if (buttonView.hasOnClickListeners()) {
            throw new IllegalStateException(
                    "Had already set a click listener to button: " + bottomAction);
        }
        buttonView.setOnClickListener(view -> {
            updateSelectedState(bottomAction, !view.isSelected());
            actionClickListener.onClick(view);
        });
    }

    /** Binds the cancel button to back key. */
    public void bindBackButtonToSystemBackKey(Activity activity) {
        findViewById(R.id.action_back).setOnClickListener(v -> activity.onBackPressed());
    }

    /** Returns {@code true} if visible. */
    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    /** Shows {@link BottomActionBar}. */
    public void show() {
        setVisibility(VISIBLE);
    }

    /** Hides {@link BottomActionBar}. */
    public void hide() {
        setVisibility(GONE);
    }

    /**
     * Adds the visibility change listener.
     *
     * @param visibilityChangeListener the listener to be notified.
     */
    public void addVisibilityChangeListener(VisibilityChangeListener visibilityChangeListener) {
        if (visibilityChangeListener == null) {
            return;
        }
        mVisibilityChangeListeners.add(visibilityChangeListener);
        visibilityChangeListener.onVisibilityChange(isVisible());
    }

    /**
     * Shows the specific actions.
     *
     * @param actions the specific actions
     */
    public void showActions(BottomAction... actions) {
        for (BottomAction action : actions) {
            mActionMap.get(action).setVisibility(VISIBLE);
        }
    }

    /**
     * Hides the specific actions.
     *
     * @param actions the specific actions
     */
    public void hideActions(BottomAction... actions) {
        for (BottomAction action : actions) {
            mActionMap.get(action).setVisibility(GONE);

            if (action.equals(mSelectedAction)) {
                mBottomSheetBehavior.setState(STATE_COLLAPSED);
            }
        }
    }

    /**
     * Shows the specific actions only. In other words, the other actions will be hidden.
     *
     * @param actions the specific actions which will be shown. Others will be hidden.
     */
    public void showActionsOnly(BottomAction... actions) {
        final Set<BottomAction> actionsSet = new HashSet<>(Arrays.asList(actions));

        mActionMap.forEach((action, view) -> {
            if (actionsSet.contains(action)) {
                showActions(action);
            } else {
                hideActions(action);
            }
        });
    }

    /**
     * All actions will be hidden.
     */
    public void hideAllActions() {
        showActionsOnly(/* No actions to show */);
    }

    /** Enables all the actions' {@link View}. */
    public void enableActions() {
        enableActions(BottomAction.values());
    }

    /** Disables all the actions' {@link View}. */
    public void disableActions() {
        disableActions(BottomAction.values());
    }

    /**
     * Enables specified actions' {@link View}.
     *
     * @param actions the specified actions to enable their views
     */
    public void enableActions(BottomAction... actions) {
        for (BottomAction action : actions) {
            mActionMap.get(action).setEnabled(true);
        }
    }

    /**
     * Disables specified actions' {@link View}.
     *
     * @param actions the specified actions to disable their views
     */
    public void disableActions(BottomAction... actions) {
        for (BottomAction action : actions) {
            mActionMap.get(action).setEnabled(false);
        }
    }

    public boolean isActionSelected(BottomAction action) {
        return mActionMap.get(action).isSelected();
    }

    /** Resets {@link BottomActionBar}. */
    public void reset() {
        hide();
        hideAllActions();
        clearActionClickListeners();
        enableActions();
        mActionMap.forEach(
                (action, view) -> updateSelectedState(action, /* selected= */ false));
        mBottomSheetView.removeAllViews();
        mContentViewMap.clear();
    }

    /** Clears all the actions' click listeners */
    private void clearActionClickListeners() {
        mActionMap.forEach((bottomAction, view) -> view.setOnClickListener(null));
        findViewById(R.id.action_back).setOnClickListener(null);
    }

    private void updateSelectedState(BottomAction action, boolean selected) {
        mActionMap.get(action).setSelected(selected);
    }
}
