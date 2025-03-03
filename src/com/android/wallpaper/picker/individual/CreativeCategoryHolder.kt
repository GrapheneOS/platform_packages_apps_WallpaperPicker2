/*
 * Copyright 2023 The Android Open Source Project
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

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.WallpaperPickerDelegate
import com.android.wallpaper.picker.individual.CreativeCategoryAdapter
import com.android.wallpaper.picker.individual.MarginItemDecoration

/**
 * CreativeCategoryHolder subclass for a creative category wallpaper tile in the individual
 * wallpaper picker grid. This helps us create a different view for the creative category tiles in
 * the individual picker.
 */
class CreativeCategoryHolder(private val activity: Activity, itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    private val persister = InjectorProvider.getInjector().getWallpaperPersister(activity)

    private var recyclerViewCreativeCategory: RecyclerView =
        itemView.requireViewById(R.id.recyclerview_container)
    private var adapter: CreativeCategoryAdapter? = null

    init {
        recyclerViewCreativeCategory.layoutManager =
            LinearLayoutManager(itemView.context, RecyclerView.HORIZONTAL, false)
        recyclerViewCreativeCategory.addItemDecoration(
            MarginItemDecoration(
                activity.resources.getDimensionPixelSize(
                    R.dimen.creative_category_individual_item_view_space
                )
            )
        )
    }

    fun bind(items: List<WallpaperInfo>, height: Int) {
        if (adapter == null) {
            adapter =
                CreativeCategoryAdapter(
                        layoutInflater = LayoutInflater.from(activity),
                        tileSizePx = height,
                        onCategoryClicked = { showPreview(it) },
                    )
                    .apply { setItems(items) }
            recyclerViewCreativeCategory.adapter = adapter
        } else {
            adapter?.setItems(items)
        }
    }

    private fun showPreview(wallpaperInfo: WallpaperInfo) {
        persister.setWallpaperInfoInPreview(wallpaperInfo)
        wallpaperInfo.showPreview(
            activity,
            InjectorProvider.getInjector().getPreviewActivityIntentFactory(),
            if (wallpaperInfo is LiveWallpaperInfo)
                WallpaperPickerDelegate.PREVIEW_LIVE_WALLPAPER_REQUEST_CODE
            else WallpaperPickerDelegate.PREVIEW_WALLPAPER_REQUEST_CODE,
            true,
        )
    }
}
