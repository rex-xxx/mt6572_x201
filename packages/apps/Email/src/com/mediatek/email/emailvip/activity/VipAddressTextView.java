package com.mediatek.email.emailvip.activity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.TextView;

import com.android.emailcommon.mail.Address;
import com.android.ex.chips.MTKRecipientEditTextView;

/**
 * M : This is a RecipientEditTextView which has a custom onItemClickListener.
 * add selected item into database directly.
 */
public class VipAddressTextView extends MTKRecipientEditTextView {
    private VipListFragment mListFragment = null;

    public VipAddressTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTargetFragment(VipListFragment listFragment) {
        mListFragment = listFragment;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onItemClick(parent, view, position, id);
        // Add the clicked item into database directly when user click the popup list item.
        Address[] addresses = getAddresses(this);
        if (addresses.length > 0 && mListFragment != null) {
            mListFragment.onAddVip(addresses);
            setText("");
        }
    }

    private static Address[] getAddresses(TextView view) {
        Address[] addresses = Address.parse(view.getText().toString().trim(), false);
        return addresses;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // if KEYCODE_ENTER, don't auto complete the address.
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            return false;
        }
        return super.onKeyUp(keyCode, event);
    }
}

