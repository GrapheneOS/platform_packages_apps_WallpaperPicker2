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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewStub
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.category.ui.binder.BannerProvider
import com.android.wallpaper.picker.category.ui.view.adapter.CategoryAdapter
import com.android.wallpaper.picker.category.ui.view.adapter.CuratedPhotosAdapter
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

// TODO (b/409841415): Decouple this view holder from suggested photos logic
/** This view holder caches reference to pertinent views in a list of section view */
class CategorySectionViewHolder(itemView: View, private val windowWidth: Int) :
    RecyclerView.ViewHolder(itemView) {

    // recycler view for the tiles
    private val sectionTiles: RecyclerView = itemView.requireViewById(R.id.category_wallpaper_tiles)
    // title for the section
    private val sectionTitle: TextView = itemView.requireViewById(R.id.section_title)
    private val morePhotosButton: Button = itemView.requireViewById(R.id.more_photos_button)
    private val categoryHeader: RelativeLayout = itemView.requireViewById(R.id.category_header)

    private val morePhotosLabel: TextView = itemView.requireViewById(R.id.more_photos_label)

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

            // setting the icon color of the button
            ColorUpdateBinder.bind(
                setColor = { color ->
                    TextViewCompat.setCompoundDrawableTintList(
                        morePhotosButton,
                        ColorStateList.valueOf(color),
                    )
                },
                color = colorUpdateViewModel.colorOnPrimary,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )

            // setting the text color of the button
            ColorUpdateBinder.bind(
                setColor = { color -> morePhotosButton.setTextColor(color) },
                color = colorUpdateViewModel.colorOnPrimary,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )

            // setting background of the button
            ColorUpdateBinder.bind(
                setColor = { color ->
                    DrawableCompat.setTint(DrawableCompat.wrap(morePhotosButton.background), color)
                },
                color = colorUpdateViewModel.colorPrimary,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        }

        if (item.sectionTitle != null) {
            sectionTitle.text = item.sectionTitle
            morePhotosLabel.text = item.sectionTitle
            sectionTitle.visibility = View.VISIBLE
            morePhotosLabel.visibility = View.VISIBLE
        } else {
            sectionTitle.visibility = View.GONE
            morePhotosLabel.visibility = View.GONE
        }

        // TODO: this probably is not necessary but if in the case the sections get updated we
        //  should just update the adapter instead of instantiating a new instance
        when (item) {
            // This is the display type for suggested photos carousel
            is PhotosViewModel -> {
                sectionTitle.visibility = View.GONE
                // in case there are no suggested photos or suggested photos are less than 3
                if (!item.isSuggestedPhotoCarouselVisible) {
                    val signInBannerView = bannerProvider?.getSignInBanner()
                    val layoutParams = morePhotosButton.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END)
                    layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    morePhotosButton.layoutParams = layoutParams
                    morePhotosButton.text = itemView.context.getString(R.string.choose_a_photo)
                    morePhotosButton.setOnClickListener { _ -> item.onSectionClicked?.invoke() }
                    val pendingIntentForPhotos = item.pendingIntent
                    val dismissButton = bannerProvider?.getDismissButton(signInBannerView)
                    val signInButton = bannerProvider?.getSignInButton(signInBannerView)

                    if (item.status == PhotosErrorData.UNAUTHENTICATED && !isSignInBannerVisible) {
                        val viewStub = categoryHeader.findViewById<ViewStub>(R.id.sign_in_banner_id)
                        if (viewStub != null) {
                            val viewStubLayoutParams = viewStub.layoutParams
                            val index = categoryHeader.indexOfChild(viewStub)
                            categoryHeader.removeView(viewStub)
                            signInBannerView?.layoutParams = viewStubLayoutParams
                            categoryHeader.addView(signInBannerView, index)

                            val bannerTitle = bannerProvider?.getBannerTitle(signInBannerView)
                            val bannerDescription =
                                bannerProvider?.getBannerDescription(signInBannerView)
                            val photoIcon = bannerProvider?.getIcon(signInBannerView)
                            dismissButton?.setBackgroundColor(Color.TRANSPARENT)

                            // setting background for the overall sign in banner
                            ColorUpdateBinder.bind(
                                setColor = { color ->
                                    signInBannerView
                                        ?.background
                                        ?.let { DrawableCompat.wrap(it) }
                                        ?.let { DrawableCompat.setTint(it, color) }
                                },
                                color = colorUpdateViewModel.colorSurfaceContainerHigh,
                                shouldAnimate = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )

                            // setting text color of the banner title
                            ColorUpdateBinder.bind(
                                setColor = { color -> bannerTitle?.setTextColor(color) },
                                color = colorUpdateViewModel.colorOnSurfaceVariant,
                                shouldAnimate = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )

                            // setting text color of the banner description
                            ColorUpdateBinder.bind(
                                setColor = { color -> bannerDescription?.setTextColor(color) },
                                color = colorUpdateViewModel.colorOnSurfaceVariant,
                                shouldAnimate = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )

                            // setting color of the icon itself
                            ColorUpdateBinder.bind(
                                setColor = { color -> photoIcon?.setColorFilter(color) },
                                color = colorUpdateViewModel.colorOnPrimary,
                                shouldAnimate = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )

                            // setting background of the photos icon
                            ColorUpdateBinder.bind(
                                setColor = { color ->
                                    photoIcon
                                        ?.background
                                        ?.let { DrawableCompat.wrap(it) }
                                        ?.let { DrawableCompat.setTint(it, color) }
                                },
                                color = colorUpdateViewModel.colorPrimary,
                                shouldAnimate = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )

                            // setting background for the sign in Button
                            ColorUpdateBinder.bind(
                                setColor = { color ->
                                    signInButton
                                        ?.background
                                        ?.let { DrawableCompat.wrap(it) }
                                        ?.let { DrawableCompat.setTint(it, color) }
                                },
                                color = colorUpdateViewModel.colorPrimary,
                                shouldAnimate = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )

                            // setting text color for the dismiss Button
                            ColorUpdateBinder.bind(
                                setColor = { color -> dismissButton?.setTextColor(color) },
                                color = colorUpdateViewModel.colorPrimary,
                                shouldAnimate = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )

                            // setting text color for the sign in Button
                            ColorUpdateBinder.bind(
                                setColor = { color -> signInButton?.setTextColor(color) },
                                color = colorUpdateViewModel.colorOnPrimary,
                                shouldAnimate = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )
                        }
                    }

                    // This is needed in order to allow activity starts using pending intent
                    // Ref:
                    // https://developer.android.com/guide/components/activities/background-starts
                    val options = ActivityOptions.makeBasic()
                    options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
                    )
                    val bundle = options.toBundle()

                    dismissButton?.setOnClickListener { _ ->
                        signInBannerView?.visibility = View.GONE
                        onSignInBannerDismissed(true)
                    }

                    signInButton?.setOnClickListener({ _ ->
                        try {
                            pendingIntentForPhotos?.send(bundle)
                        } catch (e: PendingIntent.CanceledException) {
                            // nothing will happen in this case, so we can simply log
                            Log.e(TAG, "PendingIntent was canceled: $e")
                        }
                    })
                    // we hide the title called suggested photos in this case
                    morePhotosLabel.visibility = View.GONE
                } else {
                    morePhotosLabel.visibility = View.VISIBLE
                    sectionTiles.adapter = CuratedPhotosAdapter(item.tileViewModels)
                    val layoutManagerCuratedPhotos = CarouselLayoutManager()
                    sectionTiles.layoutManager = layoutManagerCuratedPhotos
                    val snapHelper = CarouselSnapHelper()
                    snapHelper.attachToRecyclerView(sectionTiles)
                    morePhotosButton.setOnClickListener { _ -> item.onSectionClicked?.invoke() }
                }
            }
            else -> {
                morePhotosLabel.visibility = View.GONE
                morePhotosButton.visibility = View.GONE

                if (item.tileViewModels.isEmpty()) {
                    sectionTiles.isVisible = false
                    categoryHeader.visibility = View.GONE
                    sectionTitle.visibility = View.GONE
                    return
                } else {
                    sectionTiles.isVisible = true
                    categoryHeader.visibility = View.VISIBLE
                }

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
