/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.group;

import android.content.ContentUris;
import android.content.Context;
//The following lines are provided and maintained by Mediatek Inc.
import android.content.res.Resources;
//The previous lines are provided and maintained by Mediatek Inc.
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Groups;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.contacts.ContactsApplication;
import com.android.contacts.GroupListLoader;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.internal.util.Objects;

// The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountFilterUtil;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import java.util.List;
import com.mediatek.contacts.util.LogUtils;
// The previous lines are provided and maintained by Mediatek Inc.

/**
 * Adapter to populate the list of groups.
 */
public class GroupBrowseListAdapter extends BaseAdapter {

    private static final String TAG = GroupBrowseListAdapter.class.getSimpleName();
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final AccountTypeManager mAccountTypeManager;

    private Cursor mCursor;

    private boolean mSelectionVisible;
    private Uri mSelectedGroupUri;

    public GroupBrowseListAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mAccountTypeManager = AccountTypeManager.getInstance(mContext);
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;

        // If there's no selected group already and the cursor is valid, then by default, select the
        // first group
        if (mSelectedGroupUri == null && cursor != null && cursor.getCount() > 0) {
            GroupListItem firstItem = getItem(0);
            long groupId = (firstItem == null) ? null : firstItem.getGroupId();
            // The following lines are provided and maintained by Mediatek Inc.
            // mSelectedGroupUri = getGroupUriFromId(groupId);
            mSelectedGroupUri = getGroupUriFromIdAndAccountInfo(groupId,
                    firstItem.getAccountName(), firstItem.getAccountType());
            // The previous lines are provided and maintained by Mediatek Inc.
        }

        notifyDataSetChanged();
    }

    public int getSelectedGroupPosition() {
        if (mSelectedGroupUri == null || mCursor == null || mCursor.getCount() == 0) {
            return -1;
        }

        int index = 0;
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            long groupId = mCursor.getLong(GroupListLoader.GROUP_ID);
            // The following lines are provided and maintained by Mediatek Inc.
            //Uri uri = getGroupUriFromId(groupId);

            String accountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
            String accountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
            // uri = groupUriWithAccountInfo(uri, accountName, accountType);
            Uri uri = getGroupUriFromIdAndAccountInfo(groupId, accountName, accountType);
            // The previous lines are provided and maintained by Mediatek Inc.
            if (mSelectedGroupUri.equals(uri)) {
                  return index;
            }
            index++;
        }
        return -1;
    }

    public void setSelectionVisible(boolean flag) {
        mSelectionVisible = flag;
    }

    public void setSelectedGroup(Uri groupUri) {
        mSelectedGroupUri = groupUri;
    }

    private boolean isSelectedGroup(Uri groupUri) {
        return mSelectedGroupUri != null && mSelectedGroupUri.equals(groupUri);
    }

    public Uri getSelectedGroup() {
        return mSelectedGroupUri;
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public GroupListItem getItem(int position) {
        if (mCursor == null || mCursor.isClosed() || !mCursor.moveToPosition(position)) {
            LogUtils.e(TAG, "mCursor: " + mCursor + ", position: " + position);
            return null;
        }
        String accountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
        String accountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
        String dataSet = mCursor.getString(GroupListLoader.DATA_SET);
        long groupId = mCursor.getLong(GroupListLoader.GROUP_ID);
        String title = mCursor.getString(GroupListLoader.TITLE);
        int memberCount = mCursor.getInt(GroupListLoader.MEMBER_COUNT);

        // Figure out if this is the first group for this account name / account type pair by
        // checking the previous entry. This is to determine whether or not we need to display an
        // account header in this item.
        int previousIndex = position - 1;
        boolean isFirstGroupInAccount = true;
        if (previousIndex >= 0 && mCursor.moveToPosition(previousIndex)) {
            String previousGroupAccountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
            String previousGroupAccountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
            String previousGroupDataSet = mCursor.getString(GroupListLoader.DATA_SET);

            if (accountName.equals(previousGroupAccountName) &&
                    accountType.equals(previousGroupAccountType) &&
                    Objects.equal(dataSet, previousGroupDataSet)) {
                isFirstGroupInAccount = false;
            }
        }

        return new GroupListItem(accountName, accountType, dataSet, groupId, title,
                isFirstGroupInAccount, memberCount);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GroupListItem entry = getItem(position);
        View result;
        GroupListItemViewCache viewCache;
        if (convertView != null) {
            result = convertView;
            viewCache = (GroupListItemViewCache) result.getTag();
        } else {
            /*
             * New Feature by Mediatek Begin.
             *   Original Android's code: 
             *     result = mLayoutInflater.inflate(R.layout.group_browse_list_item, parent, false);
             *   CR ID: ALPS00302773
             *   Descriptions: Select contact group for Message.
             */
            result = mLayoutInflater.inflate(getGroupListItemLayout(), parent, false);
            /*
             * New Feature by Mediatek End.
             */
            viewCache = new GroupListItemViewCache(result);
            result.setTag(viewCache);
        }

        // Add a header if this is the first group in an account and hide the divider
        if (entry.isFirstGroupInAccount()) {
            bindHeaderView(entry, viewCache);
            viewCache.accountHeader.setVisibility(View.VISIBLE);
            viewCache.divider.setVisibility(View.GONE);
            if (position == 0) {
                // Have the list's top padding in the first header.
                //
                // This allows the ListView to show correct fading effect on top.
                // If we have topPadding in the ListView itself, an inappropriate padding is
                // inserted between fading items and the top edge.
                viewCache.accountHeaderExtraTopPadding.setVisibility(View.VISIBLE);
            } else {
                viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
            }
        } else {
            viewCache.accountHeader.setVisibility(View.GONE);
            viewCache.divider.setVisibility(View.VISIBLE);
            viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
        }

        // Bind the group data
        // The following lines are provided and maintained by Mediatek Inc.
        //Uri groupUri = getGroupUriFromId(entry.getGroupId());
        Uri groupUri = getGroupUriFromIdAndAccountInfo(entry.getGroupId(), entry.getAccountName(), entry.getAccountType());
        // The previous lines are provided and maintained by Mediatek Inc.
        String memberCountString = mContext.getResources().getQuantityString(
                R.plurals.group_list_num_contacts_in_group, entry.getMemberCount(),
                entry.getMemberCount());
        viewCache.setUri(groupUri);
        viewCache.groupTitle.setText(entry.getTitle());
        viewCache.groupMemberCount.setText(memberCountString);

        if (mSelectionVisible) {
            result.setActivated(isSelectedGroup(groupUri));
        }
        /** M:set check box status */
        setViewWithCheckBox(result, position);

        return result;
    }

    private void bindHeaderView(GroupListItem entry, GroupListItemViewCache viewCache) {
        AccountType accountType = mAccountTypeManager.getAccountType(
                entry.getAccountType(), entry.getDataSet());
        viewCache.accountType.setText(accountType.getDisplayLabel(mContext).toString());
        if (AccountWithDataSetEx.isLocalPhone(accountType.accountType)) {
            viewCache.accountName.setVisibility(View.GONE);
        } else {
            /*
             * Bug Fix by Mediatek Begin.
             *   Original Android's code:
             *     viewCache.accountName.setText(entry.getAccountName());
             *   CR ID: ALPS00117716
             *   Descriptions: 
             */
            viewCache.accountName.setVisibility(View.VISIBLE);
            String displayName = null;
            displayName = AccountFilterUtil.getAccountDisplayNameByAccount(
                    entry.getAccountType(), entry.getAccountName());
            if (null == displayName) {
                viewCache.accountName.setText(entry.getAccountName());
            } else {
                viewCache.accountName.setText(displayName);
            }
            /*
             * Bug Fix by Mediatek End.
             */
        }
    }

    private static Uri getGroupUriFromId(long groupId) {
        return ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);
    }

    // The following lines are provided and maintained by Mediatek Inc.
    private Uri getGroupUriFromIdAndAccountInfo(long groupId, String accountName, String accountType) {
        Uri retUri = ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);
        if (accountName != null && accountType != null) {
            retUri = groupUriWithAccountInfo(retUri, accountName, accountType);
        }
        return retUri;
    }
    // The previous lines are provided and maintained by Mediatek Inc.

    /**
     * Cache of the children views of a contact detail entry represented by a
     * {@link GroupListItem}
     */
    public static class GroupListItemViewCache {
        public final TextView accountType;
        public final TextView accountName;
        public final TextView groupTitle;
        public final TextView groupMemberCount;
        public final View accountHeader;
        public final View accountHeaderExtraTopPadding;
        public final View divider;
        private Uri mUri;

        public GroupListItemViewCache(View view) {
            accountType = (TextView) view.findViewById(R.id.account_type);
            
            // MTK_THEMEMANAGER_APP
            if (FeatureOption.MTK_THEMEMANAGER_APP) {
                Resources res = ContactsApplication.getInstance().getResources();
                int textColor = res.getThemeMainColor();
                if (textColor != 0) {
                    accountType.setTextColor(textColor);
                }
            }
            // MTK_THEMEMANAGER_APP
            
            accountName = (TextView) view.findViewById(R.id.account_name);
            groupTitle = (TextView) view.findViewById(R.id.label);
            groupMemberCount = (TextView) view.findViewById(R.id.count);
            accountHeader = view.findViewById(R.id.group_list_header);
            accountHeaderExtraTopPadding = view.findViewById(R.id.header_extra_top_padding);
            divider = view.findViewById(R.id.divider);
        }

        public void setUri(Uri uri) {
            mUri = uri;
        }

        public Uri getUri() {
            return mUri;
        }
    }

    // The following lines are provided and maintained by Mediatek Inc.
    private Uri groupUriWithAccountInfo(final Uri groupUri, String accountName, String accountType) {
        if (groupUri == null) {
            return groupUri;
        }

        Uri retUri = groupUri;

        AccountWithDataSet account = null;
        final List<AccountWithDataSet> accounts = AccountTypeManager.getInstance(mContext)
                .getGroupWritableAccounts();
        int i = 0;
        int slotId = -1;
        for (AccountWithDataSet ac : accounts) {
            if (ac.name.equals(accountName) && ac.type.equals(accountType)) {
                account = accounts.get(i);
                if (account instanceof AccountWithDataSetEx) {
                    slotId = ((AccountWithDataSetEx) account).getSlotId();
                }
            }
            i++;
        }
        retUri = groupUri.buildUpon().appendPath(String.valueOf(slotId)).appendPath(accountName)
                .appendPath(accountType).build();

        return retUri;
    }

    protected int getGroupListItemLayout() {
        return R.layout.group_browse_list_item;
    }

    /**
     * M: set checkbox's status when screen is landscape.
     * 
     * @param view
     *            view operation is on
     * @param position
     *            view's positon
     */
    protected void setViewWithCheckBox(View view, int position) {
        // do noting here, override in child class
    }

    // The previous lines are provided and maintained by Mediatek Inc.
}
