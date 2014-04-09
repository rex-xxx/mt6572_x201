/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.mms.R;
import com.android.mms.model.AudioModel;
import com.android.mms.model.ImageModel;
import com.android.mms.model.Model;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.VideoModel;
import com.android.mms.util.ItemLoadedCallback;
import com.android.mms.util.ItemLoadedFuture;
import com.android.mms.util.ThumbnailManager.ImageLoaded;

/// M:
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.android.mms.util.ThumbnailManager;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.drm.EncapsulatedDrmManagerClient;
import com.mediatek.encapsulation.com.mediatek.internal.EncapsulatedR;
import com.mediatek.drm.OmaDrmUiUtils;


public class MmsThumbnailPresenter extends Presenter {
    private static final String TAG = "MmsThumbnailPresenter";
    private ItemLoadedCallback mOnLoadedCallback;
    private ItemLoadedFuture mItemLoadedFuture;

    /// M: Code analyze 002, new feature, personal use on device by Hongduo Wang. @{
    private Context mContext;
    private int mSlideCount = 0;
    /// @}

    public MmsThumbnailPresenter(Context context, ViewInterface view, Model model) {
        super(context, view, model);
        /// M: Code analyze 002, new feature, personal use on device by Hongduo Wang. @{
        mContext = context;
        /// @}
    }

    @Override
    public void present(ItemLoadedCallback callback) {
        mOnLoadedCallback = callback;
        SlideModel slide = ((SlideshowModel) mModel).get(0);
        /// M: Code analyze 002, new feature, personal use on device by Hongduo Wang. @{
        mSlideCount = ((SlideshowModel) mModel).size();
        /// @}
        if (slide != null) {
            MmsLog.i(TAG, "The first slide is not null.");
            presentFirstSlide((SlideViewInterface) mView, slide);
        }
    }

    private void presentFirstSlide(SlideViewInterface view, SlideModel slide) {
        view.reset();
        /// M: Code analyze 002, new feature, personal use on device by Hongduo Wang. @{
        boolean imageVisibility = true;
        /// @}
        if (slide.hasImage()) {
            MmsLog.i(TAG, "The first slide has image.");
            presentImageThumbnail(view, slide.getImage());
        } else if (slide.hasVideo()) {
            MmsLog.i(TAG, "The first slide has video.");
            presentVideoThumbnail(view, slide.getVideo());
        } else if (slide.hasAudio()) {
            MmsLog.i(TAG, "The first slide has audio.");
            presentAudioThumbnail(view, slide.getAudio());
            /// M: Code analyze 001, For fix bug ALPS00116961, shown in the
            // attachment list after replaces it with an audio in MMS.When slide
            // has audio, we should hide prvious thumbnail. @{
            imageVisibility = false;
            /// @}
        /// M: Code analyze 002, new feature, personal use on device by Hongduo Wang. @{
        } else {
            MmsLog.i(TAG, "The first slide has only text.");
            imageVisibility = false;
        }
        view.setImageVisibility(imageVisibility);
        /// @}
    }

    private ItemLoadedCallback<ImageLoaded> mImageLoadedCallback =
            new ItemLoadedCallback<ImageLoaded>() {
        public void onItemLoaded(ImageLoaded imageLoaded, Throwable exception) {
            if (exception == null) {
                /// M: google jb.mr1 pathc, remove and fully reloaded the next time
                /// When a pdu or image is canceled during loading @{
                if (mItemLoadedFuture != null) {
                    synchronized(mItemLoadedFuture) {
                        mItemLoadedFuture.setIsDone(true);
                    }
                }
                /// @}
                if (mOnLoadedCallback != null) {
                    mOnLoadedCallback.onItemLoaded(imageLoaded, exception);
                } else {
                    // Right now we're only handling image and video loaded callbacks.
                    SlideModel slide = ((SlideshowModel) mModel).get(0);
                    if (slide != null) {
                        if (slide.hasVideo() && imageLoaded.mIsVideo) {
                            /*
                             * M: It means that the thumbnail may be wrong if the thumbnail's uri is not same with the
                             * slide model's uri. But there is one case we must be consider that: after we add video,
                             * compose will save the saft, so the slide model's uri will be changed. At the same time,
                             * compose will try to get thumbnail with the old media uri which must be not start with
                             * "content://mms/part". So, when present it, the thumbnail's uri will be different with the
                             * slide model's uri. @{
                             */
                            Uri slideUri = ThumbnailManager.getThumbnailUri(slide.getVideo());
                            String thumbnailUriStr = imageLoaded.getUri().toString();
                            if (slideUri.equals(imageLoaded.getUri())) {
                                ((SlideViewInterface) mView).setVideoThumbnail(null,
                                    imageLoaded.mBitmap);
                            } else if (!thumbnailUriStr.startsWith("content://mms/part")) {
                                ((SlideViewInterface) mView).setVideoThumbnail(null,
                                    imageLoaded.mBitmap);
                            }
                            /**@}*/
                        } else if (slide.hasImage() && !imageLoaded.mIsVideo) {
                            /*
                             * M: It means that the thumbnail may be wrong if the thumbnail's uri is not same with the
                             * slide model's uri. But there is one case we must be consider that: after we add video,
                             * compose will save the saft, so the slide model's uri will be changed. At the same time,
                             * compose will try to get thumbnail with the old media uri which must be not start with
                             * "content://mms/part". So, when present it, the thumbnail's uri will be different with the
                             * slide model's uri. @{
                             */
                            Uri slideUri = ThumbnailManager.getThumbnailUri(slide.getImage());
                            String thumbnailUriStr = imageLoaded.getUri().toString();
                            if (slideUri.equals(imageLoaded.getUri())) {
                                ((SlideViewInterface) mView).setImage(null, imageLoaded.mBitmap);
                            } else if (!thumbnailUriStr.startsWith("content://mms/part")) {
                                ((SlideViewInterface) mView).setImage(null, imageLoaded.mBitmap);
                            }
                            /**@}*/
                        }
                    }
                }
            }
        }
    };

    private void presentVideoThumbnail(SlideViewInterface view, VideoModel video) {
        mItemLoadedFuture = video.loadThumbnailBitmap(mImageLoadedCallback);
    }

    private void presentImageThumbnail(SlideViewInterface view, ImageModel image) {
        /// M: Code analyze 002, new feature, personal use on device by Hongduo Wang. @{
        if (image == null) {
            MmsLog.e(TAG, "presentImageThumbnail(). iamge is null");
            return;
        }
        if (image != null) {
            MmsLog.d(TAG, "MmsThumbnailPresent. presentImageThumbnail. image src:" + image.getSrc());
        }

        if (EncapsulatedFeatureOption.MTK_DRM_APP) {
            String extName = image.getSrc().substring(image.getSrc().lastIndexOf('.') + 1);
            if (extName.equals("dcf") && mSlideCount == 1) {
                Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.ic_missing_thumbnail_picture);
                Drawable front = mContext.getResources().getDrawable(
                    EncapsulatedR.drawable.drm_red_lock);
                EncapsulatedDrmManagerClient drmManager = new EncapsulatedDrmManagerClient(mContext);
                Bitmap drmBitmap = OmaDrmUiUtils.overlayBitmap(drmManager, bitmap, front);
                view.setImage(image.getSrc(), drmBitmap);
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    bitmap = null;
                }
            } else {
                mItemLoadedFuture = image.loadThumbnailBitmap(mImageLoadedCallback);
            }
        } else {
            mItemLoadedFuture = image.loadThumbnailBitmap(mImageLoadedCallback);
        }
        /// @}
    }

    protected void presentAudioThumbnail(SlideViewInterface view, AudioModel audio) {
        view.setAudio(audio.getUri(), audio.getSrc(), audio.getExtras());
    }

    public void onModelChanged(Model model, boolean dataChanged) {
        // TODO Auto-generated method stub
    }

    public void cancelBackgroundLoading() {
        // Currently we only support background loading of thumbnails. If we extend background
        // loading to other media types, we should add a cancelLoading API to Model.
        SlideModel slide = ((SlideshowModel) mModel).get(0);
        if (slide != null && slide.hasImage()) {
            slide.getImage().cancelThumbnailLoading();
        }
    }

}
