/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.wallpaper.module;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.wallpaper.compat.WallpaperManagerCompat;
import com.android.wallpaper.effects.EffectsController;
import com.android.wallpaper.effects.EffectsController.EffectsServiceListener;
import com.android.wallpaper.model.CategoryProvider;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.monitor.PerformanceMonitor;
import com.android.wallpaper.network.Requester;
import com.android.wallpaper.picker.PreviewFragment.PreviewMode;
import com.android.wallpaper.picker.individual.IndividualPickerFragment;
import com.android.wallpaper.util.DisplayUtils;

/**
 * Interface for a provider of "injected dependencies." (NOTE: The term "injector" is somewhat of a
 * misnomer; this is more aptly a service registry as part of a service locator design pattern.)
 */
public interface Injector {
    /**
     * Get {@link AlarmManagerWrapper}
     */
    AlarmManagerWrapper getAlarmManagerWrapper(Context context);

    /**
     * Get {@link BitmapCropper}
     */
    BitmapCropper getBitmapCropper();

    /**
     * Get {@link CategoryProvider}
     */
    CategoryProvider getCategoryProvider(Context context);

    /**
     * Get {@link CurrentWallpaperInfoFactory}
     */
    CurrentWallpaperInfoFactory getCurrentWallpaperInfoFactory(Context context);

    /**
     * Get {@link CustomizationSections}
     */
    CustomizationSections getCustomizationSections();

    /**
     * Get {@link Intent} for a deep link
     */
    Intent getDeepLinkRedirectIntent(Context context, Uri uri);

    /**
     * Get {@link DisplayUtils}
     */
    DisplayUtils getDisplayUtils(Context context);

    /**
     * Get {@link DisplayUtils}
     */
    String getDownloadableIntentAction();

    /**
     * Get {@link DrawableLayerResolver}
     */
    DrawableLayerResolver getDrawableLayerResolver();

    /**
     * Get {@link EffectsController}
     */
    @Nullable
    EffectsController getEffectsController(Context context, EffectsServiceListener listener);

    /**
     * Get {@link ExploreIntentChecker}
     */
    ExploreIntentChecker getExploreIntentChecker(Context context);

    /**
     * Get {@link IndividualPickerFragment}
     */
    IndividualPickerFragment getIndividualPickerFragment(String collectionId);

    /**
     * Get {@link LiveWallpaperInfoFactory}
     */
    LiveWallpaperInfoFactory getLiveWallpaperInfoFactory(Context context);

    /**
     * Get {@link NetworkStatusNotifier}
     */
    NetworkStatusNotifier getNetworkStatusNotifier(Context context);

    /**
     * Get {@link PackageStatusNotifier}
     */
    PackageStatusNotifier getPackageStatusNotifier(Context context);

    /**
     * Get {@link PartnerProvider}
     */
    PartnerProvider getPartnerProvider(Context context);

    /**
     * Get {@link PerformanceMonitor}
     */
    PerformanceMonitor getPerformanceMonitor();

    /**
     * Get {@link Fragment} for previewing the wallpaper
     * TODO b/242908637 Remove this method when migrating to the new wallpaper preview screen
     */
    Fragment getPreviewFragment(
            Context context,
            WallpaperInfo wallpaperInfo,
            @PreviewMode int mode,
            boolean viewAsHome,
            boolean viewFullScreen,
            boolean testingModeEnabled);

    /**
     * Get {@link Requester}
     */
    Requester getRequester(Context context);

    /**
     * Get {@link SystemFeatureChecker}
     */
    SystemFeatureChecker getSystemFeatureChecker();

    /**
     * Get {@link UserEventLogger}
     */
    UserEventLogger getUserEventLogger(Context context);

    /**
     * Get {@link WallpaperManagerCompat}
     */
    WallpaperManagerCompat getWallpaperManagerCompat(Context context);

    /**
     * Get {@link WallpaperPersister}
     */
    WallpaperPersister getWallpaperPersister(Context context);

    /**
     * Get {@link WallpaperPreferences}
     */
    WallpaperPreferences getPreferences(Context context);

    /**
     * Get {@link WallpaperPreviewFragmentManager}
     */
    WallpaperPreviewFragmentManager getWallpaperPreviewFragmentManager();

    /**
     * Get {@link WallpaperRefresher}
     */
    WallpaperRefresher getWallpaperRefresher(Context context);

    /**
     * Get {@link WallpaperRotationRefresher}
     */
    WallpaperRotationRefresher getWallpaperRotationRefresher();

    /**
     * Get {@link WallpaperStatusChecker}
     */
    WallpaperStatusChecker getWallpaperStatusChecker();
}
