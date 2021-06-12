package com.android.wallpaper.module;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import com.android.wallpaper.model.HubSectionController;
import com.android.wallpaper.model.PermissionRequester;
import com.android.wallpaper.model.WallpaperColorsViewModel;
import com.android.wallpaper.model.WallpaperPreviewNavigator;
import com.android.wallpaper.model.WallpaperSectionController;
import com.android.wallpaper.model.WorkspaceViewModel;

import java.util.ArrayList;
import java.util.List;

/** {@link HubSections} for the wallpaper picker. */
public final class WallpaperPickerSections implements HubSections {

    @Override
    public List<HubSectionController<?>> getAllSectionControllers(Activity activity,
            LifecycleOwner lifecycleOwner, WallpaperColorsViewModel wallpaperColorsViewModel,
            WorkspaceViewModel workspaceViewModel, PermissionRequester permissionRequester,
            WallpaperPreviewNavigator wallpaperPreviewNavigator,
            HubSectionController.HubSectionNavigationController hubSectionNavigationController,
            @Nullable Bundle savedInstanceState) {
        List<HubSectionController<?>> sectionControllers = new ArrayList<>();

        sectionControllers.add(new WallpaperSectionController(
                activity, lifecycleOwner, permissionRequester, wallpaperColorsViewModel,
                workspaceViewModel, hubSectionNavigationController, wallpaperPreviewNavigator,
                savedInstanceState));

        return sectionControllers;
    }
}
