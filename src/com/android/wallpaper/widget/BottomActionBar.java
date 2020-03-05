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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.wallpaper.R;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/** A {@code ViewGroup} which provides the specific actions for the user to interact with. */
public class BottomActionBar extends FrameLayout {

    /** The action items in the bottom action bar. */
    public enum BottomAction {
        CANCEL, ROTATION, INFORMATION, EDIT, APPLY,
    }

    private final Map<BottomAction, View> mActionList = new EnumMap<>(BottomAction.class);

    public BottomActionBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.bottom_actions_layout, this, true);

        mActionList.put(BottomAction.CANCEL, findViewById(R.id.action_cancel));
        mActionList.put(BottomAction.ROTATION, findViewById(R.id.action_rotation));
        mActionList.put(BottomAction.INFORMATION, findViewById(R.id.action_information));
        mActionList.put(BottomAction.EDIT, findViewById(R.id.action_edit));
        mActionList.put(BottomAction.APPLY, findViewById(R.id.action_apply));
    }

    /**
     * Sets the click listener to the specific action.
     *
     * @param bottomAction the specific action
     * @param actionClickListener the click listener for the action
     */
    public void setActionClickListener(
            BottomAction bottomAction, OnClickListener actionClickListener) {
        mActionList.get(bottomAction).setOnClickListener(actionClickListener);
    }

    /** Clears all the actions' click listeners */
    public void clearActionClickListeners() {
        mActionList.forEach((bottomAction, view) -> view.setOnClickListener(null));
    }

    /**
     * Shows the specific actions.
     *
     * @param actions the specific actions
     */
    public void showActions(EnumSet<BottomAction> actions) {
        showActions(actions, true);
    }

    /**
     * Hides the specific actions.
     *
     * @param actions the specific actions
     */
    public void hideActions(EnumSet<BottomAction> actions) {
        showActions(actions, false);
    }

    /**
     * Shows the specific actions only. In other words, the other actions will be hidden.
     *
     * @param actions the specific actions which will be shown. Others will be hidden.
     */
    public void showActionsOnly(EnumSet<BottomAction> actions) {
        showActions(actions);
        hideActions(EnumSet.complementOf(actions));
    }

    /** Enables all the actions' {@link View}. */
    public void enableActions() {
        enableActions(true);
    }

    /** Disables all the actions' {@link View}. */
    public void disableActions() {
        enableActions(false);
    }

    private void enableActions(boolean enable) {
        mActionList.forEach((bottomAction, view) -> view.setEnabled(enable));
    }

    private void showActions(EnumSet<BottomAction> actions, boolean show) {
        actions.forEach(bottomAction ->
                mActionList.get(bottomAction).setVisibility(show ? VISIBLE : GONE));
    }
}
