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

import java.util.ArrayList;

import com.android.email.R;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.provider.VipMember;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.Utility;
import com.android.ex.chips.AccountSpecifier;
import com.android.ex.chips.BaseRecipientAdapter;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.util.Rfc822Tokenizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AutoCompleteTextView.Validator;
import android.widget.ListView;


/**
 * M: The Fragment was used for contain of all the VIP feature's UI components.
 *
 */
public class VipListFragment extends ListFragment {
    // UI Support
    private Activity mActivity;
    private ListView mListView;
    private VipListAdapter mListAdapter;
    private VipAddressTextView mSearchVipView;
    private AccountSpecifier mAddressAdapter;
    private View mSearchContent;
    private int mLastItemCount;
    /** Arbitrary number for use with the loader manager */
    private static final int VIP_LOADER_ID = 1;
    /** Argument name(s) */
    private static final String ARG_ACCOUNT_ID = "accountId";
    private Long mImmutableAccountId;
    private int mVipNumber;
    private String mNewVipAddress = null;

    private Callback mCallback;
    /*
     * M: defined to record the email address of a vip which clicked to save or
     * look in contacts list @{
     */
    private String mRecordEmailAddress = null;

    /* @} */

    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
        /**
         * Called when the vip numbers changed.
         */
        public void onVipMemberChanged(int vipNumber);
    }

    public static VipListFragment newInstance(Long accountID) {
        VipListFragment f = new VipListFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ARG_ACCOUNT_ID, accountID);
        f.setArguments(bundle);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mListAdapter = new VipListAdapter(mActivity);
        mListAdapter.setFragment(this);
        setListAdapter(mListAdapter);
        mImmutableAccountId = getArguments().getLong(ARG_ACCOUNT_ID);
        mCallback = (Callback)mActivity;

        ListPhotoManager.getInstance(mActivity).refreshCache();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView = getListView();
        mListView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
                    mListAdapter.setScrollingState(true);
                } else {
                    mListAdapter.setScrollingState(false);
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
            }
        });
        setListShown(false);
        startLoading();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListAdapter.stopLoadingAvatars();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.email_vip_fragment, container, false);
        mSearchVipView = (VipAddressTextView) view.findViewById(R.id.search_vip);
        mSearchVipView.setTargetFragment(this);
        mAddressAdapter = new RecipientAdapter(getActivity(),
                (VipAddressTextView) mSearchVipView);
        mSearchVipView.setAdapter((RecipientAdapter) mAddressAdapter);
        mSearchVipView.setTokenizer(new Rfc822Tokenizer());

        mSearchContent = view.findViewById(R.id.to_content);
        mSearchContent.setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        /* M: start a thread to update the username of an vip @{ */
        if (mRecordEmailAddress != null) {
            ListPhotoManager.getInstance(mActivity).refreshCache();
            EmailAsyncTask.runAsyncParallel(new Runnable() {

                @Override
                public void run() {
                    updateVipUsername(mActivity, mRecordEmailAddress);
                }
            });
        }
        /* @} */
    }

    /*
     * M: defined to record the email address of a vip which clicked to save or
     * look in contacts list @{
     */
    public void setEmailAddress(String address) {
        mRecordEmailAddress = address;
    }
    /* @} */
    /**
     * Starts the loader.
     *
     */
    private void startLoading() {
        final LoaderManager lm = getLoaderManager();
        // Update the Vip list action bar title.
        if (mCallback != null) {
            mCallback.onVipMemberChanged(mVipNumber);
        }
        lm.initLoader(VIP_LOADER_ID, null, new VipListLoaderCallbacks());
    }

    public void onAddVip(final Address[] addresses) {
        EmailAsyncTask.runAsyncParallel(new Runnable() {

            @Override
            public void run() {
                VipListFragment.this.saveAsVips(addresses);
            }

        });
    }

    private void saveAsVips(Address[] addresses) {
        ArrayList<Address> addressList = new ArrayList<Address>();
        for (Address addr : addresses) {
            addressList.add(addr);
        }
        VipMember.addVIPs(mActivity, mImmutableAccountId, addressList, new VipMember.AddVipsCallback() {
            @Override
            public void tryToAddDuplicateVip() {
                Utility.showToast(mActivity, R.string.not_add_duplicate_vip);
            }
            @Override
            public void addVipOverMax() {
                Utility.showToast(mActivity, R.string.can_not_add_vip_over_99);
            }
        });
        // Scroll to the new vip
        if (addressList.size() > 0) {
            mNewVipAddress = addressList.get(addressList.size() -1).getAddress();
            mActivity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  int position = mListAdapter.getPosition(mNewVipAddress);
                  if (position != -1) {
                      mListView.setSelection(position);
                      mNewVipAddress = null;
                  }
              }
          });
        }
    }

    private class VipListLoaderCallbacks implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return VipListAdapter.createMailboxesLoader(getActivity(), mImmutableAccountId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            // Always swap out the cursor so we don't hold a reference to a stale one.
            mListAdapter.swapCursor(data);
            setListShown(true);
            mSearchContent.setVisibility(View.VISIBLE);
            // Update the Vip list action bar title.
            if (mCallback != null) {
                mVipNumber = data.getCount();
                mCallback.onVipMemberChanged(mVipNumber);
            }
            if (mNewVipAddress != null) {
                int position = mListAdapter.getPosition(mNewVipAddress);
                if (position != -1) {
                    mListView.setSelection(position);
                }
                mNewVipAddress = null;
            }
            mLastItemCount = data.getCount();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mListAdapter.swapCursor(null);
        }
    }

    private class RecipientAdapter extends BaseRecipientAdapter {
        public RecipientAdapter(Context context, VipAddressTextView list) {
            super(context);
        }

        /**
         * Set the account when known. Causes the search to prioritize contacts from
         * that account.
         */
        @Override
        public void setAccount(android.accounts.Account account) {
            if (account != null) {
                // TODO: figure out how to infer the contacts account
                // type from the email account
                super.setAccount(new android.accounts.Account(account.name, "unknown"));
            }
        }

        @Override
        protected int getDefaultPhotoResource() {
            return R.drawable.ic_contact_picture;
        }

        @Override
        protected int getItemLayout() {
            return R.layout.chips_recipient_dropdown_item;
        }
    }

    /* M: add for updating the username of vip @{ */
    private void updateVipUsername(Context context, String EmailAddress) {
        String username = getUserNameFromContacts(EmailAddress);
        VipMember.updateVipDisplayName(context, mImmutableAccountId, EmailAddress, username);
        setEmailAddress(null);
        mListAdapter.updateAvatar(EmailAddress);
    }

    private String getUserNameFromContacts(String address) {
        StringBuilder selection = new StringBuilder();
        selection.append(CommonDataKinds.Email.ADDRESS);
        selection.append(" = ");
        selection.append("'");
        selection.append(address);
        selection.append("'");
        String username = null;
        Cursor c = null;
        try {
            c = getActivity().getContentResolver().query(CommonDataKinds.Email.CONTENT_URI,
                    new String[] { ContactsContract.Data.DISPLAY_NAME }, selection.toString(),
                    null, null);
            if (c == null) {
                return username;
            }
            if (c.moveToNext()) {
                username = c.getString(0);
            }
            return username;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    /* @ } */
}
