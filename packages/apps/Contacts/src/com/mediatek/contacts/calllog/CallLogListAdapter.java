package com.mediatek.contacts.calllog;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import com.android.contacts.calllog.CallLogAdapter;
import com.android.contacts.calllog.CallLogFragment;
import com.android.contacts.calllog.ContactInfoHelper;
import com.android.contacts.calllog.IntentProvider;


/**
 * This Adapter is used for land item select
 */
public class CallLogListAdapter extends CallLogAdapter {
    private CallLogFragment mCallLogFragment;
    private int mSelectedPosition = -1;
    private Context mContext;

    public CallLogListAdapter(Context context, CallFetcher callFetcher,
            ContactInfoHelper contactInfoHelper, CallLogFragment callLogFragment) {
        super(context, callFetcher, contactInfoHelper);
        // TODO Auto-generated constructor stub
        mCallLogFragment = callLogFragment;
        mContext = context;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public void setSelectedPosition(int selectedPosition) {
        mSelectedPosition = selectedPosition;
    }

    @Override
    protected void bindView(View view, Cursor c, int count) {
        // TODO Auto-generated method stub
        super.bindView(view, c, count);

        CallLogListItemView itemView = (CallLogListItemView) view;

        // set the the tagid in itemview to distinguish between different items
        if (mCallLogFragment != null) {
            if ((mCallLogFragment).ISTABLET_LAND) {
                itemView.setTagId(((IntentProvider) itemView.getTag())
                        .getIntent(mContext).getIntExtra("TAGID", -1));
            }
        }

        if (itemView.getTagId() == mSelectedPosition) {
            itemSetSelect(itemView, null);
            if (mCallLogFragment != null) {
                // remark the old item in mCallLogFragment
                mCallLogFragment.setOldItemView(itemView);
            }
        } else {
            // set the item transparent
            itemView.getSelectImageView().setVisibility(View.GONE);
        }

    }

    /* set item background color */
    public void itemSetSelect(CallLogListItemView newItemView,
            CallLogListItemView oldItemView) {
        if (oldItemView != null) {
            if (oldItemView.getSelectImageView() != null) {
                oldItemView.getSelectImageView().setVisibility(View.GONE);
            }
        }

        if (mCallLogFragment.ISTABLET_LAND) {
            // item can set background Color only here
            newItemView.getSelectImageView().setVisibility(View.VISIBLE);
        }
    }

}
