/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcse.plugin.contacts;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.android.contacts.ext.ContactDetailExtension;
import com.mediatek.rcse.plugin.contacts.ContactExtention.Action;
import com.mediatek.rcse.plugin.contacts.ContactExtention.OnPresenceChangedListener;
import com.mediatek.rcse.service.PluginApiManager;

import java.util.Arrays;
import java.util.HashMap;

public class ContactDetailExtensionForRCS extends ContactDetailExtension {
    private static final String TAG = "ContactExtentionForRCS";
    private ContactExtention mContactPlugin;
    public static final String RCS_DISPLAY_NAME = "rcs_display_name";
    public static final String RCS_PHONE_NUMBER = "rcs_phone_number";
    private static final int RCS_ENABLED_CONTACT = 1;
    private int mRCSIconViewWidth;
    private int mRCSIconViewHeight;
    private boolean mRCSIconViewWidthAndHeightAreReady = false;
    private String mNumber;
    private String mName;
    private Activity mActivity;
    private int mIMValue;
    private int mFTValue;
    private Context mContext;
    public static final String RCS_CONTACT_PRESENCE_CHANGED = "android.intent.action.RCS_CONTACT_PRESENCE_CHANGED";
    public static final String COMMD_FOR_RCS = "ExtenstionForRCS";
    private PluginApiManager mInstance = null;

    public ContactDetailExtensionForRCS(Context context) {
        mContext = context;
        mInstance = PluginApiManager.getInstance();
        mContactPlugin = new ContactExtention(context);
    }

    public void onContactDetailOpen(Uri contactLookupUri, String commd) {
        if (!COMMD_FOR_RCS.equals(commd)) {
            Log.i(TAG, "[onContactDetailOpen]not RCSe commd " + commd);
            return;
        }
        if (mContactPlugin != null) {
            mContactPlugin.onContactDetailOpen(contactLookupUri);
        } else {
            Log.e(TAG, "[onContactDetailOpen] mContactPlutin is null");
        }
        Log.i(TAG, "[onContactDetailOpen] contactLookupUri : " + contactLookupUri);
    }

    public void setViewVisible(View view, Activity activity, String mimetype, String data,
            String displayName, String commd, int vtcall_action_view_container,
            int vertical_divider_vtcall, int vtcall_action_button,
            int secondary_action_view_container, int secondary_action_button, int vertical_divider) {
        // TODO Auto-generated method stub
        View detailView = view;
        Log.i(TAG, "[setViewVisible] commd : " + commd);
        if (mContactPlugin != null && mContactPlugin.isEnabled() && COMMD_FOR_RCS.equals(commd)) {
            if (mimetype != null && mimetype.equals(mContactPlugin.getMimeType())
                    && detailView != null) {
                View vtcallActionViewContainer = detailView
                        .findViewById(vtcall_action_view_container);
                View vewVtCallDivider = detailView.findViewById(vertical_divider_vtcall);
                ImageView btnVtCallAction = (ImageView) detailView
                        .findViewById(vtcall_action_button);
                View secondaryActionViewContainer = detailView
                        .findViewById(secondary_action_view_container);
                ImageView secondaryActionButton = (ImageView) detailView
                        .findViewById(secondary_action_button);
                View secondaryActionDivider = detailView.findViewById(vertical_divider);
                Action[] mRCSACtions = mContactPlugin.getContactActions();
                Drawable a = null;
                Drawable b = null;
                Intent intent = null;
                mName = displayName;
                mNumber = data;
                mActivity = activity;
                if (vewVtCallDivider != null && secondaryActionDivider != null
                        && btnVtCallAction != null && secondaryActionButton != null
                        && secondaryActionViewContainer != null
                        && vtcallActionViewContainer != null) {
                    // mRCSACtions cannot be null, since
                    // mContactPlugin.getContactActions() will not return null
                    a = mRCSACtions[0].icon;
                    b = mRCSACtions[1].icon;
                    intent = mRCSACtions[1].intentAction;

                    if (mIMValue == 1 && mFTValue == 1) {
                        Log.i(TAG, "setViewVisible 1");
                        vewVtCallDivider.setVisibility(View.GONE);
                        secondaryActionDivider.setVisibility(View.VISIBLE);
                        btnVtCallAction.setVisibility(View.VISIBLE);
                        secondaryActionButton.setVisibility(View.VISIBLE);
                        secondaryActionViewContainer.setVisibility(View.VISIBLE);
                        vtcallActionViewContainer.setVisibility(View.VISIBLE);
                        btnVtCallAction.setImageDrawable(a);
                        secondaryActionButton.setImageDrawable(b);
                        vtcallActionViewContainer.setClickable(false);
                        secondaryActionButton.setTag(intent);
                        secondaryActionButton.setOnClickListener(msetScondBuottononClickListner);
                    }

                    if (mIMValue == 1 && mFTValue != 1) {
                        Log.i(TAG, "setViewVisible 2");
                        vewVtCallDivider.setVisibility(View.GONE);
                        secondaryActionDivider.setVisibility(View.GONE);
                        btnVtCallAction.setVisibility(View.GONE);
                        secondaryActionButton.setVisibility(View.VISIBLE);
                        secondaryActionViewContainer.setVisibility(View.VISIBLE);

                        secondaryActionButton.setImageDrawable(a);
                        secondaryActionViewContainer.setClickable(false);
                    }

                    if (mIMValue != 1 && mFTValue == 1) {
                        Log.i(TAG, "setViewVisible 3");
                        vewVtCallDivider.setVisibility(View.GONE);
                        secondaryActionDivider.setVisibility(View.GONE);
                        btnVtCallAction.setVisibility(View.GONE);
                        secondaryActionButton.setVisibility(View.VISIBLE);
                        secondaryActionViewContainer.setVisibility(View.VISIBLE);

                        secondaryActionButton.setImageDrawable(b);
                        secondaryActionViewContainer.setClickable(false);
                    }
                } else {
                    Log.e(TAG, "[setViewVisible] vewVtCallDivider : " + vewVtCallDivider
                            + " | secondaryActionDivider : " + secondaryActionDivider
                            + " | btnVtCallAction : " + btnVtCallAction
                            + " | secondaryActionButton : " + secondaryActionButton
                            + " | secondaryActionViewContainer : " + secondaryActionViewContainer
                            + " | vtcallActionViewContainer : " + vtcallActionViewContainer);
                }

            } else {
                Log.e(TAG, "[setViewVisible] detailView or mimetype is not equals mimetype : "
                        + mimetype + " | detailView : " + detailView);
            }
        } else {
            Log.e(TAG, "[setViewVisible] mContactPlugin is null or not enabled | mContactPlugin : "
                    + mContactPlugin);
        }
    }

    public void setViewVisibleWithCharSequence(View resultView, Activity activity, String mimeType,
            String data2, CharSequence number, String commd, int vertical_divider_vtcall,
            int vtcall_action_button, int vertical_divider, int secondary_action_button, int res5,
            int res6) {

        mActivity = activity;
        mNumber = number.toString();
        String RCSMimType = null;
        Drawable a = null;
        Drawable b = null;
        Action[] mRCSAction = null;
        if (mContactPlugin != null && mContactPlugin.isEnabled() && COMMD_FOR_RCS.equals(commd)) {
            RCSMimType = mContactPlugin.getMimeType();
            mRCSAction = mContactPlugin.getContactActions();
            if (mRCSAction[0] != null && mRCSAction[1] != null) {
                a = mRCSAction[0].icon;
                b = mRCSAction[1].icon;
            } else {
                Log.e(TAG, "setViewVisibleWithCharSequence action is null");
            }

            if (mimeType != null && RCSMimType != null && !mimeType.equals(RCSMimType)) {
                return;
            }
            final View vewFirstDivider = resultView.findViewById(vertical_divider_vtcall);
            final ImageView btnFirstAction = (ImageView) resultView
                    .findViewById(vtcall_action_button);

            final View vewSecondDivider = resultView.findViewById(vertical_divider);
            final ImageView btnSecondButton = (ImageView) resultView
                    .findViewById(secondary_action_button);
            if (vewFirstDivider != null && vewSecondDivider != null) {
                Log.i(TAG, "[setViewVisibleWithCharSequence] 1");
                vewFirstDivider.setVisibility(View.GONE);
                vewSecondDivider.setVisibility(View.GONE);
            }
            if (btnFirstAction != null && btnSecondButton != null) {
                Log.i(TAG, "[setViewVisibleWithCharSequence] 2");
                btnFirstAction.setVisibility(View.GONE);
                btnSecondButton.setVisibility(View.GONE);
            }

            if (btnFirstAction != null && mIMValue == 1 && mFTValue == 1) {
                Log.i(TAG, "[setViewVisibleWithCharSequence] 3");

                vewFirstDivider.setVisibility(View.VISIBLE);
            }

            if (btnFirstAction != null && btnSecondButton != null && mIMValue == 1 && mFTValue == 1) {
                Log.i(TAG, "[setViewVisibleWithCharSequence] 4");
                btnFirstAction.setImageDrawable(a);
                btnFirstAction.setVisibility(View.VISIBLE);
                btnFirstAction.setClickable(false);
                btnSecondButton.setTag(mRCSAction[1].intentAction);

                btnSecondButton.setImageDrawable(b);
                btnSecondButton.setVisibility(View.VISIBLE);
                btnSecondButton.setOnClickListener(msetScondBuottononClickListner);
            }

            if (btnFirstAction != null && btnSecondButton != null && mIMValue == 1 && mFTValue != 1) {
                Log.i(TAG, "[setViewVisibleWithCharSequence] 5");
                btnFirstAction.setImageDrawable(a);
                btnFirstAction.setVisibility(View.VISIBLE);
                btnFirstAction.setClickable(false);

            }

            if (btnFirstAction != null && btnSecondButton != null && mIMValue != 1 && mFTValue == 1) {
                Log.i(TAG, "[setViewVisibleWithCharSequence] 5");
                btnFirstAction.setImageDrawable(b);
                btnFirstAction.setVisibility(View.VISIBLE);
                btnFirstAction.setClickable(false);

            }
        }
        Log.i(TAG, "[setViewVisibleWithCharSequence] mimeType : " + mimeType + " | RCSMimType : "
                + RCSMimType + " | mRCSAction : " + Arrays.toString(mRCSAction));

    }

    public String getExtentionMimeType(String commd) {
        String mimeType = null;
        if (mContactPlugin != null && COMMD_FOR_RCS.equals(commd)) {
            mimeType = mContactPlugin.getMimeType();
            Log.i(TAG, "getExtentionMimeType mimeType : " + mimeType);
            return mimeType;
        } else {
            Log.e(TAG, "getExtentionMimeType mContactPlugin is null ");
            return mimeType;
        }

    }

    public int layoutExtentionIcon(int leftBound, int topBound, int bottomBound, int rightBound,
            int mGapBetweenImageAndText, ImageView mExtentionIcon, String commd) {
        if (this.isVisible(mExtentionIcon) && COMMD_FOR_RCS.equals(commd)) {
            int photoTop1 = topBound + (bottomBound - topBound - mRCSIconViewHeight) / 2;
            mExtentionIcon.layout(rightBound - (mRCSIconViewWidth), photoTop1, rightBound,
                    photoTop1 + mRCSIconViewHeight);
            rightBound -= (mRCSIconViewWidth + mGapBetweenImageAndText);
        }
        return rightBound;
    }

    public void measureExtentionIcon(ImageView mRCSIcon, String commd) {

        if (isVisible(mRCSIcon) && COMMD_FOR_RCS.equals(commd)) {
            if (!mRCSIconViewWidthAndHeightAreReady) {
                if (mContactPlugin != null) {
                    Drawable a = mContactPlugin.getAppIcon();
                    if (a != null) {
                        mRCSIconViewWidth = a.getIntrinsicWidth();
                        mRCSIconViewHeight = a.getIntrinsicHeight();
                    } else {
                        mRCSIconViewWidth = 0;
                        mRCSIconViewHeight = 0;
                    }
                } else {
                    mRCSIconViewWidth = 0;
                    mRCSIconViewHeight = 0;
                }
                Log.i(TAG, "measureExtention mRCSIconViewWidth : " + mRCSIconViewWidth
                        + " | mRCSIconViewHeight : " + mRCSIconViewHeight);
                mRCSIconViewWidthAndHeightAreReady = true;
            }
        }
    }

    public Intent getExtentionIntent(int im, int ft, String commd) {
        Intent intent = null;
        mIMValue = im;
        mFTValue = ft;
        if (mContactPlugin != null && COMMD_FOR_RCS.equals(commd)) {
            Action[] actions = mContactPlugin.getContactActions();
            if (mIMValue == 1) {
                intent = actions[0].intentAction;
            } else if (mFTValue == 1) {
                intent = actions[1].intentAction;
            }
        } else {
            Log.e(TAG, "[getExtentionIntent] mContactPlugin is null");
        }
        Log.i(TAG, "[getExtentionIntent] intent : " + intent + " | im : " + im + " | ft : " + ft
                + " | commd : " + commd);
        return intent;
    }

    public boolean getExtentionKind(String mimeType, boolean needSetName, String name, String commd) {
        if (mContactPlugin != null && mContactPlugin.isEnabled() && COMMD_FOR_RCS.equals(commd)) {
            String newMimeType = mContactPlugin.getMimeType();
            Log.i(TAG, "[getExtentionKind] newMimeType : " + newMimeType);
            if (newMimeType != null && newMimeType.equals(mimeType)) {
                if (needSetName) {
                    mName = name;
                }
                return true;
            } else {
                Log.i(TAG, "[getExtentionKind] retrun kind ");
                return false;
            }
        } else {
            return false;
        }
    }

    private OnClickListener msetScondBuottononClickListner = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = (Intent) view.getTag();
            Log.e(TAG, "msetScondBuottononClickListner.onClick():intent = "
                    + intent);
            if (intent != null) {
                Log.i(TAG, "[msetScondBuottononClickListner] name : " + mName
                        + " | number : " + mNumber);
                intent.putExtra(RCS_DISPLAY_NAME, mName);
                intent.putExtra(RCS_PHONE_NUMBER, mNumber);
                mActivity.startActivity(intent);
            }

        }
    };

    protected boolean isVisible(View view) {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    public boolean checkPluginSupport(String commd) {
        if (mContactPlugin != null && COMMD_FOR_RCS.equals(commd)) {
            boolean result = mContactPlugin.isEnabled();
            Log.i(TAG, "[isEnabled] result : " + result);
            return result;
        } else {
            Log.e(TAG, "isEnabled]mContactPlugin is null");
            return false;
        }
    }

    public String getExtensionTitles(String data, String mimeType, String kind,
            HashMap<String, String> mPhoneAndSubtitle, String commd) {
        Log.i(TAG, "[getExtensionTitles] data : " + data + " | mimeType : " + mimeType
                + " | kind : " + kind + " | mPhoneAndSubtitle : " + mPhoneAndSubtitle
                + " | commd : " + commd);
        if (!COMMD_FOR_RCS.equals(commd)) {
            return kind;
        }

        if (null != data && null != mPhoneAndSubtitle) {
            if (mContactPlugin != null && mimeType != null
                    && mimeType.equals(mContactPlugin.getMimeType())) {
                String subTitle = null;
                subTitle = mPhoneAndSubtitle.get(data);
                Log.i(TAG, "[getExtensionTitles] subTitle : " + subTitle + "| data : " + data);
                return subTitle;
            } else {
                Log.e(TAG, "getExtensionTitles return null");
                return null;
            }
        } else {
            if (mContactPlugin != null && mimeType != null
                    && mimeType.equals(mContactPlugin.getMimeType())) {
                String title = mContactPlugin.getAppTitle();
                Log.i(TAG, "[getExtensionTitles] title : " + title);
                return title;
            } else {
                Log.e(TAG, "getExtensionTitles return null");
                return kind;
            }
        }

    }

    public boolean canSetExtensionIcon(long contactId, String commd) {
        Drawable icon = null;
        Log.i(TAG, "[canSetExtensionIcon] commd : " + commd);
        if (mContactPlugin != null && mContactPlugin.isEnabled() && COMMD_FOR_RCS.equals(commd)) {
            Log.i(TAG, "[canSetExtensionIcon] contactId : " + contactId);
            mContactPlugin.addOnPresenceChangedListener(new OnPresenceChangedListener() {
                public void onPresenceChanged(long contactId, int presence) {
                    // TODO Auto-generated method stub
                    Intent intent = new Intent(RCS_CONTACT_PRESENCE_CHANGED);
                    mContext.sendBroadcast(intent);
                    Log.i(TAG, "[canSetExtensionIcon] contactId : " + contactId + " | presence : "
                            + presence);
                }
            }, contactId);
            icon = mContactPlugin.getContactPresence(contactId);
            if (null != icon) {
                return true;
            } else {
                return false;
            }
        } else {
            Log.e(TAG, "setExtentionIcon mContactPlugin : " + mContactPlugin);
            return false;
        }
    }

    public void setExtensionImageView(ImageView view, long contactId, String commd) {
        Drawable icon = null;
        if (null != mContactPlugin) {
            icon = mContactPlugin.getContactPresence(contactId);
        } else {
            Log.e(TAG, "mCallLogPlugin is null");
        }
        Log.i(TAG, "[setExtentionImageView] commd : " + commd);
        if (null != view && COMMD_FOR_RCS.equals(commd)) {
            view.setImageDrawable(icon);
        }
    }

    /**
     * get the rcs-e icon on the Detail Actvitiy's action bar
     * 
     * @return if there isn't show rcs-e icon,return null.
     */
    @Override
    public Drawable getRCSIcon(long id) {
        Log.i(TAG, "[updateRCSIconWithActionBar]");
        if (null != mContactPlugin) {
            if (mInstance.getContactPresence(id) == RCS_ENABLED_CONTACT) {
                Log.e(TAG, "getRCSIcon()-is rcs-e contact");
                return mContactPlugin.getRCSIcon();
            } else {
                Log.e(TAG, "getRCSIcon()-is not rcs-e contact");
                return null;
            }
        } else {
            Log.e(TAG, "getRCSIcon()-mCallLogPlugin is null");
            return null;
        }
    }
}
