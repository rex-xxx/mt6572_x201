
package com.mediatek.contacts.list;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;

public class ContactGroupListActivity extends ContactsActivity {

    private static final String TAG = ContactGroupListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                            | ActionBar.DISPLAY_SHOW_HOME);
            actionBar.setTitle(R.string.contact_group_list_title);
        }
        setContentView(R.layout.contact_group_list_activity);

        final ContactGroupListFragment fragment = getFragment(R.id.list_fragment);
        Button btnOk = (Button) findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                fragment.onOkClick();
            }

        });
        Button btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }

        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
