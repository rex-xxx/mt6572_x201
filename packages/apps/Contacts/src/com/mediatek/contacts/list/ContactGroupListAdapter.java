package com.mediatek.contacts.list;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.android.contacts.R;

import com.android.contacts.group.GroupBrowseListAdapter;
import com.android.contacts.group.GroupListItem;

public class ContactGroupListAdapter extends GroupBrowseListAdapter {

    // save checkstates for check boxes.
    private SparseBooleanArray mSparseBooleanArray;
    private static final String TAG = "ContactGroupListAdapter";

    public ContactGroupListAdapter(Context context) {
        super(context);
    }

    @Override
    protected int getGroupListItemLayout() {
        return R.layout.group_browse_list_item_with_checkbox;
    }

    /**
     * Set check box status after switching screen.
     * 
     * @param sparsebooleanarray saving check box states
     */
    public void setSparseBooleanArray(SparseBooleanArray sparsebooleanarray) {
        mSparseBooleanArray = sparsebooleanarray;
    }

    @Override
    protected void setViewWithCheckBox(View view, int position) {
        if (null == mSparseBooleanArray) {
            Log.w(TAG, "mSparseBooleanArray is null!");
            return;
        }
        CheckBox checkbox = (CheckBox) view.findViewById(android.R.id.checkbox);
        checkbox.setChecked(mSparseBooleanArray.get(position));
    }

    /**
     * M: fixed CR ALPS00586993 @{
     */
    @Override
    public long getItemId(int position) {
        GroupListItem item = getItem(position);
        return item == null ? -1 : item.getGroupId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
    /** @} */
}
