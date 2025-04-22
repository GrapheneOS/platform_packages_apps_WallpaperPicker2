/*
 * Copyright (C) 2025 The Android Open Source Project
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
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.category.ui.binder.BannerProvider
import com.android.wallpaper.picker.category.ui.view.adapter.CuratedPhotosAdapter
import com.android.wallpaper.picker.category.ui.viewmodel.PhotosViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.data.PhotosErrorData
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper

/** This view holder is specifically for the curated photos section. */
class CuratedPhotoSectionViewHolder(itemView: View, private val windowWidth: Int) :
    RecyclerView.ViewHolder(itemView) {

    // recycler view for the tiles
    private val sectionTiles: RecyclerView = itemView.requireViewById(R.id.category_wallpaper_tiles)

    // ideally we should re-use section title for more photos label but we cannot because of the
    // different layout params and positioning of these labels
    private val sectionTitle: TextView = itemView.requireViewById(R.id.section_title)
    private val morePhotosLabel: TextView = itemView.requireViewById(R.id.more_photos_label)

    private val morePhotosButton: Button = itemView.requireViewById(R.id.more_photos_button)
    private val categoryHeader: RelativeLayout = itemView.requireViewById(R.id.category_header)
    private val snapHelper = CarouselSnapHelper()

    init {
        snapHelper.attachToRecyclerView(sectionTiles)
    }

    fun bind(
        item: PhotosViewModel,
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

            // in case of curated photos, the sectionTitle is actually mapped to morePhotosLabel
            sectionTitle.visibility = View.GONE
            if (item.sectionTitle != null) {
                morePhotosLabel.text = item.sectionTitle
            } else {
                morePhotosLabel.visibility = View.GONE
            }

            // in case there are no suggested photos or suggested photos are less than 3
            if (!item.isSuggestedPhotoCarouselVisible) {
                val layoutParams = morePhotosButton.layoutParams as RelativeLayout.LayoutParams
                layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END)
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                morePhotosButton.layoutParams = layoutParams
                morePhotosButton.text = itemView.context.getString(R.string.choose_a_photo)
                morePhotosButton.setOnClickListener { _ -> item.onSectionClicked?.invoke() }

                // showing the sign in banner in case a user is not authenticated and the banner
                // hasn't been already dismissed by the user
                if (item.status == PhotosErrorData.UNAUTHENTICATED && !isSignInBannerVisible) {
                    val viewStub = categoryHeader.findViewById<ViewStub>(R.id.sign_in_banner_id)
                    if (viewStub != null) {
                        val signInBannerView = bannerProvider?.getSignInBanner()
                        val pendingIntentForPhotos = item.pendingIntent
                        val dismissButton = bannerProvider?.getDismissButton(signInBannerView)
                        val signInButton = bannerProvider?.getSignInButton(signInBannerView)

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

                        // This is needed in order to allow activity starts using pending intent
                        // Ref:
                        // https://developer.android.com/guide/components/activities/
                        // background-starts
                        val options = ActivityOptions.makeBasic()
                        options.setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
                        )
                        val bundle = options.toBundle()

                        dismissButton?.setOnClickListener { _ ->
                            signInBannerView?.visibility = View.GONE
                            onSignInBannerDismissed(true)
                        }

                        signInButton?.setOnClickListener { _ ->
                            try {
                                pendingIntentForPhotos?.send(bundle)
                            } catch (e: PendingIntent.CanceledException) {
                                // nothing will happen in this case, so we can simply log
                                Log.e(TAG, "PendingIntent was canceled: $e")
                            }
                        }

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
                // we hide the title called suggested photos in this case
                morePhotosLabel.visibility = View.GONE
            } else {
                morePhotosLabel.visibility = View.VISIBLE
                sectionTiles.adapter = CuratedPhotosAdapter(item.tileViewModels)
                val layoutManagerCuratedPhotos = CarouselLayoutManager()
                sectionTiles.layoutManager = layoutManagerCuratedPhotos
                morePhotosButton.setOnClickListener { _ -> item.onSectionClicked?.invoke() }
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
        private const val TAG = "CuratedPhotoSectionViewHolder"
    }
}
