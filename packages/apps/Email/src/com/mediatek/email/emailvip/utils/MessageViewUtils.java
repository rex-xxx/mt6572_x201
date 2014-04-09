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

package com.mediatek.email.emailvip.utils;

import java.util.ArrayList;

import android.R.integer;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.android.email.R;
import com.android.email.activity.ContactStatusLoader;
import com.android.email.activity.MessageCompose;
import com.android.email.activity.MessageViewFragmentBase;
import com.android.email.activity.UiUtilities;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.VipMember;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.VipMember.AddVipsCallback;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.Utility;
import com.mediatek.email.emailvip.VipMemberCache;
import com.mediatek.email.emailvip.activity.VipListFragment;
import com.mediatek.email.emailvip.utils.EllipsizeTextView.OnDrawnListener;

/**
 * This class is used for the UI of VIP new feature in MessageView.
 */
public class MessageViewUtils {
    private static final int CONTACT_STATUS_STATE_UNLOADED = 0;
    private static final int CONTACT_STATUS_STATE_UNLOADED_TRIGGERED = 1;
    private static final int CONTACT_STATUS_STATE_LOADED = 2;
    private static int mWindowWidth;
    private static boolean mIsTwoPaneUi;
    
    /**
     * Convert the @param address to SpnnableString based
     * on the state @param selected.
     *
     * @param context
     * @param address Convert this address to SpannableString.
     * @param selected
     *            If true, set vip icon to ic_email_vip_holo_dark,
     *            if false, set to ic_email_vip.
     * @return
     */
    public static SpannableString getSpannableString(Context context, TempAddress address, boolean selected) {
        if (address == null) {
            return new SpannableString("");
        }
        String personal = address.getPersonal();
        if (personal == null) {
            return new SpannableString("");
        }
        if (!address.mIsVipMember) {
            return new SpannableString(personal);
        }
        int resId = selected ? R.drawable.ic_email_vip_holo_dark : R.drawable.ic_email_vip;
        ImageSpan imageSpan = new ImageSpan(context, resId,
                DynamicDrawableSpan.ALIGN_BASELINE);
        SpannableString spannableString = new SpannableString(" " + personal);
        spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    /**
     * Get all addresses of one message.
     */
    public static TempAddress[] getAllAddresses(Message message) {
        if (message == null) {
            return new TempAddress[0];
        }
        TempAddress[] toAddresses = TempAddress.unpack(message.mTo, TempAddress.FLAG_TO_LAYOUT);
        TempAddress[] ccAddresses = TempAddress.unpack(message.mCc, TempAddress.FLAG_CC_LAYOUT);
        TempAddress[] bccAddresses = TempAddress.unpack(message.mBcc, TempAddress.FLAG_BCC_LAYOUT);
        TempAddress[] allAddresses = new TempAddress[toAddresses.length + ccAddresses.length
                + bccAddresses.length];
        System.arraycopy(toAddresses, 0, allAddresses, 0, toAddresses.length);
        System.arraycopy(ccAddresses, 0, allAddresses, toAddresses.length, ccAddresses.length);
        System.arraycopy(bccAddresses, 0, allAddresses, toAddresses.length + ccAddresses.length,
                bccAddresses.length);
        return allAddresses;
    }

    /**
     *
     * @param fragment {@link MessageViewFragmentBase}
     * @param addresses The addresses needed by {@link VipTextView}
     * @param layout The container of VipTextViews.
     * @param start The start position of addresses.
     * @param end The end position of addresses.
     */
    public static void setDetailsLayout(MessageViewFragmentBase fragment, TempAddress[] addresses,
            LinearLayout[] layout, int start, int end) {
        if (addresses.length == 0) {
            return;
        }
        if (end > addresses.length) {
            end = addresses.length;
        }
        Context context = fragment.getActivity();
        int padding = context.getResources().getDimensionPixelSize(R.dimen.vip_text_view_padding);
        int black = context.getResources().getColor(android.R.color.black);
        ImageSpan imageSpan = new ImageSpan(context, R.drawable.ic_email_vip,
                DynamicDrawableSpan.ALIGN_BASELINE);
        SpannableString spannableString = null;
        VipOnClickListener onClickListener = new VipOnClickListener(fragment, getPopupWindow(context));
        VipTextView textView = null;
        LinearLayout parent = null;
        for (int i = start; i < end; i++) {
            textView = new VipTextView(context);
            textView.setAdress(addresses[i]);
            textView.setBackgroundResource(R.drawable.chip_background);
            textView.setPadding(padding);
            textView.setTextColor(black);
            textView.setSingleLine();
            if (VipMemberCache.isVIP(addresses[i].getAddress())) {
                addresses[i].setVip(true);
                spannableString = new SpannableString(" " + addresses[i].getPersonal());
                spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(spannableString);
            } else {
                textView.setText(addresses[i].getPersonal());
            }
            textView.setOnClickListener(onClickListener);
            parent = layout[addresses[i].mParentLayoutFlag - 1];
            parent.addView(textView, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            // Add a blank view as a divider, it's height is 9 pixels,
            // this value is arbitrary.
            parent.addView(new View(context), new LayoutParams(LayoutParams.MATCH_PARENT, 9));
        }
    }

    /**
     * Show this popupWindow when click one {@link VipTextView}.
     */
    public static PopupWindow getPopupWindow(Context context) {
        Resources resources = context.getResources();
        LinearLayout view = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.mtk_vip_pop_up_window, null);
        
        mIsTwoPaneUi = UiUtilities.useTwoPane(context);
        mWindowWidth = resources.getDisplayMetrics().widthPixels;
        PopupWindow popupWindow = new PopupWindow(view, mWindowWidth,
                resources.getDimensionPixelSize(R.dimen.vip_pop_up_height));
        popupWindow.setBackgroundDrawable(resources.getDrawable(R.drawable.gradient_bg_email_widget_holo));
        popupWindow.setFocusable(true);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        return popupWindow;
    }

    /**
     * Update the UI state of mAddressesView inside MessageView.
     * @param message The corresponding {@link Message}.
     * @param context {@link MessageViewFragmentBase}
     * @param addressView The mAddressView in MessageViewFragmentBase.
     */
    public static void updateAddressesView(Message message, Context context, TextView addressView) {
        if (message == null || addressView == null) {
            return;
        }
        // To/Cc/Bcc
        final Resources res = context.getResources();
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        TempAddress[] allAddresses = MessageViewUtils.getAllAddresses(message);
        int length = allAddresses.length;
        if (length == 0 ) {
            addressView.setText("");
            return;
        }
        // Because addressView is set to single line,
        // just convert the previous 10 {@link Address} to SpannableString.
        int position = (length < 10 ? length : 10);
        boolean appendToLabel = false;
        boolean appendCcLabel = false;
        boolean appendBccLabel = false;
        for (int i = 0; i < position; i++) {
            TempAddress tempAddress = allAddresses[i];
            tempAddress.setVip(VipMemberCache.isVIP(tempAddress.getAddress()));
            switch (tempAddress.mParentLayoutFlag) {
            case TempAddress.FLAG_TO_LAYOUT:
                if (!appendToLabel) {
                    Utility.appendBold(ssb, res.getString(R.string.message_view_to_label));
                    ssb.append(" ");
                    appendToLabel = true;
                }
                break;
            case TempAddress.FLAG_CC_LAYOUT:
                if (!appendCcLabel) {
                    removeLastComma(ssb);
                    ssb.append(" ");
                    Utility.appendBold(ssb, res.getString(R.string.message_view_cc_label));
                    ssb.append(" ");
                    appendCcLabel = true;
                }
                break;
            case TempAddress.FLAG_BCC_LAYOUT:
                if (!appendBccLabel) {
                    removeLastComma(ssb);
                    ssb.append(" ");
                    Utility.appendBold(ssb, res.getString(R.string.message_view_bcc_label));
                    ssb.append(" ");
                    appendBccLabel = true;
                }
                default:
                    break;
            }

            ssb.append(MessageViewUtils.getSpannableString(context, tempAddress, false));
            ssb.append(", ");
        }
        final SpannableStringBuilder spannableString = removeLastComma(ssb);
        // When we firstly call the method getWidth() of TextView, the width returned will be 0,
        // because, at this moment, this TextView is not drawn.
        // For get the view width ASAP, please look up the {@link EllipsizeTextView}.
        if (addressView.getWidth() != 0) {
            setAddressView(addressView, spannableString);
        } else {
            if (addressView instanceof EllipsizeTextView) {
                final EllipsizeTextView etView = (EllipsizeTextView) addressView;
                etView.setOnDrawnListener(new EllipsizeTextView.OnDrawnListener() {
                    public void onDrawn() {
                        setAddressView(etView, spannableString);
                        etView.removeOnDrawnListener();
                    }
                });
            } else {
                addressView.setText(spannableString, BufferType.SPANNABLE);
            }
        }
    }

    /**
     * Ellipsize the @param spannableString and set it to @param textView.
     */
    public static void setAddressView(TextView textView, SpannableStringBuilder spannableString) {
        int width = textView.getWidth();
        CharSequence charSequence = TextUtils.ellipsize(spannableString, textView.getPaint(),
                width, TruncateAt.END);
        textView.setText(charSequence);
    }

    /**
     * If the last two characters in @param ssb are ", ", remove this two chars.
     */
    public static SpannableStringBuilder removeLastComma(SpannableStringBuilder ssb) {
        if (ssb == null) {
            return new SpannableStringBuilder();
        }
        int length = ssb.length();
        if (length > 2 && ssb.charAt(length -2) == ',') {
            ssb.delete(length -2, length);
        }
        return ssb;
    }

    public static void updateVipTextView(Context context, LinearLayout layout, TempAddress singleAddress) {
        int childCount = layout.getChildCount();
        if (childCount == 0) {
            return;
        }
        View view = null;
        VipTextView vipTextView = null;
        TempAddress tempAddress = null;
        for (int i = 0; i < childCount; i++) {
            view = null;
            vipTextView = null;
            tempAddress = null;
            view = layout.getChildAt(i);
            if (view != null && view instanceof VipTextView) {
                vipTextView = (VipTextView) view;
                tempAddress = vipTextView.getAddress();
                if (singleAddress == null) {
                    boolean isVip = VipMemberCache.isVIP(tempAddress.getAddress());
                    if (tempAddress.mIsVipMember != isVip) {
                        if (isVip) {
                            tempAddress.setVip(true);
                        } else {
                            tempAddress.setVip(false);
                        }
                        vipTextView.setText(MessageViewUtils.getSpannableString(context, tempAddress, false));
                    }
                } else {
                    String emailAddress = singleAddress.getAddress();
                    if (emailAddress != null && emailAddress.equals(tempAddress.getAddress())) {
                        tempAddress.setVip(singleAddress.mIsVipMember);
                        vipTextView.setText(MessageViewUtils.getSpannableString(context, tempAddress, false));
                    }
                }
            }
        }
    }

    /**
     * Update the UI state of {@link VipTextView} inside MessageView
     * mDetailsLayout. If @param singleAddress is not null, update only the
     * VipTextView which email address is same with @param singleAddress.
     */
    public static void updateDetailsExpanded(Context context, View root, TempAddress singleAddress) {
        if (root.getVisibility() != View.VISIBLE) {
            return;
        }
        LinearLayout toLayout = (LinearLayout) root.findViewById(R.id.to_layout);
        LinearLayout ccLayout = (LinearLayout) root.findViewById(R.id.cc_layout);
        LinearLayout bccLayout = (LinearLayout) root.findViewById(R.id.bcc_layout);
        MessageViewUtils.updateVipTextView(context, toLayout, singleAddress);
        MessageViewUtils.updateVipTextView(context, ccLayout, singleAddress);
        MessageViewUtils.updateVipTextView(context, bccLayout, singleAddress);
    }

    /**
     * Update the vip information inside MessageView mDetailsLayout and
     * mAddressesView. If the parameter {@link TempAddress} is not null, only
     * update mDetailsLayout, because mDetailsLayout may contain this
     * {@link TempAddress}.
     *
     * @param context
     * @param message The corresponding {@link Message}
     * @param detailsLayout The layout mDetailsLayout inside {@link MessageViewFragmentBase}.
     * @param addressView The TextView mAddressesView inside {@link MessageViewFragmentBase}.
     * @param singleAddress The TempAddress which vip state is changed.
     */
    public static void updateVipInformation(Context context, Message message, View detailsLayout,
            TextView addressView, TempAddress singleAddress) {
        if (singleAddress == null) {
            updateAddressesView(message, context, addressView);
            updateDetailsExpanded(context, detailsLayout, null);
        } else {
            updateDetailsExpanded(context, detailsLayout, singleAddress);
        }
    }

    /**
     * Set the visibility of table row.
     */
    public static void setRowVisibility(String addresses, View table, int rowId) {
        if (TextUtils.isEmpty(addresses)) {
            table.findViewById(rowId).setVisibility(View.GONE);
        } else {
            table.findViewById(rowId).setVisibility(View.VISIBLE);
        }
    }

    public static class TempAddress extends Address {
        public static final int FLAG_TO_LAYOUT = 1;
        public static final int FLAG_CC_LAYOUT = 2;
        public static final int FLAG_BCC_LAYOUT = 3;
        public int mParentLayoutFlag;
        public boolean mIsVipMember;

        public TempAddress(String address, String personal, int parentLayoutFlag) {
            super(address, personal);
            mParentLayoutFlag = parentLayoutFlag;
        }

        public void setVip(boolean isVip) {
            mIsVipMember = isVip;
        }

        public static TempAddress[] unpack(String addressList, int parentLayoutFlag) {
            Address[] addresses = Address.unpack(addressList);
            if (addresses.length == 0) {
                return new TempAddress[0];
            }
            TempAddress[] vipAddresses = new TempAddress[addresses.length];
            String address = null;
            String personal = null;
            for (int i = 0; i < addresses.length; i++) {
                address = addresses[i].getAddress();
                personal = addresses[i].getPersonal();
                if (TextUtils.isEmpty(personal)) {
                    personal = address;
                }
                vipAddresses[i] = new TempAddress(address, personal, parentLayoutFlag);
            }
            return vipAddresses;
        }
    }

    public static class VipTextView extends TextView {
        public TempAddress mAddress;

        public VipTextView(Context context) {
            super(context, null);
        }

        public VipTextView(Context context, TempAddress tempAddress) {
            super(context, null);
            mAddress = tempAddress;
        }

        public VipTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setPadding(int padding) {
            this.setPadding(padding, padding, padding, padding);
        }

        public void setAdress(TempAddress tempAddress) {
            mAddress = tempAddress;
        }

        public TempAddress getAddress() {
            return mAddress;
        }
    }

    public static class VipOnClickListener implements View.OnClickListener {
        private final int mVipViewPadding;
        private final int mPopupHeight;
        private final int mBlack;
        private final int mWhite;
        private final Drawable mAddVipIcon;
        private final Drawable mRemoveVipIcon;
        public MessageViewFragmentBase mFragment;
        public Context mContext;
        public PopupWindow mPopup;

        public int mContactStatusState;
        public Uri mQuickContactLookupUri;
        public ImageView mBadge;
        public TempAddress mTempAddress;
        private boolean mClicked;

        private Runnable mAddVipRunnable = new Runnable() {
            @Override
            public void run() {
                AddVipsCallback callback = new AddVipsCallback() {
                    @Override
                    public void tryToAddDuplicateVip() {
                        Utility.showToast(mContext, R.string.not_add_duplicate_vip);
                    }
                    @Override
                    public void addVipOverMax() {
                        Utility.showToast(mContext, R.string.can_not_add_vip_over_99);
                    }
                };
                VipMember.addVIP(mContext, getTempAddress(), callback);
                VipMemberCache.updateVipMemberCache();
            }
        };

        private Runnable mRemoveVipRunnable = new Runnable() {
            @Override
            public void run() {
                VipMember.deleteVipMembers(mContext, Account.ACCOUNT_ID_COMBINED_VIEW,
                        getTempAddress().getAddress());
                VipMemberCache.updateVipMemberCache();
            };
        };

        private View.OnClickListener mVipButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getTempAddress().mIsVipMember) {
                    EmailAsyncTask.runAsyncParallel(mRemoveVipRunnable);
                    getTempAddress().mIsVipMember = false;
                } else {
                    int vipCount = VipMemberCache.getVipMembersCount();
                    if (vipCount + 1 > VipMember.VIP_MAX_COUNT) {
                        Utility.showToast(mContext, R.string.can_not_add_vip_over_99);
                    } else {
                        EmailAsyncTask.runAsyncParallel(mAddVipRunnable);
                        getTempAddress().mIsVipMember = true;
                    }
                }
                if (getPopupWindow() != null) {
                    getPopupWindow().dismiss();
                }
                if (mFragment.needUpdateVipIcon(getTempAddress())) {
                    mFragment.updateSenderVipIcon(getTempAddress().mIsVipMember);
                }
                mFragment.updateVipInformation(getTempAddress());
            }
        };

        private View.OnClickListener mSenderClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageCompose.actionCompose(mContext,
                        "mailto:" + Uri.encode(mTempAddress.getAddress()), mFragment.getAccountId());
                mPopup.dismiss();
            }
        };

        public VipOnClickListener(MessageViewFragmentBase fragment, PopupWindow popupWindow) {
            mFragment = fragment;
            mContext = fragment.getActivity();
            mPopup = popupWindow;

            Resources resources = mContext.getResources();
            mVipViewPadding = resources.getDimensionPixelSize(R.dimen.vip_text_view_padding);
            mPopupHeight = resources.getDimensionPixelSize(R.dimen.vip_pop_up_height);
            mWhite = resources.getColor(android.R.color.white);
            mBlack = resources.getColor(android.R.color.black);
            mAddVipIcon = resources.getDrawable(R.drawable.ic_email_add_vip_holo_light);
            mRemoveVipIcon = resources.getDrawable(R.drawable.ic_email_remove_vip_holo_light);
        }

        public PopupWindow getPopupWindow() {
            return mPopup;
        }

        public TempAddress getTempAddress() {
            return mTempAddress;
        }

        @Override
        public void onClick(View v) {
            if (mClicked) {
                return;
            }
            mClicked = true;

            final VipTextView vipView = (VipTextView) v;
            mTempAddress = vipView.getAddress();
            if (mTempAddress == null) {
                mClicked = false;
                return;
            }

            ColorStateList list = vipView.getTextColors();
            if (list.getDefaultColor() == mWhite) {
                setVipViewState(mContext, vipView, mBlack, false, mVipViewPadding);
            } else {
                setVipViewState(mContext, vipView, mWhite, true, mVipViewPadding);
            }

            if (mPopup != null) {
                View contentView = mPopup.getContentView();
                View sendercontainer = (View)contentView.findViewById(R.id.vip_sender_container);
                sendercontainer.setOnClickListener(mSenderClickListener);
                mBadge = (ImageView) contentView.findViewById(R.id.vip_badge);
                TextView personalView = (TextView) contentView.findViewById(R.id.vip_from_name);
                personalView.setText(mTempAddress.getPersonal());

                final EllipsizeTextView addressView = (EllipsizeTextView) contentView.findViewById(R.id.vip_from_address);
                addressView.setOnDrawnListener(new EllipsizeTextView.OnDrawnListener() {
                    @Override
                    public void onDrawn() {
                        if (mTempAddress == null) {
                            return;
                        }
                        String emailAddress = mTempAddress.getAddress();
                        if (TextUtils.isEmpty(emailAddress)) {
                            addressView.setText("");
                            return;
                        }
                        TextPaint paint = addressView.getPaint();
                        int textWidth = (int) paint.measureText(emailAddress);
                        int viewWidth = addressView.getWidth();
                        if (textWidth > viewWidth) {
                            String[] parts = emailAddress.split("@");
                            int domainWidth = (int) paint.measureText("@" + parts[1]);
                            CharSequence ellipsizedUserName = TextUtils.ellipsize(parts[0], paint,
                                    viewWidth - domainWidth, TruncateAt.END);
                            emailAddress = ellipsizedUserName + "@" + parts[1];
                        }
                        addressView.setText(emailAddress);
                        addressView.removeOnDrawnListener();
                    }
                });

                ImageButton imageButton = (ImageButton)contentView.findViewById(R.id.vip_icon);
                imageButton.setImageDrawable(mTempAddress.mIsVipMember ?
                        mRemoveVipIcon : mAddVipIcon);
                imageButton.setOnClickListener(mVipButtonClickListener);

                mPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        mClicked = false;
                        setVipViewState(mContext, vipView, mBlack, false, mVipViewPadding);
                    }
                });

                // If this view's position is in the first 2/3 of phone screen,
                // show the pop up window below this view.
                // else, show the pop up above this view.
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                final Rect displayFrame = new Rect();
                v.getWindowVisibleDisplayFrame(displayFrame);
                if (location[1] <= displayFrame.bottom * 2 / 3) {
                    if(mIsTwoPaneUi) {
                    	mPopup.setWidth(mWindowWidth - location[0]);
                    	mPopup.showAtLocation(v, Gravity.NO_GRAVITY, location[0], location[1] + v.getHeight() + 2);
                    } else {
                    	mPopup.showAtLocation(v, Gravity.NO_GRAVITY, 0, location[1] + v.getHeight() + 2);
                    }
                } else {
                    if(mIsTwoPaneUi) {
                    	mPopup.setWidth(mWindowWidth - location[0]);
                    	mPopup.showAtLocation(v, Gravity.NO_GRAVITY, location[0], location[1] + v.getHeight() + 2);
                    } else {
                    	mPopup.showAtLocation(v, Gravity.NO_GRAVITY, 0, location[1] - mPopupHeight - 2);
                    }
                }

                if (mTempAddress != null) {
                    LoaderManager loaderManager = mFragment.getLoaderManager();
                    Loader loader = loaderManager.getLoader(MessageViewFragmentBase.VIP_VIEW_PHOTO_LOADER_ID);
                    if (loader != null) {
                        loader.cancelLoad();
                    }
                    loaderManager.restartLoader(MessageViewFragmentBase.VIP_VIEW_PHOTO_LOADER_ID,
                            VipViewPhotoLoaderCallbacks.createArguments(vipView.getAddress()
                                    .getAddress()), new VipViewPhotoLoaderCallbacks(this));
                }

                initContactStatusViews();

                mBadge.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getPopupWindow() != null) {
                            getPopupWindow().dismiss();
                        }
                        onClickBadge();
                    }
                });
            } else {
                mClicked = false;
            }
        }

        private void setVipViewState(Context context, VipTextView vipView, int color,
                boolean selected, int padding) {
            vipView.setTextColor(color);
            vipView.getPaint().setFakeBoldText(selected);
            vipView.setText(getSpannableString(context, vipView.getAddress(), selected));
            vipView.setBackgroundResource(selected ? R.drawable.chip_background_selected
                    : R.drawable.chip_background);
            vipView.setPadding(padding);
        }

        private void initContactStatusViews() {
            mContactStatusState = CONTACT_STATUS_STATE_UNLOADED;
            mQuickContactLookupUri = null;
            showDefaultQuickContactBadgeImage();
        }

        private void showDefaultQuickContactBadgeImage() {
            if (mBadge != null) {
                mBadge.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        public void onClickBadge() {
            if (mTempAddress == null) {
                return;
            }

            if (mContactStatusState == CONTACT_STATUS_STATE_UNLOADED) {
                // Status not loaded yet.
                mContactStatusState = CONTACT_STATUS_STATE_UNLOADED_TRIGGERED;
                return;
            }
            if (mContactStatusState == CONTACT_STATUS_STATE_UNLOADED_TRIGGERED) {
                return; // Already clicked, and waiting for the data.
            }

            /// M:Safely start Contacts, toast if catch ActivityNotFoundException. @{
            UiUtilities.showContacts(mContext, mBadge,
                    mQuickContactLookupUri, mTempAddress);
            /// @}
        }
    }

    public static class VipViewPhotoLoaderCallbacks implements
            LoaderCallbacks<ContactStatusLoader.Result> {
        private static final String BUNDLE_EMAIL_ADDRESS = "email";
        private final VipOnClickListener mVipViewOnClickListener;

        public VipViewPhotoLoaderCallbacks(VipOnClickListener vipOnClickListener) {
            mVipViewOnClickListener = vipOnClickListener;
        }

        public static Bundle createArguments(String emailAddress) {
            Bundle b = new Bundle();
            b.putString(BUNDLE_EMAIL_ADDRESS, emailAddress);
            return b;
        }

        @Override
        public Loader<ContactStatusLoader.Result> onCreateLoader(int id, Bundle args) {
            return new ContactStatusLoader(mVipViewOnClickListener.mContext, args.getString(BUNDLE_EMAIL_ADDRESS));
        }

        @Override
        public void onLoadFinished(Loader<ContactStatusLoader.Result> loader,
                ContactStatusLoader.Result result) {
            boolean triggered =
                    (mVipViewOnClickListener.mContactStatusState == CONTACT_STATUS_STATE_UNLOADED_TRIGGERED);
            mVipViewOnClickListener.mContactStatusState = CONTACT_STATUS_STATE_LOADED;
            mVipViewOnClickListener.mQuickContactLookupUri = result.mLookupUri;

            if (result.mPhoto != null && mVipViewOnClickListener.mBadge != null) {
                // photo will be null if unknown.
                mVipViewOnClickListener.mBadge.setImageBitmap(result.mPhoto);
            }

            if (triggered) {
                mVipViewOnClickListener.onClickBadge();
            }
        }

        @Override
        public void onLoaderReset(Loader<ContactStatusLoader.Result> loader) {
        }
    }
}
