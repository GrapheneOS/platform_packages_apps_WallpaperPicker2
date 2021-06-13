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
package com.android.wallpaper.picker;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.android.wallpaper.R;
import com.android.wallpaper.model.HubSectionController;
import com.android.wallpaper.model.HubSectionController.HubSectionNavigationController;
import com.android.wallpaper.model.PermissionRequester;
import com.android.wallpaper.model.WallpaperColorsViewModel;
import com.android.wallpaper.model.WallpaperPreviewNavigator;
import com.android.wallpaper.model.WorkspaceViewModel;
import com.android.wallpaper.module.HubSections;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.util.ActivityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** The Fragment UI for hub sections. */
public class HubFragment extends AppbarFragment implements HubSectionNavigationController {

    private static final String TAG = "HubFragment";
    private static final String SCROLL_POSITION_Y = "SCROLL_POSITION_Y";

    // Note that the section views will be displayed by the list ordering.
    private final List<HubSectionController<?>> mHubSectionControllers = new ArrayList<>();
    private NestedScrollView mNestedScrollView;

    /**
     * Initiates HubFragment instance
     *
     * @param title The title string of the {@code HubFragment}
     * @return HubFragment The {@code HubFragment} instance
     */
    public static HubFragment newInstance(CharSequence title) {
        HubFragment fragment = new HubFragment();
        fragment.setArguments(AppbarFragment.createArguments(title));
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.collapsing_toolbar_base_layout,
                container, /* attachToRoot= */ false);
        setContentView(view, R.layout.fragment_hub_home);
        setUpToolbar(view, ActivityUtils.isLaunchedFromSettingsRelated(getActivity().getIntent()));

        ViewGroup sectionContainer = view.findViewById(R.id.section_container);
        sectionContainer.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    windowInsets.getSystemWindowInsetBottom());
            return windowInsets.consumeSystemWindowInsets();
        });
        mNestedScrollView = view.findViewById(R.id.scroll_container);

        initHubSections(savedInstanceState);
        mHubSectionControllers.forEach(controller ->
                sectionContainer.addView(controller.createView(getContext())));
        restoreViewState(savedInstanceState);
        return view;
    }

    private void setContentView(View view, int layoutResId) {
        final ViewGroup parent = view.findViewById(R.id.content_frame);
        if (parent != null) {
            parent.removeAllViews();
        }
        LayoutInflater.from(view.getContext()).inflate(layoutResId, parent);
    }

    private void restoreViewState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mNestedScrollView.post(() ->
                    mNestedScrollView.setScrollY(savedInstanceState.getInt(SCROLL_POSITION_Y)));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (mNestedScrollView != null) {
            savedInstanceState.putInt(SCROLL_POSITION_Y, mNestedScrollView.getScrollY());
        }
        mHubSectionControllers.forEach(c -> c.onSaveInstanceState(savedInstanceState));
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected int getToolbarId() {
        return R.id.action_bar;
    }

    @Override
    public void onDestroyView() {
        mHubSectionControllers.forEach(HubSectionController::release);
        mHubSectionControllers.clear();
        super.onDestroyView();
    }

    @Override
    public void navigateTo(Fragment fragment) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        fragmentManager.executePendingTransactions();
    }

    private void initHubSections(@Nullable Bundle savedInstanceState) {
        // Release and clear if any.
        mHubSectionControllers.forEach(HubSectionController::release);
        mHubSectionControllers.clear();

        WallpaperColorsViewModel wcViewModel = new ViewModelProvider(getActivity())
                .get(WallpaperColorsViewModel.class);
        WorkspaceViewModel workspaceViewModel = new ViewModelProvider(getActivity())
                .get(WorkspaceViewModel.class);

        HubSections hubSections = InjectorProvider.getInjector().getHubSections();
        List<HubSectionController<?>> allSectionControllers =
                hubSections.getAllSectionControllers(getActivity(), getViewLifecycleOwner(),
                        wcViewModel, workspaceViewModel, getPermissionRequester(),
                        getWallpaperPreviewNavigator(), this, savedInstanceState);

        mHubSectionControllers.addAll(getAvailableSections(allSectionControllers));
    }

    private List<HubSectionController<?>> getAvailableSections (
            List<HubSectionController<?>> controllers) {
        return controllers.stream()
                .filter(controller -> {
                    if(controller.isAvailable(getContext())) {
                        return true;
                    } else {
                        controller.release();
                        Log.d(TAG, "Section is not available: " + controller);
                        return false;
                    }})
                .collect(Collectors.toList());
    }

    private PermissionRequester getPermissionRequester() {
        return (PermissionRequester) getActivity();
    }

    private WallpaperPreviewNavigator getWallpaperPreviewNavigator() {
        return (WallpaperPreviewNavigator) getActivity();
    }
}
