/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.mediatek.contacts.activities;

import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.contacts.R;

/**
 * Displays a list of call log entries.
 */
public class CallLogMultipleChoiceActivity extends CallLogMultipleDeleteActivity {
    private static final String TAG = "CallLogMultipleChoiceActivity";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.call_log_choice_multiple_actions, menu);
        return true;
    }
    

    @Override
    protected void onStop() {
        super.onStopForSubClass();
    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {

        case R.id.menu_add:
            Intent intent = new Intent();
            String ids = mFragment.getSelections();
            intent.putExtra("calllogids", ids);
            setResult(RESULT_OK, intent);
            finish();
            return true;

            // All the options menu items are handled by onMenu... methods.

            /** M: add functions corresponding to options menu items @{ */
        case R.id.menu_select_all:
            updateSelectedItemsView(mFragment.selectAllItems());
            return true;
        case R.id.menu_unselect_all:
            mFragment.unSelectAllItems();
            updateSelectedItemsView(0);
            return true;
            /** @} */

        default:
            return super.onMenuItemSelected(featureId, item);
        }
    }

    protected OnClickListener getClickListenerOfActionBarOKButton() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                String ids = mFragment.getSelections();
                intent.putExtra("calllogids", ids);
                setResult(RESULT_OK, intent);
                finish();
                return;
            }
        };
    }

    private void log(final String log) {
        Log.i(TAG, log);
    }
}
