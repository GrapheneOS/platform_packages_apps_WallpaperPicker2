/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.picker.category.ui.view.viewholder

import android.app.ActivityOptions
import android.app.PendingIntent
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewStub
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.category.ui.binder.BannerProvider
import com.android.wallpaper.picker.category.ui.view.adapter.CategoryAdapter
import com.android.wallpaper.picker.category.ui.view.adapter.CuratedPhotosAdapter
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.picker.category.ui.viewmodel.PhotosViewModel
import com.android.wallpaper.picker.category.ui.viewmodel.SectionViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.data.PhotosErrorData
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper

/** This view holder caches reference to pertinent views in a list of section view */
class CategorySectionViewHolder(itemView: View, private val windowWidth: Int) :
    RecyclerView.ViewHolder(itemView) {

    // recycler view for the tiles
    private val sectionTiles: RecyclerView = itemView.requireViewById(R.id.category_wallpaper_tiles)
    // title for the section
    private val sectionTitle: TextView = itemView.requireViewById(R.id.section_title)
    private val morePhotosButton: Button = itemView.requireViewById(R.id.more_photos_button)
    private val categoryHeader: RelativeLayout = itemView.requireViewById(R.id.category_header)

    fun bind(
        item: SectionViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
        bannerProvider: BannerProvider?,
        isSignInBannerVisible: Boolean,
        onSignInBannerDismissed: (dismissed: Boolean) -> Unit? = {},
    ) {
        val isNewPickerUi = BaseFlags.get().isNewPickerUi()
        if (isNewPickerUi) {
            ColorUpdateBinder.bind(
                setColor = { color -> sectionTitle.setTextColor(color) },
                color = colorUpdateViewModel.colorOnSurface,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        }

        // TODO: this probably is not necessary but if in the case the sections get updated we
        //  should just update the adapter instead of instantiating a new instance
        when (item.displayType) {
            // This is the display type for suggested photos carousel
            CategoriesViewModel.DisplayType.Carousel -> {
                sectionTiles.adapter = CuratedPhotosAdapter(item.tileViewModels)
                val layoutManagerCuratedPhotos = CarouselLayoutManager()
                sectionTiles.layoutManager = layoutManagerCuratedPhotos
                val snapHelper = CarouselSnapHelper()

                // in case there are no suggested photos
                if (item.tileViewModels.isEmpty()) {
                    val signInBannerView = bannerProvider?.getSignInBanner()
                    val layoutParams = morePhotosButton.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END)
                    layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    morePhotosButton.layoutParams = layoutParams
                    val pendingIntentForPhotos = (item as PhotosViewModel).pendingIntent

                    if (item.status == PhotosErrorData.UNAUTHENTICATED && !isSignInBannerVisible) {
                        val viewStub = categoryHeader.findViewById<ViewStub>(R.id.sign_in_banner_id)
                        val viewStubLayoutParams = viewStub.layoutParams
                        val index = categoryHeader.indexOfChild(viewStub)
                        categoryHeader.removeView(viewStub)
                        signInBannerView?.layoutParams = viewStubLayoutParams
                        categoryHeader.addView(signInBannerView, index)
                    }

                    // This is needed in order to allow activity starts using pending intent
                    // Ref:
                    // https://developer.android.com/guide/components/activities/background-starts
                    val options = ActivityOptions.makeBasic()
                    options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
                    )
                    val bundle = options.toBundle()

                    val dismissButton: Button? = signInBannerView?.findViewById(R.id.dismiss_button)
                    val signInButton: Button? = signInBannerView?.findViewById(R.id.sign_in_button)

                    dismissButton?.setOnClickListener({ _ ->
                        signInBannerView.visibility = View.GONE
                        onSignInBannerDismissed(true)
                    })

                    signInButton?.setOnClickListener({ _ ->
                        try {
                            pendingIntentForPhotos?.send(bundle)
                        } catch (e: PendingIntent.CanceledException) {
                            // nothing will happen in this case, so we can simply log
                            Log.e(TAG, "PendingIntent was canceled: $e")
                        }
                    })
                }
                snapHelper.attachToRecyclerView(sectionTiles)
                morePhotosButton.setOnClickListener { _ -> item.onSectionClicked?.invoke() }
            }
            else -> {
                morePhotosButton.visibility = View.GONE
                sectionTiles.adapter =
                    CategoryAdapter(
                        item.tileViewModels,
                        item.columnCount,
                        windowWidth,
                        colorUpdateViewModel,
                        shouldAnimateColor,
                        lifecycleOwner,
                    )

                val layoutManager = FlexboxLayoutManager(itemView.context)

                // Horizontal orientation
                layoutManager.flexDirection = FlexDirection.ROW

                // disable wrapping to make sure everything fits on a single row
                layoutManager.flexWrap = FlexWrap.NOWRAP

                // Stretch items to fill the horizontal axis
                layoutManager.alignItems = AlignItems.STRETCH

                // Distribute items evenly on the horizontal axis
                layoutManager.justifyContent = JustifyContent.SPACE_AROUND

                sectionTiles.layoutManager = layoutManager

                val itemDecoration =
                    HorizontalSpaceItemDecoration(
                        itemView.context.resources
                            .getDimension(R.dimen.creative_category_grid_padding_horizontal)
                            .toInt()
                    )
                sectionTiles.addItemDecoration(itemDecoration)
            }
        }

        if (item.sectionTitle != null && item.tileViewModels.isNotEmpty()) {
            sectionTitle.text = item.sectionTitle
            sectionTitle.visibility = View.VISIBLE
        } else {
            sectionTitle.visibility = View.GONE
        }
    }

    class HorizontalSpaceItemDecoration(private val horizontalSpace: Int) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.left = horizontalSpace
            }
        }
    }

    companion object {
        private const val TAG = "CategorySectionViewHolder"
    }
}
