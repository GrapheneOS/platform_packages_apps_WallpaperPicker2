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
package com.android.wallpaper.model;

import android.support.annotation.Nullable;

/**
 * Fetches and provides wallpaper categories to any registered {@link CategoryReceiver}s.
 */
public interface CategoryProvider {

    /**
     * Fetches the categories asynchronously; once ready, provides results to the given
     * {@link CategoryReceiver}.
     *
     * @param receiver     The receiver of categories.
     * @param forceRefresh Whether to force the CategoryProvider to refresh the categories
     *                     (as opposed to returning cached values from a prior fetch).
     */
    public void fetchCategories(CategoryReceiver receiver, boolean forceRefresh);

    /**
     * Returns the Category having the given collection ID. If not found, returns null.
     * <p>
     * This method should only be called for collection IDs for which the corresponding Category was
     * already fetched, so the null return case should be treated as an error by callers.
     */
    @Nullable
    public Category getCategory(String collectionId);
}
