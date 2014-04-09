/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
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

package com.mediatek.email.emailvip.activity;

import java.util.HashMap;

import com.android.email.R;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.VipMember;

import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * M: an adapter for listView, add some extra contacts information in its MatrixCursor.
 *
 */
public class VipListAdapter extends CursorAdapter {
    public static final String TAG = "VipListAdapter";
    private static final long NULL_PHOTO_ID = -1;
    private VipListFragment mVipFragment;

    /** email address -> photo id, presence */
    /* package */ static final String[] PROJECTION_PHOTO_ID_PRESENCE = new String[] {
            Contacts.PHOTO_ID,
            Contacts.CONTACT_PRESENCE
            };
    private static final int COLUMN_PHOTO_ID = 0;
    private static final int COLUMN_PRESENCE = 1;
    private final LayoutInflater mInflater;
    private Activity mActivity;
    private final AvatarLoader mAvatarLoader;
    private boolean mIsScrolling = false;

    public VipListAdapter(Context context) {
        super(context, null, 0 /* flags; no content observer */);
        mActivity = (Activity)context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAvatarLoader = new AvatarLoader(mActivity);
        mAvatarLoader.startLoading();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.email_vip_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        VipListItem item = (VipListItem)view;
        item.resetViews();
        item.setTargetActivity(mActivity);
        item.setVipFragment(mVipFragment);
        String name = cursor.getString(VipMember.DISPLAY_NAME_COLUMN);
        String address = cursor.getString(VipMember.EMAIL_ADDRESS_COLUMN);
        AvatarInfo avatarInfo = mAvatarLoader.getAvatarInfo(address, !mIsScrolling);
        if (avatarInfo != null && avatarInfo.mLookUpUri != null) {
            Uri lookupUri = Uri.parse(avatarInfo.mLookUpUri);
            item.setQuickContactLookupUri(lookupUri);
        }
        item.setVipId(cursor.getLong(VipMember.ID_PROJECTION_COLUMN));
        item.setVipName(name);
        item.setVipEmailAddress(address);
        ImageView avatar = (ImageView)item.findViewById(R.id.contact_icon);
        if (avatarInfo != null && avatarInfo.mPhotoId != NULL_PHOTO_ID) {
            item.loadContactAvatar(avatar, avatarInfo.mPhotoId);
        }
    }

    static Loader<Cursor> createMailboxesLoader(Context context, long accountId) {
        return new VipFragmentLoader(context, accountId);
    }

    /**
     * Loads vip members and add the corresponding avatar info.
     * 
     * The returned {@link Cursor} is always a {@link ClosingMatrixCursor}.
     */
    private static class VipFragmentLoader extends CursorLoader {
        private final Context mContext;
        private final long mAccountId;

        public VipFragmentLoader(Context context, long accountId) {
            super(context, VipMember.CONTENT_URI, VipMember.CONTENT_PROJECTION,
                    VipMember.SELECTION_ACCCOUNT_ID, new String[] { Long.toString(accountId)
                            }, VipMember.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
            mContext = context;
            mAccountId = accountId;
        }

        @Override
        public Cursor loadInBackground() {
            return super.loadInBackground();
        }

    }

    /**
     * Get the position of the vip with the email address.
     * This method must called from UI thread.
     * @param emailAddress the email address of the vip
     * @return the position in the vip list
     */
    int getPosition(String emailAddress) {
        if (emailAddress == null) {
            return -1;
        }
        Cursor c = getCursor();
        c.moveToPosition(-1);
        while (c.moveToNext()) {
            if (emailAddress.equalsIgnoreCase(c.getString(VipMember.EMAIL_ADDRESS_COLUMN))) {
                return c.getPosition();
            }
        }
        return -1;
    }

    public void setFragment(VipListFragment fragment) {
        mVipFragment = fragment;
    }

    public void setScrollingState(boolean isScrolling) {
        if (mIsScrolling && isScrolling != mIsScrolling) {
            notifyDataSetChanged();
        }
        mIsScrolling = isScrolling;
    }

    public void stopLoadingAvatars() {
        mAvatarLoader.stopLoading();
    }

    public void updateAvatar(String emailAddress) {
        mAvatarLoader.updateAvatar(emailAddress);
    }

    class AvatarInfo {
        long mPhotoId;
        String mLookUpUri;
    }

    /**
     * Load the avatar of vips for good performance
     */
    class AvatarLoader implements Runnable{
        private static final int AVATAR_FETCHING_LENGTH = 10;
        // update the ui if has loaded 3 avatars
        private static final int UI_UPDATE_FREQUNCY = 3;
        String[] mFetchingAvatars = new String[10];
        int mFetchingIndex = 0;
        HashMap<String, AvatarInfo> mAvatarMap = new HashMap<String, AvatarInfo>();
        Thread mLoadingThread;
        boolean mStop = false;
        Context mLoaderContext;

        AvatarLoader(Context context) {
            mLoaderContext = context;
        }

        public void updateAvatar(String emailAddress) {
            synchronized (mAvatarMap) {
                mAvatarMap.remove(emailAddress);
            }
            getAvatarInfo(emailAddress, true);
        }

        public AvatarInfo getAvatarInfo(String emailAddress, boolean load) {
            AvatarInfo Avatar = null;
            synchronized (mAvatarMap) {
                Avatar = mAvatarMap.get(emailAddress);
            }
            if (Avatar != null) {
                return Avatar;
            }
            if (load) {
                synchronized (mFetchingAvatars) {
                    mFetchingAvatars[mFetchingIndex] = emailAddress;
                    mFetchingIndex = (mFetchingIndex + 1) % AVATAR_FETCHING_LENGTH;
                    mFetchingAvatars.notify();
                }
            }
            return null;
        }

        public void startLoading() {
            if (mLoadingThread == null) {
                mStop = false;
                mLoadingThread = new Thread(this);
                mLoadingThread.start();
            }
        }

        public void stopLoading() {
            mStop = true;
            synchronized (mFetchingAvatars) {
                mFetchingAvatars.notify();
            }
            mLoadingThread = null;
        }

        @Override
        public void run() {
            int hasLoaded = 0;
            while(!mStop) {
                String emailAddress = null;
                synchronized (mFetchingAvatars) {
                    for (int i = 0; i < AVATAR_FETCHING_LENGTH; i++) {
                        emailAddress = mFetchingAvatars[i];
                        if (emailAddress != null) {
                            mFetchingAvatars[i] = null;
                            break;
                        }
                    }
                    if (emailAddress == null && !mStop && hasLoaded == 0) {
                        try {
                            mFetchingAvatars.wait();
                        } catch (InterruptedException ex) {
                            Logging.d(TAG, "AvatarLoader loading thread be interrupted");
                        }
                    }
                }
                if (mStop) {
                    return;
                }
                if (emailAddress != null) {
                    loadAvatarInfo(emailAddress);
                    hasLoaded++;
                    if (hasLoaded == UI_UPDATE_FREQUNCY) {
                        updateUi();
                        hasLoaded = 0;
                    }
                } else if (hasLoaded > 0) {
                    updateUi();
                    hasLoaded = 0;
                }
            }
        }

        private void updateUi() {
            ((Activity) mLoaderContext).runOnUiThread(new Runnable() {
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        private void loadAvatarInfo(String emailAddress) {
            // Don't deal with null
            if (emailAddress == null) {
                return;
            }

            // Don't load if avatar existed
            AvatarInfo Avatar = null;
            synchronized (mAvatarMap) {
                Avatar = mAvatarMap.get(emailAddress);
            }
            if (Avatar != null) {
                return;
            }

            // Load photo-id and lookupUri.
            long photoId = NULL_PHOTO_ID;
            Uri lookupUri = null;
            Uri uri = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress));
            Cursor c = mLoaderContext.getContentResolver().query(uri,
                    PROJECTION_PHOTO_ID_PRESENCE, null, null, null);
            try {
                if (c != null && c.moveToFirst()) {
                    photoId = c.getLong(COLUMN_PHOTO_ID);
                    lookupUri = Data.getContactLookupUri(mLoaderContext.getContentResolver(), uri);
                }
                AvatarInfo avatar = new AvatarInfo();
                avatar.mPhotoId = photoId;
                avatar.mLookUpUri = lookupUri == null ? null : lookupUri.toString();
                synchronized (mAvatarMap) {
                    mAvatarMap.put(emailAddress, avatar);
                }
            } finally {
                c.close();
            }
        }
    }
}
